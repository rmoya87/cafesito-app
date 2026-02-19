-- ==========================================================
-- PUSH NOTIFICATIONS (FCM) - CAFESITO APP
-- ==========================================================
-- Ejecuta este script en el SQL Editor de Supabase.
-- Requiere una Edge Function desplegada llamada:
--   send-notification
-- que reciba el payload del trigger y envíe el push vía FCM.
-- Requiere la extensión pg_net habilitada:
--   create extension if not exists pg_net;
--
-- Configura el endpoint de la Edge Function:
--   https://<PROJECT_REF>.functions.supabase.co/send-notification
--
-- IMPORTANTE:
-- - Usa una key REAL del proyecto (anon o service_role).
-- - Para evitar el 401 "Missing authorization header", envía
--   ambos headers y usa la clave en formato Bearer.
-- ==========================================================

create or replace function public.notify_fcm_on_notification()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    response json;
begin
    response := (
        select net.http_post(
            url := 'https://<PROJECT_REF>.functions.supabase.co/send-notification'::text,
            headers := jsonb_build_object(
                'Content-Type', 'application/json',
                'apikey', '<SUPABASE_ANON_OR_SERVICE_ROLE_KEY>',
                'authorization', 'Bearer <SUPABASE_ANON_OR_SERVICE_ROLE_KEY>'
            ),
            body := json_build_object(
                'record', row_to_json(NEW)
            )::text
        )
    );

    return NEW;
end;
$$;

drop trigger if exists notify_fcm_on_notification_insert
    on public.notifications_db;

create trigger notify_fcm_on_notification_insert
after insert on public.notifications_db
for each row
execute function public.notify_fcm_on_notification();
