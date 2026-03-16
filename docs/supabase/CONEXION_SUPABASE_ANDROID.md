# Posibles problemas de conexión con Supabase (Android)

**Estado:** vivo  
**Última actualización:** 2026-03-14  
**Ámbito:** Android app — configuración y errores que pueden parecer “de conexión”.

---

## 1. Sí, puede haber problema de conexión (o parecido)

Los fallos al usar listas, detalle de café o crear listas pueden deberse a:

| Origen | Síntoma típico | Dónde mirar |
|--------|----------------|-------------|
| **Red / URL / clave** | Timeout, “connection failed”, nada carga | § 2 |
| **Error 500 en Supabase** | Listas no cargan, crear lista falla, pantalla de error | § 3 (y `user_lists_500_troubleshooting.md`) |
| **Auth / JWT** | 401/403, “no autorizado”, datos que no aparecen estando logueado | § 4 |
| **Timeouts** | Peticiones que tardan mucho y acaban fallando | § 5 |

---

## 2. Configuración de red y claves (Android)

- La app usa **BuildConfig.SUPABASE_URL** y **BuildConfig.SUPABASE_PUBLISHABLE_KEY** (ver `app/di/SupabaseModule.kt`).
- Esas variables suelen definirse desde **`local.properties`** o variables de entorno en el build.
- **Comprobar:** que en el proyecto existan y apunten al proyecto correcto de Supabase (misma URL que en el Dashboard).
- Si la URL o la clave anon son incorrectas, las peticiones fallan (timeout o 4xx) y puede parecer “sin conexión”.

---

## 3. Error 500 (listas, user_lists, user_list_members)

Si las peticiones a **user_lists** o **user_list_members** devuelven **500**, casi siempre es por las políticas RLS que usan la función **`get_my_internal_id()`**: si esa función no existe o lanza una excepción, PostgREST devuelve 500.

**Qué hacer:**

1. Abre **`docs/supabase/fix_get_my_internal_id_no_500.sql`** (el script que tienes en el proyecto).
2. En el **Dashboard de Supabase** → **SQL Editor** → New query, pega el contenido del script y **ejecútalo**.
3. Eso hace que `get_my_internal_id()` devuelva `NULL` en caso de error en lugar de lanzar, así las políticas no provocan 500.

**Detalle y comprobaciones (nombre de columna en `users_db`, etc.):**  
→ **`docs/supabase/user_lists_500_troubleshooting.md`**

---

## 4. Auth / sesión (401, 403, datos que no aparecen)

- Las tablas protegidas por RLS usan el JWT del usuario. Si la sesión no está restaurada o el token es inválido, Supabase puede devolver 401/403.
- La app restaura sesión al arrancar (`restoreSessionFromSupabaseIfNeeded`) y usa **ConnectivityObserver** para no lanzar peticiones cuando no hay red.
- Si “hay conexión” pero listas o detalle fallan: revisa que el usuario esté logueado y que en Supabase **Authentication** exista el usuario y que **users_db** tenga una fila con el mismo `auth_id` (o la columna que uses) que `auth.uid()`.

---

## 5. Timeouts

- En **SupabaseModule** están configurados timeouts de **30 segundos** (request, connect, socket).
- Si la red es muy lenta o el backend tarda, la petición puede fallar por timeout y la app mostrar error o pantalla en blanco.
- No hay reintento automático en el cliente; el usuario puede usar “Reintentar” si la pantalla lo ofrece.

---

## 6. Resumen rápido

| Si ves… | Revisar |
|--------|--------|
| Listas no cargan / crear lista falla / 500 | Ejecutar **`docs/supabase/fix_get_my_internal_id_no_500.sql`** en Supabase y seguir **`user_lists_500_troubleshooting.md`** |
| “Sin conexión” o timeout | URL y clave en BuildConfig, red del dispositivo, firewall |
| No ves datos estando logueado | Sesión Auth, que `get_my_internal_id()` devuelva tu id (columna correcta en `users_db`) |

Si tras aplicar el fix del 500 sigues teniendo fallos, conviene revisar en Supabase (Dashboard → Logs) si las peticiones llegan y qué código de respuesta devuelven (500, 401, etc.).
