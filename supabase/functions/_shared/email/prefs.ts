import type { SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

/**
 * Email preferences, read on the server, immediately before sending.
 *
 * ### Why the server and not the phone
 *
 * A preference the client enforces is a preference that stops working the
 * moment anything else calls the function - the cron dispatcher, a retry from a
 * second phone in the same Circle, a curl from a terminal. Worse, it is a
 * preference the sender has to be trusted about: `send-alert-email` takes an
 * alert id and nothing else precisely so that the caller cannot decide who is
 * emailed, and letting the caller decide who is *not* emailed would hand the
 * same power back through the other door.
 *
 * So the switches live in `profiles.email_prefs`, they are read here under the
 * service role, and the phone's copy in `CloudState.emailPreferences` is a
 * display of the server's value rather than the value itself.
 *
 * ### A skip is reported, never swallowed
 *
 * Somebody who turned alert emails off did not receive the alert email. That is
 * a fact about who was reached, and the whole point of this project's delivery
 * ledger is that such facts are recorded rather than rounded off. Every
 * function returns a skipped recipient as
 * `{ email, status: "skipped", reason: "preference" }` and counts it as
 * delivered to nobody - the alert email's "Also notified" list says so out
 * loud, and the app shows it the same way.
 *
 * ### An address with no profile is NOT skipped
 *
 * `emergency_contacts` holds addresses of people who may have no SafeShade
 * account at all - a neighbour, a GP's surgery. They have no `email_prefs` row
 * and they never opted out of anything; withholding a fall alert from them
 * because a table lookup missed would be the most expensive possible reading of
 * an absent row. A missing profile, a missing column and a missing key all read
 * as "yes".
 */

/** The four switches. Matches the CHECK-free jsonb shape written by 0007. */
export type PrefKey = "alerts" | "circle" | "weekly_report" | "account";

/** One recipient's outcome, including the two that are not a send. */
export interface RecipientOutcome {
  email: string;
  status: "sent" | "failed" | "unknown" | "skipped";
  error: string | null;
  reason?: string;
}

/**
 * The set of lower-cased addresses that have switched [key] OFF.
 *
 * Returns a Set rather than a filtered list so the caller keeps its own
 * ordering - `send-alert-email` puts emergency contacts before circle members
 * on purpose, and a filter that re-orders them would quietly undo that.
 *
 * One query for the whole batch. A per-recipient lookup inside a send loop is
 * how a three-guardian alert becomes six round trips before the first email
 * leaves.
 */
export async function addressesOptedOut(
  admin: SupabaseClient,
  key: PrefKey,
  emails: string[],
): Promise<Set<string>> {
  const wanted = [...new Set(emails.map((e) => e.trim().toLowerCase()))];
  if (wanted.length === 0) return new Set();

  const { data, error } = await admin
    .from("profiles")
    .select("email, email_prefs")
    .in("email", wanted);

  if (error) {
    // The read failed, so nothing is known about anybody's preferences. The
    // safe reading of "unknown" for a fall alert is to send: a guardian who
    // gets an email they had switched off can switch it off again, and a
    // guardian who does not get a fall alert cannot un-miss it. This is the one
    // place in the file where the failure mode is deliberately noisy rather
    // than quiet.
    console.error("email_prefs could not be read; sending to everyone:", error.message);
    return new Set();
  }

  const off = new Set<string>();
  for (const row of data ?? []) {
    const email = typeof row.email === "string" ? row.email.trim().toLowerCase() : null;
    if (!email) continue;
    const prefs = row.email_prefs as Record<string, unknown> | null;
    // A missing object, a missing key, and anything that is not exactly the
    // boolean false all mean "yes". Only an explicit false is an opt-out.
    if (prefs && prefs[key] === false) off.add(email);
  }
  return off;
}

/** A skipped recipient, in the shape every function reports. */
export function skipped(email: string): RecipientOutcome {
  return {
    email,
    status: "skipped",
    error: null,
    reason: "preference",
  };
}
