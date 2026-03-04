-- Security Advisor fixes applied on 2026-03-02
-- Project: ubcxjmagimjhpsehqync
--
-- Notes:
-- 1) public.coffees_with_stats switched to security_invoker=true.
-- 2) Functions in public schema with mutable search_path now pin search_path=public.
-- 3) pg_net is installed in public but extrelocatable=false, so it cannot be moved to another schema.

alter view public.coffees_with_stats
set (security_invoker = true);

alter function public.get_coffee_recommendations(integer)
set search_path = public;

alter function public.get_my_internal_id()
set search_path = public;

alter function public.is_admin_user()
set search_path = public;

alter function public.set_created_at_if_null()
set search_path = public;

alter function public.set_push_notifications_ads_updated_at()
set search_path = public;

alter function public.set_users_db_updated_at()
set search_path = public;
