/**
 * Renders weekly-report.html with a fixture week and writes it to a file you
 * can open in a browser.
 *
 *     node supabase/functions/weekly-report/fixture.mjs [out.html]
 *
 * ### Why this exists
 *
 * The live project has one account, one Circle and a quiet week, so a real send
 * proves the *empty* rendering and nothing else. An infographic is exactly the
 * kind of thing that is wrong in a way nobody notices until it is in somebody's
 * inbox — a bar wider than its track, a division by zero, seven day cells that
 * do not add up to 536px — and none of that should require a fall to test.
 *
 * It touches no network and no database: `report.ts` deliberately has no
 * Supabase import, and `composeEmail` only reads compiled-in strings. Nothing
 * here can send an email.
 *
 * Node runs the `.ts` files directly by stripping the types (Node 22.18+ / 24).
 */

import { writeFileSync } from "node:fs";
import { buildWeeklyReport } from "./report.ts";
import { composeEmail } from "../_shared/email/resend.ts";
import { weeklyReportTemplate } from "../_shared/email/templates/weekly-report.ts";
import { toPlainText } from "../_shared/email/render.ts";

const DAY = 86_400_000;
const TZ = "Asia/Kolkata";

// A deliberately awkward week: two wearers, five alerts of four kinds, one of
// them still unanswered, a name with an ampersand in it, and a day with both a
// fall and forty messages so the "red beats teal" ordering is exercised.
const periodStart = new Date("2026-08-31T18:30:00.000Z"); // 1 Sep 00:00 IST
const at = (dayOffset, hour) =>
  new Date(periodStart.getTime() + dayOffset * DAY + hour * 3_600_000).toISOString();

const alerts = [
  { kind: "FALL", outcome: "CONTACTED", occurred_at: at(0, 9), acknowledged_at: at(0, 9.05), wearer_id: "w1" },
  { kind: "FALL", outcome: "DISMISSED", occurred_at: at(2, 14), acknowledged_at: at(2, 16), wearer_id: "w1" },
  { kind: "MISSED_CHECKIN", outcome: "AUTO_RESOLVED", occurred_at: at(3, 20), acknowledged_at: at(3, 20.5), wearer_id: "w2" },
  { kind: "ZONE_EXIT", outcome: "DISMISSED", occurred_at: at(4, 11), acknowledged_at: at(4, 11.2), wearer_id: "w1" },
  { kind: "SOS", outcome: "PENDING", occurred_at: at(6, 7), acknowledged_at: null, wearer_id: "w2" },
];

const messages = [];
for (let i = 0; i < 40; i++) {
  messages.push({ kind: i % 9 === 0 ? "voice" : "text", sent_at: at(i % 7, 10 + (i % 8)) });
}

const report = buildWeeklyReport({
  circleName: "Mum & Dad",
  periodStart: periodStart.toISOString(),
  periodEnd: new Date(periodStart.getTime() + 7 * DAY).toISOString(),
  locale: "en-IN",
  timeZone: TZ,
  alerts,
  messages,
  zoneEvents: [
    { kind: "EXIT", occurred_at: at(4, 11) },
    { kind: "ENTER", occurred_at: at(4, 12) },
  ],
  wearers: [
    { id: "w1", name: "Nani" },
    { id: "w2", name: "Dadu" },
  ],
  previousAlerts: [
    { kind: "FALL", outcome: "DISMISSED", occurred_at: at(-5, 9), acknowledged_at: at(-5, 10), wearer_id: "w1" },
    { kind: "FALL", outcome: "DISMISSED", occurred_at: at(-4, 9), acknowledged_at: at(-4, 10), wearer_id: "w1" },
    { kind: "SOS", outcome: "CONTACTED", occurred_at: at(-3, 9), acknowledged_at: at(-3, 10), wearer_id: "w2" },
  ],
  previousMessageCount: 51,
  hasPreviousWeek: true,
});

const html = await composeEmail(weeklyReportTemplate, {
  title: "Mum & Dad: your SafeShade week",
  preheader: "5 alerts, 4 answered, 40 messages.",
  accent: "#6FD3CC",
  ...report.vars,
  footer: "Sent by SafeShade because you are in Mum & Dad.",
});

const out = process.argv[2] ?? "weekly-report-fixture.html";
writeFileSync(out, html, "utf8");

console.log(`wrote ${out} (${html.length} bytes)`);
console.log("summary:", JSON.stringify(report.summary));
console.log("--- text/plain alternative ---");
console.log(toPlainText(html));

// Cheap geometry assertions. They are here rather than in a test file because
// the thing that breaks an email layout is a number, and a number is worth
// asserting wherever it is cheapest to do so.
const widths = [...html.matchAll(/width:(\d+)px/g)].map((m) => Number(m[1]));
const tooWide = widths.filter((w) => w > 600);
if (tooWide.length > 0) {
  console.error("FAIL: widths wider than the 600px frame:", tooWide);
  process.exit(1);
}
if (!report.hasData) {
  console.error("FAIL: the fixture week rendered as empty");
  process.exit(1);
}
console.log("geometry ok: no element wider than the 600px frame");

// ---------------------------------------------------------------------------
// The quiet week, which is the one that actually ships.
//
// A Circle with nothing in it must render one sentence, not a grid of zeros —
// and `{{^has_data}}` immediately followed by `{{#has_data}}` on the same key is
// precisely the shape a non-greedy section regex can get wrong. This costs a
// millisecond and is the only proof that path has ever been rendered.
const empty = buildWeeklyReport({
  circleName: "Mum & Dad",
  periodStart: periodStart.toISOString(),
  periodEnd: new Date(periodStart.getTime() + 7 * DAY).toISOString(),
  locale: "en-IN",
  timeZone: TZ,
  alerts: [],
  messages: [],
  zoneEvents: [],
  wearers: [{ id: "w1", name: "Nani" }],
  previousAlerts: [],
  previousMessageCount: 0,
  hasPreviousWeek: false,
});

const emptyHtml = await composeEmail(weeklyReportTemplate, {
  title: "Mum & Dad: your SafeShade week",
  preheader: "A quiet week.",
  accent: "#6FD3CC",
  ...empty.vars,
  footer: "Sent by SafeShade because you are in Mum & Dad.",
});

const emptyProblems = [];
if (empty.hasData) emptyProblems.push("an empty week reported hasData: true");
if (emptyHtml.includes("{{")) emptyProblems.push("unrendered {{...}} left in the quiet-week HTML");
if (!emptyHtml.includes("Nothing was recorded")) emptyProblems.push("the quiet-week sentence is missing");
if (emptyHtml.includes("days with data")) emptyProblems.push("the statistics strip rendered for an empty week");
if (emptyProblems.length > 0) {
  console.error("FAIL (quiet week):", emptyProblems.join("; "));
  process.exit(1);
}
console.log("quiet week ok: one sentence, no zero grid, nothing unrendered");
console.log("--- quiet week, text/plain ---");
console.log(toPlainText(emptyHtml));
