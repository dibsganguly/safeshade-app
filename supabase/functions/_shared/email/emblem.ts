/**
 * The SafeShade emblem, inlined.
 *
 * Generated from docs/Logo/SafeShade Emblem Logo.png with Pillow, by hand --
 * there is no generator script, and the exact recipe is written down in
 * supabase/README.md under "The emblem does not render in Gmail" so that
 * regenerating it does not become guesswork. The source is a 2000x2000 canvas
 * whose artwork occupies
 * only the middle of it (alpha bbox 447,182 to 1545,1862), so it is CROPPED TO
 * THE ALPHA BOUNDING BOX FIRST and only then scaled. Skipping the crop is how
 * every layout number around a SafeShade drawable ends up being a lie - it has
 * happened twice in this project, to splash_emblem and to brand_tagline.
 *
 * ### Why it is 40x61 and eight colours, and stored in short lines
 *
 * It used to be a 96px-wide, 64-colour PNG: 4,238 characters of base64, in
 * every deploy of every function. That is a problem for one reason that has
 * nothing to do with bytes - it is not reviewable. A single wrong character in
 * the middle of it produces a file that compiles, deploys, sends, and shows a
 * broken image, and no diff, test or log anywhere says so. It happened: a
 * re-deploy carried a corrupted copy and the only thing that caught it was
 * comparing the last seventy characters by hand.
 *
 * So it is now as small as it can be while still being the emblem: 40x61 (the
 * header slot is 26x40, so this is 1.5x for a dense screen), eight colours,
 * GIF, flattened onto the masthead charcoal because there is no transparency to
 * carry. 948 characters, in 56-character lines that a person can actually check.
 *
 * ### KNOWN LIMITATION: Gmail strips data: URIs in <img src>
 *
 * Gmail readers - which is most readers - see the alt text, which is why the
 * alt text is the brand name and not "logo". The fix is a hosted https URL,
 * which needs a domain or a public bucket upload; see supabase/README.md. Until
 * then the wordmark beside it in layout.html is what the header actually is.
 */
export const EMBLEM_DATA_URI =
  "data:image/gif;base64," +
  "R0lGODdhKAA9AIIAAGyYiyQoLyIoLiIoLSAoLyAmLR8lLB0hKCwAAAAA" +
  "KAA9AEAI/wAFCDhAsCDBAgESClzIsKFDhwcKGDBwAIDFixgBBDg4sWNH" +
  "iQcMPBwosUDFjAEoFlhJkeBEkwQHcPRowKTKjgZrGiBwIMDFjSt7ZrT4" +
  "0iPCiDRNZozIM6NEmlB3HhA49aNQjRFN+sQYMqrHgQIGOBSL1KhLrzTB" +
  "ih3JtsDAslCnumVL0iOBlSy93j1ItyFcmAa3DgUANyrPuXVfCh4MVOtg" +
  "i4WPljyJMmTTi11fPvUKUgBNypj3LkVrda3AiZ4/L4a8WW/EvgLJkv5M" +
  "ErZtAW4N3l5YFO3cgqZ3U50sWKHL1kZ5vqaL1PFjjBtJe16+MCvO43gJ" +
  "Lk45G/UBspOfY/9tfXV0WpmtC9xtOfRl+fabVwKtCXq81J/NtSec+dXk" +
  "Zu2VSbRdYZwVhNxlmN0HHYFRVWXWUFm9Nx9pJuFGVmq9hWeedNQtJFtS" +
  "5U2I1kDBjSQbct21FJZwKxKEYVQwrciiQ7k5GJSMfclUE155FTDAjxQx" +
  "VKJt6qVYE270iYRjX2UBVtCOXglAgIpLQqThUAHIxKBmQVY5HH3i4Ydi" +
  "R9M9BFdPCa0G4WwDKfnlR3epB5OahI3pUXRvUpTmk9kJFd1EA7wUKFQ8" +
  "1aUUYxz1+N2CUU135WMTHoplZCK2hOZgEclEp0WD3vnXjkVKqFN9+HU6" +
  "0XzOAYCeVIIdMGWqnBKT6GN+k4LU6keW2inAhO9hFtRiW0IFUmE8reaq" +
  "pKx11yaBCEIG5oJ2WpWYe8YS0KxGOiVllUhiWVVrr8F2NOVpNkpoWX2V" +
  "ChvSQiV51NNZyNqnrGkVwkhqug0ueWJUG8421ZAtvkhfQv7NFiPAHk6L" +
  "q79tshhStPbO2GK4Vk0lsUA1ClwxbhczVCNyFSJ8ccad7RYQADs=";
