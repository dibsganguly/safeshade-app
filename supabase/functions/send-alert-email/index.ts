import { createClient, type SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { composeEmail } from "../_shared/email/resend.ts";
import { sendEmail, type DeliveryResult } from "../_shared/email/resend.ts";
import { escapeHtml } from "../_shared/email/render.ts";

/**
 * send-alert-email
 *
 * Request:  { "alert_id": "<uuid>" }
 * Response: { "deliveries": [ { "recipient", "status", "error" } ] }
 *
 * ============================================================================
 * The request body is ONE FIELD, and that is the security model
 * ============================================================================
 *
 * It takes an alert id and nothing else. Not a recipient list, not a subject,
 * not a body. Every one of those is resolved here, server-side, under the
 * service role.
 *
 * If the caller could name the recipients, this function would be an open mail
 * relay wearing SafeShade's branding: anybody with the anon key could send
 * arbitrary text to arbitrary addresses, from a sender that says SafeShade, and
 * the first anyone would know is when it started being reported as phishing.
 * Resolving recipients from `emergency_contacts` and `circle_members` means the
 * only addresses this function can ever reach are ones somebody deliberately
 * added to a Circle.
 *
 * ============================================================================
 * The caller is checked BEFORE the service role is used
 * ============================================================================
 *
 * The service role bypasses row-level security completely. So the caller's own
 * JWT is used first, through a separate client, to read the alert: if RLS lets
 * them see it they are a member of its circle, and if it does not, this returns
 * 403 and stops. Without that step, `{"alert_id": "<any uuid>"}` from any signed
 * -in account would email a stranger's Circle about a stranger's fall.
 *
 * That is why the check is a *read through the user's client* rather than a
 * membership query under the service role: the policy is already written, it is
 * tested by being the thing the whole app relies on, and re-implementing it here
 * would be a second definition of "who may see this" that can drift.
 *
 * ============================================================================
 * Nothing is recorded as sent before Resend answers 2xx
 * ============================================================================
 *
 * Each recipient gets a `queued` row first, then the send, then an UPDATE with
 * the real outcome. `sent` only ever comes from a 2xx. `failed` carries Resend's
 * own message. `unknown` is the genuinely ambiguous case and is never rounded to
 * either neighbour. See `_shared/email/resend.ts`.
 *
 * Note on the sender: with no domain yet, `onboarding@resend.dev` can only
 * deliver to the Resend account owner's own address. Every other recipient comes
 * back 403 and is recorded as a `failed` delivery with Resend's words. That is
 * deliberate and visible, not swallowed.
 */

interface AlertRow {
  id: string;
  circle_id: string;
  wearer_id: string | null;
  kind: string | null;
  occurred_at: string | null;
  lat: number | null;
  lon: number | null;
  location_label: string | null;
  note: string | null;
}

const KIND_LABELS: Record<string, string> = {
  FALL: "A fall was detected",
  SOS: "SOS pressed on the device",
  PHONE_SOS: "SOS sent from the phone",
  MISSED_CHECKIN: "A check-in was missed",
  ZONE_EXIT: "Left a safe zone",
  JOURNEY_OVERDUE: "A journey is overdue",
};

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") {
    return json({ error: "POST only" }, 405);
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    return json({ error: "not signed in" }, 401);
  }

  let alertId: string;
  try {
    const body = await req.json() as { alert_id?: string };
    if (!body.alert_id) return json({ error: "alert_id is required" }, 400);
    alertId = body.alert_id;
  } catch {
    return json({ error: "body must be JSON" }, 400);
  }

  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

  // 1. The caller's own client. RLS applies. This is the authorisation check.
  const asUser = createClient(url, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });

  const { data: visible } = await asUser
    .from("alerts")
    .select("id")
    .eq("id", alertId)
    .is("deleted_at", null)
    .maybeSingle();

  if (!visible) {
    // Deliberately the same answer for "no such alert" and "not your alert".
    // Distinguishing them would let anybody enumerate which alert ids exist.
    return json({ error: "not found" }, 404);
  }

  // 2. Everything from here runs under the service role, on an alert the caller
  //    has already been proven to be entitled to.
  const admin = createClient(url, serviceKey);

  const { data: alert, error: alertError } = await admin
    .from("alerts")
    .select("id, circle_id, wearer_id, kind, occurred_at, lat, lon, location_label, note")
    .eq("id", alertId)
    .single<AlertRow>();

  if (alertError || !alert) {
    return json({ error: "alert could not be read" }, 500);
  }

  const recipients = await resolveRecipients(admin, alert);
  if (recipients.length === 0) {
    // Not an error, and not a silent success either. An empty list is a real
    // and important answer: nobody in this Circle has an email address, so
    // nobody was told. The app must be able to say that.
    return json({ deliveries: [] }, 200);
  }

  const wearerName = await lookupWearerName(admin, alert.wearer_id);
  const medicalRows = await buildMedicalRows(admin, alert);
  const headline = `${KIND_LABELS[alert.kind ?? ""] ?? "A SafeShade alert"} - ${wearerName}`;

  // 3. Queue a row per recipient before sending anything, so that a crash
  //    mid-loop leaves evidence that a send was attempted rather than nothing
  //    at all.
  const queued = recipients.map((r) => ({
    id: crypto.randomUUID(),
    circle_id: alert.circle_id,
    alert_id: alert.id,
    recipient: r,
    channel: "email",
    status: "queued",
  }));
  await admin.from("alert_deliveries").insert(queued);

  // 4. Send, sequentially. Not in parallel: Resend rate-limits, and a burst
  //    that trips the limit turns "three guardians notified" into "one
  //    notified, two 429s".
  const results: DeliveryResult[] = [];
  for (let i = 0; i < recipients.length; i++) {
    const to = recipients[i];
    const html = await composeEmail("alert", {
      title: headline,
      preheader: headline,
      accent: "#E5484D",
      headline,
      wearer_name: wearerName,
      when_label: formatWhen(alert.occurred_at),
      where_clause: formatWhere(alert),
      map_url: mapUrl(alert),
      medical_rows: medicalRows,
      // Who else was reached, as known so far. The first recipient's email
      // cannot list the outcomes of sends that have not happened yet, so it
      // lists the addresses and says the outcomes are still in flight rather
      // than claiming they succeeded.
      delivery_rows: deliveryRowsHtml(recipients, results, to),
      footer: "Sent by SafeShade because an alert was raised in your Circle.",
    });

    const result = await sendEmail({
      to,
      subject: headline,
      html,
      // Per recipient, never per alert. A key of just the alert id would make
      // Resend return the FIRST recipient's cached response for everybody else,
      // so contacts two and three would be recorded as notified and would
      // receive nothing.
      idempotencyKey: `${alert.id}:${to}`,
    });
    results.push(result);

    await admin
      .from("alert_deliveries")
      .update({
        status: result.status,
        error: result.error,
        provider_id: result.providerId,
        sent_at: result.status === "sent" ? new Date().toISOString() : null,
      })
      .eq("id", queued[i].id);
  }

  return json({
    deliveries: results.map((r) => ({
      recipient: r.recipient,
      status: r.status,
      error: r.error,
    })),
  }, 200);
});

/**
 * Everybody who should hear about this, resolved server-side.
 *
 * Two sources, deduplicated:
 *  - `emergency_contacts` with `notify_by_email` and an address.
 *  - `circle_members`, through their `profiles.email`.
 *
 * Order matters: emergency contacts first, by `priority`, because if a rate
 * limit or a quota cuts the loop short, the people who must be told are the
 * ones who were told.
 */
async function resolveRecipients(admin: SupabaseClient, alert: AlertRow): Promise<string[]> {
  const out: string[] = [];

  const { data: contacts } = await admin
    .from("emergency_contacts")
    .select("email, priority, notify_by_email")
    .eq("circle_id", alert.circle_id)
    .is("deleted_at", null)
    .order("priority", { ascending: true });

  for (const c of contacts ?? []) {
    if (c.notify_by_email !== false && typeof c.email === "string" && c.email.includes("@")) {
      out.push(c.email.trim().toLowerCase());
    }
  }

  const { data: members } = await admin
    .from("circle_members")
    .select("user_id")
    .eq("circle_id", alert.circle_id)
    .is("deleted_at", null);

  const memberIds = (members ?? []).map((m) => m.user_id).filter(Boolean);
  if (memberIds.length > 0) {
    const { data: profiles } = await admin
      .from("profiles")
      .select("email")
      .in("id", memberIds);
    for (const p of profiles ?? []) {
      if (typeof p.email === "string" && p.email.includes("@")) {
        out.push(p.email.trim().toLowerCase());
      }
    }
  }

  // Dedupe while preserving the priority order established above.
  return [...new Set(out)];
}

async function lookupWearerName(admin: SupabaseClient, wearerId: string | null): Promise<string> {
  if (!wearerId) return "the wearer";
  const { data } = await admin
    .from("wearers")
    .select("name")
    .eq("id", wearerId)
    .maybeSingle();
  const name = data?.name;
  return typeof name === "string" && name.trim() ? name.trim() : "the wearer";
}

/**
 * The Medical ID, as table rows.
 *
 * Included in the email rather than linked. A paramedic in a stairwell should
 * not have to sign in to anything, and a link into an app they do not have is
 * worse than no link at all.
 *
 * Every value is escaped: these fields are free text a guardian typed, and one
 * of them reaching a mail client as live HTML would be a real problem.
 */
async function buildMedicalRows(admin: SupabaseClient, alert: AlertRow): Promise<string> {
  const { data } = await admin
    .from("medical_ids")
    .select("blood_type, allergies, conditions, medications, age, organ_donor, notes")
    .eq("circle_id", alert.circle_id)
    .is("deleted_at", null)
    .limit(1)
    .maybeSingle();

  if (!data) {
    return `<div style="color:#6A7078;">No Medical ID has been filled in for this Circle.</div>`;
  }

  const fields: Array<[string, unknown]> = [
    ["Blood type", data.blood_type],
    ["Age", data.age],
    ["Allergies", data.allergies],
    ["Conditions", data.conditions],
    ["Medications", data.medications],
    ["Organ donor", data.organ_donor ? "Yes" : null],
    ["Notes", data.notes],
  ];

  const rows = fields
    .filter(([, v]) => v !== null && v !== undefined && String(v).trim() !== "" && String(v) !== "0")
    .map(([label, v]) =>
      `<div style="margin-bottom:4px;"><span style="color:#6A7078;">${escapeHtml(label)}:</span> <strong style="color:#22282E;">${escapeHtml(v)}</strong></div>`
    );

  return rows.length
    ? rows.join("")
    : `<div style="color:#6A7078;">The Medical ID for this Circle is empty.</div>`;
}

/**
 * Who else was reached, as HTML.
 *
 * Honest about what is not yet known. An address whose send has not happened
 * yet says "being notified", not "notified" - the recipient of the first email
 * genuinely does not know how the second one went.
 */
function deliveryRowsHtml(
  recipients: string[],
  done: DeliveryResult[],
  self: string,
): string {
  const byRecipient = new Map(done.map((d) => [d.recipient, d]));
  return recipients
    .map((r) => {
      const label = r === self ? `${escapeHtml(r)} (you)` : escapeHtml(r);
      const d = byRecipient.get(r);
      if (!d) {
        return `<div style="margin-bottom:3px;">${label} &mdash; <span style="color:#6A7078;">being notified</span></div>`;
      }
      const colour = d.status === "sent" ? "#2E7D6B" : d.status === "failed" ? "#E5484D" : "#8A6D1F";
      const word = d.status === "sent" ? "notified" : d.status === "failed" ? "not reached" : "outcome unknown";
      return `<div style="margin-bottom:3px;">${label} &mdash; <span style="color:${colour};">${word}</span></div>`;
    })
    .join("");
}

function formatWhen(occurredAt: string | null): string {
  if (!occurredAt) return "The time was not recorded.";
  const d = new Date(occurredAt);
  if (Number.isNaN(d.getTime())) return "The time was not recorded.";
  return d.toUTCString().replace("GMT", "UTC") + ".";
}

function formatWhere(alert: AlertRow): string {
  if (alert.location_label) return ` Near ${alert.location_label}.`;
  if (alert.lat !== null && alert.lon !== null) {
    return ` At ${alert.lat.toFixed(5)}, ${alert.lon.toFixed(5)}.`;
  }
  return " No location was recorded.";
}

/**
 * A maps link.
 *
 * `geo:` would open a native app but is unreliable in mail clients, and an
 * https link works everywhere including a desktop. With no coordinates it still
 * returns a searchable link rather than a dead button, because a dead button in
 * an emergency email is worse than a link that finds nothing.
 */
function mapUrl(alert: AlertRow): string {
  if (alert.lat !== null && alert.lon !== null) {
    return `https://www.google.com/maps/search/?api=1&query=${alert.lat},${alert.lon}`;
  }
  const q = encodeURIComponent(alert.location_label ?? "");
  return `https://www.google.com/maps/search/?api=1&query=${q}`;
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
