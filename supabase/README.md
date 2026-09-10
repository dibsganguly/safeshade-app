# SafeShade Cloud — setting it up

Everything the app needs on the server side lives in this folder. The project
is **SafeShade App** (`qlgbxhlbzyykxagsvwzv`, ap-southeast-1); the schema is
applied through `0007`, all five edge functions are deployed, and
`local.properties` points the app at it. Mail does reach an inbox -- a Circle
invitation was delivered on 2026-09-06. What has **not** happened is a single
*authenticated* call to any function, because the dashboard Magic Link template
has never been pasted and the sign-in email therefore carries no token (see
§6). Read "What is not done" at the end before trusting any claim here about
what a signed-in caller sees.

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
| `functions/notify-joined/` | Tells a Circle's owners that somebody accepted their invitation. Takes `{ token }`. |
| `functions/send-account-deleted/` | Emails the account that a deletion was requested, immediately before `delete_account()` runs. Takes nothing at all. |
| `functions/weekly-report/` | The Monday summary for one Circle, as an infographic. Takes `{ circle_id }`. Its `report.ts` is pure and its `fixture.mjs` renders it with made-up data so the layout can be checked without a fall. |
| `migrations/0002_advisor_fixes.sql` | The security advisor's findings from `0001`, fixed. |
| `migrations/0003_function_grants.sql` | Execute grants on the `security definer` functions, revoked from `public` first. Its header lists the six advisor lines that are intentional. |
| `migrations/0004_realtime_publication.sql` | Puts `alerts` and `messages` in the `supabase_realtime` publication. Without it a Postgres-changes subscription reports SUBSCRIBED and then delivers nothing, forever, with no error. |
| `migrations/0005_bootstrap_circle.sql` | Every account owns exactly one Circle, from the moment it exists. See § "Every account owns exactly one Circle". |
| `migrations/0007_email_notifications.sql` | Everything the email system needs: the `notified_at` / `joined_notified_at` claim stamps that make the dedupe server-side, `invites.accepted_by`, `profiles.email_prefs` and its one writer `set_email_prefs`, `'skipped'` on `alert_deliveries.status`, and the pg_cron + pg_net + Vault dispatcher for the weekly report. |
| `migrations/0006_voice_messages.sql` | Voice notes on the Circle thread: `messages` gains `kind` (`text` or `voice`, `not null default 'text'`), `audio_path`, `duration_ms` and `waveform`. Re-states the private `voice` bucket and its circle-scoped policies idempotently, and **adds the UPDATE policy** they were missing — the app uploads with `upsert`, which is an UPDATE on a retry and was denied without it. |
| `functions/_shared/email/` | The branded templates (`.html`, the source of truth), the renderer, the Resend transport, and the generated `templates/*.ts` — one module per template, so a function ships only the emails it can send. |
| `auth-templates/` | **Generated.** The three sign-in emails, ready to paste into the dashboard, with every placeholder already a Supabase `{{ .X }}`. See §6. |
| `../tools/gen_email_templates.py` | Regenerates `templates/*.ts` and `auth-templates/` from the `.html` files. Run it after editing any of them. |

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
supabase functions deploy notify-joined
supabase functions deploy send-account-deleted
supabase functions deploy weekly-report
```

**All five are already deployed** (`verify_jwt` on) -- they were pushed through
the Supabase MCP rather than the CLI, which is why there is still no
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
`functions/_shared/email/` into **one TypeScript module per template** under
`functions/_shared/email/templates/`, each holding that template's body and its
subject line. A function imports the templates it can send, by name, so its
import list is also the list of emails it is capable of sending -- and
`send-account-deleted` no longer ships the fall alert and the whole weekly
report in its bundle.

The subject lives in the `.html` file's `<title>`, is lifted out by the
generator, and is read back through `subjectFor(template, vars)`. A function
that writes its own subject string is a function whose subject drifts from its
body.

**Edit the `.html` files -- they are still the source of truth -- then run the
generator and redeploy:**

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
documents, layout and body already merged, brand image URLs and accent already inlined,
and every placeholder already resolved to a Supabase Go-template variable.

Go to **Authentication -> Email Templates** and paste each file whole.

The dashboard's **Subject** box is a separate field and is **not** read from the
pasted file. Type the subject in by hand; `python tools/gen_email_templates.py`
prints this same table when it runs.

| Dashboard template | Paste this file | Subject to type | Supabase supplies |
|---|---|---|---|
| **Magic Link** | `auth-templates/magic-link-otp.html` | Your SafeShade sign-in code | `{{ .Token }}` -- the six-digit code, which is what the app asks for first |
| **Magic Link** (alternative) | `auth-templates/magic-link.html` | Sign in to SafeShade | `{{ .ConfirmationURL }}` -- a link instead, for reading mail on a laptop |
| **Confirm signup** | `auth-templates/confirm-signup.html` | Confirm your email for SafeShade | `{{ .Token }}` -- a code, not a link. Read the next paragraph before "correcting" this. |
| **Reset Password** | `auth-templates/reset-password.html` | Reset your SafeShade password | `{{ .ConfirmationURL }}` and `{{ .Token }}` -- both, because GoTrue populates both for a recovery |
| **Change Email Address** | `auth-templates/change-email.html` | Confirm your new email address for SafeShade | `{{ .ConfirmationURL }}`, `{{ .Email }}` (the current address) and `{{ .NewEmail }}` (supported in this template and no other) |
| **Reauthentication** | `auth-templates/reauthentication.html` | Your SafeShade confirmation code | `{{ .Token }}` only -- there is no confirmation URL for a reauthentication, which is why that file has no button |

**The last three cannot fire on SafeShade today, and that is deliberate.** Reset
Password and Reauthentication need a password sign-in, and the app offers a
six-digit code and Google (`signInWithPassword` exists on `CloudClient` with no
screen behind it); Change Email Address needs `updateUser({ email })`, which
nothing calls. They are generated and pasted anyway so that those three
dashboard slots are not still holding Supabase's unbranded defaults on the day
one of those flows ships -- which is the day nobody is looking at email design.

### THIS IS STILL BROKEN, AND IT BLOCKS SIGN-IN

**Paste the file's contents, not its path.** On 2026-09-07 a code email
delivered through Resend arrived with a body that read, in full,
`supabase/auth-templates/magic-link-otp.html`: the path had been pasted into
the dashboard's template body.

**It is still like that.** A sign-in email requested at 11:54 UTC on 2026-09-07
(Resend id `cf066206-d813-4a1a-b9de-7e9cb1c4b433`) was delivered with exactly
that one line as both its HTML and its text part. So the Magic Link email
currently carries **no token of any kind**, and nobody can sign in to this
project by email until somebody opens the dashboard and fixes it. Every
authenticated smoke test below is blocked on this one paste.

Open the file, select everything, copy, and paste that into **Message body**.
Set **Subject** from the table above. Check with a real request: the delivered
mail in Resend's log should show the SafeShade layout and a six-digit code, not
a file name.

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

---

## 6a. The notification emails, and the rules they obey

Five functions, and between them ten designs in
`functions/_shared/email/*.html`. Every one renders into the same `layout.html`
frame, redesigned on 2026-09-10 after the owner saw the first one in Gmail
and found it flat: a white card on a teal-tinted page, centred throughout. The
masthead is the hosted full logo alone -- emblem, wordmark and the two-colour
tagline ("Your Everything Safety Companion", as the logo sets it) already
combined in that one image; under it a 6px band coloured by what the message
is for (teal = a confirmation, amber = attention, trip red = an emergency,
and red appears on nothing routine) with an amber tail that is the logo's
dot. Each body opens with a small pill in the same colour naming the
occasion, then the heading. Codes sit in a charcoal box in teal digits;
routine buttons are teal pills with charcoal text, and the alert's button
stays charcoal so that the red is never something text has to be read on.
There is no logo above the footer, and the footer says why you received it,
that replies are not read, the address the owner supplied that day
(`SafeShade, Patia, OD, India | CQ5D-OTPM`, typed in exactly as given), and
`© 2026 SafeShade. All rights reserved.`

Archivo is loaded from Google Fonts for the clients that load web fonts (Apple
Mail, iOS, Outlook for Mac, Samsung); Gmail and Outlook for Windows do not,
and show the system face from the fallback stack. Gmail is what the owner
reads, so Gmail is what to judge the design in.

The subject lives in each file's `<title>` and is lifted into that template's
generated module, so the subject and the body cannot drift apart.

| Template | Sent by | Subject |
|---|---|---|
| `alert.html` | `send-alert-email` | *the headline*, e.g. "A fall was detected - Nani" |
| `invite.html` | `send-invite` | "{inviter} added you to {circle} on SafeShade" |
| `joined.html` | `notify-joined` | "{who} joined {circle} on SafeShade" |
| `account-deleted.html` | `send-account-deleted` | "A request to delete your SafeShade account" |
| `weekly-report.html` | `weekly-report` | "{circle}: your SafeShade week" |
| `otp.html` | Supabase Auth | "Your SafeShade sign-in code" |
| `magic-link.html` | Supabase Auth | "Sign in to SafeShade" |
| `reset-password.html` | Supabase Auth | "Reset your SafeShade password" |
| `change-email.html` | Supabase Auth | "Confirm your new email address for SafeShade" |
| `reauthentication.html` | Supabase Auth | "Your SafeShade confirmation code" |

### The dedupe is on the server, always

Three of these are triggered by a phone, and a phone retries. So
`send-alert-email` and `notify-joined` **claim** their row before they send
anything:

```sql
update alerts set notified_at = now() where id = $1 and notified_at is null
```

If that returns no row, another invocation already ran the send pass and this
one sends nothing -- it returns the existing `alert_deliveries` rows with
`already_notified: true`, so a caller that retried still learns who was reached
rather than being told "done". Resend's `Idempotency-Key` is the second layer,
not the first: trusting it alone would leave the *phone* deciding how many
emails a guardian gets.

`notified_at` means **the pass ran**. It never means anybody was reached.
`alert_deliveries` is the record of that, and it is still written from Resend's
answer and from nothing else.

`notify-joined` additionally requires that the caller *is* the person who
accepted the invitation -- `invites.accepted_by`, set by `accept_invite`.
Without that column, anybody who had seen an invite link could make the server
email an owner "X joined your Circle" at any moment, from SafeShade's own
address, saying something untrue.

### Preferences are read by the sender, not by the phone

`profiles.email_prefs` is a jsonb with four keys -- `alerts`, `circle`,
`weekly_report`, `account` -- and every sending function reads it immediately
before sending. A skipped recipient is reported as
`{ email, status: "skipped", reason: "preference" }`, is written to
`alert_deliveries` with `status = 'skipped'`, and appears in the alert email's
"Also notified" list as *alert emails switched off*. It is never counted as
delivered and never counted as a failure.

Two deliberate exceptions:

* **Invitations are not gated.** The person being invited usually has no
  SafeShade account, so there is no preference to read, and an invitation is a
  request somebody made to them by name rather than a notification.
* **Supabase Auth mail is not gated.** Sign-in codes, address confirmation and
  reauthentication are sign-in mechanics. Somebody who has switched everything
  off still has to be able to sign in, and Supabase sends them from its own
  templates without consulting this column anyway.

An address with **no profile row** -- a neighbour in `emergency_contacts`, a
GP's surgery -- is never skipped. They never opted out of anything, and
withholding a fall alert from them because a table lookup missed would be the
most expensive possible reading of an absent row.

The one writer is `set_email_prefs(jsonb)`: it validates that every key is known
and every value is a real boolean, merges rather than replaces, and returns what
it stored. The app writes through it and updates `CloudState.emailPreferences`
from the **answer**, so a switch is never drawn in a position the server has not
confirmed. Before the first read that field is null, and the settings page shows
dashes rather than switches.

### What the app calls, and from where

| Email | App call site |
|---|---|
| alert | `cloud/AlertEmailNotifier.kt`, hung off `SyncEngine.onPushed` -- after the alert row is accepted by the server, and only for `outcome = PENDING`. That filter is what stops a backfill emailing a family about months of old falls. |
| joined | `CircleManager.CircleActions.acceptInvite`, straight after the join is recorded. Logged, never surfaced: the join succeeded whether or not somebody else's inbox did. |
| account deleted | `CloudAuth.deleteAccount`, **before** `delete_account()` -- afterwards there is no address left to write to. Its failure is logged and the deletion proceeds. |
| weekly report | `CircleManager.CircleActions.sendWeeklyReportNow()`, and the Monday cron. |

**A known gap:** a crash between the alert row being pushed and the invoke
completing loses that email -- the row is on the server with `notified_at` still
null and nothing tries again. Closing it needs a server-side sweeper over
`alerts_circle_notified_idx`, which is why that index exists.

---

## 6b. The weekly report, and its schedule

`weekly-report` has two callers and checks them differently:

* a signed-in guardian's JWT says `role: authenticated`, and membership of the
  named circle is proven by a read through **their** client, where RLS decides;
* the Monday job presents the project's `service_role` key, whose JWT says
  `role: service_role`, and there is no user to check membership for.

`verify_jwt` is on, so the gateway has already verified the signature before the
function reads the claim.

The job is `public.dispatch_weekly_reports()`, run by pg_cron at **01:30 UTC on
Mondays, which is 07:00 IST**. Fixed IST rather than each Circle's local
morning, and that is a limitation rather than a choice: `profiles.locale` is a
*language*, no table on this schema carries a time zone, and inferring one from
a language is wrong for every country with more than one.

**No key appears in `0007` and none may ever be added to it.** The dispatcher
reads two secrets out of Supabase Vault **by name**. Create them once, from the
dashboard's SQL editor:

```sql
select vault.create_secret(
  'https://<ref>.supabase.co/functions/v1',
  'safeshade_functions_url',
  'Base URL for SafeShade edge functions'
);
select vault.create_secret(
  '<the service_role key from Project Settings -> API>',
  'safeshade_service_role_key',
  'Used only by dispatch_weekly_reports() over pg_net'
);
```

**Until both exist the Monday job does nothing** and writes a notice saying so.
That is the intended failure: a schedule that quietly stops is better than a key
in a migration. The in-app "send this week's report now" button does not go
through this path at all and works without either secret.

Check the job with
`select * from cron.job where jobname = 'safeshade-weekly-report';` and its
history in `cron.job_run_details`.

### The report itself

`weekly-report/report.ts` is pure -- no Supabase import -- so the layout can be
exercised without a project, a circle or a fall:

```bash
node supabase/functions/weekly-report/fixture.mjs out.html
```

writes a rendered week you can open in a browser, prints the `text/plain`
alternative, and fails if any element is wider than the 600px frame. Every width
in the infographic is a pixel computed against `CONTENT_WIDTH = 536`, because
Outlook renders through Word and ignores percentage widths on table cells often
enough that a percentage bar chart collapses or overflows. Nothing in it is an
image: the bars are coloured table cells, the day strip is seven coloured cells,
and the wearer discs are letters in a round-cornered cell -- so all of it
survives Gmail, and all of it appears in the plain-text part.

It counts only what the tables hold: `alerts`, `messages` and `zone_events`.
There is no "nights out of range" and no wellbeing score -- `device_sightings`
is empty and nothing writes a connectivity history, so a number for either would
be invented, and a number in a weekly report is read as a measurement. **A week
with nothing in it renders one sentence saying so**, not a grid of zeros: a
guardian who reads a chart of nothing every Monday learns to ignore it,
including on the Monday it is not empty.

### The images are hosted, because Gmail strips inlined ones

`layout.html` carries one image, from `functions/_shared/email/brand.ts`:
the full logo, alone, in the masthead. It is an https URL into the public
**`brand`** bucket (`migrations/0008_brand_bucket.sql`), not a `data:` URI:
Gmail strips `data:` in `<img src>`, and for a week every Gmail reader saw a
broken-image glyph and the alt text where the emblem should have been.
`emblem.png` stays in the bucket and in `brand.ts` for any future use, but the
layout does not reference it.

The files are in `supabase/brand/` — `masthead.png` (1200×336, shown at 600×168: the logo on its beige ground with the top corners rounded, because Gmail on Android repaints any light cell but never the pixels of an image), `emblem.png` (73×112, kept for future use)
and `logo.png` (480×292, shown at 200 wide in the masthead), both exported at twice their
display size from `docs/Logo/` after cropping to the artwork's bounding box
(the sources are 2000px canvases with the art in the middle). **Upload both
to the `brand` bucket from the dashboard** (Storage → brand → Upload); nothing
in the app writes there, and the bucket is readable by anyone, which is what an
email image has to be.

`tools/gen_email_templates.py` sends a HEAD request to both URLs and refuses
to write anything while either is missing, because a 404 in a masthead is
worse than the alt text was. `--skip-image-check` is for editing offline and
says so on the screen; do not deploy from a run that used it.

To change a logo: replace the PNG in `supabase/brand/`, upload it over the old
one (the bucket policy allows update), and redeploy nothing — the URL is the
same. Bump the file name only if a client's image cache must be beaten, and
then update `brand.ts`, regenerate, redeploy all five, and re-paste the auth
templates.

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
project has lost time before. Last checked 2026-09-07.

### Verified

- **Email delivery works.** `RESEND_API_KEY` is set, custom SMTP points Supabase
  Auth at Resend, and mail has been delivered to `dibsganguly@gmail.com` — a
  Circle invitation on 2026-09-06 and sign-in mail since.
- **The weekly report's layout, with data.** `node
  supabase/functions/weekly-report/fixture.mjs` renders the full infographic
  from a fixture week and asserts nothing exceeds the 600px frame; the rendered
  page was opened and read. The plain-text alternative carries the same numbers,
  the arrows included.
- **The weekly report's layout with nothing in it.** The same command then
  builds a week with no alerts, no messages and no zone events, and fails if it
  renders anything but the one sentence — no statistics strip, and nothing left
  unrendered. `{{^has_data}}` immediately followed by `{{#has_data}}` on the
  same key is the shape a section regex gets wrong, and the quiet week is the
  one most Circles will actually receive.
- **The generator's placeholder gate.** Every `auth-templates/*.html` was
  regenerated and scanned: every surviving `{{...}}` is a Supabase `{{ .X }}`
  form, and `.NewEmail` appears only in `change-email.html`, which is the only
  template Supabase supports it in.
- **The schema.** `0007` is applied. `alerts.notified_at`,
  `invites.accepted_by`, `invites.joined_notified_at`, `profiles.email_prefs`
  and the widened `alert_deliveries.status` all exist; `pg_net` is installed and
  `cron.job` holds `safeshade-weekly-report`.
- **The app.** `gradlew.bat assembleDebug` and `testDebugUnitTest` are green —
  285 unit tests, none failing.

### NOT verified, and why

- **No function has ever been invoked by a signed-in user.** This is the big
  one, and it has a single cause: the dashboard's Magic Link template still
  contains the literal string `supabase/auth-templates/magic-link-otp.html`
  instead of the template (see §6), so the sign-in email carries no token and no
  session can be obtained. Creating a second test account fails too — Resend's
  `onboarding@resend.dev` refuses every address except the account owner's, so
  GoTrue cannot send its confirmation email. **Paste the auth templates and the
  whole smoke test below becomes possible in five minutes.**
- **So: no live send of the alert, joined, account-deleted or weekly-report
  emails.** Their rendering is proven only through the fixture renderer and the
  generator; their *delivery* is unproven.
- **The Monday job has never run**, and cannot until the two Vault secrets in
  §6b exist.
- **The auth templates have not been pasted into the dashboard**, so what
  Supabase actually sends today is not what is in `auth-templates/`.
- **Nothing has read `profiles.email_prefs` in anger.** The round trip is unit
  tested (`EmailPrefsTest`) and `set_email_prefs` is applied, but no phone has
  written a preference and no function has skipped a recipient because of one.
- **`vitals_samples` is an empty table for a feature that does not exist.** The
  deck marks HR / SpO₂ / temperature as *Planned* and the wearable's
  `HEALTH_CHAR` carries the Medical ID, not vitals. The table is there so the
  schema is not migrated the week the sensor lands. Do not read its existence as
  the feature shipping.

### The smoke test to run once sign-in works

With a signed-in user's access token:

```bash
# 1. Give the Circle a deliverable recipient. send-alert-email excludes the
#    CALLER from the circle-member half, so with one account the owner's own
#    address has to arrive as an emergency contact.
#    (SQL editor)
insert into public.emergency_contacts
  (id, circle_id, name, email, priority, notify_by_email, notify_by_sms)
select gen_random_uuid(), id, 'Smoke test', '<the Resend account owner>', 1, true, false
from public.circles limit 1;

# 2. The four functions.
curl -X POST "$URL/functions/v1/send-alert-email"      -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" -d '{"alert_id":"<uuid>"}'
curl -X POST "$URL/functions/v1/weekly-report"         -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" -d '{"circle_id":"<uuid>"}'
curl -X POST "$URL/functions/v1/notify-joined"         -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" -d '{"token":"<an invite token this account accepted>"}'
curl -X POST "$URL/functions/v1/send-account-deleted"  -H "Authorization: Bearer $JWT" -H "Content-Type: application/json" -d '{}'
```

Then read the delivered HTML back in Resend and check it shows the SafeShade
layout — not a file path, not raw `{{mustache}}`. Re-run `send-alert-email` with
the same id: it must answer `already_notified: true` and send nothing.

**`send-account-deleted` emails a real person to say their account is being
deleted.** It does not delete anything by itself, but do not fire it at an
address that will be alarmed by it.

Afterwards, delete the smoke-test emergency contact and clear the stamps:

```sql
delete from public.emergency_contacts where name = 'Smoke test';
update public.alerts set notified_at = null where id = '<uuid>';
```
