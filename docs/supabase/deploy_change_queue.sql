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

-- Seguridad: RLS activado; sin políticas para anon/authenticated, solo el backend (service_role / Edge Functions) puede acceder.
alter table public.deploy_change_events enable row level security;

-- Políticas: no se crean políticas para anon ni authenticated, así que la tabla es invisible e inmodificable desde la API pública.
-- Las Edge Functions trigger-coffees-build y consume-deploy-changes usan SUPABASE_SERVICE_ROLE_KEY y omiten RLS.

-- Limpieza: la Edge Function consume-deploy-changes borra cada vez que se ejecuta
-- las filas con processed_at anterior a 7 días. Si quieres hacer limpieza manual
-- o con pg_cron (p. ej. cada semana), puedes usar:
--
-- delete from public.deploy_change_events
-- where processed_at is not null and processed_at < now() - interval '7 days';
