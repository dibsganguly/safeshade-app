import { createClient, type SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { composeEmail, sendEmail, subjectFor } from "../_shared/email/resend.ts";
import { addressesOptedOut, skipped, type RecipientOutcome } from "../_shared/email/prefs.ts";
import { weeklyReportTemplate } from "../_shared/email/templates/weekly-report.ts";
import { buildWeeklyReport, type AlertFact, type MessageFact, type ZoneFact } from "./report.ts";

/**
 * weekly-report
 *
 * Request:  { "circle_id": "<uuid>", "scheduled"?: true }
 * Response: { "circle_id", "period_start", "period_end", "has_data",
 *             "summary": {...},
 *             "deliveries": [ { "email", "status", "error", "reason"? } ] }
 *
 * ============================================================================
 * Two callers, two different proofs, one code path afterwards
 * ============================================================================
 *
 *  - **A signed-in guardian**, from the app's "send this week's report now"
 *    button. Their JWT says `role: authenticated`, and membership of the named
 *    circle is proven the way every other function proves it: a read through
 *    THEIR client, where RLS decides. Not a membership query under the service
 *    role — that would be a second definition of "who is in this Circle", and
 *    the copy that drifts is always the one nobody is looking at.
 *
 *  - **The Monday schedule**, from `public.dispatch_weekly_reports()` over
 *    pg_net. It presents the project's service_role key, whose JWT says
 *    `role: service_role`, and there is no user to check membership for. That
 *    claim is the authorisation: the key is held in Supabase Vault, read by a
 *    `security definer` function that no API role may execute, and it never
 *    appears in a migration or a file.
 *
 * The platform has already verified the signature before this code runs
 * (`verify_jwt` is on), so the claim is read, not trusted blind — a forged
 * `role` never reaches here, because a token with a forged claim never gets
 * past the gateway.
 *
 * ============================================================================
 * What it counts, and what it refuses to count
 * ============================================================================
 *
 * `alerts`, `messages` and `zone_events`, over seven days, for one circle. That
 * is what the schema holds. There is deliberately no "nights the wearable was
 * out of range": `device_sightings` is empty, nothing writes a connectivity
 * history, and a number for it would be invented. A metric in a weekly report
 * is read as a measurement.
 *
 * A week with nothing in it renders one sentence saying so — see report.ts.
 *
 * ============================================================================
 * There is no claim stamp on this one, on purpose
 * ============================================================================
 *
 * Unlike the alert and the joined notice, a repeat here is a person pressing a
 * button twice and expecting a second copy. Resend's Idempotency-Key is keyed
 * on the circle, the recipient and the WEEK, so the schedule cannot double-send
 * on a retry, while a guardian who asks again next week gets next week's.
 */

/**
 * No table on this schema carries a time zone — `profiles.locale` is a
 * language, and inferring a zone from a language is wrong for every country
 * with more than one. So the day buckets and the "07:00" in the cron schedule
 * are both IST, said plainly here and in supabase/README.md rather than
 * presented as per-user local time.
 */
const DEFAULT_TIME_ZONE = "Asia/Kolkata";
const DEFAULT_LOCALE = "en-IN";
const DAY_MS = 86_400_000;

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") return json({ error: "POST only" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return json({ error: "not signed in" }, 401);

  let circleId: string;
  try {
    const body = await req.json() as { circle_id?: string };
    if (!body.circle_id) return json({ error: "circle_id is required" }, 400);
    circleId = body.circle_id;
  } catch {
    return json({ error: "body must be JSON" }, 400);
  }

  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

  const isScheduled = jwtRole(authHeader) === "service_role";

  if (!isScheduled) {
    // The membership check, through the caller's own client. A non-member sees
    // no row and gets the same answer as somebody naming a circle that does not
    // exist, so this cannot be used to discover circle ids.
    const asUser = createClient(url, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data: visible } = await asUser
      .from("circles")
      .select("id")
      .eq("id", circleId)
      .is("deleted_at", null)
      .maybeSingle();
    if (!visible) return json({ error: "not found" }, 404);
  }

  const admin = createClient(url, serviceKey);

  // Seven day-buckets ending with today, in IST. Starting at a midnight rather
  // than at "now minus 168 hours" is what makes the day strip line up with the
  // days a person actually had.
  const now = new Date();
  const todayStart = startOfDayIn(now, DEFAULT_TIME_ZONE);
  const periodStart = new Date(todayStart.getTime() - 6 * DAY_MS);
  const periodEnd = new Date(todayStart.getTime() + DAY_MS);
  const previousStart = new Date(periodStart.getTime() - 7 * DAY_MS);

  const circle = await lookupCircle(admin, circleId);
  if (!circle) return json({ error: "not found" }, 404);

  const [alerts, previousAlerts, messages, previousMessages, zoneEvents, wearers] =
    await Promise.all([
      readAlerts(admin, circleId, periodStart, periodEnd),
      readAlerts(admin, circleId, previousStart, periodStart),
      readMessages(admin, circleId, periodStart, periodEnd),
      readMessages(admin, circleId, previousStart, periodStart),
      readZoneEvents(admin, circleId, periodStart, periodEnd),
      readWearers(admin, circleId),
    ]);

  const report = buildWeeklyReport({
    circleName: circle.name,
    periodStart: periodStart.toISOString(),
    periodEnd: periodEnd.toISOString(),
    locale: DEFAULT_LOCALE,
    timeZone: DEFAULT_TIME_ZONE,
    alerts,
    messages,
    zoneEvents,
    wearers,
    previousAlerts,
    previousMessageCount: previousMessages.length,
    // A circle younger than the previous window has no previous week, and "down
    // from nothing" is not a trend.
    hasPreviousWeek: circle.createdAt !== null &&
      new Date(circle.createdAt).getTime() <= previousStart.getTime(),
  });

  const recipients = await readMemberEmails(admin, circleId);
  if (recipients.length === 0) {
    return json({
      circle_id: circleId,
      period_start: periodStart.toISOString(),
      period_end: periodEnd.toISOString(),
      has_data: report.hasData,
      summary: report.summary,
      deliveries: [],
    }, 200);
  }

  const optedOut = await addressesOptedOut(admin, "weekly_report", recipients);
  const subject = subjectFor(weeklyReportTemplate, { circle_name: circle.name });
  // The week, as a stable string, so a retry of the same week is deduplicated
  // by Resend and next week is not.
  const weekKey = periodStart.toISOString().slice(0, 10);

  const html = await composeEmail(weeklyReportTemplate, {
    title: subject,
    preheader: report.hasData
      ? `${report.summary.alerts} alerts, ${report.summary.answered} answered, ${report.summary.messages} messages.`
      : "Nothing was recorded in this Circle in the last seven days.",
    // Teal. A report is a reassurance and must never look like an alert.
    accent: "#6FD3CC",
    ...report.vars,
    footer: `Sent by SafeShade because you are in ${circle.name}. You can turn this weekly summary off in SafeShade under Account.`,
  });

  const deliveries: RecipientOutcome[] = [];
  for (const to of recipients) {
    if (optedOut.has(to)) {
      deliveries.push(skipped(to));
      continue;
    }
    const result = await sendEmail({
      to,
      subject,
      html,
      idempotencyKey: `weekly:${circleId}:${weekKey}:${to}`,
    });
    deliveries.push({ email: to, status: result.status, error: result.error });
  }

  return json({
    circle_id: circleId,
    period_start: periodStart.toISOString(),
    period_end: periodEnd.toISOString(),
    has_data: report.hasData,
    summary: report.summary,
    deliveries,
  }, 200);
});

/**
 * The `role` claim, read from an already-verified JWT.
 *
 * Not a verification: the gateway has done that, and a second, hand-rolled
 * signature check here would be a second implementation of the thing that must
 * not be got wrong. This only reads what the verified token says it is.
 */
function jwtRole(authHeader: string): string | null {
  const token = authHeader.replace(/^Bearer\s+/i, "").trim();
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    const payload = JSON.parse(
      atob(parts[1].replace(/-/g, "+").replace(/_/g, "/")),
    ) as { role?: string };
    return payload.role ?? null;
  } catch {
    return null;
  }
}

/** Midnight of [at]'s calendar day in [timeZone], as an instant. */
function startOfDayIn(at: Date, timeZone: string): Date {
  // en-CA is YYYY-MM-DD. Formatting in the target zone and re-parsing is the
  // shortest correct way to get "the start of that day there" without a tz
  // library, and it is exact for whole-hour and half-hour offsets, which
  // includes IST.
  const ymd = new Intl.DateTimeFormat("en-CA", { timeZone }).format(at);
  const offsetMinutes = zoneOffsetMinutes(at, timeZone);
  return new Date(new Date(`${ymd}T00:00:00Z`).getTime() - offsetMinutes * 60_000);
}

function zoneOffsetMinutes(at: Date, timeZone: string): number {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone,
    hour12: false,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).formatToParts(at);
  const get = (t: string) => Number(parts.find((p) => p.type === t)?.value ?? "0");
  const asUtc = Date.UTC(
    get("year"),
    get("month") - 1,
    get("day"),
    get("hour") % 24,
    get("minute"),
    get("second"),
  );
  return Math.round((asUtc - at.getTime()) / 60_000);
}

async function lookupCircle(
  admin: SupabaseClient,
  circleId: string,
): Promise<{ name: string; createdAt: string | null } | null> {
  const { data } = await admin
    .from("circles")
    .select("name, created_at")
    .eq("id", circleId)
    .is("deleted_at", null)
    .maybeSingle();
  if (!data) return null;
  return {
    name: data.name?.trim() || "Your SafeShade Circle",
    createdAt: data.created_at ?? null,
  };
}

async function readAlerts(
  admin: SupabaseClient,
  circleId: string,
  from: Date,
  to: Date,
): Promise<AlertFact[]> {
  const { data } = await admin
    .from("alerts")
    .select("kind, outcome, occurred_at, acknowledged_at, wearer_id")
    .eq("circle_id", circleId)
    .is("deleted_at", null)
    .gte("occurred_at", from.toISOString())
    .lt("occurred_at", to.toISOString());
  return (data ?? []) as AlertFact[];
}

async function readMessages(
  admin: SupabaseClient,
  circleId: string,
  from: Date,
  to: Date,
): Promise<MessageFact[]> {
  const { data } = await admin
    .from("messages")
    .select("kind, sent_at")
    .eq("circle_id", circleId)
    .is("deleted_at", null)
    .gte("sent_at", from.toISOString())
    .lt("sent_at", to.toISOString());
  return (data ?? []) as MessageFact[];
}

async function readZoneEvents(
  admin: SupabaseClient,
  circleId: string,
  from: Date,
  to: Date,
): Promise<ZoneFact[]> {
  const { data } = await admin
    .from("zone_events")
    .select("kind, occurred_at")
    .eq("circle_id", circleId)
    .is("deleted_at", null)
    .gte("occurred_at", from.toISOString())
    .lt("occurred_at", to.toISOString());
  return (data ?? []) as ZoneFact[];
}

async function readWearers(admin: SupabaseClient, circleId: string) {
  const { data } = await admin
    .from("wearers")
    .select("id, name")
    .eq("circle_id", circleId)
    .is("deleted_at", null);
  return (data ?? []) as Array<{ id: string; name: string | null }>;
}

/** Every member's address. Resolved here, never taken from the caller. */
async function readMemberEmails(
  admin: SupabaseClient,
  circleId: string,
): Promise<string[]> {
  const { data: members } = await admin
    .from("circle_members")
    .select("user_id")
    .eq("circle_id", circleId)
    .is("deleted_at", null);
  const ids = (members ?? []).map((m) => m.user_id).filter(Boolean);
  if (ids.length === 0) return [];

  const { data: profiles } = await admin
    .from("profiles")
    .select("email")
    .in("id", ids);

  const out = new Set<string>();
  for (const p of profiles ?? []) {
    if (typeof p.email === "string" && p.email.includes("@")) {
      out.add(p.email.trim().toLowerCase());
    }
  }
  return [...out];
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
