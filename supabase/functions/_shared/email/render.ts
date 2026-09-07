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
 *
 * ### And one block form, added for exactly one reason
 *
 *  - `{{#key}}…{{/key}}` keeps the block when `key` is truthy;
 *    `{{^key}}…{{/key}}` keeps it when `key` is falsy.
 *
 * The weekly report is the reason. A week in which nothing happened has to say
 * so in one line — not draw a bar chart of zeros and a seven-day strip of empty
 * cells, which is a picture of a working product rather than a picture of the
 * week. Without a block form, that decision would have to be made in TypeScript
 * by concatenating markup, and the `.html` file would stop being the place the
 * email is actually designed.
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
 * Whether a value keeps a `{{#key}}` block.
 *
 * `0` is false, and that is deliberate rather than incidental: every section
 * this gates is "did anything happen", and a count of zero is exactly the case
 * the block must not be drawn for.
 */
function truthy(v: unknown): boolean {
  if (v === null || v === undefined || v === false) return false;
  if (v === 0 || v === "0" || v === "") return false;
  return true;
}

const SECTION_RE = /\{\{([#^])\s*([\w.]+)\s*\}\}([\s\S]*?)\{\{\/\s*\2\s*\}\}/g;

/**
 * Resolves `{{#key}}…{{/key}}` and `{{^key}}…{{/key}}` blocks.
 *
 * ### The loop is not an optimisation, it is the nesting
 *
 * `String.replace` does not re-scan what it substituted, so one pass resolves
 * only the outermost block and leaves anything inside it as literal braces in
 * the finished email. The weekly report has exactly that shape — a
 * `{{#comparison_rows}}` inside the `{{#has_data}}` that decides whether the
 * grid is drawn at all — and the first version of this shipped
 * `{{#comparison_rows}}` into the rendered output, which is how the depth limit
 * came to be here rather than in a comment saying nesting is unsupported.
 *
 * Bounded rather than `while (true)`: a malformed template with an unclosed tag
 * must not spin an edge function that sends fall alerts. Four levels is more
 * than any of these templates uses, and anything deeper simply stops being
 * substituted — visibly, in review, rather than silently at runtime.
 *
 * Blocks of the same key do not nest inside one another: the pattern is
 * non-greedy and stops at the first matching close tag.
 */
function renderSections(template: string, vars: Vars): string {
  let out = template;
  for (let depth = 0; depth < 4; depth++) {
    const next = out.replace(
      SECTION_RE,
      (_m, kind: string, key: string, block: string) => {
        const keep = kind === "#" ? truthy(vars[key]) : !truthy(vars[key]);
        return keep ? block : "";
      },
    );
    if (next === out) break;
    out = next;
  }
  return out;
}

/**
 * Substitutes `{{key}}` (escaped) and `{{{key}}}` (raw) in [template].
 *
 * Blocks are resolved first, then raw, then escaped: a block that is dropped
 * must not have its placeholders substituted on the way out, and the raw pass
 * must run before the escaped one so that a raw substitution cannot
 * accidentally be re-matched by the escaped pattern.
 */
export function render(template: string, vars: Vars): string {
  const raw = renderSections(template, vars).replace(
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
 * Substitutes into a **subject line**, without HTML escaping.
 *
 * A subject is not HTML and must not be escaped as if it were. `render` would
 * turn a circle called "Mum & Dad" into `Mum &amp; Dad` in the inbox list,
 * which is the one place the mistake is guaranteed to be seen and impossible to
 * hide. Newlines are stripped instead, because that is the injection that
 * matters here: a `\n` in a header value can forge another header.
 */
export function renderSubject(template: string, vars: Vars): string {
  return template
    .replace(/\{\{\{?\s*([\w.]+)\s*\}?\}\}/g, (_m, key: string) => {
      const v = vars[key];
      return v === null || v === undefined ? "" : String(v);
    })
    .replace(/[\r\n]+/g, " ")
    .trim();
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
    .replace(/&middot;/g, "·")
    .replace(/&mdash;/g, "—")
    // Numeric entities, because the weekly report's week-over-week arrows are
    // &#9650; and &#9660; and a plain-text part that reads "&#9650; Alerts: 2
    // more" is the notification preview somebody sees on a locked phone. The
    // named passes run first so that &amp;#39; cannot be double-decoded.
    .replace(/&#(\d+);/g, (_m, code: string) => {
      const n = Number(code);
      return n > 0 && n <= 0x10FFFF ? String.fromCodePoint(n) : _m;
    })
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
