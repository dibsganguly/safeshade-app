-- ============================================================================
-- SafeShade Cloud - advisor fixes after 0001_init
--
-- Everything here answers a line from `get_advisors` run against the live
-- project on 2026-09-07, immediately after 0001 was applied. Nothing changes
-- the data model.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. function_search_path_mutable
--    A function without a pinned search_path resolves unqualified names off the
--    caller's path. Both of these only reference qualified names, but pinning
--    is free and the linter is right to insist.
-- ---------------------------------------------------------------------------
alter function public.touch_updated_at() set search_path = public;
alter function public.path_circle_id(text) set search_path = public;

-- ---------------------------------------------------------------------------
-- 2. anon can execute SECURITY DEFINER functions
--    Supabase grants EXECUTE on new public-schema functions to anon and
--    authenticated by default privilege, so `revoke ... from public` in 0001
--    did not reach anon. None of these has any business being reachable by a
--    request with no session.
-- ---------------------------------------------------------------------------
revoke execute on function public.is_circle_member(uuid) from anon;
revoke execute on function public.is_circle_owner(uuid) from anon;
revoke execute on function public.is_circle_actor(uuid) from anon;
revoke execute on function public.accept_invite(uuid) from anon;
revoke execute on function public.delete_account() from anon;

-- Trigger functions are called by the trigger as the table owner; no API role
-- needs to call them directly.
revoke execute on function public.handle_new_user() from anon, authenticated;
revoke execute on function public.touch_updated_at() from anon, authenticated;

-- The refresh is pg_cron's job (it runs as postgres). Not an API.
revoke execute on function public.refresh_heatmap_cells() from anon, authenticated;

-- The is_circle_* helpers stay executable by `authenticated`: every RLS policy
-- calls them in the querying role's context, and revoking that would make
-- every circle-scoped read fail with "permission denied for function".

-- ---------------------------------------------------------------------------
-- 3. materialized_view_in_api
--    A materialized view has no RLS. Exposed directly it is readable by anyone
--    with the anon key. It is already privacy-safe by construction (1.1 km
--    cells, k >= 5), but the heat map is a Plus feature and the deck's
--    community layer, so it goes behind a signed-in function that also lets
--    the app ask for a bounding box instead of the whole planet.
-- ---------------------------------------------------------------------------
revoke select on public.heatmap_cells from anon, authenticated;

create or replace function public.heatmap_in(
  min_lat double precision,
  max_lat double precision,
  min_lon double precision,
  max_lon double precision
)
returns table (
  lat_cell numeric,
  lon_cell numeric,
  kind text,
  incidents bigint,
  last_at timestamptz
)
language sql
security definer
stable
set search_path = public
as $$
  select h.lat_cell, h.lon_cell, h.kind, h.incidents, h.last_at
  from public.heatmap_cells h
  where h.lat_cell between min_lat and max_lat
    and h.lon_cell between min_lon and max_lon
  limit 5000;
$$;

revoke all on function public.heatmap_in(double precision, double precision, double precision, double precision) from public, anon;
grant execute on function public.heatmap_in(double precision, double precision, double precision, double precision) to authenticated;

-- ---------------------------------------------------------------------------
-- 4. auth_rls_initplan
--    `auth.uid()` inline in a policy is re-evaluated per row; wrapped in a
--    scalar subquery it is evaluated once per statement. Same predicate, same
--    result, one evaluation.
-- ---------------------------------------------------------------------------
drop policy if exists "profiles are readable by their owner" on public.profiles;
create policy "profiles are readable by their owner"
  on public.profiles for select using ((select auth.uid()) = id);
drop policy if exists "profiles are inserted by their owner" on public.profiles;
create policy "profiles are inserted by their owner"
  on public.profiles for insert with check ((select auth.uid()) = id);
drop policy if exists "profiles are updated by their owner" on public.profiles;
create policy "profiles are updated by their owner"
  on public.profiles for update using ((select auth.uid()) = id);

drop policy if exists "circles are created by their owner" on public.circles;
create policy "circles are created by their owner"
  on public.circles for insert with check ((select auth.uid()) = owner_id);

drop policy if exists "members are added by an owner, or bootstrapped" on public.circle_members;
create policy "members are added by an owner, or bootstrapped"
  on public.circle_members for insert
  with check (
    public.is_circle_owner(circle_id)
    or (
      (select auth.uid()) = user_id
      and role = 'owner'
      and exists (
        select 1 from public.circles c
        where c.id = circle_id and c.owner_id = (select auth.uid())
      )
    )
  );

drop policy if exists "subscriptions are readable by their owner" on public.subscriptions;
create policy "subscriptions are readable by their owner"
  on public.subscriptions for select using ((select auth.uid()) = user_id);

drop policy if exists "sightings may be reported by any signed-in user" on public.device_sightings;
create policy "sightings may be reported by any signed-in user"
  on public.device_sightings for insert
  with check ((select auth.uid()) is not null);

-- ---------------------------------------------------------------------------
-- 5. unindexed_foreign_keys
--    Only the keys the app will actually filter on. A per-wearer read
--    ("this wearer's zones") and a per-alert read ("this alert's deliveries
--    and evidence") are the two shapes the screens use. The audit columns
--    (acknowledged_by, invited_by, author_id, reporter_id) are never a filter
--    and stay unindexed on purpose.
-- ---------------------------------------------------------------------------
create index if not exists alert_deliveries_alert_idx on public.alert_deliveries (alert_id);
create index if not exists evidence_alert_idx on public.evidence (alert_id);
create index if not exists alerts_wearer_idx on public.alerts (wearer_id);
create index if not exists devices_wearer_idx on public.devices (wearer_id);
create index if not exists medical_ids_wearer_idx on public.medical_ids (wearer_id);
create index if not exists emergency_contacts_wearer_idx on public.emergency_contacts (wearer_id);
create index if not exists messages_wearer_idx on public.messages (wearer_id);
create index if not exists zones_wearer_idx on public.zones (wearer_id);
create index if not exists zone_events_zone_idx on public.zone_events (zone_id);
create index if not exists vitals_samples_wearer_idx on public.vitals_samples (wearer_id);
create index if not exists wearers_user_idx on public.wearers (user_id);
