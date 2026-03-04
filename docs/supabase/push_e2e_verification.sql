-- ==========================================================
-- CAFESITO - VERIFICACION E2E TIMELINE + NOTIFICACIONES + PUSH
-- ==========================================================
-- Uso:
-- 1) Ejecuta seccion por seccion en Supabase SQL Editor.
-- 2) Ajusta los parametros de la seccion [PARAMS].
-- 3) Las secciones de insercion estan marcadas como "TEST".
--
-- Objetivo:
-- - Confirmar que followers pueden ver nuevos posts.
-- - Confirmar que menciones crean notifications_db.
-- - Confirmar que trigger de push y Edge Function estan operativos.
-- - Validar que el PUSH NATIVO (bandeja del sistema) llega al dispositivo.
-- ==========================================================

-- ==========================================================
-- [0] PARAMS (EDITAR)
-- ==========================================================
-- Sustituye por IDs reales de tu entorno:
--   p_author_id   = usuario que publica/menciona
--   p_target_id   = usuario que deberia recibir notificacion
--   p_post_id     = post existente para probar mention en comentario
--
-- Nota: deja p_post_id en NULL para usar un post reciente de p_author_id.
with params as (
  select
    1924119502::bigint as p_author_id,
    -755118960::bigint as p_target_id,
    null::text as p_post_id
)
select * from params;


-- ==========================================================
-- [1] SANITY CHECK DE DATOS BASE
-- ==========================================================
-- Verifica que author y target existen.
with params as (
  select
    1924119502::bigint as p_author_id,
    -755118960::bigint as p_target_id
)
select u.id, u.username
from public.users_db u
join params p on u.id in (p.p_author_id, p.p_target_id)
order by u.id;

-- Verifica que existe relacion follow target <- author
-- (author sigue a target o target sigue a author segun caso que quieras validar).
with params as (
  select
    1924119502::bigint as p_author_id,
    -755118960::bigint as p_target_id
)
select *
from public.follows f
join params p on true
where (f.follower_id = p.p_author_id and f.followed_id = p.p_target_id)
   or (f.follower_id = p_target_id and f.followed_id = p_author_id)
order by created_at desc
limit 20;

-- Ver post recientes del author
with params as (
  select 1924119502::bigint as p_author_id
)
select id, user_id, comment, timestamp
from public.posts_db
where user_id = (select p_author_id from params)
order by timestamp desc
limit 20;


-- ==========================================================
-- [2] NOTIFICACIONES DB Y RPC
-- ==========================================================
-- Verificar funciones RPC requeridas.
select n.nspname as schema_name, p.proname as function_name
from pg_proc p
join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public'
  and p.proname in (
    'create_notification',
    'get_notifications_for_user',
    'mark_notification_read',
    'mark_all_notifications_read',
    'delete_notification'
  )
order by p.proname;

-- Contar notificaciones actuales del target via RPC.
with params as (
  select -755118960::bigint as p_target_id
)
select count(*) as current_notifications
from public.get_notifications_for_user((select p_target_id from params));


-- ==========================================================
-- [3] TEST: CREAR NOTIFICACION DE MENCION VIA RPC
-- ==========================================================
-- Deberia insertar fila en notifications_db.
with params as (
  select
    1924119502::bigint as p_author_id,
    -755118960::bigint as p_target_id
),
author as (
  select username
  from public.users_db
  where id = (select p_author_id from params)
  limit 1
)
select public.create_notification(
  (select p_target_id from params),
  'MENTION',
  coalesce((select username from author), 'diagnostic_author'),
  'diagnostic mention',
  (extract(epoch from now())::bigint * 1000),
  'diag-post:999'
);

-- Verifica que entro la notificacion.
with params as (
  select -755118960::bigint as p_target_id
)
select id, user_id, type, from_username, message, timestamp, is_read, related_id
from public.get_notifications_for_user((select p_target_id from params))
order by timestamp desc
limit 10;


-- ==========================================================
-- [3b] VALIDACION PUSH NATIVO (bandeja del sistema)
-- ==========================================================
-- Objetivo: que la notificacion aparezca en la BANDEJA del movil (fuera de la app).
-- Cadena: insert notifications_db -> trigger -> pg_net -> Edge Function -> FCM -> dispositivo.
--
-- PRE-requisito: el user_id que recibe (p_target_id) DEBE tener token en user_fcm_tokens.
-- Ese dispositivo debe haber abierto la app con ese usuario para registrar el token.
--
-- Sustituye -755118960 por el user_id que debe recibir el push en tu test.
-- ==========================================================
--
-- Paso A: Comprobar que el usuario tiene token FCM (si 0 filas, el push NUNCA llegara).
with target_user as (select -755118960::bigint as uid)
select user_id, count(*) as token_count
from public.user_fcm_tokens
where user_id = (select uid from target_user)
group by user_id;
-- Esperado: >= 1. Si 0 -> abrir app en el movil con ese usuario para registrar token.
--
-- Paso B: Trigger bien configurado (sin <PROJECT_REF> ni <SUPABASE_ANON_OR_SERVICE_ROLE_KEY>).
select
  case
    when pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) like '%<PROJECT_REF>%'
      then 'ERROR: sustituye <PROJECT_REF> por tu proyecto Supabase'
    when pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) like '%<SUPABASE_ANON_OR_SERVICE_ROLE_KEY>%'
      then 'ERROR: sustituye la key por anon o service_role real'
    else 'OK: trigger configurado con URL y key'
  end as trigger_config;
--
-- Paso C: Disparar UNA notificacion para ese usuario (cambia -755118960 si usas otro target).
with params as (
  select
    -755118960::bigint as p_target_id,
    (select username from public.users_db where id = -755118960 limit 1) as target_username
),
author as (
  select username from public.users_db limit 1
)
select public.create_notification(
  (select p_target_id from params),
  'MENTION',
  coalesce((select username from author), 'test'),
  'Validacion push nativo',
  (extract(epoch from now())::bigint * 1000),
  'diag-push:1'
);
--
-- Paso D: Ver si el trigger llamo a la Edge Function y que respondio (en unos segundos).
select id, status_code, error_msg, created
from net._http_response
order by created desc
limit 5;
-- 200 = Edge Function OK (FCM puede haber enviado). 401/404/500/502 = ver [4] y logs Edge Function.
--
-- Paso E: En el MOVIL (usuario -755118960): mirar la BANDEJA de notificaciones del sistema.
-- Debe aparecer "Validacion push nativo" o similar. Si no: revisar logs de send-notification en Supabase.


-- ==========================================================
-- [4] TRIGGER PUSH + PG_NET + EDGE FUNCTION
-- ==========================================================
--
-- [4a] SI net._http_response DEVUELVE 401 (Unauthorized)
-- ==========================================================
-- Causa: la Edge Function exige Authorization valido. El trigger debe usar
-- la URL real de tu proyecto y una key real (anon o service_role).
--
-- 1) En Supabase: Project Settings -> API
--    - Project URL: https://XXXX.functions.supabase.co -> PROJECT_REF = XXXX
--    - Project API keys: copia "service_role" (o "anon" si prefieres)
--
-- 2) Sustituye abajo PROJECT_REF y TU_SERVICE_ROLE_KEY y ejecuta UNA vez:
/*
create or replace function public.notify_fcm_on_notification()
returns trigger language plpgsql security definer set search_path = public as $$
declare request_id bigint;
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
end; $$;
*/
-- Despues de ejecutar lo de arriba (con tus valores), vuelve a lanzar [3b] Paso C y D.
--
-- Extension pg_net activa?
select extname
from pg_extension
where extname = 'pg_net';

-- Trigger presente y habilitado?
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

-- Definicion real de la funcion de trigger.
select pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure);

-- Detectar placeholders sin sustituir.
select pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) as fn_sql
where pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) like '%<PROJECT_REF>%'
   or pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) like '%<SUPABASE_ANON_OR_SERVICE_ROLE_KEY>%';

-- Comprobar si header authorization existe.
select
  case
    when pg_get_functiondef('public.notify_fcm_on_notification()'::regprocedure) ilike '%authorization%'
      then 'OK: authorization header presente'
    else 'ERROR: falta authorization header en notify_fcm_on_notification()'
  end as auth_header_check;


-- ==========================================================
-- [5] TOKENS FCM
-- ==========================================================
-- Debe existir al menos 1 token para usuario target.
with params as (
  select -755118960::bigint as p_target_id
)
select user_id, count(*) as token_count
from public.user_fcm_tokens
where user_id = (select p_target_id from params)
group by user_id;

-- Diagnostico global de duplicados (si hay, pueden provocar ruido).
select user_id, count(*) as rows_per_user
from public.user_fcm_tokens
group by user_id
having count(*) > 1
order by rows_per_user desc;


-- ==========================================================
-- [6] RESPUESTAS HTTP DE PG_NET (EDGE FUNCTION)
-- ==========================================================
-- Requiere permisos para leer net._http_response.
select
  r.id,
  r.status_code,
  r.content_type,
  r.error_msg,
  r.created
from net._http_response r
order by r.created desc
limit 30;

-- Ver cuerpo de errores recientes (401/404/500, etc.).
select
  r.id,
  r.status_code,
  convert_from(r.content::bytea, 'utf8') as response_body,
  r.created
from net._http_response r
where r.status_code >= 400
order by r.created desc
limit 30;


-- ==========================================================
-- [7] TEST: POST NUEVO Y VISIBILIDAD EN TIMELINE
-- ==========================================================
-- Inserta post de prueba del author.
-- IMPORTANTE: si RLS no permite insert directo, prueba desde app/web autenticada.
with params as (
  select 1924119502::bigint as p_author_id
)
insert into public.posts_db (id, user_id, image_url, comment, timestamp)
values (
  'diag_post_' || extract(epoch from now())::bigint::text,
  (select p_author_id from params),
  '',
  'diagnostic timeline post',
  extract(epoch from now())::bigint * 1000
)
returning id, user_id, comment, timestamp;

-- Verifica que el post de prueba esta.
select id, user_id, comment, timestamp
from public.posts_db
where id like 'diag_post_%'
order by timestamp desc
limit 10;

-- Limpieza de posts de diagnostico (opcional).
-- delete from public.posts_db where id like 'diag_post_%';


-- ==========================================================
-- [8] TEST: MENCION EN COMENTARIO Y NOTIFICACION RESULTANTE
-- ==========================================================
-- Toma post de params o ultimo post del author.
with params as (
  select
    1924119502::bigint as p_author_id,
    -755118960::bigint as p_target_id,
    null::text as p_post_id
),
author as (
  select id, username
  from public.users_db
  where id = (select p_author_id from params)
  limit 1
),
target as (
  select id, username
  from public.users_db
  where id = (select p_target_id from params)
  limit 1
),
post_pick as (
  select coalesce(
    (select p_post_id from params),
    (select p.id from public.posts_db p where p.user_id = (select p_author_id from params) order by p.timestamp desc limit 1)
  ) as post_id
),
new_comment as (
  insert into public.comments_db (post_id, user_id, text, timestamp)
  values (
    (select post_id from post_pick),
    (select id from author),
    '@' || (select username from target) || ' diagnostic mention in comment',
    extract(epoch from now())::bigint * 1000
  )
  returning id, post_id, user_id, text, timestamp
)
select * from new_comment;

-- Si tu backend/app genera la notificacion por logica de cliente,
-- valida que exista en notifications del target:
with params as (
  select -755118960::bigint as p_target_id
)
select id, user_id, type, from_username, message, timestamp, related_id
from public.get_notifications_for_user((select p_target_id from params))
where type in ('MENTION', 'COMMENT', 'FOLLOW')
order by timestamp desc
limit 20;


-- ==========================================================
-- [9] LIMPIEZA DE NOTIFICACIONES DIAGNOSTICO (OPCIONAL)
-- ==========================================================
-- Borra notificaciones de prueba para no ensuciar entorno.
delete from public.notifications_db
where message ilike '%diagnostic%'
   or related_id like 'diag-%'
   or related_id like 'diag_%';

-- Borra comentarios de diagnostico (si quieres limpieza completa).
delete from public.comments_db
where text ilike '%diagnostic mention in comment%';

