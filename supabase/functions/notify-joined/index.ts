import { createClient, type SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { composeEmail, sendEmail, subjectFor } from "../_shared/email/resend.ts";
import { addressesOptedOut, skipped, type RecipientOutcome } from "../_shared/email/prefs.ts";
import { joinedTemplate } from "../_shared/email/templates/joined.ts";

/**
 * notify-joined
 *
 * Request:  { "token": "<the invite token the app just accepted>" }
 * Response: { "already_notified": bool, "deliveries": [ RecipientOutcome ] }
 *
 * Tells a Circle's owners that somebody accepted their invitation. Invoked by
 * the app immediately after `accept_invite` succeeds.
 *
 * ============================================================================
 * The caller must BE the person who joined
 * ============================================================================
 *
 * The token alone is not authorisation for this. It is authorisation to *join*
 * - that is what `accept_invite` uses it for - and by the time this function is
 * called it has already been spent. Anybody who saw the invite link could
 * otherwise make the server email an owner "Dad joined your Circle" at any
 * moment, from SafeShade's own address, saying something that is not true.
 *
 * So there are three conditions, all of them checked under the service role
 * against the row rather than against anything the caller said:
 *
 *   1. the invite exists and `accepted_at` is not null - it really was accepted;
 *   2. `accepted_by` equals the caller's uid - by THIS person;
 *   3. `joined_notified_at` is still null - and nobody has been told yet.
 *
 * (3) is taken as a conditional UPDATE, the same claim-first pattern as
 * `alerts.notified_at`, so a retrying phone cannot produce a second email.
 *
 * ============================================================================
 * It goes to the owners, and only the owners
 * ============================================================================
 *
 * Not to the whole Circle. "Somebody new can see where your mother is" is a
 * fact the person responsible for the Circle needs; for everybody else it is
 * one more email that trains them to ignore the sender. The owner is also the
 * only person who can act on it - membership is owner-write, so a guardian who
 * disagreed could do nothing about it anyway.
 *
 * The joiner is not emailed. They know: they just tapped the link.
 */

const ROLE_LABELS: Record<string, string> = {
  owner: "an owner",
  guardian: "a guardian",
  viewer: "a viewer",
};

const ROLE_DESCRIPTIONS: Record<string, string> = {
  owner: "Invite and remove people, and change anything in the Circle.",
  guardian:
    "See alerts and acknowledge them, send messages, see the location and safe zones, and change device settings.",
  viewer:
    "See alerts, messages, the location and safe zones. They cannot change anything.",
};

Deno.serve(async (req: Request): Promise<Response> => {
  if (req.method !== "POST") return json({ error: "POST only" }, 405);

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return json({ error: "not signed in" }, 401);

  let token: string;
  try {
    const body = await req.json() as { token?: string };
    if (!body.token) return json({ error: "token is required" }, 400);
    token = body.token.trim();
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
  const callerId = userData?.user?.id ?? null;
  if (!callerId) return json({ error: "not signed in" }, 401);

  const admin = createClient(url, serviceKey);

  const { data: invite } = await admin
    .from("invites")
    .select("id, circle_id, role, email, accepted_at, accepted_by, joined_notified_at")
    .eq("token", token)
    .maybeSingle();

  // One answer for "no such token", "not accepted yet" and "accepted by
  // somebody else". Distinguishing them would turn this into an oracle for
  // which invite tokens exist and which have been used.
  if (!invite || !invite.accepted_at || invite.accepted_by !== callerId) {
    return json({ error: "not found" }, 404);
  }

  // Claim it. See the header comment.
  const { data: claimed } = await admin
    .from("invites")
    .update({ joined_notified_at: new Date().toISOString() })
    .eq("id", invite.id)
    .is("joined_notified_at", null)
    .select("id")
    .maybeSingle();

  if (!claimed) {
    return json({ already_notified: true, deliveries: [] }, 200);
  }

  const circleName = await lookupCircleName(admin, invite.circle_id);
  const joiner = await lookupProfile(admin, callerId);
  const joinerName = joiner.name ?? joiner.email ?? "Somebody";
  const roleLabel = ROLE_LABELS[invite.role] ?? "a guardian";
  const whenLabel = formatWhen(invite.accepted_at);

  const owners = await lookupOwnerEmails(admin, invite.circle_id, callerId);
  if (owners.length === 0) {
    // Honest empty answer: the owner of this Circle has no address on their
    // profile, so nobody could be told.
    return json({ already_notified: false, deliveries: [] }, 200);
  }

  const optedOut = await addressesOptedOut(admin, "circle", owners);
  const subject = subjectFor(joinedTemplate, {
    joiner_name: joinerName,
    circle_name: circleName,
  });

  const deliveries: RecipientOutcome[] = [];
  for (const to of owners) {
    if (optedOut.has(to)) {
      deliveries.push(skipped(to));
      continue;
    }
    const html = await composeEmail(joinedTemplate, {
      title: subject,
      preheader: `${joinerName} accepted your invitation to ${circleName}.`,
      // Teal: the thing the owner asked for has happened.
      accent: "#6FD3CC",
      joiner_name: joinerName,
      joiner_email: joiner.email ?? invite.email ?? "not recorded",
      circle_name: circleName,
      role_label: roleLabel,
      role_description: ROLE_DESCRIPTIONS[invite.role] ?? ROLE_DESCRIPTIONS.guardian,
      when_label: whenLabel,
      footer: `Sent by SafeShade because you own ${circleName}.`,
    });

    // Per invite AND per recipient. Per invite alone would make Resend hand the
    // first owner's cached response back for the second owner, who would then
    // be recorded as told and would receive nothing.
    const result = await sendEmail({
      to,
      subject,
      html,
      idempotencyKey: `joined:${invite.id}:${to}`,
    });
    deliveries.push({ email: to, status: result.status, error: result.error });
  }

  return json({ already_notified: false, deliveries }, 200);
});

async function lookupCircleName(admin: SupabaseClient, circleId: string): Promise<string> {
  const { data } = await admin
    .from("circles")
    .select("name")
    .eq("id", circleId)
    .maybeSingle();
  const name = data?.name;
  return typeof name === "string" && name.trim() ? name.trim() : "your SafeShade Circle";
}

async function lookupProfile(
  admin: SupabaseClient,
  userId: string,
): Promise<{ name: string | null; email: string | null }> {
  const { data } = await admin
    .from("profiles")
    .select("display_name, email")
    .eq("id", userId)
    .maybeSingle();
  return {
    name: data?.display_name?.trim() || null,
    email: data?.email?.trim().toLowerCase() || null,
  };
}

/**
 * The owners' addresses, minus the joiner's own.
 *
 * The subtraction is not cosmetic: `accept_invite` accepts an `owner`-role
 * invitation, so the person who just joined can themselves be an owner, and
 * without this they would be emailed to tell them what they had just done.
 */
async function lookupOwnerEmails(
  admin: SupabaseClient,
  circleId: string,
  joinerId: string,
): Promise<string[]> {
  const { data: members } = await admin
    .from("circle_members")
    .select("user_id")
    .eq("circle_id", circleId)
    .eq("role", "owner")
    .is("deleted_at", null);

  const ids = (members ?? []).map((m) => m.user_id).filter((id) => id && id !== joinerId);
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

function formatWhen(iso: string | null): string {
  if (!iso) return "just now";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "just now";
  return d.toUTCString().replace("GMT", "UTC");
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
