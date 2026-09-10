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

/** The full logo — emblem, wordmark and tagline. Not referenced by the layout now. */
export const LOGO_URL = `${BRAND_BASE}/logo.png`;

/**
 * The whole masthead as one image: the full logo on its beige ground, with
 * the card's top corners already rounded. Gmail on Android recolours any
 * background it judges light, and no CSS or bgcolor stops it; pixels inside
 * an image are the one thing it leaves alone. 1200×336, shown at 600×168.
 */
export const MASTHEAD_URL = `${BRAND_BASE}/masthead.png`;
