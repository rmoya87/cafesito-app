# Despensa independiente de actividad y listas

## Regla de negocio

**Al eliminar una actividad (entrada del diario) o una lista personalizada, el café que está en la despensa NO debe borrarse.**

- **Eliminar actividad** = borrar una fila en `diary_entries`. No debe tocar `pantry_items`.
- **Eliminar lista** = borrar en `user_lists` (y en cascada `user_list_items`). No debe tocar `pantry_items`.

La despensa (`pantry_items`) es independiente: un mismo café puede estar en una entrada del diario, en una o varias listas y en la despensa. Quitar de actividad o de una lista no implica quitar de la despensa.

## Implementación

### App (Android y WebApp)

- `deleteDiaryEntry` / `handleDeleteDiaryEntry`: solo borran la entrada del diario por `id`; no llaman a `deletePantryItem` ni modifican `pantry_items`.
- `deleteUserList` / eliminar lista: solo borran `user_lists` (y en BD `user_list_items` por FK CASCADE); no tocan `pantry_items`.

### Supabase

- No debe existir ningún **trigger** en `diary_entries` ni en `user_lists` / `user_list_items` que elimine filas de `pantry_items`.
- La tabla `pantry_items` no debe tener **FK con ON DELETE CASCADE** hacia `diary_entries` ni hacia `user_list_items` o `user_lists`.

El único borrado de `pantry_items` debe ser:

1. Explícito por el usuario (acción "Eliminar de la despensa" o "Marcar como terminado").
2. En el proceso de eliminación de cuenta (`hard_delete_user_data`), que borra todos los datos del usuario.

## Comprobar en Supabase

Para verificar que no hay triggers que borren en `pantry_items` al eliminar en otras tablas, en el SQL Editor:

```sql
-- Triggers que afectan a pantry_items
SELECT tgname, tgrelid::regclass AS table_name, proname AS function_name
FROM pg_trigger t
JOIN pg_proc p ON t.tgfoid = p.oid
JOIN pg_class c ON t.tgrelid = c.oid
WHERE tgrelid IN ('public.diary_entries'::regclass, 'public.user_lists'::regclass, 'public.user_list_items'::regclass)
  AND NOT tgisinternal;
```

Si aparece algún trigger, revisar la función asociada para asegurarse de que no ejecuta `DELETE FROM pantry_items`.
