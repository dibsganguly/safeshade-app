import { createClient, type SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { composeEmail, sendEmail, subjectFor, type DeliveryResult } from "../_shared/email/resend.ts";
import { escapeHtml } from "../_shared/email/render.ts";
import { addressesOptedOut, type RecipientOutcome } from "../_shared/email/prefs.ts";
import { alertTemplate } from "../_shared/email/templates/alert.ts";

/**
 * send-alert-email
 *
 * Request:  { "alert_id": "<uuid>" }
 * Response: { "alert_id", "already_notified": bool,
 *             "deliveries": [ { "email", "status", "error", "reason"? } ] }
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
 * added to a Circle. For the same reason the caller cannot suppress a
 * recipient either - see `_shared/email/prefs.ts`.
 *
 * ============================================================================
 * The caller is checked BEFORE the service role is used
 * ============================================================================
 *
 * The service role bypasses row-level security completely. So the caller's own
 * JWT is used first, through a separate client, to read the alert: if RLS lets
 * them see it they are a member of its circle, and if it does not, this returns
 * 404 and stops. Without that step, `{"alert_id": "<any uuid>"}` from any signed
 * -in account would email a stranger's Circle about a stranger's fall.
 *
 * That is why the check is a *read through the user's client* rather than a
 * membership query under the service role: the policy is already written, it is
 * tested by being the thing the whole app relies on, and re-implementing it here
 * would be a second definition of "who may see this" that can drift.
 *
 * ============================================================================
 * The alert is CLAIMED before anything is sent, and the claim is the dedupe
 * ============================================================================
 *
 * The app invokes this the moment an alert row is pushed, and the app retries.
 * So the first thing that happens after authorisation is
 *
 *     update alerts set notified_at = now() where id = $1 and notified_at is null
 *
 * and if that returns no row, another invocation already ran the send pass and
 * this one sends nothing. The dedupe is on the server because the phone must
 * not be the thing deciding how many emails a guardian gets - a phone that
 * loses three responses would otherwise send four copies of one fall alert.
 *
 * `notified_at` means THE PASS RAN. It does not mean anybody was reached; the
 * `alert_deliveries` rows are the record of that, and they are still written
 * from Resend's answer and from nothing else. A second invoke returns those
 * rows with `already_notified: true`, so a caller that retried still learns the
 * truth about who was reached rather than being told "done".
 *
 * ============================================================================
 * Nothing is recorded as sent before Resend answers 2xx
 * ============================================================================
 *
 * Each recipient gets a `queued` row first, then the send, then an UPDATE with
 * the real outcome. `sent` only ever comes from a 2xx. `failed` carries Resend's
 * own message. `unknown` is the genuinely ambiguous case and is never rounded to
 * either neighbour. `skipped` is the person who turned alert emails off - not a
 * failure, and not a delivery either. See `_shared/email/resend.ts`.
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

/**
 * The app's only registered deep link (see AndroidManifest.xml). It opens
 * SafeShade; nothing yet routes the `alert` parameter to the alert itself, and
 * the template says so rather than implying otherwise.
 */
const APP_URL = "safeshade://login-callback";

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

  const { data: userData } = await asUser.auth.getUser();
  const callerEmail = userData?.user?.email?.trim().toLowerCase() ?? null;

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

  // 3. Claim it. One statement, conditional on the stamp still being null, so
  //    two invocations racing each other produce exactly one send pass.
  const { data: claimed } = await admin
    .from("alerts")
    .update({ notified_at: new Date().toISOString() })
    .eq("id", alertId)
    .is("notified_at", null)
    .select("id")
    .maybeSingle();

  if (!claimed) {
    // Somebody already ran the pass. Report what it achieved rather than a
    // bare "ok": a retrying caller still needs to know who was reached.
    const { data: existing } = await admin
      .from("alert_deliveries")
      .select("recipient, status, error")
      .eq("alert_id", alertId);
    return json({
      alert_id: alertId,
      already_notified: true,
      deliveries: (existing ?? []).map((d) => ({
        email: d.recipient,
        status: d.status,
        error: d.error,
      })),
    }, 200);
  }

  const { data: alert, error: alertError } = await admin
    .from("alerts")
    .select("id, circle_id, wearer_id, kind, occurred_at, lat, lon, location_label, note")
    .eq("id", alertId)
    .single<AlertRow>();

  if (alertError || !alert) {
    return json({ error: "alert could not be read" }, 500);
  }

  const recipients = await resolveRecipients(admin, alert, callerEmail);
  if (recipients.length === 0) {
    // Not an error, and not a silent success either. An empty list is a real
    // and important answer: nobody else in this Circle has an email address, so
    // nobody was told. The app must be able to say that.
    return json({ alert_id: alert.id, already_notified: false, deliveries: [] }, 200);
  }

  // 4. Preferences, read server-side, once for the whole batch.
  const optedOut = await addressesOptedOut(admin, "alerts", recipients);
  const toSend = recipients.filter((r) => !optedOut.has(r));
  const toSkip = recipients.filter((r) => optedOut.has(r));

  const wearerName = await lookupWearerName(admin, alert.wearer_id);
  const medicalRows = await buildMedicalRows(admin, alert);
  const headline = `${KIND_LABELS[alert.kind ?? ""] ?? "A SafeShade alert"} - ${wearerName}`;
  const subject = subjectFor(alertTemplate, { headline });

  // 5. A row per recipient before anything is sent, so that a crash mid-loop
  //    leaves evidence that a send was attempted rather than nothing at all.
  //    The skipped ones are written in their final state immediately: they are
  //    not queued for anything.
  const queued = toSend.map((r) => ({
    id: crypto.randomUUID(),
    circle_id: alert.circle_id,
    alert_id: alert.id,
    recipient: r,
    channel: "email",
    status: "queued",
  }));
  if (queued.length > 0) await admin.from("alert_deliveries").insert(queued);
  if (toSkip.length > 0) {
    await admin.from("alert_deliveries").insert(toSkip.map((r) => ({
      id: crypto.randomUUID(),
      circle_id: alert.circle_id,
      alert_id: alert.id,
      recipient: r,
      channel: "email",
      status: "skipped",
      error: "this address has alert emails switched off",
    })));
  }

  // 6. Send, sequentially. Not in parallel: Resend rate-limits, and a burst
  //    that trips the limit turns "three guardians notified" into "one
  //    notified, two 429s".
  const results: DeliveryResult[] = [];
  for (let i = 0; i < toSend.length; i++) {
    const to = toSend[i];
    const html = await composeEmail(alertTemplate, {
      title: subject,
      preheader: headline,
      accent: "#E5484D",
      headline,
      wearer_name: wearerName,
      when_label: formatWhen(alert.occurred_at),
      where_label: formatWhere(alert),
      map_url: mapUrl(alert),
      app_url: APP_URL,
      medical_rows: medicalRows,
      // Who else was reached, as known so far. The first recipient's email
      // cannot list the outcomes of sends that have not happened yet, so it
      // lists the addresses and says the outcomes are still in flight rather
      // than claiming they succeeded.
      delivery_rows: deliveryRowsHtml(toSend, toSkip, results, to),
      footer: "Sent by SafeShade because an alert was raised in your Circle.",
    });

    const result = await sendEmail({
      to,
      subject,
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

  const deliveries: RecipientOutcome[] = [
    ...results.map((r) => ({
      email: r.recipient,
      status: r.status,
      error: r.error,
    })),
    ...toSkip.map((email) => ({
      email,
      status: "skipped" as const,
      error: null,
      reason: "preference",
    })),
  ];

  return json({ alert_id: alert.id, already_notified: false, deliveries }, 200);
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
 *
 * The caller is removed from the CIRCLE MEMBER half only. The phone that raised
 * this alert does not need an email telling it what it just sent, and a person
 * whose own inbox fills with their own alerts stops reading them. But an
 * emergency contact who happens to share that address was added by hand, on
 * purpose, as somebody to reach in an emergency, and that is not a duplicate to
 * be tidied away.
 */
async function resolveRecipients(
  admin: SupabaseClient,
  alert: AlertRow,
  callerEmail: string | null,
): Promise<string[]> {
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
        const email = p.email.trim().toLowerCase();
        if (email !== callerEmail) out.push(email);
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
 * genuinely does not know how the second one went. An address that was skipped
 * says so, and says why, because a guardian deciding whether to drive needs to
 * know the difference between "the email bounced" and "she has these switched
 * off".
 */
function deliveryRowsHtml(
  sending: string[],
  skippedList: string[],
  done: DeliveryResult[],
  self: string,
): string {
  const byRecipient = new Map(done.map((d) => [d.recipient, d]));
  const sendingRows = sending.map((r) => {
    const label = r === self ? `${escapeHtml(r)} (you)` : escapeHtml(r);
    const d = byRecipient.get(r);
    if (!d) {
      return `<div style="margin-bottom:3px;">${label} &mdash; <span style="color:#6A7078;">being notified</span></div>`;
    }
    const colour = d.status === "sent" ? "#2E7D6B" : d.status === "failed" ? "#E5484D" : "#8A6D1F";
    const word = d.status === "sent" ? "notified" : d.status === "failed" ? "not reached" : "outcome unknown";
    return `<div style="margin-bottom:3px;">${label} &mdash; <span style="color:${colour};">${word}</span></div>`;
  });
  const skippedRows = skippedList.map((r) =>
    `<div style="margin-bottom:3px;">${escapeHtml(r)} &mdash; <span style="color:#6A7078;">alert emails switched off</span></div>`
  );
  const all = [...sendingRows, ...skippedRows];
  return all.length
    ? all.join("")
    : `<div style="color:#6A7078;">Nobody else in this Circle has an email address.</div>`;
}

function formatWhen(occurredAt: string | null): string {
  if (!occurredAt) return "Not recorded";
  const d = new Date(occurredAt);
  if (Number.isNaN(d.getTime())) return "Not recorded";
  return d.toUTCString().replace("GMT", "UTC");
}

/**
 * Where, in words.
 *
 * `lat` and `lon` are null on every alert from a phone whose owner turned the
 * "share the place" switch off, and `location_label` may be null too. "Not
 * recorded" is the honest rendering of that and it is not the same sentence as
 * "at 0.00000, 0.00000".
 */
function formatWhere(alert: AlertRow): string {
  if (alert.location_label) return `Near ${alert.location_label}`;
  if (alert.lat !== null && alert.lon !== null) {
    return `${alert.lat.toFixed(5)}, ${alert.lon.toFixed(5)}`;
  }
  return "Not recorded";
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
