import { EMBLEM_DATA_URI } from "./emblem.ts";
import { renderEmail, toPlainText, type Vars } from "./render.ts";

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
 * Loads the layout and one body template and renders them together.
 *
 * ### The template files have to be deployed alongside the function
 *
 * `Deno.readTextFile` against `import.meta.url` only works if the `.html` files
 * are actually uploaded, which the Supabase CLI does for everything under the
 * functions directory. If a deployment ever drops them, this falls back to a
 * plain but complete message rather than failing the send: an alert email that
 * looks unstyled is vastly better than an alert email that does not arrive, and
 * this is the one product where that trade is not close.
 */
export async function composeEmail(
  bodyTemplate: "otp" | "magic-link" | "invite" | "alert" | "digest",
  vars: Vars,
): Promise<string> {
  try {
    const layout = await readTemplate("layout.html");
    const body = await readTemplate(`${bodyTemplate}.html`);
    return renderEmail(layout, body, { emblem: EMBLEM_DATA_URI, ...vars });
  } catch (e) {
    console.error(`template load failed (${bodyTemplate}): ${errorMessage(e)}`);
    return fallbackHtml(vars);
  }
}

async function readTemplate(name: string): Promise<string> {
  return await Deno.readTextFile(new URL(`./${name}`, import.meta.url));
}

/** Unstyled, complete, and legible. See {@link composeEmail}. */
function fallbackHtml(vars: Vars): string {
  const heading = String(vars.headline ?? vars.title ?? "SafeShade");
  const line = String(vars.preheader ?? "");
  const url = vars.map_url ?? vars.action_url;
  const link = url ? `<p><a href="${String(url)}">${String(url)}</a></p>` : "";
  return `<!doctype html><html><body style="font-family:Arial,sans-serif;color:#22282E;">
<h1 style="font-size:22px;">${escapeMinimal(heading)}</h1>
<p>${escapeMinimal(line)}</p>${link}
<p style="color:#6A7078;font-size:12px;">SafeShade</p>
</body></html>`;
}

function escapeMinimal(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}
