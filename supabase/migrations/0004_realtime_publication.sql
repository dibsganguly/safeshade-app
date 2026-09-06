-- =============================================================================
-- 0004_realtime_publication.sql
--
-- Puts `public.alerts` and `public.messages` into the `supabase_realtime`
-- publication, which is the only thing that makes Postgres Changes fire for
-- them.
--
-- WHY THIS IS A MIGRATION AND NOT A SETTING SOMEBODY REMEMBERS
--
-- Realtime is not a per-table toggle in the client. A subscription to a table
-- outside the publication is accepted by the server, reports SUBSCRIBED, and
-- then simply never delivers a row. There is no error, nothing in the logs, and
-- nothing in the app to look at: a guardian's phone would sit on the Circle tab
-- looking calm while a fall alert lands in the database beside it. That failure
-- is invisible in exactly the situation the product exists for, so membership
-- belongs in version control next to the tables themselves.
--
-- WHY ONLY THESE TWO
--
-- handoff7 §6 item 5: Realtime is for `alerts` and `messages` only. Everything
-- else syncs through the outbox and the `updated_at` pull, which is cheaper, is
-- already written, and does not need a socket held open. A publication that
-- names every table would put every zone-radius edit and every vitals row on a
-- websocket to every phone in the Circle for no benefit, and `medical_ids` and
-- `evidence` are not data anybody should be streaming by default.
--
-- Realtime still applies row-level security to what it sends, so this does not
-- widen who can see what. It widens what is *published*, and RLS decides who
-- receives it.
--
-- REPLICA IDENTITY IS DELIBERATELY LEFT ALONE
--
-- The client filters on `circle_id=eq.<id>`, which Realtime evaluates against
-- the NEW record for INSERT and UPDATE. `REPLICA IDENTITY FULL` would only be
-- needed to receive the OLD record (or to filter DELETEs), and it makes every
-- update write the whole old row to the WAL. The app's Realtime consumer takes
-- the new row for INSERT and UPDATE and nothing else -- deletions arrive
-- through the pull as rows with `deleted_at` set. See `CloudClient.changes`.
--
-- IDEMPOTENT
--
-- `alter publication ... add table` errors if the table is already a member, so
-- membership is checked in `pg_publication_tables` first. Re-running this file
-- is a no-op, which matters because `0001_init.sql` was written to be re-run
-- and this one sits in the same folder.
-- =============================================================================

do $$
declare
  -- A publication declared FOR ALL TABLES cannot have tables added to it, and
  -- the attempt is an error rather than a no-op. Supabase's default publication
  -- is not one, but a project where somebody recreated it by hand might be, and
  -- failing the whole migration over a publication that already includes these
  -- tables would be a worse outcome than skipping.
  all_tables boolean;
begin
  if not exists (
    select 1 from pg_publication where pubname = 'supabase_realtime'
  ) then
    -- Nothing to add to. Creating it here would be creating Realtime's own
    -- object out from under it, so this reports and stops instead.
    raise notice 'publication supabase_realtime does not exist; nothing added';
    return;
  end if;

  select puballtables into all_tables
  from pg_publication
  where pubname = 'supabase_realtime';

  if all_tables then
    raise notice 'supabase_realtime is FOR ALL TABLES; alerts and messages are already published';
    return;
  end if;

  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'alerts'
  ) then
    alter publication supabase_realtime add table public.alerts;
  end if;

  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'messages'
  ) then
    alter publication supabase_realtime add table public.messages;
  end if;
end
$$;
