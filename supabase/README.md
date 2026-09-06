# SafeShade Cloud — setting it up

Everything the app needs on the server side lives in this folder. **None of it
has been run.** It was written against a project that does not exist yet, on a
machine with no `SUPABASE_URL` in `local.properties`, so treat the first
`db push` as the real test rather than as a formality.

Until you finish step 3, the Android app builds and runs exactly as before:
`CloudContainer` sees a blank `BuildConfig.SUPABASE_URL` and installs a disabled
client, every cloud call returns `CloudResult.Disabled`, and the UI shows
nothing rather than an error. **The app is offline-first; the cloud is an
addition to it, never a dependency of it.**

---

## Contents

| Path | What it is |
|---|---|
| `migrations/0001_init.sql` | The whole schema: 19 tables, RLS on every one, the heat-map materialized view, the storage buckets, `handle_new_user`, `delete_account`. |
| `functions/send-alert-email/` | Emails a Circle when an alert is raised. Takes `{ alert_id }` and nothing else. |
| `functions/send-invite/` | Creates an invite row and emails the link. |
| `functions/_shared/email/` | The branded templates, the renderer, and the Resend transport. |

---

## 1. Make the project

The user asked for **a new project in a new organisation.**

1. Go to <https://supabase.com/dashboard> and create the organisation first —
   there is no API or MCP tool for creating an organisation, so this step has to
   be done in the dashboard by hand.
2. Create a project inside it. Pick the region closest to the users (for India,
   `ap-south-1` / Mumbai); **the region cannot be changed later** and it sets the
   latency on every alert.
3. Note down, from **Project Settings → API**:
   - the **Project URL** (`https://<ref>.supabase.co`)
   - the **anon / publishable key**
   - the **service_role key** — this one never leaves the server. It bypasses
     row-level security completely. It must never appear in `local.properties`,
     in the APK, in a commit, or in a screenshot.

## 2. Apply the schema

With the Supabase CLI:

```bash
supabase link --project-ref <ref>
supabase db push
```

Or paste `migrations/0001_init.sql` into **SQL Editor → New query** and run it.
It is written to be idempotent — `create table if not exists`, `create or
replace function`, and a `drop policy if exists` before every `create policy` —
so re-running it after an edit is safe.

**Two things to check afterwards, because both fail quietly:**

- **Database → Tables**: every table must show the *RLS enabled* badge. This
  project has automatic RLS off, and a table without it is readable and writable
  by every authenticated user of the project with no error anywhere. On a schema
  holding medical records and the locations of vulnerable people that is the
  single most important thing on this page.
- **Database → Extensions → `pg_cron`**: the migration tries
  `create extension if not exists pg_cron` inside a guarded block. On some plans
  it cannot be created from SQL and must be switched on here. Without it the
  heat-map materialized view is never refreshed and simply stays stale — the
  migration still succeeds, and nothing tells you. If the heat map never
  updates, this is the first thing to look at. You can always refresh it by hand
  with `select public.refresh_heatmap_cells();`.

**A third thing to know before anyone joins a Circle.** Membership is
owner-write: the policy deliberately does *not* let a person insert their own
`circle_members` row, because circle ids travel in invite links and in every
synced row, so "you need an id nobody publishes" is not an access control.
Joining goes through `accept_invite(<token>)`, a `security definer` function
gated on the server-minted invite token. **Nothing in the app calls it yet** —
`CloudClient` has no generic RPC method, so Phase 2 either adds one or reaches
`postgrest.rpc` directly the way `deleteAccount()` does.

Related: reads use `is_circle_member`, writes use `is_circle_actor`, which
excludes `viewer`. That is what makes the viewer role mean anything.

## 3. Point the app at it

Add to `local.properties` in the repo root (git-ignored; **this file is not
created by the build and must not be committed**):

```properties
SUPABASE_URL=https://<ref>.supabase.co
SUPABASE_ANON_KEY=<the anon key>
GOOGLE_WEB_CLIENT_ID=<see docs/wizards/google-signin.md>
```

Then `gradlew.bat assembleDebug`. `app/build.gradle.kts` reads these through its
`prop()` helper into `BuildConfig`; blank means disabled, and both the URL and
the key must be present — a URL with no key produces a client that builds fine
and then 401s on every call, which is far more confusing than being cleanly off.

The values can also come from environment variables of the same names, which is
how CI should supply them.

## 4. Resend, and the sender-address problem

1. Create an account at <https://resend.com> and make an API key.
2. Set it as a function secret:

   ```bash
   supabase secrets set RESEND_API_KEY=re_xxxxxxxx
   ```

### The limitation you will hit within five minutes

Both functions send from **`SafeShade <onboarding@resend.dev>`**, which is
Resend's shared testing sender because SafeShade has no domain yet.

**It can only deliver to the email address that owns the Resend account.**
Every other recipient comes back as a 403 whose message says so.

That is handled honestly rather than hidden: the failure is written to
`alert_deliveries` as `status = 'failed'` with Resend's own words in `error`,
the response's `deliveries` array reports it, and the alert email's "Also
notified" list says *not reached*. An alert that reached nobody says it reached
nobody. **Do not "fix" this by suppressing the error** — the app's standing rule
is that a message reporting success and reaching nobody is worse than one
reporting failure.

To actually fix it: add a domain in **Resend → Domains**, add the DKIM and SPF
records it gives you to your DNS, wait for verification, then change
`FROM_ADDRESS` in `functions/_shared/email/resend.ts` to something like
`SafeShade <alerts@yourdomain>`. That is the whole change.

## 5. Deploy the functions

```bash
supabase functions deploy send-alert-email
supabase functions deploy send-invite
```

`SUPABASE_URL`, `SUPABASE_ANON_KEY` and `SUPABASE_SERVICE_ROLE_KEY` are injected
into every function by the platform — do not set them as secrets yourself.

**Check the templates got uploaded.** The functions load their HTML with
`Deno.readTextFile` relative to `import.meta.url`, and the CLI uploads
everything under the functions directory. If a deployment ever drops the `.html`
files the send does not fail — `composeEmail` falls back to a plain but complete
message and logs `template load failed`. An alert email that looks unstyled is
far better than one that does not arrive, but a `template load failed` line in
**Edge Functions → Logs** means the deploy is broken and nobody would otherwise
notice. The fix, on a recent CLI, is a `supabase/config.toml` entry:

```toml
[functions.send-alert-email]
static_files = ["./functions/_shared/email/*.html"]
```

That file is deliberately **not** committed here: a `config.toml` containing a
key an older CLI does not recognise fails *every* `supabase` command, including
`db push`, and none of this could be tested from the machine it was written on.
Add it once you know your CLI version accepts it.

Smoke test, with a real alert id and a signed-in user's JWT:

```bash
curl -X POST "https://<ref>.supabase.co/functions/v1/send-alert-email" \
  -H "Authorization: Bearer <a user access token>" \
  -H "Content-Type: application/json" \
  -d '{"alert_id":"<uuid>"}'
```

A 404 means the caller is not a member of that alert's Circle — that is the
authorisation check working, not a bug. The functions never take a recipient
list; if they did, anyone with the anon key could send arbitrary mail from a
sender that says SafeShade.

## 6. Auth email templates in the dashboard

Supabase sends the sign-in emails itself, from its own templates, so the two
files here have to be pasted in.

Go to **Authentication → Email Templates**.

| Template in the dashboard | Paste | Then swap |
|---|---|---|
| **Magic Link** | `functions/_shared/email/otp.html` if you want the six-digit code (what the app asks for first), or `magic-link.html` for a link | `{{code}}` → `{{ .Token }}`, or `{{action_url}}` → `{{ .ConfirmationURL }}` |
| **Confirm signup** | `magic-link.html` | `{{action_url}}` → `{{ .ConfirmationURL }}` |

**The placeholder syntaxes are different and they look alike.** These files use
`{{name}}`, handled by `render.ts`. Supabase uses Go templates, `{{ .Token }}`.
Pasting a file unchanged sends everybody the literal text `{{code}}`, and it
will not be obvious in the preview.

The body templates are fragments, not whole documents — they are designed to sit
inside `layout.html`. For the dashboard, paste `layout.html` and replace its
`{{{content}}}` line with the body file's contents, then replace `{{emblem}}`
with a hosted image URL (see the note below) and `{{accent}}` with `#6FD3CC`.

### The emblem does not render in Gmail

`layout.html` inlines the emblem as a `data:` URI, generated from
`docs/Logo/SafeShade Emblem Logo.png` — cropped to its alpha bounding box first
(the source is a 2000×2000 canvas with the artwork in the middle of it), scaled
to 96px wide and quantized to 64 colours, which is 3.1 KB.

**Gmail strips `data:` URIs in `<img src>`.** Gmail readers see the alt text,
which is why the alt text is `SafeShade` and not `logo`. Once there is a domain,
upload the PNG to the public `avatars` bucket (or anywhere with an https URL)
and replace `EMBLEM_DATA_URI` in `functions/_shared/email/emblem.ts` with it.

## 7. Optional: send the auth emails through Resend too

Supabase's built-in SMTP is rate-limited hard (a handful of emails per hour) and
is explicitly not for production. To point it at Resend, go to
**Project Settings → Authentication → SMTP Settings** and enable custom SMTP:

| Field | Value |
|---|---|
| Host | `smtp.resend.com` |
| Port | `465` |
| Username | `resend` |
| Password | your Resend API key (the `re_...` value, used as the password) |
| Sender email | `onboarding@resend.dev`, or your verified domain address |
| Sender name | `SafeShade` |

Port 465 is implicit TLS. If your environment blocks it, Resend also accepts
587 with STARTTLS.

The same domain limitation from step 4 applies here: until a domain is
verified, sign-in emails only reach the Resend account owner.

---

## What is not done

Stated plainly, because a half-built thing that looks finished is how this
project has lost time before:

- **Nothing here has been executed.** No project exists, so the SQL has never
  been applied, the functions have never been deployed, and no email has ever
  been sent. Syntax and logic were reviewed by reading; that is not the same as
  a green run.
- **The app does not upload anything yet.** `SyncEngine` has `NoPayloadSource`
  installed, so the outbox queues records and the drain finds no bodies to send.
  Wiring the repositories in is Phase 2. A null payload is a *skip*, not a
  failure, precisely so the UI does not report failures that never happened.
- **Nothing pulls yet either.** `NoPullSource`. Writing rows back needs a merge
  rule per table — last-write-wins is fine for a zone's radius and completely
  wrong for an alert's outcome, where "dismissed by the guardian" must not be
  overwritten by a stale "pending" from another phone.
- **Deep links are registered but not handled.** The manifest has the
  `safeshade://login-callback` intent filter and `CloudClient.handleDeepLink`
  exists, but `MainActivity` does not call it. Until it does, the OTP and
  password sign-ins work and the OAuth-redirect path does not complete.
- **`vitals_samples` is an empty table for a feature that does not exist.** The
  deck marks HR / SpO₂ / temperature as *Planned* and the wearable's
  `HEALTH_CHAR` carries the Medical ID, not vitals. The table is there so the
  schema is not migrated the week the sensor lands. Do not read its existence as
  the feature shipping.
