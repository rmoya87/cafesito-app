# 500 en user_lists / user_list_members

Si las peticiones a `user_lists` o `user_list_members` devuelven **500 Internal Server Error**, o **no puedes crear listas** (el servidor responde 500 al INSERT), la causa está en las políticas RLS que usan `get_my_internal_id()`. Esa función puede no existir, lanzar una excepción o usar un `search_path` incorrecto.

**Mientras no corrijas la función en Supabase**, la app (web y Android) seguirá mostrando listas vacías y no podrá crear listas; los 500 se evitan en el cliente devolviendo listas vacías, pero crear lista fallará con un mensaje de permisos/servidor.

---

## Qué hacer exactamente en Supabase (pasos en orden)

### Paso 1: Aplicar el fix que evita el 500

1. Abre el **Dashboard de Supabase** del proyecto.
2. Ve a **SQL Editor** → **New query**.
3. Abre en tu repo el archivo **`docs/supabase/fix_get_my_internal_id_no_500.sql`**.
4. Copia todo su contenido (desde `-- FIX` hasta el final del `$$;`) y pégalo en el editor.
5. **Ejecuta** el script (Run).

Con esto, `get_my_internal_id()` deja de lanzar: si algo falla, devuelve `NULL` y las políticas no devuelven filas en lugar de provocar 500.

### Paso 2: Comprobar el nombre de la columna en `users_db`

La función del fix busca el id con `WHERE auth_id = auth.uid()`. En tu base de datos la columna que guarda el UUID de Auth puede tener otro nombre.

1. En Supabase, **SQL Editor**, ejecuta:

```sql
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'users_db'
  AND data_type = 'uuid';
```

2. Anota el `column_name` (p. ej. `auth_uid`, `auth_uuid`, `id` si es UUID, etc.).
3. Si **no** es `auth_id`, vuelve a **Paso 1** pero antes edita el script: donde pone `auth_id` en la línea del `SELECT`, sustituye por el nombre que te haya salido. Vuelve a ejecutar el script.

### Paso 3: Comprobar que devuelve tu usuario (opcional)

Con la app abierta y sesión iniciada (o con un JWT válido), no se puede probar bien desde el SQL Editor porque ahí no hay JWT. Si tras el fix **ya no hay 500** pero **no ves tus listas**, suele ser que la función devuelve `NULL` para tu usuario. Revisa:

- Que en `users_db` exista una fila con tu usuario y que la columna UUID (la que uses en la función) coincida con el usuario de Auth.
- Que la función use exactamente ese nombre de columna (Paso 2).

---

## Causas habituales del 500 (referencia)

1. **La función `get_my_internal_id()` no existe**  
   Las políticas de `user_lists` y `user_list_members` dependen de ella. El script `fix_get_my_internal_id_no_500.sql` la crea o reemplaza.

2. **La función lanzaba una excepción**  
   Por ejemplo, un `SELECT` que no encuentra fila y no estaba manejado, o un error de tipo. La versión del fix captura excepciones y devuelve `NULL`, así PostgREST ya no devuelve 500.

3. **Columna incorrecta en `users_db`**  
   Si la columna que enlaza con `auth.uid()` no se llama `auth_id`, la función devuelve `NULL` y no verás listas, pero no 500. Ajusta el nombre en el script como en el Paso 2.

4. **`search_path` incorrecto**  
   El script fija `SET search_path = public` en la función. Si además aplicaste `docs/supabase/security_advisor_fixes_2026-03-02.sql`, no hace falta cambiar nada más.

---

## Comprobaciones SQL de diagnóstico (opcional)

```sql
-- ¿Existe la función?
SELECT pg_get_functiondef(oid) AS definition
FROM pg_proc
WHERE proname = 'get_my_internal_id';

-- ¿Qué columnas UUID tiene users_db? (para saber qué nombre usar en la función)
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'users_db' AND data_type = 'uuid';
```

---

## Resumen

| Acción | Dónde |
|--------|--------|
| Ejecutar fix que evita 500 y define `get_my_internal_id()` | **SQL Editor** → pegar y ejecutar `docs/supabase/fix_get_my_internal_id_no_500.sql` |
| Ajustar nombre de columna si no es `auth_id` | Editar en ese script la línea `WHERE auth_id = v_uid` con el nombre correcto y volver a ejecutarlo |

Después de esto, los 500 en `user_lists` y `user_list_members` deberían desaparecer y podrás crear listas de nuevo. Si las listas siguen vacías, la función está devolviendo `NULL`: revisa que la columna en `users_db` sea la correcta y que exista la fila del usuario.

---

## Ver el error real en Supabase (opcional)

Si el script da error o quieres ver por qué falla una petición:

1. En el **Dashboard de Supabase** → **Logs** → **Postgres** (o **API**), revisa las entradas cuando la app hace la petición.
2. Ahí suele aparecer el mensaje de PostgreSQL (p. ej. "column auth_id does not exist" o "function get_my_internal_id() threw an exception").
3. Si aparece **"cannot change return type"** al ejecutar el fix: tu función actual devuelve `bigint`. Usa el bloque alternativo al final de `fix_get_my_internal_id_no_500.sql` (el que tiene `RETURNS bigint`).
