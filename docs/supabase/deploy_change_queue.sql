-- Cola de cambios para despliegue nocturno (web).
-- Ejecutar en Supabase SQL Editor.

create table if not exists public.deploy_change_events (
  id bigserial primary key,
  resource text not null,
  operation text not null check (operation in ('INSERT', 'UPDATE', 'DELETE', 'UNKNOWN')),
  payload jsonb,
  created_at timestamptz not null default now(),
  processed_at timestamptz
);

create index if not exists idx_deploy_change_events_pending
  on public.deploy_change_events (resource, processed_at, created_at desc);

create index if not exists idx_deploy_change_events_created_at
  on public.deploy_change_events (created_at desc);
