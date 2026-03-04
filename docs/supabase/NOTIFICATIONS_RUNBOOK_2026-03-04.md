# Notificaciones Timeline/Push - Runbook de Prevencion

Fecha: 4 de marzo de 2026
Incidente: menciones sin push en Android y desalineacion entre clientes (web/android) y backend.

## 1) Resumen ejecutivo

Se detectaron dos fallos combinados:

1. No siempre se creaba la notificacion `MENTION` en `notifications_db` desde cliente (versiones antiguas web/android).
2. Aunque existia notificacion, el trigger SQL de push podia fallar por configuracion/firmas antiguas de `pg_net`.

Resultado: el usuario no recibia push nativo en Android aunque la accion de mencion se hubiese hecho.

## 2) Causas raiz confirmadas

### 2.1 Falta de insercion de `MENTION` en cliente

- El cliente web desplegado en produccion no tenia en todos los entornos la logica de menciones en `createPost`.
- Si el cliente no crea fila en `notifications_db`, no hay trigger de push.

### 2.2 Trigger SQL de push con firma obsoleta/headers no consistentes

- `net.http_post` cambio a firma con `body jsonb`.
- Variantes antiguas con `body::text` o headers inconsistentes devolvian errores (`401`/`400`), o no ejecutaban como se esperaba.

### 2.3 Token FCM en dispositivo sin refresco fiable en cada acceso

- Algunos usuarios mantenian token antiguo.
- `onNewToken` por si solo no garantiza refresco frecuente.

## 3) Cambios aplicados (permanentes)

## 3.1 Supabase: push trigger actualizado

Archivo: `docs/supabase/notifications_push.sql`

- Uso de firma actual:
  - `body := jsonb_build_object(...)`
  - `params := '{}'::jsonb`
  - `timeout_milliseconds := 10000`
- Header obligatorio:
  - `Authorization: Bearer <SERVICE_ROLE_KEY>`
  - `apikey: <SERVICE_ROLE_KEY>`

Script de reparacion rapida:
- `docs/supabase/fix_trigger_401.sql`

## 3.2 Supabase: menciones server-side (defensa ante clientes antiguos)

Archivo: `docs/supabase/mentions_server_side.sql`

Incluye:

1. Dedupe historico de `MENTION`.
2. Indice unico parcial para evitar duplicados:
   - `(user_id, type, from_username, related_id)` donde `type='MENTION'`.
3. Trigger `AFTER INSERT` en `posts_db` para extraer `@usuario` y crear `MENTION`.
4. Trigger `AFTER INSERT` en `comments_db` para extraer `@usuario` y crear `MENTION`.

Con esto, aunque un cliente no cree notificacion, el backend la crea igualmente.

## 3.3 Android: refresh de token FCM en cada acceso

Archivos:
- `app/src/main/java/com/cafesito/app/MainActivity.kt`
- `app/src/main/java/com/cafesito/app/startup/AppSessionCoordinator.kt`

Cambios:

1. Refresco en `onAuthenticated(...)` (ya existente, mantenido).
2. Refresco en cada `onStart()` de `MainActivity` mediante `onAppForeground(...)`.
3. Reintentos de obtencion de token (hasta 3 intentos con backoff).
4. `syncPendingFcmTokenIfAny()` en foreground para vaciar pendientes.

Objetivo: reducir casos de token obsoleto tras largas ausencias o cambios de dispositivo/app.

## 4) Checklist de despliegue obligatorio

1. Ejecutar SQL:
   - `notifications_push.sql`
   - `mentions_server_side.sql`
2. Verificar triggers activos:
   - `notify_fcm_on_notification_insert`
   - `notify_mentions_post_insert`
   - `notify_mentions_comment_insert`
3. Validar respuesta de `net._http_response` tras prueba:
   - Debe aparecer `status_code = 200`
4. Verificar token del usuario destino en `user_fcm_tokens`.
5. Publicar cliente web/android para alinear UX (aunque backend ya protege menciones).

## 5) Pruebas E2E operativas

Usar:
- `docs/supabase/push_e2e_verification.sql`

Escenarios minimos:

1. Crear mencion en post.
2. Crear mencion en comentario.
3. Confirmar fila en `notifications_db`.
4. Confirmar `net._http_response` con `200`.
5. Confirmar llegada en bandeja Android.

## 6) Monitoreo recomendado (cada incidente)

1. Ultimas respuestas HTTP:
```sql
select id, status_code, created
from net._http_response
order by created desc
limit 20;
```

2. Tokens por usuario:
```sql
select user_id, count(*) as token_count, max(created_at) as last_created_at
from public.user_fcm_tokens
group by user_id
order by last_created_at desc;
```

3. Menciones recientes:
```sql
select id, user_id, from_username, message, timestamp, related_id
from public.notifications_db
where type = 'MENTION'
order by timestamp desc
limit 50;
```

## 7) Regla de prevencion de regresiones

Nunca volver a desplegar triggers de push/menciones con:

- `net.http_post(... body := ...::text)` (obsoleto)
- ausencia de `Authorization: Bearer ...`
- logica de menciones solo cliente sin respaldo server-side

Toda modificacion en notificaciones debe cerrarse con prueba E2E real:
- insercion en DB
- respuesta `200`
- push visible en dispositivo

