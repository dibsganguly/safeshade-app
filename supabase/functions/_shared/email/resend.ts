import { EMBLEM_DATA_URI } from "./emblem.ts";
import { renderEmail, toPlainText, type Vars } from "./render.ts";
import { BODY_TEMPLATES, LAYOUT_HTML, type BodyTemplateName } from "./templates.ts";

/**
 * The Resend transport, and the one rule it exists to enforce.
 *
 * ### Nothing is reported as sent before Resend says so
 *
 * {@link sendEmail} returns a discriminated result, never a boolean and never a
 * thrown-away promise. The caller writes `alert_deliveries` from that result and
 * from nothing else.
 *
 * This is not a stylistic preference. The app's existing SMS path calls
 * `sendMultipartTextMessage` with null sent/delivery intents, so `Sent` there
 * means "handed to the radio" - and the one real defect of the previous release
 * was a confirmation tick shown on a quick message that had failed, watched,
 * filmed, and written up as verified without anybody asking what it was
 * confirming. Email is the first channel in this product that *can* tell the
 * truth about delivery. Spending that on an optimistic write would be a waste
 * of the only honest signal there is.
 *
 * ### Three outcomes, not two
 *
 *  - `sent` - Resend returned 2xx and an id.
 *  - `failed` - Resend returned non-2xx. Its own message is carried through.
 *  - `unknown` - the request left this function and the call threw before an
 *    answer came back. The mail may or may not have gone. Reporting that as
 *    either success or failure would be a guess presented as a fact, so it gets
 *    its own value, and `alert_deliveries.status` has a CHECK constraint that
 *    permits it.
 *
 * ### The sender address
 *
 * `onboarding@resend.dev` is Resend's shared testing sender and it is used here
 * because SafeShade has no domain yet. **It can only deliver to the email
 * address that owns the Resend account.** Every other recipient comes back as a
 * 403 with a message saying so, and that is treated as a perfectly ordinary
 * `failed` delivery, with Resend's own words recorded. It is not special-cased
 * or hidden: an alert that reached nobody must say it reached nobody.
 *
 * Fixing it is one line here plus a DNS record; see supabase/README.md.
 */

const RESEND_ENDPOINT = "https://api.resend.com/emails";

/** See the note on the sender address above. */
export const FROM_ADDRESS = "SafeShade <onboarding@resend.dev>";

export type DeliveryStatus = "sent" | "failed" | "unknown";

export interface DeliveryResult {
  recipient: string;
  status: DeliveryStatus;
  error: string | null;
  providerId: string | null;
}

export interface SendRequest {
  to: string;
  subject: string;
  /** The rendered HTML. Use {@link composeEmail}. */
  html: string;
  /**
   * Deduplication key.
   *
   * Resend returns the cached response for a repeated key rather than sending
   * again, which is exactly what is wanted when this function is retried: a
   * guardian must not receive four copies of one fall alert because the caller
   * lost three responses.
   *
   * It MUST include the recipient. Keying on the alert id alone would send the
   * first contact's email and then return that same cached response for every
   * other contact - so the second and third people would be recorded as
   * successfully notified and would never receive anything. That is the exact
   * shape of failure this whole file is written against.
   */
  idempotencyKey: string;
}

/**
 * Sends one email.
 *
 * Never throws. Every failure path produces a {@link DeliveryResult} the caller
 * can write to `alert_deliveries` verbatim.
 */
export async function sendEmail(req: SendRequest): Promise<DeliveryResult> {
  const apiKey = Deno.env.get("RESEND_API_KEY");
  if (!apiKey) {
    // A configuration failure, not a transport one, and it is reported as a
    // failed delivery rather than a 500 so that the alert row still records
    // that nobody was reached and why.
    return {
      recipient: req.to,
      status: "failed",
      error: "RESEND_API_KEY is not set on this project",
      providerId: null,
    };
  }

  let response: Response;
  try {
    response = await fetch(RESEND_ENDPOINT, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json",
        "Idempotency-Key": req.idempotencyKey,
      },
      body: JSON.stringify({
        from: FROM_ADDRESS,
        to: [req.to],
        subject: req.subject,
        html: req.html,
        // Included on every send. Not for the handful of people reading mail in
        // a terminal - for spam scoring, which penalises HTML-only mail, and
        // for the preview line on a locked phone.
        text: toPlainText(req.html),
      }),
    });
  } catch (e) {
    // The request left and nothing came back. See the three-outcomes note.
    return {
      recipient: req.to,
      status: "unknown",
      error: `no response from the email provider: ${errorMessage(e)}`,
      providerId: null,
    };
  }

  const bodyText = await response.text().catch(() => "");

  if (!response.ok) {
    return {
      recipient: req.to,
      status: "failed",
      error: providerMessage(bodyText, response.status),
      providerId: null,
    };
  }

  let providerId: string | null = null;
  try {
    providerId = (JSON.parse(bodyText) as { id?: string }).id ?? null;
  } catch {
    // A 2xx with an unparseable body is still a 2xx. Resend accepted it; we
    // simply have no id to file it under.
    providerId = null;
  }

  return { recipient: req.to, status: "sent", error: null, providerId };
}

/** Resend's own message, or the bare status if it did not give one. */
function providerMessage(bodyText: string, status: number): string {
  try {
    const parsed = JSON.parse(bodyText) as { message?: string; error?: string };
    const m = parsed.message ?? parsed.error;
    if (m) return m;
  } catch {
    // fall through
  }
  return bodyText.trim() ? bodyText.trim().slice(0, 500) : `provider returned ${status}`;
}

function errorMessage(e: unknown): string {
  return e instanceof Error ? e.message : String(e);
}

/**
 * Renders one body template into the shared layout.
 *
 * ### The templates are compiled in, not read from disk
 *
 * This used to call `Deno.readTextFile` against `import.meta.url` and fall back
 * to unbranded HTML when the read failed. Whether the `.html` files survive a
 * deploy was never proven - the MCP deploy path uploads only the files it is
 * handed, and the CLI path needs a `config.toml` entry this repo deliberately
 * does not commit. So a missing template was a silent downgrade: the mail still
 * went, and only a `template load failed` line in the logs said why it looked
 * wrong.
 *
 * `templates.ts` is generated from the same `.html` files by
 * `tools/gen_email_templates.py` and imported like any other module. A missing
 * template is now a build failure, which is a failure somebody sees. There is
 * no fallback here any more because there is nothing left to fall back from,
 * and a dead fallback path reads in review as a live safety net.
 *
 * Still `async` because both callers `await` it and because the signature
 * should not have to change again if a template ever needs fetching.
 */
// deno-lint-ignore require-await
export async function composeEmail(
  bodyTemplate: BodyTemplateName,
  vars: Vars,
): Promise<string> {
  return renderEmail(LAYOUT_HTML, BODY_TEMPLATES[bodyTemplate], {
    emblem: EMBLEM_DATA_URI,
    ...vars,
  });
}
