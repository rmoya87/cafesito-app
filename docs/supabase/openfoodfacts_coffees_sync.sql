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
--    https://ubcxjmagimjhpsehqync.supabase.co/functions/v1/sync-openfoodfacts-coffees
--    Bearer <SERVICE_ROLE_KEY>
-- ==========================================================

-- Llamada manual (opcional) para ejecutar la sync al momento.
-- Tip debug: puedes limitar páginas para pruebas cambiando body a:
--   '{"max_pages": 2}'::jsonb
-- Si quieres que falle en vez de devolver éxito parcial ante error OFF:
--   '{"max_pages": 10, "stop_on_off_error": true}'::jsonb
select
  net.http_post(
    url := 'https://ubcxjmagimjhpsehqync.supabase.co/functions/v1/sync-openfoodfacts-coffees'::text,
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer <SERVICE_ROLE_KEY>'
    )::jsonb,
    body := '{}'::jsonb
  ) as request_id;

-- Programa semanal: lunes a las 03:00 UTC.
-- Cambia el cron si prefieres otro día/hora.
--
-- Nota:
-- Si aparece "schema cron does not exist", primero habilita pg_cron.
-- En algunos proyectos Supabase, pg_cron debe activarse desde Dashboard > Database > Extensions.
do $do$
declare
  job_sql text;
begin
  if exists (select 1 from pg_namespace where nspname = 'cron') then
    execute $unschedule$
      select cron.unschedule('weekly_openfoodfacts_coffees_sync')
      where exists (select 1 from cron.job where jobname = 'weekly_openfoodfacts_coffees_sync')
    $unschedule$;

    job_sql := $job$
      select net.http_post(
        url := 'https://ubcxjmagimjhpsehqync.supabase.co/functions/v1/sync-openfoodfacts-coffees'::text,
        headers := jsonb_build_object(
          'Content-Type', 'application/json',
          'Authorization', 'Bearer <SERVICE_ROLE_KEY>'
        )::jsonb,
        body := '{}'::jsonb
      )
    $job$;

    execute format(
      'select cron.schedule(%L, %L, %L)',
      'weekly_openfoodfacts_coffees_sync',
      '0 3 * * 1',
      job_sql
    );
  else
    raise notice 'pg_cron no está habilitado. Ejecuta: create extension if not exists pg_cron;';
  end if;
end
$do$;

-- Ver jobs activos (si pg_cron está habilitado)
-- select * from cron.job;

-- Ver ejecuciones
-- select * from cron.job_run_details order by start_time desc limit 20;
