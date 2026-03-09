-- =============================================================================
-- Ciclo de vida de eliminación de cuenta (30 días)
-- Ejecutar en Supabase SQL Editor (Dashboard → SQL Editor).
-- =============================================================================

-- 1) Columnas en users_db
-- -----------------------
alter table if exists public.users_db
  add column if not exists account_status text default 'active';

alter table if exists public.users_db
  add column if not exists deactivation_requested_at bigint;

alter table if exists public.users_db
  add column if not exists scheduled_deletion_at bigint;

comment on column public.users_db.account_status is 'active | inactive_pending_deletion';
comment on column public.users_db.deactivation_requested_at is 'Unix timestamp ms cuando el usuario solicitó la baja';
comment on column public.users_db.scheduled_deletion_at is 'Unix timestamp ms a partir del cual se puede borrar la cuenta';

-- Valores por defecto para filas existentes
update public.users_db
set account_status = coalesce(account_status, 'active')
where account_status is null;

-- 2) Índice para el job de borrado automático
-- -------------------------------------------
create index if not exists idx_users_db_pending_deletion
  on public.users_db (account_status, scheduled_deletion_at)
  where account_status = 'inactive_pending_deletion';

-- 3) Función: borrado en cascada de todos los datos de un usuario
-- --------------------------------------------------------------
-- Orden respetando FKs: tags de posts → comments, likes → posts → follows, notifications, etc. → user
create or replace function public.hard_delete_user_data(p_user_id bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  r record;
begin
  -- Tags de los posts del usuario
  delete from public.post_coffee_tags
  where post_id in (select id from public.posts_db where user_id = p_user_id);

  delete from public.comments_db where user_id = p_user_id;
  delete from public.likes_db where user_id = p_user_id;
  delete from public.local_favorites where user_id = p_user_id;
  delete from public.reviews_db where user_id = p_user_id;
  delete from public.coffee_sensory_profiles where user_id = p_user_id;
  delete from public.diary_entries where user_id = p_user_id;
  delete from public.pantry_items where user_id = p_user_id;
  delete from public.pantry_historical where user_id = p_user_id;
  delete from public.notifications_db where user_id = p_user_id;
  delete from public.follows where follower_id = p_user_id or followed_id = p_user_id;
  delete from public.posts_db where user_id = p_user_id;

  -- Tokens FCM si existen
  delete from public.user_fcm_tokens where user_id = p_user_id;

  delete from public.users_db where id = p_user_id;
end;
$$;

comment on function public.hard_delete_user_data(bigint) is 'Elimina en cascada todos los datos del usuario (posts, comentarios, favoritos, etc.) y finalmente el usuario. Usar para cuentas con scheduled_deletion_at vencido.';

-- 4) Función: procesar cuentas pendientes de borrado (scheduled_deletion_at <= ahora)
-- -----------------------------------------------------------------------------------
-- Devuelve el número de cuentas eliminadas. Ejecutar por cron o manualmente.
create or replace function public.process_pending_account_deletions()
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare
  now_ms bigint;
  deleted_count integer := 0;
  ids bigint[];
  uid bigint;
begin
  now_ms := (extract(epoch from now()) * 1000)::bigint;

  select array_agg(id) into ids
  from public.users_db
  where account_status = 'inactive_pending_deletion'
    and scheduled_deletion_at is not null
    and scheduled_deletion_at <= now_ms;

  if ids is not null then
    foreach uid in array ids loop
      perform public.hard_delete_user_data(uid);
      deleted_count := deleted_count + 1;
    end loop;
  end if;

  return deleted_count;
end;
$$;

comment on function public.process_pending_account_deletions() is 'Borra permanentemente las cuentas con estado inactive_pending_deletion y scheduled_deletion_at ya pasado. Devolver número de cuentas eliminadas.';

-- 5) Programar el borrado automático (elegir una opción)
-- ------------------------------------------------------
--
-- Opción A — pg_cron (Supabase Pro): en SQL Editor ejecuta:
--
--   select cron.schedule(
--     'process-pending-account-deletions',
--     '0 3 * * *',
--     $$select public.process_pending_account_deletions()$$
--   );
--
-- (Todos los días a las 03:00 UTC. Para ver jobs: select * from cron.job;)
--
-- Opción B — Edge Function + Cron de Supabase:
--   1. Desplegar la Edge Function en docs/supabase/edge-functions/process-pending-account-deletions/
--   2. Dashboard → Edge Functions → process-pending-account-deletions → Cron: añadir "0 3 * * *" (diario 03:00 UTC)
--
-- Opción C — Sin cron: las apps (Web y Android) ya ejecutan sync al iniciar sesión y borran
--   los datos en el cliente si scheduled_deletion_at <= ahora. Las cuentas se eliminan al
--   intentar login tras 30 días; para borrado automático en servidor sin login, usa A o B.
