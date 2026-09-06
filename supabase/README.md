# SafeShade Cloud — setting it up

Everything the app needs on the server side lives in this folder. The project
is **SafeShade App** (`qlgbxhlbzyykxagsvwzv`, ap-southeast-1); the schema is
applied, both edge functions are deployed, and `local.properties` points the app
at it. What has **not** happened is a single real email or a single
authenticated call: no user account exists yet, so every claim below about what
a signed-in caller sees is reasoned from the code and the policies, not observed.

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
| `migrations/0004_realtime_publication.sql` | Puts `alerts` and `messages` in the `supabase_realtime` publication. Without it a Postgres-changes subscription reports SUBSCRIBED and then delivers nothing, forever, with no error. |
| `functions/_shared/email/` | The branded templates (`.html`, the source of truth), the renderer, the Resend transport, and the generated `templates.ts`. |
| `auth-templates/` | **Generated.** The three sign-in emails, ready to paste into the dashboard, with every placeholder already a Supabase `{{ .X }}`. See §6. |
| `../tools/gen_email_templates.py` | Regenerates `templates.ts` and `auth-templates/` from the `.html` files. Run it after editing any of them. |

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
gated on the server-minted invite token. `CloudClient.rpc(function, args)` is
the way in, and `CircleActions.acceptInvite(token)` is the caller.

Related: reads use `is_circle_member`, writes use `is_circle_actor`, which
excludes `viewer`. That is what makes the viewer role mean anything.

### Every account owns exactly one Circle — `0005_bootstrap_circle.sql`

`0001`'s `handle_new_user()` trigger creates a `profiles` row and nothing else.
That is not enough to sync anything: `circle_id` is `not null` on every
circle-scoped table, so a freshly signed-in guardian with no circle fails its
first push on a foreign key, for every table, forever — and the app reports that
to them as "this did not reach SafeShade Cloud", which is true and completely
unactionable.

`0005` fixes it in three pieces sharing one body:

- **`bootstrap_circle_for(uid)`** — finds the user's live owned circle, or
  creates the `circles` row and the `owner` `circle_members` row together.
  Idempotent: called twice, the second call returns the first call's circle.
  Executable by no API role at all.
- **`handle_new_user()`**, re-created to call it, so a new sign-up gets its
  circle inside the transaction that creates the auth user. The circle half is
  exception-guarded on purpose — a person who cannot create an account is a
  worse outcome than a person whose circle is created a second later.
- **`ensure_own_circle()`** — the rpc the app calls. `authenticated` only
  (revoked from `public` first, then granted — see `0003`). It reads
  `auth.uid()` itself, so the worst a stolen session can do is create the circle
  it already has.

**What it does for an account that already existed:** the file ends with an
idempotent backfill over `auth.users`, so every account created before `0005`
has its circle the moment the migration is applied, without waiting for that
person to open the app. If one is somehow still missing, the app's first
`ensure_own_circle()` on sign-in creates it.

The app caches the returned circle id in its own DataStore file
(`safeshade_cloud`, key `circle_id_v1`) and clears it on sign-out along with
every pull cursor — a second account on the same phone must not inherit the
first one's family.

**Advisor:** this adds a **seventh** "signed-in users can execute a SECURITY
DEFINER function" line, for `ensure_own_circle`, alongside the six listed in
`0003`. It is intentional for the same reason they are. The only other line
`get_advisors` reports is *Leaked Password Protection Disabled*, which is a
dashboard toggle (Authentication → Providers → Email), not a schema fault.

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

**Both are already deployed** (version 1, `verify_jwt` on) -- they were pushed
through the Supabase MCP rather than the CLI, which is why there is still no
`supabase/config.toml` and why the CLI has never been linked to this project.
The MCP upload mirrors the repo tree, so `_shared/email/*.ts` sits beside the
function directory and the committed `../_shared/email/...` imports resolve
unchanged. Redeploying with the CLI would work the same way.

`SUPABASE_URL`, `SUPABASE_ANON_KEY` and `SUPABASE_SERVICE_ROLE_KEY` are injected
into every function by the platform — do not set them as secrets yourself.

**The templates are compiled in, not uploaded.** They used to be `.html` files
read at runtime with `Deno.readTextFile`, with a fallback to unbranded HTML if
the read failed -- which meant a deploy that dropped them still sent mail, and
only a `template load failed` line in **Edge Functions -> Logs** said why the
alert looked wrong. Nobody watches that.

`tools/gen_email_templates.py` now turns every `.html` in
`functions/_shared/email/` into string constants in `templates.ts`, which
`resend.ts` imports like any other module. **Edit the `.html` files -- they are
still the source of truth -- then run the generator and redeploy:**

```bash
python tools/gen_email_templates.py
```

A missing template is now a build failure rather than a silent downgrade, so
there is no `config.toml` `static_files` entry to add and no fallback path left
to reason about.


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

Supabase sends the sign-in emails itself, from its own templates, so they have
to be pasted in. **`auth-templates/` holds them ready to paste** -- whole
documents, layout and body already merged, emblem and accent already inlined,
and every placeholder already resolved to a Supabase Go-template variable.

Go to **Authentication -> Email Templates** and paste each file whole.

| Dashboard template | Paste this file | Supabase supplies |
|---|---|---|
| **Magic Link** | `auth-templates/magic-link-otp.html` | `{{ .Token }}` -- the six-digit code, which is what the app asks for first |
| **Magic Link** (alternative) | `auth-templates/magic-link.html` | `{{ .ConfirmationURL }}` -- a link instead, for reading mail on a laptop |
| **Confirm signup** | `auth-templates/confirm-signup.html` | `{{ .Token }}` -- a code, not a link. Read the next paragraph before "correcting" this. |

**Confirm signup carries a code, and that is not a mistake.** GoTrue's
`SendMagicLink` sends the **Confirm signup** template, not the Magic Link one,
when the address has no account yet -- one call, two templates, chosen by
whether the user already exists. The app calls `signInWith(OTP)` with
`createUser = true` and then shows a six-digit field, so *the first email any
new guardian ever receives is this one*. If it held a link they would be looking
at a button while the app waited for six digits, with the deep-link handler not
yet wired: stuck on the first screen, on the first try. `verifyOtp` accepts the
signup confirmation token exactly as it accepts a magic-link token, so
`verifyEmailOtp` in the app needs no change.

Nothing has to be edited after pasting. Do **not** hand-edit the files either:
they are generated, and an edit is lost the next time anybody runs

```bash
python tools/gen_email_templates.py
```

Edit `functions/_shared/email/layout.html` and the body file, then regenerate.

**Why this is generated rather than described.** The two placeholder syntaxes
look alike and behave completely differently. The source templates use
`{{code}}`, handled by `render.ts`; Supabase uses Go templates, `{{ .Token }}`.
A file pasted with the wrong one does not merely look odd -- Go reads
`{{code}}` as a call to a function it does not have and refuses to render the
template at all, and the dashboard preview does not make that obvious. The
generator resolves every placeholder and then **greps its own output for any
`{{` that is not a `{{ .X }}` form**, exiting non-zero rather than writing a
file that would send braces to a real person. It also strips the source files'
HTML comments, which discuss `{{code}}` in prose and would otherwise trip the
same trap.

**There is no invite template here, on purpose.** `invite.html` needs
`inviter_name`, `circle_name`, `role_label`, `role_description` and
`expires_at_label`, and Supabase Auth has no variable for any of them -- they
would only exist via `.Data` on an `inviteUserByEmail` call, which nothing in
SafeShade makes. Circle invitations go out through the `send-invite` edge
function and Resend, which renders `invite.html` with real values.

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

- **No email has ever been sent.** `RESEND_API_KEY` is not set on the project,
  so `sendEmail` returns `failed` with "RESEND_API_KEY is not set on this
  project" and writes that to `alert_deliveries` — correct behaviour, and also
  proof of nothing. Until a key is set and one message lands in an inbox, the
  whole email path is unverified.
- **No authenticated call has been made.** No user account exists on the
  project. Both functions have been booted (an anon JWT gets `send-alert-email`
  to its 404 and `send-invite` to its 401, which are their own authorisation
  checks answering correctly), but nothing has ever run as a real member of a
  real Circle, so the RLS-driven paths through them are unexercised.
- **Realtime has never delivered a row.** `alerts` and `messages` are in the
  publication and `CloudClient.changes` is implemented, but no subscription has
  ever been opened against the live project.
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
