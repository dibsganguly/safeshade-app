-- ============================================================================
-- SafeShade Cloud - initial schema
--
-- Run with:  supabase db push
-- or paste into the SQL editor in the Supabase dashboard. See supabase/README.md.
--
-- ---------------------------------------------------------------------------
-- The five rules this file follows everywhere, and why
-- ---------------------------------------------------------------------------
--
-- 1. IDS ARE MINTED BY THE CLIENT.
--    Every `id` is a uuid the phone generates before the row exists. That is
--    what makes offline-first possible at all: a fall raised in a basement has
--    to be referenceable by its evidence upload and its delivery records long
--    before any server has seen it. The one exception is `invites.token`, which
--    is minted here by gen_random_uuid() - a token the client can choose is a
--    token an attacker can choose, and knowing it is the whole authorisation
--    to join a circle.
--
-- 2. `circle_id` IS DENORMALISED ONTO EVERY CIRCLE-SCOPED ROW.
--    Not for query convenience. Every row-level-security policy is
--    `is_circle_member(circle_id)`, and a policy that had to join to find the
--    circle would run that join on every row of every read.
--
-- 3. `updated_at` IS MAINTAINED BY A TRIGGER, NOT BY THE CLIENT.
--    It is the entire pull protocol: a phone asks for rows changed since its
--    cursor. A client-set timestamp is a client-set cursor, and a phone whose
--    clock is a minute fast would silently skip everything written in that
--    minute, forever.
--
-- 4. DELETES ARE SOFT.
--    A hard delete is invisible to a phone that has been offline for a week -
--    a row that stopped existing produces no change to pull, so the deleted
--    safe zone reappears on their map and never goes away. A `deleted_at`
--    tombstone is a change like any other.
--
-- 5. ROW LEVEL SECURITY IS ENABLED ON EVERY SINGLE TABLE, EXPLICITLY.
--    This project has automatic RLS OFF. A table created without
--    `enable row level security` is readable and writable by every
--    authenticated user of the project, with no error and no warning anywhere.
--    On a schema holding medical records, home addresses and the locations of
--    vulnerable people, that is the single most dangerous line that can be
--    left out. Every `create table` below is followed immediately by its
--    `alter table ... enable row level security`.
-- ============================================================================

create extension if not exists "pgcrypto";

-- ============================================================================
-- Helpers
-- ============================================================================

-- `updated_at` maintenance. See rule 3.
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- The authorisation predicate the whole schema rests on.
--
-- SECURITY DEFINER is required, not a shortcut: a policy on `circle_members`
-- that itself queries `circle_members` recurses and Postgres refuses it. Running
-- as the definer bypasses RLS *inside this function only*, and the function
-- reads nothing but the caller's own membership.
--
-- `set search_path = public` is what stops a caller creating a `circle_members`
-- table in a schema earlier on their own search path and having this function
-- read that instead. Without it, this function is a privilege-escalation hole.
--
-- STABLE lets the planner call it once per query rather than once per row.
create or replace function public.is_circle_member(c uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1
    from public.circle_members m
    where m.circle_id = c
      and m.user_id = auth.uid()
      and m.deleted_at is null
  );
$$;

-- Owner-only actions: inviting, removing members, deleting the circle.
create or replace function public.is_circle_owner(c uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1
    from public.circle_members m
    where m.circle_id = c
      and m.user_id = auth.uid()
      and m.role = 'owner'
      and m.deleted_at is null
  );
$$;

-- May *change* things in this circle, as opposed to merely seeing them.
--
-- This is what makes the `viewer` role mean anything. Without it every
-- circle-scoped write policy would be is_circle_member, and a viewer could edit
-- the Medical ID and tombstone the safe zones - while the app's own
-- documentation said they could "only look". A permission that is described but
-- not enforced is worse than no permission at all: it is a claim the code does
-- not keep.
create or replace function public.is_circle_actor(c uuid)
returns boolean
language sql
security definer
stable
set search_path = public
as $$
  select exists (
    select 1
    from public.circle_members m
    where m.circle_id = c
      and m.user_id = auth.uid()
      and m.role in ('owner', 'guardian')
      and m.deleted_at is null
  );
$$;

-- The circle id encoded in a storage object's first path segment, or null.
--
-- Storage policies are all evaluated against every object, not only against the
-- ones whose bucket they name, and Postgres does not guarantee that
-- `bucket_id = 'evidence' and ...` short-circuits. So an `avatars/me.png` upload
-- can have its name fed to the evidence policy's cast and fail the whole insert
-- with `invalid input syntax for type uuid`. Checking the shape first and
-- returning null makes that case simply false instead.
create or replace function public.path_circle_id(object_name text)
returns uuid
language sql
immutable
as $$
  select case
    when (storage.foldername(object_name))[1]
         ~ '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
    then ((storage.foldername(object_name))[1])::uuid
    else null
  end;
$$;

-- ============================================================================
-- profiles  (user-scoped)
-- ============================================================================

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text,
  display_name text,
  avatar_id text,
  role text check (role in ('guardian', 'companion')),
  phone text,
  locale text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

alter table public.profiles enable row level security;

drop policy if exists "profiles are readable by their owner" on public.profiles;
create policy "profiles are readable by their owner"
  on public.profiles for select using (auth.uid() = id);
drop policy if exists "profiles are inserted by their owner" on public.profiles;
create policy "profiles are inserted by their owner"
  on public.profiles for insert with check (auth.uid() = id);
drop policy if exists "profiles are updated by their owner" on public.profiles;
create policy "profiles are updated by their owner"
  on public.profiles for update using (auth.uid() = id);

-- A profile row is created for every new account by a trigger rather than by
-- the app. The app cannot be trusted to do it: the very first thing a new
-- account does is sync, and a sync that references a profile row that does not
-- exist yet fails a foreign key on a screen the user has not reached.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, display_name)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data ->> 'full_name', split_part(new.email, '@', 1))
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ============================================================================
-- circles
-- ============================================================================

create table if not exists public.circles (
  id uuid primary key,
  name text,
  owner_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

alter table public.circles enable row level security;

drop policy if exists "circles are readable by their members" on public.circles;
create policy "circles are readable by their members"
  on public.circles for select using (public.is_circle_member(id));
-- Insert is checked against owner_id rather than membership: at the moment a
-- circle is created there are no members yet, so is_circle_member would refuse
-- every first circle.
drop policy if exists "circles are created by their owner" on public.circles;
create policy "circles are created by their owner"
  on public.circles for insert with check (auth.uid() = owner_id);
drop policy if exists "circles are updated by their owner" on public.circles;
create policy "circles are updated by their owner"
  on public.circles for update using (public.is_circle_owner(id));

create index if not exists circles_owner_updated_idx
  on public.circles (owner_id, updated_at);

-- ============================================================================
-- circle_members
-- ============================================================================

create table if not exists public.circle_members (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  -- The authorisation model, in full. owner: invite and remove. guardian: act -
  -- acknowledge alerts, message, change settings. viewer: look only.
  role text not null default 'viewer' check (role in ('owner', 'guardian', 'viewer')),
  display_name text,
  -- The avatar gallery is shipped in the app, so this stores which one was
  -- picked, not a URL. Redrawing the artwork then is not a data migration.
  avatar_id text,
  joined_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  unique (circle_id, user_id)
);

alter table public.circle_members enable row level security;

drop policy if exists "members are readable by the circle" on public.circle_members;
create policy "members are readable by the circle"
  on public.circle_members for select using (public.is_circle_member(circle_id));

-- MEMBERSHIP IS OWNER-WRITE. There is exactly one narrow exception and it is
-- the bootstrap.
--
-- The obvious-looking `or auth.uid() = user_id` - "a person may add
-- themselves" - is a privilege escalation, and a subtle one: circle ids travel
-- in invite links and in every synced row, so anybody who has ever seen one,
-- including a member who was removed yesterday, could insert themselves back in
-- with role = 'owner'. "They need an id nobody publishes" is not an access
-- control.
--
-- The exception below only permits the very first row of a circle: you may add
-- YOURSELF, as OWNER, to a circle whose `owner_id` is already you - which only
-- the "circles are created by their owner" policy above could have arranged.
-- Every subsequent membership goes through an owner, or through
-- accept_invite() further down, which runs security definer and checks a
-- server-minted token.
drop policy if exists "members are added by an owner or by themselves"
  on public.circle_members;
drop policy if exists "members are added by an owner, or bootstrapped"
  on public.circle_members;
create policy "members are added by an owner, or bootstrapped"
  on public.circle_members for insert
  with check (
    public.is_circle_owner(circle_id)
    or (
      auth.uid() = user_id
      and role = 'owner'
      and exists (
        select 1 from public.circles c
        where c.id = circle_id and c.owner_id = auth.uid()
      )
    )
  );

-- UPDATE is owner-only, with no self exception at all.
--
-- With one, a viewer could set their own row's `role` to 'owner' - RLS gates
-- which rows you may update, not which columns, so "may edit my own row" means
-- "may promote myself". A member who wants to change their name or their avatar
-- edits `profiles`, which they already own and which carries both columns.
drop policy if exists "members are updated by an owner or by themselves"
  on public.circle_members;
drop policy if exists "members are updated by an owner" on public.circle_members;
create policy "members are updated by an owner"
  on public.circle_members for update
  using (public.is_circle_owner(circle_id));

create index if not exists circle_members_circle_updated_idx
  on public.circle_members (circle_id, updated_at);
create index if not exists circle_members_user_idx
  on public.circle_members (user_id);

-- ============================================================================
-- Circle-scoped tables
--
-- From here down every table has the same shape: client-minted id, a not-null
-- circle_id, updated_at, deleted_at, a (circle_id, updated_at) index, RLS on,
-- and three policies using is_circle_member. Repetitive on purpose - a table
-- that quietly differs from the others is a table whose policies nobody reads.
-- ============================================================================

create table if not exists public.wearers (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  name text,
  -- Optional: the elderly-parent case the product is built around is a
  -- guardian's phone and a wearable, with the wearer never opening the app.
  user_id uuid references auth.users(id) on delete set null,
  persona_mode text,
  date_of_birth date,
  avatar_id text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.devices (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  wearer_id uuid references public.wearers(id) on delete set null,
  name text,
  model text check (model in ('s1', '5g', 'spark')),
  ble_address text,
  serial text,
  firmware_version text,
  icon_type text,
  battery_percent int,
  last_seen_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.medical_ids (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  wearer_id uuid references public.wearers(id) on delete cascade,
  -- Field order matches data/Models.kt MedicalId, which matches the eleven
  -- positional fields the firmware parses off HEALTH_CHAR. Three
  -- representations of one record; keeping the order identical is the cheapest
  -- way to notice when one of them gains a field the others have not.
  blood_type text,
  emergency_contact text,
  contact_name text,
  allergies text,
  age int,
  conditions text,
  medications text,
  secondary_contact_name text,
  secondary_contact text,
  organ_donor boolean default false,
  notes text,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.emergency_contacts (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  wearer_id uuid references public.wearers(id) on delete cascade,
  name text,
  -- Stored exactly as it was typed. A number silently "corrected" is a number
  -- nobody can audit; this project already has one contact carrying eleven
  -- digits after the country code with no way to tell which is the extra one.
  phone text,
  email text,
  relationship text,
  priority int default 0,
  notify_by_sms boolean default true,
  notify_by_email boolean default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.alerts (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  wearer_id uuid references public.wearers(id) on delete set null,
  device_id uuid references public.devices(id) on delete set null,
  -- Enum *names*, never ordinals. PersonaMode gained AUTO at index 0 once and
  -- every ordinal after it moved; a stored ordinal would have silently
  -- re-pointed every historical alert at the wrong kind.
  kind text check (kind in (
    'FALL', 'SOS', 'PHONE_SOS', 'MISSED_CHECKIN', 'ZONE_EXIT', 'JOURNEY_OVERDUE'
  )),
  outcome text check (outcome in (
    'PENDING', 'DISMISSED', 'CONTACTED', 'AUTO_RESOLVED'
  )),
  occurred_at timestamptz,
  lat double precision,
  lon double precision,
  location_label text,
  note text,
  was_emergency_contacted boolean default false,
  acknowledged_by uuid references auth.users(id) on delete set null,
  acknowledged_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

-- The delivery ledger. Written ONLY by the edge function, under the service
-- role - never by a phone. Nothing on the device can honestly draw a delivery
-- receipt (SMS is handed to the radio with null sent/delivery intents, and BLE
-- writes have no application-level ack), so this table is where the app finally
-- gets to tell the truth about whether anybody was actually reached.
create table if not exists public.alert_deliveries (
  id uuid primary key default gen_random_uuid(),
  circle_id uuid not null references public.circles(id) on delete cascade,
  alert_id uuid not null references public.alerts(id) on delete cascade,
  recipient text,
  channel text check (channel in ('email', 'sms', 'push')),
  -- 'unknown' is not laziness. It is the genuinely ambiguous case: the request
  -- left the function and the call threw after the bytes went out. Rendering it
  -- as either success or failure would be a guess presented as a fact.
  status text not null default 'queued'
    check (status in ('queued', 'sent', 'failed', 'unknown')),
  error text,
  provider_id text,
  sent_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.messages (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  wearer_id uuid references public.wearers(id) on delete set null,
  author_id uuid references auth.users(id) on delete set null,
  text text,
  -- Direction is explicit and not derived. On the BLE side the two directions
  -- are two different characteristics with two different firmware handlers, and
  -- getting them the wrong way round makes the wearable buzz at its own wearer
  -- with their own reply. A message replayed onto a fresh phone must not lose
  -- which way it was going.
  from_guardian boolean default true,
  channel text check (channel in ('BLE', 'SMS', 'CLOUD')),
  sent_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

-- Nothing writes this yet. The deck marks HR / SpO2 / temperature as *Planned*;
-- HEALTH_CHAR on the wearable carries the Medical ID, not vitals. The table
-- exists so the schema is not migrated the week the sensor lands. Do not read
-- its existence as the feature shipping.
create table if not exists public.vitals_samples (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  wearer_id uuid references public.wearers(id) on delete cascade,
  device_id uuid references public.devices(id) on delete set null,
  measured_at timestamptz,
  heart_rate_bpm int,
  spo2_percent int,
  body_temp_c double precision,
  ambient_db double precision,
  battery_percent int,
  -- So a stubbed sample can never be mistaken for a measured one.
  source text check (source in ('device', 'phone', 'simulated')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.evidence (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  alert_id uuid references public.alerts(id) on delete set null,
  wearer_id uuid references public.wearers(id) on delete set null,
  kind text check (kind in ('audio', 'photo', 'note')),
  bucket text,
  storage_path text,
  content_type text,
  byte_size bigint,
  duration_seconds int,
  captured_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.zones (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  wearer_id uuid references public.wearers(id) on delete set null,
  name text,
  lat double precision,
  lon double precision,
  -- No default of 0: a zero-radius zone matches nothing and silently stops
  -- alerting, which is the worst possible failure for a safe zone.
  radius_meters double precision default 200,
  alert_on_exit boolean default true,
  alert_on_enter boolean default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.zone_events (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  zone_id uuid references public.zones(id) on delete set null,
  wearer_id uuid references public.wearers(id) on delete set null,
  kind text check (kind in ('enter', 'exit')),
  occurred_at timestamptz,
  lat double precision,
  lon double precision,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table if not exists public.smart_home_hooks (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  name text,
  trigger text check (trigger in (
    'fall', 'sos', 'zone_exit', 'zone_enter', 'low_battery', 'check_in_missed'
  )),
  provider text check (provider in ('webhook', 'home_assistant', 'ifttt', 'matter')),
  endpoint_url text,
  -- Signs the outbound call. Readable only by this circle, and never logged.
  secret text,
  enabled boolean default true,
  last_fired_at timestamptz,
  last_error text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

-- RLS, indexes and triggers for every circle-scoped table above, in one loop so
-- that adding a table to the list is the whole change and a table cannot be
-- half-protected by an oversight.
do $$
declare
  t text;
  circle_tables text[] := array[
    'wearers', 'devices', 'medical_ids', 'emergency_contacts', 'alerts',
    'alert_deliveries', 'messages', 'vitals_samples', 'evidence', 'zones',
    'zone_events', 'smart_home_hooks'
  ];
begin
  foreach t in array circle_tables loop
    execute format('alter table public.%I enable row level security', t);

    execute format(
      'create index if not exists %I on public.%I (circle_id, updated_at)',
      t || '_circle_updated_idx', t
    );

    execute format('drop policy if exists "read by circle" on public.%I', t);
    execute format(
      'create policy "read by circle" on public.%I for select
         using (public.is_circle_member(circle_id))', t
    );

    -- Writes take is_circle_ACTOR, not is_circle_member. That one word is the
    -- whole of the `viewer` role: a viewer passes the select policy and fails
    -- these, so "can only look" is true in the database rather than only in the
    -- documentation.
    execute format('drop policy if exists "insert by circle" on public.%I', t);
    execute format(
      'create policy "insert by circle" on public.%I for insert
         with check (public.is_circle_actor(circle_id))', t
    );

    execute format('drop policy if exists "update by circle" on public.%I', t);
    execute format(
      'create policy "update by circle" on public.%I for update
         using (public.is_circle_actor(circle_id))', t
    );

    -- No DELETE policy anywhere, deliberately. Rule 4: deletes are soft, and a
    -- soft delete is an UPDATE. Omitting the policy makes a hard delete simply
    -- impossible rather than merely discouraged.

    execute format('drop trigger if exists touch_%I on public.%I', t, t);
    execute format(
      'create trigger touch_%I before update on public.%I
         for each row execute function public.touch_updated_at()', t, t
    );
  end loop;
end $$;

-- alert_deliveries is written by the service role, which bypasses RLS. The
-- circle-member INSERT policy above therefore never applies to the function -
-- it is there so that a phone cannot forge a "sent" record for an email nobody
-- received. Tighten it to read-only for clients:
drop policy if exists "insert by circle" on public.alert_deliveries;
drop policy if exists "update by circle" on public.alert_deliveries;

-- Triggers for the tables not in the loop.
drop trigger if exists touch_profiles on public.profiles;
create trigger touch_profiles before update on public.profiles
  for each row execute function public.touch_updated_at();

drop trigger if exists touch_circles on public.circles;
create trigger touch_circles before update on public.circles
  for each row execute function public.touch_updated_at();

drop trigger if exists touch_circle_members on public.circle_members;
create trigger touch_circle_members before update on public.circle_members
  for each row execute function public.touch_updated_at();

-- ============================================================================
-- subscriptions  (user-scoped)
--
-- Scoped to a person, not a circle: the deck sells Free / Plus / Pro to a
-- person, and a guardian who pays should keep their tier when they leave one
-- circle and join another.
-- ============================================================================

create table if not exists public.subscriptions (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  tier text not null default 'free' check (tier in ('free', 'plus', 'pro')),
  status text,
  renews_at timestamptz,
  provider text,
  provider_ref text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

alter table public.subscriptions enable row level security;

drop policy if exists "subscriptions are readable by their owner" on public.subscriptions;
create policy "subscriptions are readable by their owner"
  on public.subscriptions for select using (auth.uid() = user_id);
-- No insert or update policy for clients. A phone that could write its own
-- tier could give itself Pro, so this table is written by the billing webhook
-- under the service role and is read-only to everybody else.

create index if not exists subscriptions_user_updated_idx
  on public.subscriptions (user_id, updated_at);

drop trigger if exists touch_subscriptions on public.subscriptions;
create trigger touch_subscriptions before update on public.subscriptions
  for each row execute function public.touch_updated_at();

-- ============================================================================
-- invites  (owner-write)
-- ============================================================================

create table if not exists public.invites (
  id uuid primary key,
  circle_id uuid not null references public.circles(id) on delete cascade,
  email text,
  role text not null default 'guardian' check (role in ('owner', 'guardian', 'viewer')),
  -- Server-minted. See rule 1.
  token uuid not null default gen_random_uuid(),
  invited_by uuid references auth.users(id) on delete set null,
  accepted_at timestamptz,
  expires_at timestamptz not null default (now() + interval '14 days'),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  unique (token)
);

alter table public.invites enable row level security;

drop policy if exists "invites are readable by the circle" on public.invites;
create policy "invites are readable by the circle"
  on public.invites for select using (public.is_circle_member(circle_id));
drop policy if exists "invites are created by an owner" on public.invites;
create policy "invites are created by an owner"
  on public.invites for insert with check (public.is_circle_owner(circle_id));
drop policy if exists "invites are updated by an owner" on public.invites;
create policy "invites are updated by an owner"
  on public.invites for update using (public.is_circle_owner(circle_id));

-- ----------------------------------------------------------------------------
-- accept_invite(token)
--
-- The other half of "membership is owner-write". A person accepting an invite
-- cannot insert their own circle_members row - see the policies above for why
-- that would be an escalation - so acceptance goes through here, security
-- definer, gated on a token the database itself minted.
--
-- The token is the authorisation. It is a uuid the client never chose, it is
-- checked for expiry and for having not already been used, and the role comes
-- off the invite row rather than from the caller. So the worst somebody can do
-- with a guessed circle id is nothing at all.
--
-- Idempotent: accepting twice, or re-accepting after being removed, updates the
-- existing membership rather than failing on the (circle_id, user_id) unique
-- constraint. A person who taps an emailed link twice should not see an error.
--
-- NOTE: nothing calls this yet. CloudClient has no generic rpc() - deleteAccount
-- reaches postgrest directly - so Phase 2 either adds one or does the same.
-- ----------------------------------------------------------------------------
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
     set accepted_at = now()
   where id = inv.id;

  return inv.circle_id;
end;
$$;

revoke all on function public.accept_invite(uuid) from public;
grant execute on function public.accept_invite(uuid) to authenticated;

create index if not exists invites_circle_updated_idx
  on public.invites (circle_id, updated_at);

drop trigger if exists touch_invites on public.invites;
create trigger touch_invites before update on public.invites
  for each row execute function public.touch_updated_at();

-- ============================================================================
-- firmware_releases  (public read)
--
-- The one publicly readable table, and intentionally so: a device checking
-- whether it is out of date should not need a session. Nothing here is secret -
-- the images are signed, or they are not trustworthy whether or not the list is
-- private. Not circle-scoped: a release belongs to a product line.
-- ============================================================================

create table if not exists public.firmware_releases (
  id uuid primary key,
  model text check (model in ('s1', '5g', 'spark')),
  version text,
  -- Monotonic, so "newer" is a comparison and not a string parse.
  version_code int,
  storage_path text,
  sha256 text,
  byte_size bigint,
  release_notes text,
  mandatory boolean default false,
  published_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

alter table public.firmware_releases enable row level security;

drop policy if exists "firmware releases are readable by anyone" on public.firmware_releases;
create policy "firmware releases are readable by anyone"
  on public.firmware_releases for select using (true);
-- No write policy at all. Releases are published from the dashboard or CI under
-- the service role.

create index if not exists firmware_releases_model_idx
  on public.firmware_releases (model, version_code desc);

drop trigger if exists touch_firmware_releases on public.firmware_releases;
create trigger touch_firmware_releases before update on public.firmware_releases
  for each row execute function public.touch_updated_at();

-- ============================================================================
-- device_sightings  (the mesh relay, and the heat map)
--
-- Deliberately NOT circle-scoped, and it is the only table like that. The whole
-- feature is that a lost wearable is found because a stranger's SafeShade app
-- walked past it, so the insert policy cannot be is_circle_member. Reading a
-- sighting back IS circle-scoped, through a lookup on the device.
--
-- reporter_id is nullable and should usually stay null: the passer-by does not
-- need to be identified for the sighting to be useful, and identifying them
-- turns a helpful feature into a tracking network.
-- ============================================================================

create table if not exists public.device_sightings (
  id uuid primary key,
  device_id uuid references public.devices(id) on delete cascade,
  ble_address text,
  seen_at timestamptz not null default now(),
  lat double precision,
  lon double precision,
  accuracy_meters double precision,
  rssi int,
  reporter_id uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

alter table public.device_sightings enable row level security;

drop policy if exists "sightings are readable by the device's circle"
  on public.device_sightings;
create policy "sightings are readable by the device's circle"
  on public.device_sightings for select
  using (
    exists (
      select 1 from public.devices d
      where d.id = device_sightings.device_id
        and public.is_circle_member(d.circle_id)
    )
  );

-- Any signed-in phone may report a sighting. That is the feature.
drop policy if exists "sightings may be reported by any signed-in user"
  on public.device_sightings;
create policy "sightings may be reported by any signed-in user"
  on public.device_sightings for insert
  with check (auth.uid() is not null);

create index if not exists device_sightings_device_seen_idx
  on public.device_sightings (device_id, seen_at desc);
create index if not exists device_sightings_seen_idx
  on public.device_sightings (seen_at);

drop trigger if exists touch_device_sightings on public.device_sightings;
create trigger touch_device_sightings before update on public.device_sightings
  for each row execute function public.touch_updated_at();

-- ============================================================================
-- heatmap_cells  (materialized view)
--
-- The safety heat map. Two decisions make it publishable at all:
--
--  * round(lat, 2) is about a 1.1 km cell at the equator. Fine enough to show
--    which junction is dangerous, coarse enough that it is not an address.
--  * having count(*) >= 5 means no cell can ever be traced back to one
--    person's route. Without that floor, a heat map of a village is a map of
--    one wearer's walk to the shops.
--
-- Materialized, not a view: it aggregates the whole alert table, and computing
-- that on every map pan would be the most expensive query in the product.
-- ============================================================================

drop materialized view if exists public.heatmap_cells;
create materialized view public.heatmap_cells as
  select
    round(lat::numeric, 2) as lat_cell,
    round(lon::numeric, 2) as lon_cell,
    kind,
    count(*) as incidents,
    max(occurred_at) as last_at
  from public.alerts
  where deleted_at is null
    and lat is not null
    and lon is not null
  group by 1, 2, 3
  having count(*) >= 5;

-- REFRESH MATERIALIZED VIEW CONCURRENTLY requires a unique index on the view.
-- Without it the refresh takes an ACCESS EXCLUSIVE lock and every map read
-- blocks for its duration.
create unique index if not exists heatmap_cells_key_idx
  on public.heatmap_cells (lat_cell, lon_cell, kind);

create or replace function public.refresh_heatmap_cells()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  refresh materialized view concurrently public.heatmap_cells;
end;
$$;

-- Hourly refresh, if pg_cron is available.
--
-- pg_cron cannot be created by SQL on every Supabase plan - on some it must be
-- switched on in Dashboard -> Database -> Extensions first. So this whole block
-- is guarded: without pg_cron the migration still succeeds and the heat map is
-- simply stale until refresh_heatmap_cells() is called by something else.
-- IF THE HEAT MAP NEVER UPDATES, THIS IS THE FIRST THING TO CHECK.
do $$
begin
  begin
    create extension if not exists pg_cron;
  exception when others then
    raise notice 'pg_cron not available; enable it in the dashboard to schedule the heat map refresh';
  end;

  if exists (select 1 from pg_extension where extname = 'pg_cron') then
    perform cron.unschedule('safeshade-heatmap-refresh')
      where exists (select 1 from cron.job where jobname = 'safeshade-heatmap-refresh');
    perform cron.schedule(
      'safeshade-heatmap-refresh',
      '0 * * * *',
      $cron$ select public.refresh_heatmap_cells(); $cron$
    );
  end if;
end $$;

-- ============================================================================
-- delete_account()
--
-- Deleting an auth.users row needs the service role, and the service role must
-- never be in an APK. This runs SECURITY DEFINER and deletes auth.uid() and
-- only auth.uid() - so the worst a stolen session can do is delete its own
-- account, which it could do by asking anyway.
--
-- Everything else goes with it: profiles cascades from auth.users, circles
-- cascade from their owner, and every circle-scoped table cascades from its
-- circle.
-- ============================================================================

create or replace function public.delete_account()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  uid uuid := auth.uid();
begin
  if uid is null then
    raise exception 'not signed in';
  end if;
  delete from auth.users where id = uid;
end;
$$;

revoke all on function public.delete_account() from public;
grant execute on function public.delete_account() to authenticated;

-- ============================================================================
-- Storage buckets
--
-- evidence and voice are PRIVATE. What the app uploads there is fall-evidence
-- audio and photographs of where somebody collapsed; a public bucket URL is
-- guessable, permanent and un-revokable. They are read back through signed URLs
-- that expire.
--
-- firmware and avatars are public: an image the device fetches before it has a
-- session, and a face that appears next to a name.
-- ============================================================================

insert into storage.buckets (id, name, public)
values
  ('evidence', 'evidence', false),
  ('firmware', 'firmware', true),
  ('voice',    'voice',    false),
  ('avatars',  'avatars',  true)
on conflict (id) do nothing;

-- Private buckets: an object's first path segment is its circle id, so
-- membership of that circle is the whole access rule. e.g.
--   evidence/<circle_id>/<alert_id>/<uuid>.m4a
--
-- The cast goes through path_circle_id() rather than being written inline.
-- Every storage policy is evaluated against every object regardless of bucket,
-- and Postgres does not promise that `bucket_id = 'evidence' and ...`
-- short-circuits - so an inline `(storage.foldername(name))[1]::uuid` can be
-- handed `avatars/me.png` and fail an unrelated upload with
-- "invalid input syntax for type uuid". path_circle_id returns null for a
-- non-uuid segment, and is_circle_member(null) is false.
drop policy if exists "evidence is readable by its circle" on storage.objects;
create policy "evidence is readable by its circle"
  on storage.objects for select
  using (
    bucket_id = 'evidence'
    and public.is_circle_member(public.path_circle_id(name))
  );

drop policy if exists "evidence is writable by its circle" on storage.objects;
create policy "evidence is writable by its circle"
  on storage.objects for insert
  with check (
    bucket_id = 'evidence'
    and public.is_circle_actor(public.path_circle_id(name))
  );

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

-- Public buckets still need an INSERT policy; "public" only affects reads.
drop policy if exists "avatars are writable by signed-in users" on storage.objects;
create policy "avatars are writable by signed-in users"
  on storage.objects for insert
  with check (bucket_id = 'avatars' and auth.uid() is not null);

-- No client write policy for 'firmware'. Images are published under the
-- service role.
