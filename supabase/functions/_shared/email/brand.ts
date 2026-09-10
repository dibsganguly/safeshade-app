/**
 * The two images every email carries, as hosted https URLs.
 *
 * ### Why hosted, and not inlined
 *
 * The emblem used to be a `data:` URI in `emblem.ts`. Gmail strips `data:`
 * URIs in `<img src>`, and Gmail is most readers, so the masthead showed the
 * alt text and a broken-image glyph on the one email a new guardian ever
 * opens first. A hosted image is proxied by Gmail and shown.
 *
 * ### Where they live
 *
 * The public `brand` bucket of the SafeShade App project (`0008_brand_bucket`).
 * The files are `supabase/brand/emblem.png` (73×112, shown at 36×55) and
 * `supabase/brand/logo.png` (480×292, shown at 220 wide) — both exported at
 * twice their display size so they stay crisp on a phone. Upload them from
 * the dashboard (Storage → brand); nothing in the app writes there.
 *
 * ### The one hazard
 *
 * A URL that returns 404 shows a broken-image icon in Gmail, which is worse
 * than alt text. `tools/gen_email_templates.py` fetches both URLs and refuses
 * to generate while either is missing, so a deploy cannot precede the upload.
 */

const BRAND_BASE = "https://qlgbxhlbzyykxagsvwzv.supabase.co/storage/v1/object/public/brand";

/** The shield emblem, for the masthead. */
export const EMBLEM_URL = `${BRAND_BASE}/emblem.png`;

/** The full logo — emblem, wordmark and tagline — for above the footer. */
export const LOGO_URL = `${BRAND_BASE}/logo.png`;
