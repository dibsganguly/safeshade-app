-- ============================================================================
-- SafeShade Cloud - every account owns exactly one Circle
--
-- WHY THIS FILE EXISTS
--
-- `handle_new_user()` in 0001_init.sql inserts a `profiles` row and nothing
-- else. That leaves a freshly signed-in guardian with no circle, and every
-- circle-scoped row the phone wants to push carries a NOT NULL `circle_id`
-- that references `public.circles`. So without this file the first sync of a
-- new account fails on a foreign key, for every table, forever - and the
-- failure surfaces to the user as "this did not reach SafeShade Cloud", which
-- is true and completely unactionable.
--
-- The phone cannot fix it for itself either. `circle_members` is owner-write:
-- the one bootstrap clause in its INSERT policy lets a user add themselves as
-- `owner` only when they already own the `circles` row, and inserting the
-- `circles` row plus the membership row from the client would be two round
-- trips that can half-fail. One SECURITY DEFINER function does both or
-- neither.
--
-- THREE ENTRY POINTS, ONE BODY
--
--  * `bootstrap_circle_for(uid)` - the body. Not reachable from any API role.
--  * `handle_new_user()` - re-created to call it, so new sign-ups get a circle
--    inside the same transaction that creates their auth user.
--  * `ensure_own_circle()` - the rpc the app calls on every sign-in, which
--    covers accounts that were created before this file existed and any case
--    where the trigger did not run.
--
-- IDEMPOTENT, AND "EXACTLY ONE" IS ENFORCED BY LOOKUP, NOT BY A CONSTRAINT
--
-- A partial unique index on `circles (owner_id) where deleted_at is null`
-- would be stricter, but it would also make `accept_invite` on a second
-- circle - or any future "this person owns two households" - fail at the
-- database rather than at a product decision. The function looks for a live
-- owned circle first and returns it; that makes calling it on every sign-in
-- free.
--
-- BACKFILL
--
-- Existing `auth.users` rows get their circle here rather than waiting for the
-- rpc, so an account created during Phase 1 is whole the moment this file is
-- applied.
--
-- ADVISOR
--
-- This adds a seventh "signed-in users can execute a SECURITY DEFINER
-- function" line, for `ensure_own_circle`. It is intentional, for the same
-- reason as the six in 0003: the function reads `auth.uid()` itself and does
-- nothing at all for a request with no session.
-- ============================================================================

create or replace function public.bootstrap_circle_for(uid uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $fn$
declare
  existing uuid;
  new_id uuid;
  owner_name text;
begin
  if uid is null then
    return null;
  end if;

  select c.id into existing
  from public.circles c
  where c.owner_id = uid
    and c.deleted_at is null
  order by c.created_at
  limit 1;

  if existing is not null then
    -- The circle exists but the membership row might not, if a previous run
    -- was interrupted between the two inserts. Both halves are repaired.
    insert into public.circle_members (id, circle_id, user_id, role)
    values (gen_random_uuid(), existing, uid, 'owner')
    on conflict (circle_id, user_id) do update
      set role = 'owner',
          deleted_at = null,
          updated_at = now();
    return existing;
  end if;

  select coalesce(nullif(trim(p.display_name), ''), split_part(p.email, '@', 1))
    into owner_name
  from public.profiles p
  where p.id = uid;

  new_id := gen_random_uuid();

  insert into public.circles (id, name, owner_id)
  values (
    new_id,
    -- Named after the person, not "My Circle": the name appears in the invite
    -- email as "<inviter> added you to <circle name>", and "My Circle" there
    -- reads as a mistake.
    coalesce(nullif(owner_name, ''), 'SafeShade') || '''s Circle',
    uid
  );

  insert into public.circle_members (id, circle_id, user_id, role)
  values (gen_random_uuid(), new_id, uid, 'owner')
  on conflict (circle_id, user_id) do update
    set role = 'owner',
        deleted_at = null,
        updated_at = now();

  return new_id;
end;
$fn$;

-- ----------------------------------------------------------------------------
-- handle_new_user, re-created
--
-- Same profile insert as 0001, then the circle. The whole body is exception
-- guarded around the circle half on purpose: this runs inside the transaction
-- that creates the auth user, and a failure here would fail the sign-up. A
-- person who cannot create an account is a worse outcome than a person whose
-- circle is created a second later by `ensure_own_circle()` instead.
-- ----------------------------------------------------------------------------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $fn$
begin
  insert into public.profiles (id, email, display_name)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1))
  )
  on conflict (id) do nothing;

  begin
    perform public.bootstrap_circle_for(new.id);
  exception when others then
    raise notice 'circle bootstrap deferred for %', new.id;
  end;

  return new;
end;
$fn$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ----------------------------------------------------------------------------
-- ensure_own_circle()
--
-- What the app calls. Returns the caller's own circle id, creating it if this
-- account predates the trigger above. `auth.uid()` and nothing else, so the
-- worst a stolen session can do is create the circle it already has.
-- ----------------------------------------------------------------------------
create or replace function public.ensure_own_circle()
returns uuid
language plpgsql
security definer
set search_path = public
as $fn$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'not signed in';
  end if;

  -- A profile row is a foreign-key-free convenience, but the circle name is
  -- read off it, so make sure it exists for an account created before the
  -- trigger did.
  insert into public.profiles (id, email)
  select uid, u.email from auth.users u where u.id = uid
  on conflict (id) do nothing;

  return public.bootstrap_circle_for(uid);
end;
$fn$;

-- Postgres grants EXECUTE to PUBLIC at creation; revoking from `anon` alone
-- does nothing. See 0003.
revoke all on function public.bootstrap_circle_for(uuid) from public, anon, authenticated;
revoke all on function public.handle_new_user() from public, anon, authenticated;
revoke all on function public.ensure_own_circle() from public, anon;
grant execute on function public.ensure_own_circle() to authenticated;

-- ----------------------------------------------------------------------------
-- Backfill. Idempotent by construction - bootstrap_circle_for returns the
-- existing circle when there is one.
-- ----------------------------------------------------------------------------
do $bf$
declare
  u record;
begin
  for u in select id from auth.users loop
    perform public.bootstrap_circle_for(u.id);
  end loop;
end $bf$;
