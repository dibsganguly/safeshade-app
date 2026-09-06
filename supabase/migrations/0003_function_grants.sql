-- ============================================================================
-- SafeShade Cloud - function grants, done properly
--
-- 0002 revoked EXECUTE from `anon` on the definer functions and the advisor
-- still listed them: Postgres grants EXECUTE on a new function to PUBLIC at
-- creation, and a revoke from one role does not touch the PUBLIC grant that
-- every role inherits. So: revoke from PUBLIC first, then grant back exactly
-- the roles that need each function.
--
-- After this, the advisor's remaining "signed-in users can execute SECURITY
-- DEFINER function" lines are intentional and are listed here so nobody
-- re-litigates them:
--   is_circle_member / is_circle_owner / is_circle_actor - called from every
--     RLS policy in the querying role's context; authenticated must execute.
--   accept_invite / delete_account / heatmap_in - the app's own RPCs; each
--     checks auth.uid() itself and does nothing for a null session.
-- ============================================================================

revoke all on function public.is_circle_member(uuid) from public, anon;
revoke all on function public.is_circle_owner(uuid) from public, anon;
revoke all on function public.is_circle_actor(uuid) from public, anon;
grant execute on function public.is_circle_member(uuid) to authenticated, service_role;
grant execute on function public.is_circle_owner(uuid) to authenticated, service_role;
grant execute on function public.is_circle_actor(uuid) to authenticated, service_role;

-- Trigger and cron functions: no API role at all.
revoke all on function public.handle_new_user() from public, anon, authenticated;
revoke all on function public.touch_updated_at() from public, anon, authenticated;
revoke all on function public.refresh_heatmap_cells() from public, anon, authenticated;
revoke all on function public.path_circle_id(text) from public, anon;
grant execute on function public.path_circle_id(text) to authenticated, service_role;

-- App RPCs: signed-in only. (Already revoked from public in 0001/0002; stated
-- again so this file is the complete picture.)
revoke all on function public.accept_invite(uuid) from public, anon;
revoke all on function public.delete_account() from public, anon;
revoke all on function public.heatmap_in(double precision, double precision, double precision, double precision) from public, anon;
grant execute on function public.accept_invite(uuid) to authenticated;
grant execute on function public.delete_account() to authenticated;
grant execute on function public.heatmap_in(double precision, double precision, double precision, double precision) to authenticated;
