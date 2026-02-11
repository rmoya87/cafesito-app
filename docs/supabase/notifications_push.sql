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
-- - Usa SERVICE_ROLE_KEY como secreto en el header Authorization.
-- - El cuerpo incluye NEW.* con datos de notifications_db.
-- - Verifica que existan tokens:
--   select user_id, count(*) from public.user_fcm_tokens group by user_id;
-- ==========================================================

-- 1) Función que llama a la Edge Function
create or replace function public.notify_fcm_on_notification()
returns trigger
language plpgsql
security definer
as $$
declare
    response json;
begin
    -- Llama a la Edge Function con el registro insertado.
    -- Usa pg_net para ejecutar la llamada HTTP.
    response := (
        select net.http_post(
            url := 'https://<PROJECT_REF>.functions.supabase.co/send-notification'::text,
            headers := jsonb_build_object(
                'Content-Type', 'application/json',
                'Authorization', 'Bearer <SERVICE_ROLE_KEY>'
            )::jsonb,
            body := json_build_object(
                'record', row_to_json(NEW)
            )::text
        )
    );

    return NEW;
end;
$$;

-- 2) Trigger al insertar notificaciones
drop trigger if exists notify_fcm_on_notification_insert
    on public.notifications_db;

create trigger notify_fcm_on_notification_insert
after insert on public.notifications_db
for each row
execute function public.notify_fcm_on_notification();
