-- Direct Share event logging (backend)
-- Estado: Sprint 2 (baseline)
-- Objetivo: persistir eventos share_* además de GA4/GTM para auditoría y métricas internas.

create table if not exists public.share_event_logs (
  id bigserial primary key,
  user_id integer null,
  platform text not null check (platform in ('android', 'web')),
  event_name text not null check (event_name in ('share_opened', 'share_target_shown', 'share_target_clicked', 'share_completed', 'share_failed')),
  origin_screen text null,
  content_type text null,
  target_type text null,
  target_id text null,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists idx_share_event_logs_created_at on public.share_event_logs (created_at desc);
create index if not exists idx_share_event_logs_event_name on public.share_event_logs (event_name);
create index if not exists idx_share_event_logs_platform on public.share_event_logs (platform);
create index if not exists idx_share_event_logs_user_id on public.share_event_logs (user_id);

alter table public.share_event_logs enable row level security;

-- Lectura solo service_role (para BI/server). Clientes no deben leer estos eventos.
drop policy if exists "share_event_logs_select_service_role" on public.share_event_logs;
create policy "share_event_logs_select_service_role"
on public.share_event_logs
for select
to service_role
using (true);

-- Inserción permitida para usuarios autenticados; user_id se normaliza en la RPC.
drop policy if exists "share_event_logs_insert_authenticated" on public.share_event_logs;
create policy "share_event_logs_insert_authenticated"
on public.share_event_logs
for insert
to authenticated
with check (true);

create or replace function public.log_share_event(
  p_event_name text,
  p_platform text,
  p_origin_screen text default null,
  p_content_type text default null,
  p_target_type text default null,
  p_target_id text default null,
  p_metadata jsonb default '{}'::jsonb
)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user_id integer;
  v_id bigint;
begin
  -- Si existe helper interno, lo usamos; si no, queda null (eventos anónimos permitidos).
  begin
    v_user_id := get_my_internal_id();
  exception when others then
    v_user_id := null;
  end;

  insert into public.share_event_logs (
    user_id,
    platform,
    event_name,
    origin_screen,
    content_type,
    target_type,
    target_id,
    metadata
  ) values (
    v_user_id,
    lower(trim(p_platform)),
    lower(trim(p_event_name)),
    nullif(trim(coalesce(p_origin_screen, '')), ''),
    nullif(trim(coalesce(p_content_type, '')), ''),
    nullif(trim(coalesce(p_target_type, '')), ''),
    nullif(trim(coalesce(p_target_id, '')), ''),
    coalesce(p_metadata, '{}'::jsonb)
  )
  returning id into v_id;

  return v_id;
end;
$$;

grant execute on function public.log_share_event(text, text, text, text, text, text, jsonb) to authenticated, service_role;
