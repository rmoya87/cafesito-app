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
do $$
begin
  if exists (select 1 from pg_namespace where nspname = 'cron') then
    execute $q$
      select cron.unschedule('weekly_openfoodfacts_coffees_sync')
      where exists (select 1 from cron.job where jobname = 'weekly_openfoodfacts_coffees_sync')
    $q$;

    execute $q$
      select cron.schedule(
        'weekly_openfoodfacts_coffees_sync',
        '0 3 * * 1',
        $$
        select
          net.http_post(
            url := 'https://ubcxjmagimjhpsehqync.supabase.co/functions/v1/sync-openfoodfacts-coffees'::text,
            headers := jsonb_build_object(
              'Content-Type', 'application/json',
              'Authorization', 'Bearer <SERVICE_ROLE_KEY>'
            )::jsonb,
            body := '{}'::jsonb
          );
        $$
      )
    $q$;
  else
    raise notice 'pg_cron no está habilitado. Ejecuta: create extension if not exists pg_cron;';
  end if;
end $$;

-- Ver jobs activos (si pg_cron está habilitado)
-- select * from cron.job;

-- Ver ejecuciones
-- select * from cron.job_run_details order by start_time desc limit 20;
