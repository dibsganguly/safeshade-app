/**
 * A deliberately tiny template renderer.
 *
 * ### Why not a template library
 *
 * Because every one of them is a dependency that runs inside an edge function
 * that sends emails about somebody having fallen over. The whole substitution
 * problem here is `{{name}}` in an HTML file, and the only part that is
 * actually easy to get wrong is escaping — so that part is the part that gets
 * written explicitly rather than trusted to a transitive dependency.
 *
 * ### Escaping is the point
 *
 * Wearer names, circle names and note text all come from user input and all end
 * up in these emails. A wearer named `<img src=x onerror=...>` would otherwise
 * ship live HTML into a guardian's inbox, and mail clients that render HTML are
 * exactly the audience for that. So {@link render} escapes by default and there
 * is a separate, loud opt-out.
 *
 * ### The two forms
 *
 *  - `{{key}}` — escaped. Use this for everything that came from a user.
 *  - `{{{key}}}` — raw. Exists for exactly one job: dropping an already-rendered
 *    template body into the layout's content slot. Passing user input through
 *    it is a bug, and the triple brace is meant to be visible enough in review
 *    that it gets caught.
 *
 * An unknown placeholder is replaced with the empty string rather than left in
 * the output. A guardian receiving an email that says "Hello {{wearer_name}}"
 * loses whatever confidence they had in the product.
 */

export type Vars = Record<string, string | number | boolean | null | undefined>;

/** HTML-escapes a value for use in element text or a quoted attribute. */
export function escapeHtml(value: unknown): string {
  if (value === null || value === undefined) return "";
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

/**
 * Substitutes `{{key}}` (escaped) and `{{{key}}}` (raw) in [template].
 *
 * The raw pass runs first so that a raw substitution cannot accidentally be
 * re-matched by the escaped pattern.
 */
export function render(template: string, vars: Vars): string {
  const raw = template.replace(
    /\{\{\{\s*([\w.]+)\s*\}\}\}/g,
    (_m, key: string) => {
      const v = vars[key];
      return v === null || v === undefined ? "" : String(v);
    },
  );
  return raw.replace(
    /\{\{\s*([\w.]+)\s*\}\}/g,
    (_m, key: string) => escapeHtml(vars[key]),
  );
}

/**
 * Renders a body template into the shared layout.
 *
 * @param layout the contents of `layout.html`.
 * @param body the contents of one of the body templates.
 * @param vars values for both. The layout and the body share one namespace on
 *   purpose - `heading` and `preheader` belong to the layout but are written by
 *   the body's author, and two namespaces would mean two places to look.
 */
export function renderEmail(layout: string, body: string, vars: Vars): string {
  return render(layout, { ...vars, content: render(body, vars) });
}

/**
 * A plain-text alternative, derived from the HTML.
 *
 * Every send includes one. Not for the handful of people reading mail in a
 * terminal - for spam scoring, which penalises HTML-only mail, and for the
 * notification preview on a locked phone. An alert email whose preview line
 * reads as markup is an alert somebody scrolls past.
 */
export function toPlainText(html: string): string {
  return html
    .replace(/<style[\s\S]*?<\/style>/gi, "")
    .replace(/<head[\s\S]*?<\/head>/gi, "")
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<\/(p|div|tr|h1|h2|h3|li)>/gi, "\n")
    .replace(/<[^>]+>/g, "")
    .replace(/&nbsp;/g, " ")
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/\n{3,}/g, "\n\n")
    .split("\n")
    .map((l) => l.trim())
    .join("\n")
    .trim();
}
