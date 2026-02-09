-- ==========================================================
-- NOTIFICACIONES (Timeline) - CAFESITO APP
-- ==========================================================
-- Ejecuta este script en el SQL Editor de Supabase.
-- Crea la tabla, índices y políticas necesarias para
-- persistir notificaciones entre dispositivos y reinstalaciones.
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
