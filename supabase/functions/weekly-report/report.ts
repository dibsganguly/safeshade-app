import { escapeHtml, type Vars } from "../_shared/email/render.ts";

/**
 * Turning a week of rows into the numbers and the bars the email draws.
 *
 * ### Why this is a separate module with no Supabase import in it
 *
 * So it can be run. `index.ts` needs a project, a circle, a service role key
 * and a live Postgres to produce one email; this file needs a plain object. The
 * layout of an infographic is exactly the kind of thing that is wrong in a way
 * nobody notices until it is in an inbox — a bar 400px wide inside a 386px
 * track, a division by zero on a quiet week — and none of that should require a
 * fall alert to test.
 *
 * `tools/render_weekly_fixture.mjs` renders it with a fixture and writes an
 * HTML file you can open. That is how the full grid was checked; the live send
 * on this project was an empty week, and the report says so.
 *
 * ### Every width here is a pixel
 *
 * Outlook renders through Word, which ignores percentage widths on table cells
 * often enough that a percentage bar chart collapses or overflows. So the
 * geometry is computed here against {@link CONTENT_WIDTH} and emitted as
 * integers. If the layout ever changes width, this constant is the one place
 * that knows.
 *
 * ### Nothing here invents a metric
 *
 * The counts come from `alerts`, `messages` and `zone_events`, which are the
 * tables that actually hold rows. There is no "nights out of range", no
 * "battery health", no wellbeing score: `device_sightings` is empty and nothing
 * writes a connectivity history, so a number for it would be a number somebody
 * made up. A week with no data says so in one sentence rather than drawing a
 * grid of zeros, which is a picture of a working product rather than a picture
 * of the week.
 */

/** 600px frame minus the panel's 32px padding either side. */
export const CONTENT_WIDTH = 536;

/** The label column of a bar row, and the track that follows it. */
const LABEL_WIDTH = 150;
const TRACK_WIDTH = CONTENT_WIDTH - LABEL_WIDTH; // 386
/** Room kept at the end of the track for the count, right-aligned. */
const COUNT_WIDTH = 44;
const BAR_MAX = TRACK_WIDTH - COUNT_WIDTH; // 342

const CHARCOAL = "#22282E";
const TEAL = "#6FD3CC";
const AMBER = "#F5A623";
const TRIP_RED = "#E5484D";
const BONE = "#F2EFE9";
const QUIET = "#E6E1D8";

export const KIND_LABELS: Record<string, string> = {
  FALL: "Falls",
  SOS: "SOS on the device",
  PHONE_SOS: "SOS from the phone",
  MISSED_CHECKIN: "Missed check-ins",
  ZONE_EXIT: "Left a safe zone",
  JOURNEY_OVERDUE: "Overdue journeys",
};

/** Red is for the three kinds that mean somebody may be hurt. */
const EMERGENCY_KINDS = new Set(["FALL", "SOS", "PHONE_SOS"]);

function kindColour(kind: string | null): string {
  return EMERGENCY_KINDS.has(kind ?? "") ? TRIP_RED : AMBER;
}

/** One alert, reduced to what the report reads. */
export interface AlertFact {
  kind: string | null;
  outcome: string | null;
  occurred_at: string | null;
  acknowledged_at: string | null;
  wearer_id: string | null;
}

/** One safe-zone crossing. */
export interface ZoneFact {
  kind: string | null;
  occurred_at: string | null;
}

/** One message or voice note. */
export interface MessageFact {
  kind: string | null;
  sent_at: string | null;
}

export interface WearerFact {
  id: string;
  name: string | null;
}

/** Everything `index.ts` reads out of Postgres, and nothing else. */
export interface WeekInput {
  circleName: string;
  /** Inclusive start, exclusive end, both ISO. */
  periodStart: string;
  periodEnd: string;
  /** From `profiles.locale`, or a fallback. Used for date formatting only. */
  locale: string;
  timeZone: string;
  alerts: AlertFact[];
  messages: MessageFact[];
  zoneEvents: ZoneFact[];
  wearers: WearerFact[];
  /** The seven days before `periodStart`, for the week-over-week arrows. */
  previousAlerts: AlertFact[];
  previousMessageCount: number;
  /**
   * Whether the previous week is a real week or simply before this Circle
   * existed. False suppresses every comparison: "down from nothing" is not a
   * trend, it is an account that is one week old.
   */
  hasPreviousWeek: boolean;
}

export interface WeeklyReport {
  vars: Vars;
  /** True when there is anything at all to draw. */
  hasData: boolean;
  /** The same numbers the HTML shows, for logs and for the response. */
  summary: {
    alerts: number;
    answered: number;
    messages: number;
    voiceNotes: number;
    zoneEvents: number;
    coveredDays: number;
  };
}

/** An alert with any outcome other than PENDING has been dealt with by somebody. */
function isAnswered(a: AlertFact): boolean {
  const o = (a.outcome ?? "PENDING").toUpperCase();
  return o !== "PENDING";
}

function dayKey(iso: string | null, timeZone: string): string | null {
  if (!iso) return null;
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return null;
  // en-CA gives YYYY-MM-DD, which sorts and compares as a string.
  return new Intl.DateTimeFormat("en-CA", { timeZone }).format(d);
}

export function buildWeeklyReport(input: WeekInput): WeeklyReport {
  const tz = input.timeZone;
  const alerts = input.alerts;
  const answered = alerts.filter(isAnswered).length;
  const voiceNotes = input.messages.filter((m) => m.kind === "voice").length;
  const messages = input.messages.length;
  const zoneEvents = input.zoneEvents.length;

  // The seven day buckets of the window, in order.
  const days: string[] = [];
  const start = new Date(input.periodStart);
  for (let i = 0; i < 7; i++) {
    const d = new Date(start.getTime() + i * 86_400_000);
    days.push(new Intl.DateTimeFormat("en-CA", { timeZone: tz }).format(d));
  }

  const perDay = new Map<string, { emergency: number; other: number; chatter: number }>();
  for (const key of days) perDay.set(key, { emergency: 0, other: 0, chatter: 0 });
  const bump = (key: string | null, field: "emergency" | "other" | "chatter") => {
    if (!key) return;
    const cell = perDay.get(key);
    if (cell) cell[field] += 1;
  };
  for (const a of alerts) {
    bump(dayKey(a.occurred_at, tz), EMERGENCY_KINDS.has(a.kind ?? "") ? "emergency" : "other");
  }
  for (const z of input.zoneEvents) bump(dayKey(z.occurred_at, tz), "other");
  for (const m of input.messages) bump(dayKey(m.sent_at, tz), "chatter");

  const coveredDays = [...perDay.values()]
    .filter((c) => c.emergency + c.other + c.chatter > 0).length;

  const hasData = alerts.length + messages + zoneEvents > 0;

  const summary = {
    alerts: alerts.length,
    answered,
    messages,
    voiceNotes,
    zoneEvents,
    coveredDays,
  };

  if (!hasData) {
    return {
      hasData: false,
      summary,
      vars: {
        circle_name: input.circleName,
        period_label: periodLabel(input),
        has_data: false,
        quiet_caveat: quietCaveat(input),
      },
    };
  }

  return {
    hasData: true,
    summary,
    vars: {
      circle_name: input.circleName,
      period_label: periodLabel(input),
      has_data: true,
      alert_count: alerts.length,
      answered_count: answered,
      // Green only when everything raised was dealt with. An "answered" number
      // that is green at 2 out of 5 would be reassuring about the wrong thing.
      answered_colour: alerts.length > 0 && answered === alerts.length
        ? "#2E7D6B"
        : answered < alerts.length
        ? AMBER
        : CHARCOAL,
      message_count: messages,
      covered_days: coveredDays,
      comparison_rows: comparisonRows(input, alerts.length, messages),
      kind_bars: kindBars(alerts),
      day_cells: dayCells(days, perDay, input.locale, tz),
      day_legend: dayLegend(alerts, input),
      wearer_panels: wearerPanels(input, alerts),
    },
  };
}

function periodLabel(input: WeekInput): string {
  const fmt = new Intl.DateTimeFormat(input.locale, {
    timeZone: input.timeZone,
    day: "numeric",
    month: "short",
    year: "numeric",
  });
  const start = new Date(input.periodStart);
  // The window is exclusive at the end; the label is not, or it would name a
  // day the report says nothing about.
  const lastDay = new Date(new Date(input.periodEnd).getTime() - 1);
  return `${fmt.format(start)} to ${fmt.format(lastDay)}`;
}

function quietCaveat(input: WeekInput): string {
  return input.wearers.length > 0
    ? "That is usually what a good week looks like."
    : "There is no wearer set up in this Circle yet, which is the more likely reason.";
}

/**
 * Week-over-week, as text arrows.
 *
 * Omitted entirely when there is no previous week, rather than drawn as a flat
 * line: "no change" and "nothing to compare with" are different statements and
 * only one of them is true for a Circle that is eight days old.
 */
function comparisonRows(input: WeekInput, alerts: number, messages: number): string {
  if (!input.hasPreviousWeek) return "";
  const rows = [
    arrowRow("Alerts", alerts, input.previousAlerts.length),
    arrowRow("Messages", messages, input.previousMessageCount),
  ];
  return rows.join("");
}

function arrowRow(label: string, now: number, before: number): string {
  const delta = now - before;
  // Up is not good and down is not bad — more alerts is worse, more messages is
  // usually better — so the arrows are charcoal and the words carry the meaning.
  const glyph = delta > 0 ? "&#9650;" : delta < 0 ? "&#9660;" : "&mdash;";
  const words = delta === 0
    ? `the same as the week before (${before})`
    : `${Math.abs(delta)} ${delta > 0 ? "more" : "fewer"} than the week before (${before})`;
  return `<div style="margin-bottom:3px;"><span style="color:${CHARCOAL};">${glyph}</span> ` +
    `<strong style="color:${CHARCOAL};">${escapeHtml(label)}:</strong> ${escapeHtml(words)}</div>`;
}

/** One `<tr>` per kind that actually occurred. A kind with no rows is not a bar of zero. */
function kindBars(alerts: AlertFact[]): string {
  const counts = new Map<string, number>();
  for (const a of alerts) {
    const k = a.kind ?? "OTHER";
    counts.set(k, (counts.get(k) ?? 0) + 1);
  }
  if (counts.size === 0) return "";
  const max = Math.max(...counts.values());
  const ordered = [...counts.entries()].sort((a, b) => b[1] - a[1]);

  return ordered.map(([kind, count]) => {
    // At least 6px, so a single event is still a visible mark rather than a
    // label with nothing beside it.
    const bar = Math.max(6, Math.round((count / max) * BAR_MAX));
    const rest = Math.max(0, BAR_MAX - bar);
    const colour = kindColour(kind);
    const label = KIND_LABELS[kind] ?? kind;
    return `<tr>
    <td width="${LABEL_WIDTH}" style="width:${LABEL_WIDTH}px; padding:5px 8px 5px 0; font-size:13px; line-height:18px; color:#4A5058; vertical-align:middle;">${escapeHtml(label)}</td>
    <td width="${TRACK_WIDTH}" style="width:${TRACK_WIDTH}px; padding:5px 0; vertical-align:middle;">
      <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="${TRACK_WIDTH}" style="width:${TRACK_WIDTH}px;">
        <tr>
          <td width="${bar}" style="width:${bar}px; height:14px; line-height:14px; font-size:0; background:${colour}; border-radius:7px;">&nbsp;</td>
          <td width="${rest}" style="width:${rest}px; height:14px; line-height:14px; font-size:0;">&nbsp;</td>
          <td width="${COUNT_WIDTH}" align="right" style="width:${COUNT_WIDTH}px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:13px; font-weight:700; color:${CHARCOAL};">${count}</td>
        </tr>
      </table>
    </td>
  </tr>`;
  }).join("\n");
}

/**
 * Seven cells, one per day, coloured by the most serious thing that happened.
 *
 * Red beats amber beats teal beats nothing, and the ordering is the point: a
 * day with one fall and forty messages is a red day.
 */
function dayCells(
  days: string[],
  perDay: Map<string, { emergency: number; other: number; chatter: number }>,
  locale: string,
  timeZone: string,
): string {
  const width = Math.floor(CONTENT_WIDTH / 7); // 76
  const initial = new Intl.DateTimeFormat(locale, { timeZone, weekday: "short" });
  return days.map((key) => {
    const cell = perDay.get(key) ?? { emergency: 0, other: 0, chatter: 0 };
    const total = cell.emergency + cell.other + cell.chatter;
    const colour = cell.emergency > 0
      ? TRIP_RED
      : cell.other > 0
      ? AMBER
      : cell.chatter > 0
      ? TEAL
      : QUIET;
    // A day cell has to be readable when the fill is red and when it is bone.
    // Charcoal ink on all four is the only combination that measures on every
    // one of them, which is DESIGN.md's rule for a hued fill.
    const label = initial.format(new Date(`${key}T12:00:00Z`));
    return `<td width="${width}" align="center" style="width:${width}px; padding:0 2px;">
      <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="${width - 4}" style="width:${width - 4}px;">
        <tr><td align="center" style="background:${colour}; border-radius:8px; padding:10px 0;">
          <div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:11px; line-height:15px; font-weight:700; color:${CHARCOAL};">${escapeHtml(label)}</div>
          <div style="font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:16px; line-height:20px; font-weight:800; color:${CHARCOAL};">${total > 0 ? total : "&middot;"}</div>
        </td></tr>
      </table>
    </td>`;
  }).join("\n");
}

/**
 * The legend, and the one number that is worth a sentence of its own.
 *
 * How long an alert stayed unanswered is the number this whole product is about
 * — it is the gap between something happening and somebody knowing — so it is
 * written in words rather than left as a bar nobody decodes.
 */
function dayLegend(alerts: AlertFact[], input: WeekInput): string {
  const parts = ["Red: a fall or an SOS. Amber: another alert or a zone crossing. Teal: messages only."];

  const waits = alerts
    .map((a) => waitMinutes(a))
    .filter((m): m is number => m !== null);
  if (waits.length > 0) {
    const longest = Math.max(...waits);
    parts.push(`Longest wait before somebody answered: ${describeMinutes(longest)}.`);
  }
  const stillOpen = alerts.filter((a) => !isAnswered(a)).length;
  if (stillOpen > 0) {
    parts.push(
      `${stillOpen} ${stillOpen === 1 ? "alert is" : "alerts are"} still unanswered.`,
    );
  }
  if (input.zoneEvents.length > 0) {
    parts.push(`${input.zoneEvents.length} safe-zone crossings.`);
  }
  return parts.join(" ");
}

function waitMinutes(a: AlertFact): number | null {
  if (!a.occurred_at || !a.acknowledged_at) return null;
  const from = new Date(a.occurred_at).getTime();
  const to = new Date(a.acknowledged_at).getTime();
  if (Number.isNaN(from) || Number.isNaN(to) || to < from) return null;
  return Math.round((to - from) / 60_000);
}

function describeMinutes(m: number): string {
  if (m < 1) return "under a minute";
  if (m < 60) return `${m} minute${m === 1 ? "" : "s"}`;
  const hours = Math.round(m / 6) / 10;
  return `${hours} hour${hours === 1 ? "" : "s"}`;
}

/**
 * One panel per wearer, with the initial in a coloured disc.
 *
 * A disc drawn as a table cell with a background and a letter in it, not as an
 * image: Gmail strips the data: URI this project has to use for images, so an
 * avatar would be a hole in the layout for most readers.
 */
function wearerPanels(input: WeekInput, alerts: AlertFact[]): string {
  if (input.wearers.length === 0) return "";
  return input.wearers.map((w) => {
    const name = (w.name ?? "").trim() || "Unnamed wearer";
    const mine = alerts.filter((a) => a.wearer_id === w.id);
    const answeredCount = mine.filter(isAnswered).length;
    const initial = escapeHtml(name.slice(0, 1).toUpperCase());
    const line = mine.length === 0
      ? "No alerts this week."
      : `${mine.length} alert${mine.length === 1 ? "" : "s"}, ${answeredCount} answered.`;
    return `<table role="presentation" width="${CONTENT_WIDTH}" cellpadding="0" cellspacing="0" border="0" style="width:${CONTENT_WIDTH}px; margin:0 0 10px; border:1px solid #DED8CE; border-radius:10px;">
  <tr>
    <td width="66" style="width:66px; padding:14px 0 14px 14px; vertical-align:middle;">
      <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="40" style="width:40px;">
        <tr><td align="center" height="40" style="width:40px; height:40px; background:${BONE}; border-radius:20px; font-family:Archivo, 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size:17px; font-weight:800; color:${CHARCOAL};">${initial}</td></tr>
      </table>
    </td>
    <td style="padding:14px 16px 14px 0; vertical-align:middle; font-size:14px; line-height:21px; color:#4A5058;">
      <strong style="color:${CHARCOAL};">${escapeHtml(name)}</strong><br>${escapeHtml(line)}
    </td>
  </tr>
</table>`;
  }).join("\n");
}
