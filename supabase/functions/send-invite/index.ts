import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { composeEmail, sendEmail } from "../_shared/email/resend.ts";

/**
 * send-invite
 *
 * Request:  { "circle_id": "<uuid>", "email": "...", "role": "guardian" }
 * Response: { "invite_id", "expires_at", "delivery": { status, error } }
 *
 * ============================================================================
 * Only an owner may invite, and that is checked against the policy, not here
 * ============================================================================
 *
 * The invite row is inserted through the **caller's own client**, so the
 * `"invites are created by an owner"` policy in `0001_init.sql` is what decides
 * whether the insert happens. A non-owner gets a policy refusal and this
 * function returns 403 without ever touching the service role.
 *
 * Doing it this way rather than checking membership under the service role
 * matters: an authorisation rule that exists in two places drifts, and the copy
 * that drifts is always the one nobody is looking at. There is one definition
 * of "may invite", it lives in SQL, and this function obeys it rather than
 * restating it.
 *
 * The service role is used for exactly one thing afterwards: reading the circle
 * name and the inviter's display name for the email body, which the caller
 * could supply but must not - a caller-supplied circle name in an email that
 * says "you have been added to X" is a phishing template with extra steps.
 *
 * ============================================================================
 * The invite is created before the email is attempted, and the email's outcome
 * is reported separately
 * ============================================================================
 *
 * The row is the invitation; the email is a convenience for delivering the link.
 * If Resend fails, the invite still exists and the owner can copy the link out
 * of the app - so the response carries `invite_id` AND the delivery outcome, and
 * the UI must show both. Returning only a success would tell an owner their
 * sister had been invited when the mail bounced.
 *
 * With no domain yet, `onboarding@resend.dev` only delivers to the Resend
 * account owner's address; every other invite comes back as a `failed` delivery
 * carrying Resend's own message. See supabase/README.md.
 */

const ROLE_DESCRIPTIONS: Record<string, string> = {
  owner: "Invite and remove people, and change anything in the Circle.",
  guardian:
    "See alerts and acknowledge them, send messages, see the location and safe zones, and change device settings.",
  viewer:
    "See alerts, messages, the location and safe zones. You will not be able to change anything.",
};

const ROLE_LABELS: Record<string, string> = {
  owner: "an owner",
  guardian: "a guardian",
  viewer: "a viewer",
};

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") return json({ error: "POST only" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return json({ error: "not signed in" }, 401);

  let circleId: string;
  let email: string;
  let role: string;
  try {
    const body = await req.json() as {
      circle_id?: string;
      email?: string;
      role?: string;
    };
    if (!body.circle_id) return json({ error: "circle_id is required" }, 400);
    if (!body.email || !body.email.includes("@")) {
      return json({ error: "a valid email is required" }, 400);
    }
    circleId = body.circle_id;
    email = body.email.trim().toLowerCase();
    role = body.role ?? "guardian";
    if (!ROLE_LABELS[role]) return json({ error: "unknown role" }, 400);
  } catch {
    return json({ error: "body must be JSON" }, 400);
  }

  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

  const asUser = createClient(url, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });

  const { data: userData } = await asUser.auth.getUser();
  const inviterId = userData?.user?.id ?? null;
  if (!inviterId) return json({ error: "not signed in" }, 401);

  // The insert IS the authorisation check. See the header comment.
  //
  // `token` is deliberately not supplied: the column defaults to
  // gen_random_uuid(), so the value is minted by the database. A
  // client-supplied invite token is a token an attacker can also choose, and
  // knowing it is the entire authorisation to join a Circle.
  const inviteId = crypto.randomUUID();
  const { data: invite, error: insertError } = await asUser
    .from("invites")
    .insert({
      id: inviteId,
      circle_id: circleId,
      email,
      role,
      invited_by: inviterId,
    })
    .select("id, token, expires_at")
    .single();

  if (insertError || !invite) {
    // A policy refusal and a genuine database error are both reported as 403
    // here rather than distinguished, because distinguishing them would let a
    // non-member probe which circle ids exist.
    return json({ error: "you are not allowed to invite to this Circle" }, 403);
  }

  // Service role, for display strings only.
  const admin = createClient(url, serviceKey);

  const { data: circle } = await admin
    .from("circles")
    .select("name")
    .eq("id", circleId)
    .maybeSingle();

  const { data: inviterProfile } = await admin
    .from("profiles")
    .select("display_name, email")
    .eq("id", inviterId)
    .maybeSingle();

  const { data: wearer } = await admin
    .from("wearers")
    .select("name")
    .eq("circle_id", circleId)
    .is("deleted_at", null)
    .limit(1)
    .maybeSingle();

  const circleName = circle?.name?.trim() || "a SafeShade Circle";
  const inviterName = inviterProfile?.display_name?.trim() ||
    inviterProfile?.email?.trim() ||
    "Someone";
  const wearerName = wearer?.name?.trim() || "someone they care for";

  // The deep link the app registers. Same scheme and host as the auth callback
  // so there is one entry point to handle in MainActivity rather than two.
  const actionUrl = `safeshade://login-callback?invite=${invite.token}`;

  const subject = `${inviterName} added you to ${circleName} on SafeShade`;
  const html = await composeEmail("invite", {
    title: subject,
    preheader: `Join ${circleName} as ${ROLE_LABELS[role]} and be told if there is a fall.`,
    // Amber: this asks somebody to do something, but nothing is wrong.
    accent: "#F5A623",
    inviter_name: inviterName,
    circle_name: circleName,
    wearer_name: wearerName,
    role_label: ROLE_LABELS[role],
    role_description: ROLE_DESCRIPTIONS[role],
    action_url: actionUrl,
    expires_at_label: formatDate(invite.expires_at),
    footer: `Sent by SafeShade at the request of ${inviterName}.`,
  });

  const delivery = await sendEmail({
    to: email,
    subject,
    html,
    // Per invite id, which is already unique per (circle, email, moment). A
    // retry of the same call resends nothing; a genuinely new invitation gets a
    // new id and therefore a new key.
    idempotencyKey: `invite:${invite.id}`,
  });

  return json({
    invite_id: invite.id,
    expires_at: invite.expires_at,
    // Reported separately from the invite's own success, always. See the
    // header comment: the row is the invitation, the email is only the delivery.
    delivery: { status: delivery.status, error: delivery.error },
  }, 200);
});

function formatDate(iso: string | null): string {
  if (!iso) return "in two weeks";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "in two weeks";
  return d.toUTCString().replace("GMT", "UTC");
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
