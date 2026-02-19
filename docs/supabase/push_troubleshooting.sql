-- ==========================================================
-- PUSH TROUBLESHOOTING - CAFESITO
-- ==========================================================
-- Ejecuta estos checks en SQL Editor cuando no aparezcan logs
-- en la Edge Function send-notification.

-- 1) Extensión necesaria para trigger HTTP
select extname
from pg_extension
where extname = 'pg_net';

-- 2) Confirmar que el trigger existe y está habilitado
select
    tg.tgname as trigger_name,
    c.relname as table_name,
    n.nspname as schema_name,
    tg.tgenabled as enabled
from pg_trigger tg
join pg_class c on c.oid = tg.tgrelid
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public'
  and c.relname = 'notifications_db'
  and tg.tgname = 'notify_fcm_on_notification_insert'
  and not tg.tgisinternal;

-- 3) Ver definición real de la función de trigger
select pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure);

-- 4) Comprobar que hay tokens para el usuario destino
select user_id, count(*) as token_count
from public.user_fcm_tokens
group by user_id
order by token_count desc;

-- 5) Insert de prueba para disparar push (ajusta user_id/username)
--    Si no hay logs en Edge Functions tras esto, el problema está
--    antes de la función (trigger/pg_net/url/auth).
-- insert into public.notifications_db (
--   user_id, type, from_username, message, timestamp, related_id
-- ) values (
--   123, 'FOLLOW', 'usuario_test', 'test push', extract(epoch from now())::bigint * 1000, null
-- );

-- 6) Verificar que NO quedaron placeholders en la función de trigger
--    (si aparecen <PROJECT_REF> o <SERVICE_ROLE_KEY>, hay que corregirla).
select pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) as fn_sql
where pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) like '%<PROJECT_REF>%'
   or pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) like '%<SERVICE_ROLE_KEY>%'
   or pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) like '%<SUPABASE_ANON_OR_SERVICE_ROLE_KEY>%';

-- 7) Inspeccionar respuestas HTTP de pg_net para ver si Supabase Functions
--    está devolviendo 401/404/500 y por eso no ves notificaciones.
--    (requiere permisos para leer esquema net)
select
    r.id,
    r.status_code,
    r.content_type,
    r.error_msg,
    r.created
from net._http_response r
order by r.created desc
limit 20;


-- 8) Ver cuerpo de las últimas respuestas (útil para 401)
select
    r.id,
    r.status_code,
    convert_from(r.content::bytea, 'utf8') as response_body,
    r.created
from net._http_response r
where r.status_code >= 400
order by r.created desc
limit 20;


-- 9) Verificar que la función actual sí contiene header authorization
select
    case
        when pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) ilike '%authorization%'
            then 'OK: authorization header presente'
        else 'ERROR: falta authorization header en notify_fcm_on_notification()'
    end as auth_header_check;
