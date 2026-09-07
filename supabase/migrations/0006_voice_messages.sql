-- ============================================================
-- 0006_voice_messages.sql
--
-- Voice notes on the Circle thread.
--
-- A voice note is a message, not a table of its own. The Talk thread and the
-- quick-message thread are one conversation, and a second table would mean two
-- orderings to reconcile for it - which is the sort of thing that is fine until
-- a guardian reads a reply before the question it answers.
--
-- So `messages` grows four columns and a `kind` discriminator. The audio itself
-- never enters Postgres: it is an `.m4a` in the private `voice` bucket at
-- `<circle_id>/<message_id>.m4a`, and the row carries the path.
--
-- ### `kind` is `not null default 'text'`
--
-- Every row already in the table is a text message, and the app encodes rows
-- with explicit nulls (see `PayloadResolver`) - so a nullable `kind` would
-- arrive as a literal null on every push and the discriminator would be
-- unreadable. The default fills the existing rows; the app sends 'text'
-- explicitly for every new one.
--
-- ### The bucket and its policies were created by 0001
--
-- `voice` (private), with SELECT for `is_circle_member` and INSERT for
-- `is_circle_actor`, both keyed on `path_circle_id(name)` - the circle id in the
-- object's first path segment. Both are re-stated here idempotently so this file
-- can be read on its own, and one policy is genuinely new:
--
-- **UPDATE.** The app uploads with `upsert = true`, which sends `x-upsert: true`
-- and updates `storage.objects` when the object already exists. Without an
-- UPDATE policy that retry is denied - and a retry is exactly the path a note
-- takes after a failed upload, which is the case that most needs to work.
-- ============================================================

alter table public.messages
  add column if not exists kind text not null default 'text',
  -- `<circle_id>/<message_id>.m4a` in the `voice` bucket. Null for text.
  add column if not exists audio_path text,
  add column if not exists duration_ms integer,
  -- A small array of 0..1 amplitudes, captured while recording. The phone keeps
  -- only the encoded .m4a and never the raw PCM, so the picture cannot be
  -- derived later - it travels with the row or it does not exist.
  add column if not exists waveform jsonb;

-- Idempotent: a CHECK constraint has no `if not exists`.
do $$
begin
  alter table public.messages
    add constraint messages_kind_check check (kind in ('text', 'voice'));
exception
  when duplicate_object then null;
end
$$;

comment on column public.messages.kind is
  'text | voice. A voice row carries audio_path and no text.';

-- ------------------------------------------------------------
-- Storage: the private `voice` bucket
-- ------------------------------------------------------------

insert into storage.buckets (id, name, public)
values ('voice', 'voice', false)
on conflict (id) do nothing;

drop policy if exists "voice is readable by its circle" on storage.objects;
create policy "voice is readable by its circle"
  on storage.objects for select
  using (
    bucket_id = 'voice'
    and public.is_circle_member(public.path_circle_id(name))
  );

drop policy if exists "voice is writable by its circle" on storage.objects;
create policy "voice is writable by its circle"
  on storage.objects for insert
  with check (
    bucket_id = 'voice'
    and public.is_circle_actor(public.path_circle_id(name))
  );

-- New in this migration. See the header: `upsert = true` on a retry is an
-- UPDATE, and a viewer must not be able to overwrite somebody's recording -
-- hence is_circle_ACTOR on both halves, matching the INSERT policy.
drop policy if exists "voice is replaceable by its circle" on storage.objects;
create policy "voice is replaceable by its circle"
  on storage.objects for update
  using (
    bucket_id = 'voice'
    and public.is_circle_actor(public.path_circle_id(name))
  )
  with check (
    bucket_id = 'voice'
    and public.is_circle_actor(public.path_circle_id(name))
  );
