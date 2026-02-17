-- ==========================================================
-- NOTIFICACIONES (Timeline) - CAFESITO APP
-- ==========================================================
-- Ejecuta este script en el SQL Editor de Supabase.
-- Crea la tabla, índices y políticas necesarias para
-- persistir notificaciones entre dispositivos y reinstalaciones.
-- Para push notifications vía Edge Function, usa:
--   docs/supabase/notifications_push.sql
-- ==========================================================

create table if not exists public.notifications_db (
    id bigserial primary key,
    user_id bigint not null,
    type text not null,
    from_username text not null,
    message text not null,
    timestamp bigint not null,
    is_read boolean not null default false,
    related_id text
);

create index if not exists idx_notifications_user_id
    on public.notifications_db (user_id);

create index if not exists idx_notifications_user_timestamp
    on public.notifications_db (user_id, timestamp desc);

create index if not exists idx_notifications_user_read
    on public.notifications_db (user_id, is_read);

alter table public.notifications_db enable row level security;

-- Ajusta estas políticas si usas otro esquema de autenticación.
-- Las notificaciones de follow/mención se insertan mediante una función SECURITY DEFINER.
do $$
begin
    if not exists (
        select 1
        from pg_policies
        where schemaname = 'public'
          and tablename = 'notifications_db'
          and policyname = 'Notifications are viewable by owner'
    ) then
        create policy "Notifications are viewable by owner"
            on public.notifications_db
            for select
            using (auth.uid()::text = user_id::text);
    end if;

    if not exists (
        select 1
        from pg_policies
        where schemaname = 'public'
          and tablename = 'notifications_db'
          and policyname = 'Notifications are insertable by authenticated users'
    ) then
        create policy "Notifications are insertable by authenticated users"
            on public.notifications_db
            for insert
            with check (auth.uid() is not null);
    end if;

    if not exists (
        select 1
        from pg_policies
        where schemaname = 'public'
          and tablename = 'notifications_db'
          and policyname = 'Notifications are updatable by owner'
    ) then
        create policy "Notifications are updatable by owner"
            on public.notifications_db
            for update
            using (auth.uid()::text = user_id::text);
    end if;

    if not exists (
        select 1
        from pg_policies
        where schemaname = 'public'
          and tablename = 'notifications_db'
          and policyname = 'Notifications are deletable by owner'
    ) then
        create policy "Notifications are deletable by owner"
            on public.notifications_db
            for delete
            using (auth.uid()::text = user_id::text);
    end if;
end $$;

-- ==========================================================
-- RPC segura para crear notificaciones desde el cliente
-- ==========================================================
create or replace function public.create_notification(
    p_user_id bigint,
    p_type text,
    p_from_username text,
    p_message text,
    p_timestamp bigint,
    p_related_id text default null
) returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.notifications_db (
        user_id,
        type,
        from_username,
        message,
        timestamp,
        related_id
    ) values (
        p_user_id,
        p_type,
        p_from_username,
        p_message,
        p_timestamp,
        p_related_id
    );
end;
$$;

grant execute on function public.create_notification(
    bigint, text, text, text, bigint, text
) to authenticated;

-- ==========================================================
-- RPCs de lectura/actualización/borrado para evitar bloqueos
-- por RLS cuando se usa un user_id numérico de la app.
-- ==========================================================
create or replace function public.get_notifications_for_user(
    p_user_id bigint
) returns setof public.notifications_db
language sql
security definer
set search_path = public
as $$
    select *
    from public.notifications_db
    where user_id = p_user_id
    order by timestamp desc;
$$;

create or replace function public.mark_notification_read(
    p_notification_id bigint
) returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    update public.notifications_db
    set is_read = true
    where id = p_notification_id;
end;
$$;

create or replace function public.mark_all_notifications_read(
    p_user_id bigint
) returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    update public.notifications_db
    set is_read = true
    where user_id = p_user_id;
end;
$$;

create or replace function public.delete_notification(
    p_notification_id bigint
) returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    delete from public.notifications_db
    where id = p_notification_id;
end;
$$;

grant execute on function public.get_notifications_for_user(bigint) to authenticated;
grant execute on function public.mark_notification_read(bigint) to authenticated;
grant execute on function public.mark_all_notifications_read(bigint) to authenticated;
grant execute on function public.delete_notification(bigint) to authenticated;


-- ==========================================================
-- Endurecimiento recomendado (opcional):
-- restringe acceso directo de tablas y fuerza uso de RLS/RPC.
-- ==========================================================
revoke all on table public.notifications_db from anon;
revoke all on table public.notifications_db from authenticated;
grant select, insert, update, delete on table public.notifications_db to authenticated;


-- ==========================================================
-- FIXES recomendados para incidencias en producción
-- ==========================================================
-- 1) Evitar ambigüedad de RPC create_notification por sobrecarga
--    (integer vs bigint). Conservamos la versión bigint.
drop function if exists public.create_notification(integer, text, text, text, bigint, text);

-- 2) Si no tienes pg_net disponible, desactiva trigger de push en DB
--    para que no bloquee inserts en notifications_db.
--    (El push puede mantenerse vía Edge Functions / backend separado).
do $$
begin
    if not exists (select 1 from pg_extension where extname = 'pg_net') then
        drop trigger if exists notify_fcm_on_notification_insert on public.notifications_db;
    end if;
end $$;


-- 3) Garantizar claves únicas para upsert de tokens FCM
--    Primero limpiamos duplicados existentes para evitar error 23505.
--    Conservamos la fila más reciente (id mayor) por cada clave.
with ranked_by_user as (
    select id,
           row_number() over (partition by user_id order by id desc) as rn
    from public.user_fcm_tokens
),
to_delete_user as (
    select id from ranked_by_user where rn > 1
)
delete from public.user_fcm_tokens
where id in (select id from to_delete_user);

with ranked_by_token as (
    select id,
           row_number() over (partition by fcm_token order by id desc) as rn
    from public.user_fcm_tokens
),
to_delete_token as (
    select id from ranked_by_token where rn > 1
)
delete from public.user_fcm_tokens
where id in (select id from to_delete_token);

create unique index if not exists idx_user_fcm_tokens_token_unique
    on public.user_fcm_tokens (fcm_token);
create unique index if not exists idx_user_fcm_tokens_user_unique
    on public.user_fcm_tokens (user_id);
