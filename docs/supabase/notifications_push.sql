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
-- - Usa una key REAL del proyecto (preferible service_role).
-- - Usa SIEMPRE header Authorization en formato Bearer.
-- - Con pg_net actual, body debe enviarse como jsonb (no text).
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
        url := 'https://<PROJECT_REF>.functions.supabase.co/send-notification',
        body := jsonb_build_object(
            'record', to_jsonb(NEW)
        ),
        params := '{}'::jsonb,
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'apikey', '<SUPABASE_SERVICE_ROLE_KEY>',
            'Authorization', 'Bearer <SUPABASE_SERVICE_ROLE_KEY>'
        ),
        timeout_milliseconds := 10000
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
