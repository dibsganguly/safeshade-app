-- ===========================================================================
-- 0007_email_notifications.sql
--
-- Everything the email system needs that 0001..0006 does not already have.
--
-- Five separate problems, one file:
--
--  1. SERVER-SIDE DEDUPE. Three of the new emails are triggered by a phone, and
--     a phone retries. `alerts.notified_at` and `invites.joined_notified_at`
--     are claim stamps: the sending function takes the stamp with a conditional
--     UPDATE before it sends anything, so a second invoke finds nothing to
--     claim and sends nothing. The claim is the dedupe. Resend's
--     Idempotency-Key is the second layer, not the first — trusting it alone
--     would mean the *phone* decides how many emails a guardian gets.
--
--     A stamp means "the send pass ran", never "somebody was reached".
--     `alert_deliveries` remains the only record of who was actually reached,
--     and it is still written from Resend's answer and from nothing else.
--
--  2. WHO ACCEPTED AN INVITE. `notify-joined` tells a Circle owner that
--     somebody joined. To do that honestly it has to verify that the caller IS
--     the person who joined, and `invites` recorded only *that* it was accepted,
--     never by whom. `accepted_by` closes that: without it any member of the
--     circle could make the server send "X joined" about somebody else.
--
--  3. EMAIL PREFERENCES. `profiles.email_prefs`, honoured by the sending
--     functions rather than by the phone. A preference the client enforces is a
--     preference that stops working the moment anything else calls the
--     function.
--
--  4. THE WEEKLY REPORT SCHEDULE. pg_cron + pg_net + Vault. No key appears in
--     this file: the secrets are created out of band with
--     `vault.create_secret(...)` and are referenced here BY NAME only.
--
--  5. A FOURTH DELIVERY OUTCOME. `alert_deliveries.status` gains 'skipped', so
--     "this person switched alert emails off" is recorded as its own thing
--     rather than being written down as a failure or, worse, as a send.
--
-- Idempotent throughout, in the style of 0001: `add column if not exists`,
-- `create or replace`, `drop policy if exists` before every `create policy`.
-- ===========================================================================


-- ---------------------------------------------------------------------------
-- 1. alerts.notified_at — the alert-email claim stamp
-- ---------------------------------------------------------------------------

alter table public.alerts
  add column if not exists notified_at timestamptz;

comment on column public.alerts.notified_at is
  'When send-alert-email claimed this alert and ran its send pass. Set by the '
  'edge function under the service role, never by a phone. Its presence means '
  'the pass ran, NOT that anybody was reached — alert_deliveries is the record '
  'of who was reached, and it is written from Resend''s answer only.';

-- The claim is `update ... where id = $1 and notified_at is null`, so the
-- planner wants the pk (it has it) and nothing else. This index is for the
-- opposite question, which the app and any future sweeper ask: which alerts in
-- this circle were never notified.
create index if not exists alerts_circle_notified_idx
  on public.alerts (circle_id, notified_at);


-- ---------------------------------------------------------------------------
-- 2. invites.accepted_by / joined_notified_at, and accept_invite recording it
-- ---------------------------------------------------------------------------

alter table public.invites
  add column if not exists accepted_by uuid references auth.users(id) on delete set null;

alter table public.invites
  add column if not exists joined_notified_at timestamptz;

comment on column public.invites.accepted_by is
  'The account that called accept_invite() on this row. notify-joined requires '
  'the caller to BE this person before it will tell the owner that somebody '
  'joined; without it, any member could make the server send that mail about '
  'somebody else.';

comment on column public.invites.joined_notified_at is
  'When notify-joined claimed this invite. See alerts.notified_at.';

-- Re-created only to record `accepted_by`. Everything else — the single
-- error message for expired/used/never-existed, the on-conflict revive, the
-- `security definer` and the search_path pin — is 0001's, unchanged.
--
-- `create or replace` keeps existing grants, but the revoke/grant pair is
-- re-stated the way 0001 and 0003 do it: a function whose grants depend on
-- which migrations have run in which order is a function nobody can reason
-- about from the files.
create or replace function public.accept_invite(p_token uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  inv public.invites;
begin
  if uid is null then
    raise exception 'not signed in';
  end if;

  select * into inv
  from public.invites
  where token = p_token
    and accepted_at is null
    and deleted_at is null
    and expires_at > now();

  if inv.id is null then
    -- One message for expired, used, and never-existed. Distinguishing them
    -- would turn this into a token oracle.
    raise exception 'this invitation is no longer valid';
  end if;

  insert into public.circle_members (id, circle_id, user_id, role)
  values (gen_random_uuid(), inv.circle_id, uid, inv.role)
  on conflict (circle_id, user_id) do update
    set role = excluded.role,
        deleted_at = null,
        updated_at = now();

  update public.invites
     set accepted_at = now(),
         accepted_by = uid
   where id = inv.id;

  return inv.circle_id;
end;
$$;

revoke all on function public.accept_invite(uuid) from public;
grant execute on function public.accept_invite(uuid) to authenticated;


-- ---------------------------------------------------------------------------
-- 3. profiles.email_prefs — which emails this person wants
-- ---------------------------------------------------------------------------
--
-- Four keys, and the split is by what the mail is FOR rather than by which
-- function sends it:
--
--   alerts        a fall or an SOS. Turning this off is a real decision and the
--                 settings page has to say so.
--   circle        somebody joined, an invitation was accepted, membership
--                 changed.
--   weekly_report the Monday summary.
--   account       account-level notices, such as a deletion request.
--
-- The Supabase Auth mails — sign-in codes, address confirmation,
-- reauthentication — are NOT gated by any of these. They are sign-in mechanics:
-- a person who has switched every notification off still has to be able to sign
-- in, and Supabase sends them anyway, from its own templates, without consulting
-- this column.
--
-- `not null default` rather than a nullable column: a null here would have to
-- be read as "unknown", and every sending function would need a rule for it.
-- Defaulting to all-on and letting the person turn things off is the only
-- reading that does not silently withhold a fall alert from somebody who never
-- opened the settings page.

alter table public.profiles
  add column if not exists email_prefs jsonb not null
  default '{"alerts": true, "circle": true, "weekly_report": true, "account": true}'::jsonb;

comment on column public.profiles.email_prefs is
  'Per-account email switches, read by every sending edge function before it '
  'sends. Keys: alerts, circle, weekly_report, account. A missing key reads as '
  'true. Supabase Auth mails are not gated by this — they are sign-in '
  'mechanics, not notifications.';

-- The one writer, and the reason it is a function rather than a PostgREST
-- update: this validates the shape. A settings page that PATCHes
-- `email_prefs` directly can store `{"alerts": "no"}` — a truthy string — and
-- the next fall alert is skipped for a reason nobody can see. Here, anything
-- that is not a JSON boolean is rejected, and the row that is actually stored
-- is returned so the client updates its state from the server's answer rather
-- than from what it hoped it sent.
create or replace function public.set_email_prefs(p_prefs jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
  merged jsonb;
  k text;
begin
  if uid is null then
    raise exception 'not signed in';
  end if;
  if p_prefs is null or jsonb_typeof(p_prefs) <> 'object' then
    raise exception 'email preferences must be a JSON object';
  end if;

  for k in select jsonb_object_keys(p_prefs) loop
    if k not in ('alerts', 'circle', 'weekly_report', 'account') then
      raise exception 'unknown email preference: %', k;
    end if;
    if jsonb_typeof(p_prefs -> k) <> 'boolean' then
      raise exception 'email preference % must be true or false', k;
    end if;
  end loop;

  -- Merge rather than replace: a client built against three keys must not
  -- silently reset a fourth one it has never heard of back to its default.
  update public.profiles
     set email_prefs = coalesce(email_prefs, '{}'::jsonb) || p_prefs,
         updated_at = now()
   where id = uid
  returning email_prefs into merged;

  if merged is null then
    raise exception 'no profile for this account';
  end if;

  return merged;
end;
$$;

revoke all on function public.set_email_prefs(jsonb) from public;
grant execute on function public.set_email_prefs(jsonb) to authenticated;


-- ---------------------------------------------------------------------------
-- 4. The weekly report schedule
-- ---------------------------------------------------------------------------
--
-- pg_cron is already installed on this project. pg_net is what lets a cron job
-- reach an edge function at all.

do $$
begin
  create extension if not exists pg_net with schema extensions;
exception when others then
  raise notice 'pg_net could not be created from SQL; enable it in '
               'Database -> Extensions or the weekly report will never be '
               'dispatched on a schedule. Everything else in 0007 is unaffected.';
end;
$$;

-- NO KEY APPEARS IN THIS FILE, AND NONE MAY EVER BE ADDED TO IT.
--
-- The dispatcher reads two secrets out of Supabase Vault by name:
--
--   safeshade_functions_url    e.g. https://<ref>.supabase.co/functions/v1
--   safeshade_service_role_key the project's service_role key
--
-- They are created once, out of band, with
--
--   select vault.create_secret('<value>', '<name>', '<description>');
--
-- run from the SQL editor or the MCP — never committed, never printed. If
-- either is missing the dispatcher logs a notice and does nothing: a schedule
-- that quietly stops is better than a key in a migration, and the "send this
-- week's report now" button in the app does not go through here at all.
--
-- The service_role key is what the function checks. `weekly-report` reads the
-- `role` claim out of the caller's JWT and takes the scheduled path only when
-- it is `service_role`; a signed-in guardian's JWT says `authenticated` and
-- takes the membership-checked path instead. That is one shared secret fewer
-- than a bespoke header, and it is the secret the platform already rotates.

create or replace function public.dispatch_weekly_reports()
returns int
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  base_url text;
  key text;
  c record;
  sent int := 0;
begin
  select decrypted_secret into base_url
    from vault.decrypted_secrets where name = 'safeshade_functions_url';
  select decrypted_secret into key
    from vault.decrypted_secrets where name = 'safeshade_service_role_key';

  if base_url is null or key is null then
    raise notice 'weekly report not dispatched: the Vault secrets '
                 'safeshade_functions_url and safeshade_service_role_key are '
                 'not both set on this project.';
    return 0;
  end if;

  -- One request per circle. Not one request for all of them: a circle whose
  -- report throws must not take the other circles' reports down with it, and
  -- the function's own response already reports per-recipient outcomes.
  for c in
    select id from public.circles where deleted_at is null
  loop
    perform net.http_post(
      url := base_url || '/weekly-report',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer ' || key
      ),
      body := jsonb_build_object('circle_id', c.id, 'scheduled', true),
      timeout_milliseconds := 20000
    );
    sent := sent + 1;
  end loop;

  return sent;
end;
$$;

-- Nobody calls this over the API. cron runs as the table owner.
revoke all on function public.dispatch_weekly_reports() from public;
revoke all on function public.dispatch_weekly_reports() from anon, authenticated;

-- Monday 01:30 UTC = Monday 07:00 IST.
--
-- Fixed IST, not per-circle local time, and that is a limitation rather than a
-- choice: `profiles.locale` exists but no table on this schema carries a time
-- zone, and inferring a zone from a locale is wrong for every country with more
-- than one. When a zone column lands, this becomes an hourly job that fires for
-- the circles whose local time is 07:00. Until then the README says 07:00 IST
-- and means it.
do $$
begin
  perform cron.unschedule('safeshade-weekly-report');
exception when others then
  null;  -- not scheduled yet, which is the normal case on a first apply
end;
$$;

do $$
begin
  perform cron.schedule(
    'safeshade-weekly-report',
    '30 1 * * 1',
    'select public.dispatch_weekly_reports()'
  );
exception when others then
  raise notice 'cron.schedule failed; the weekly report has no schedule. The '
               'in-app "send this week''s report now" button is unaffected.';
end;
$$;


-- ---------------------------------------------------------------------------
-- 5. alert_deliveries.status gains 'skipped'
-- ---------------------------------------------------------------------------
--
-- Somebody who has switched alert emails off did not receive the alert email.
-- That is a fact about who was reached, and this table exists to hold exactly
-- those facts. Without a value for it the row would have to be written as
-- `failed` — which would say Resend refused, when Resend was never asked — or
-- not written at all, which would make the person invisible in the ledger and
-- in the "Also notified" list.
--
-- 0001's four values stay as they were; this only widens the CHECK.
alter table public.alert_deliveries
  drop constraint if exists alert_deliveries_status_check;

alter table public.alert_deliveries
  add constraint alert_deliveries_status_check
  check (status in ('queued', 'sent', 'failed', 'unknown', 'skipped'));
