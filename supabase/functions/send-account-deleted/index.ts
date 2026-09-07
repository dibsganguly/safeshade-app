import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { composeEmail, sendEmail, subjectFor } from "../_shared/email/resend.ts";
import { addressesOptedOut, skipped, type RecipientOutcome } from "../_shared/email/prefs.ts";
import { accountDeletedTemplate } from "../_shared/email/templates/account-deleted.ts";

/**
 * send-account-deleted
 *
 * Request:  {}          - it takes nothing at all
 * Response: { "delivery": RecipientOutcome }
 *
 * ============================================================================
 * It takes no body, and that is the whole security model
 * ============================================================================
 *
 * The account is the caller's, the address is read from `auth.users` under the
 * service role, and there is no parameter anybody could use to point this at
 * somebody else. A function that accepted an email address would let anybody
 * with the anon key send "your SafeShade account is being deleted" to any
 * address they liked, from SafeShade's own sender - which is a credible enough
 * scare that people would act on it.
 *
 * ============================================================================
 * It runs BEFORE delete_account(), and the wording follows from that
 * ============================================================================
 *
 * After `delete_account()` there is no `auth.users` row and therefore no
 * address to send to. So this is called first, which means at the moment the
 * email is composed the deletion has NOT happened - and the email says a
 * request was made, not that an account was deleted. The app's standing rule
 * (nothing reports an outcome the server has not observed) applies to the tense
 * of a sentence in an email exactly as it applies to a tick on a screen.
 *
 * ============================================================================
 * A failure here NEVER blocks the deletion
 * ============================================================================
 *
 * This function returns 200 with a `failed` delivery rather than an error
 * status wherever it can, and `CloudAuth.deleteAccount` ignores the result and
 * proceeds either way. Somebody who has asked to be deleted is entitled to be
 * deleted; an email provider having a bad afternoon is not a reason to keep
 * their medical record on a server.
 *
 * The `account` preference gates it, and that is defensible for this one
 * because the mail is a receipt rather than a warning: the person requesting
 * the deletion is the person reading it, and they are watching the app do it.
 */

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") return json({ error: "POST only" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return json({ error: "not signed in" }, 401);

  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

  const asUser = createClient(url, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData } = await asUser.auth.getUser();
  const uid = userData?.user?.id ?? null;
  if (!uid) return json({ error: "not signed in" }, 401);

  const admin = createClient(url, serviceKey);

  // The address comes from auth, not from the profile and not from the caller.
  // `profiles.email` is a copy kept in step by a trigger; the account's real
  // address is the one Supabase will stop being able to reach in a moment.
  const { data: authUser } = await admin.auth.admin.getUserById(uid);
  const to = authUser?.user?.email?.trim().toLowerCase() ?? null;

  if (!to) {
    // A real answer, not an error: an account created through a provider that
    // gave no address cannot be emailed, and the deletion goes ahead regardless.
    return json({
      delivery: {
        email: null,
        status: "failed",
        error: "this account has no email address",
      },
    }, 200);
  }

  const optedOut = await addressesOptedOut(admin, "account", [to]);
  if (optedOut.has(to)) {
    const outcome: RecipientOutcome = skipped(to);
    return json({ delivery: outcome }, 200);
  }

  const whenLabel = new Date().toUTCString().replace("GMT", "UTC");
  const subject = subjectFor(accountDeletedTemplate, {});
  const html = await composeEmail(accountDeletedTemplate, {
    title: subject,
    preheader: "A request to delete this SafeShade account was just made.",
    // Amber: attention, not emergency.
    accent: "#F5A623",
    account_email: to,
    when_label: whenLabel,
    footer: "Sent by SafeShade to the address on the account being deleted.",
  });

  // Per account, not per moment. Somebody who taps delete, loses the response
  // and taps again should get one email, not two - and unlike the alert path
  // there is no row left afterwards to hang a claim stamp on.
  const result = await sendEmail({
    to,
    subject,
    html,
    idempotencyKey: `account-deleted:${uid}`,
  });

  return json({
    delivery: { email: result.recipient, status: result.status, error: result.error },
  }, 200);
});

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
