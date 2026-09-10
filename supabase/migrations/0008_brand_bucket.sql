-- 0008: a public `brand` bucket for the images every email carries.
--
-- Gmail strips data: URIs in <img src>, so the emblem inlined into the email
-- layout never rendered for most readers (they saw the alt text "SafeShade").
-- The fix is a hosted https image, and this project has no domain, so the
-- images live in Supabase Storage. A dedicated bucket rather than `avatars`:
-- that one is user space, and a brand asset in it would be one delete away
-- from a broken masthead in every email.
--
-- Read is public (the bucket flag). Write is any signed-in user, like
-- `avatars`, which is what lets the owner upload from the dashboard while
-- nothing in the app ever writes here.
--
-- Idempotent, like every migration in this folder.

insert into storage.buckets (id, name, public)
values ('brand', 'brand', true)
on conflict (id) do update set public = excluded.public;

drop policy if exists "brand is writable by signed-in users" on storage.objects;
create policy "brand is writable by signed-in users"
  on storage.objects for insert
  with check (bucket_id = 'brand' and auth.uid() is not null);

drop policy if exists "brand is replaceable by signed-in users" on storage.objects;
create policy "brand is replaceable by signed-in users"
  on storage.objects for update
  using (bucket_id = 'brand' and auth.uid() is not null)
  with check (bucket_id = 'brand' and auth.uid() is not null);
