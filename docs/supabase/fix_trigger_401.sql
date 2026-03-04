-- ==========================================================
-- CORREGIR 401/400: configurar trigger de push con tu proyecto
-- ==========================================================
-- Problemas típicos al llamar la Edge Function desde pg_net:
-- 1) 401 Missing authorization header -> falta Authorization: Bearer <KEY>
-- 2) 400 Bad Request -> headers duplicados/inválidos o endpoint incorrecto
-- 3) Trigger sin efecto -> firma antigua de net.http_post (body::text)
--
-- PASOS:
-- 1) Sustituye PROJECT_REF por el ID de tu proyecto.
-- 2) Sustituye TU_SERVICE_ROLE_KEY por la clave service_role real.
-- 3) Ejecuta este script en Supabase SQL Editor.
-- 4) Repite la prueba y revisa net._http_response (debe ser 200).
-- ==========================================================

create or replace function public.notify_fcm_on_notification()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    request_id bigint;
begin
    request_id := net.http_post(
        url := 'https://PROJECT_REF.functions.supabase.co/send-notification',
        body := jsonb_build_object('record', to_jsonb(NEW)),
        params := '{}'::jsonb,
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'apikey', 'TU_SERVICE_ROLE_KEY',
            'Authorization', 'Bearer TU_SERVICE_ROLE_KEY'
        ),
        timeout_milliseconds := 10000
    );

    return NEW;
end;
$$;
