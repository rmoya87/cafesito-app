-- ==========================================================
-- OPEN FOOD FACTS -> COFFEES (SYNC + CRON SEMANAL)
-- ==========================================================
-- 1) Despliega la Edge Function:
--    sync-openfoodfacts-coffees
--
-- 2) Activa extensiones (si no están):
--    create extension if not exists pg_net;
--    create extension if not exists pg_cron;
--
-- 3) Configura endpoint y service role:
--    https://<PROJECT_REF>.functions.supabase.co/sync-openfoodfacts-coffees
--    Bearer <SERVICE_ROLE_KEY>
-- ==========================================================

-- Llamada manual (opcional) para ejecutar la sync al momento.
select
  net.http_post(
    url := 'https://<PROJECT_REF>.functions.supabase.co/sync-openfoodfacts-coffees'::text,
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer <SERVICE_ROLE_KEY>'
    )::jsonb,
    body := '{}'::text
  ) as request_id;

-- Programa semanal: lunes a las 03:00 UTC.
-- Cambia el cron si prefieres otro día/hora.
select cron.unschedule('weekly_openfoodfacts_coffees_sync');

select cron.schedule(
  'weekly_openfoodfacts_coffees_sync',
  '0 3 * * 1',
  $$
  select
    net.http_post(
      url := 'https://<PROJECT_REF>.functions.supabase.co/sync-openfoodfacts-coffees'::text,
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer <SERVICE_ROLE_KEY>'
      )::jsonb,
      body := '{}'::text
    );
  $$
);

-- Ver jobs activos
-- select * from cron.job;

-- Ver ejecuciones
-- select * from cron.job_run_details order by start_time desc limit 20;
