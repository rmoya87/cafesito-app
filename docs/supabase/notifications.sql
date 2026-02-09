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
-- Para permitir notificaciones generadas por otros usuarios (seguimientos/menciones),
-- se permite insert a cualquier usuario autenticado.
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
end $$;
