# Revisión de ficheros .sql en docs/supabase

**Propósito:** Resumen de cada script SQL: para qué sirve y si tiene sentido conservarlo.

---

## Conclusión general

**Sí tiene sentido guardarlos todos.** Son la fuente de verdad para:

- **Nuevos entornos:** aplicar esquema, RLS, RPCs y triggers en orden.
- **Troubleshooting:** runbooks y consultas de diagnóstico.
- **Auditoría:** qué se aplicó y en qué orden (listas, notificaciones, borrado de cuenta, etc.).

Recomendación: **mantener todos**; opcionalmente marcar como históricos los que son *one-off* ya ejecutados (dry-run, fixes puntuales con fecha) y dejar en el nombre o en comentarios que son referencia.

---

## Por categoría

### 1. Esquema base y listas (orden de ejecución lógico)

| Fichero | Uso | ¿Guardar? |
|---------|-----|-----------|
| **user_lists.sql** | Tablas `user_lists`, `user_list_items`, RLS inicial. Base de listas. | ✅ Sí. Es la base. |
| **user_list_members_and_invitations.sql** | Tablas miembros e invitaciones, RPCs `create_list_invitation`, `accept_list_invitation`, `decline_list_invitation`, `join_public_list`. | ✅ Sí. Dependencia de listas compartidas. |
| **user_lists_privacy_column.sql** | Columna `privacy` (public/invitation/private). | ✅ Sí. Migración re-aplicable (ADD COLUMN IF NOT EXISTS). |
| **user_lists_members_can_edit.sql** | Columna `members_can_edit` y políticas RLS insert/delete en `user_list_items`. | ✅ Sí. |
| **user_lists_members_can_invite.sql** | Columna `members_can_invite` y RPC `create_list_invitation` ampliada. | ✅ Sí. |
| **user_list_members_leave_policy.sql** | Política para que un miembro pueda abandonar la lista (DELETE propia fila). | ✅ Sí. |
| **user_lists_join_by_link.sql** | RPCs `get_list_info_for_join`, `join_list_by_link` (unirse por enlace sin invitación). | ✅ Sí. Citado en REGISTRO, OPCIONES_DE_LISTA, ANDROID. |

### 2. Notificaciones y push

| Fichero | Uso | ¿Guardar? |
|---------|-----|-----------|
| **notifications.sql** | Tabla `notifications_db`, índices, RLS, `create_notification`. | ✅ Sí. Base de notificaciones. |
| **notifications_push.sql** | Trigger `notify_fcm_on_notification` (pg_net → Edge Function). Placeholders PROJECT_REF / key. | ✅ Sí. Referencia para configurar push. |
| **mentions_server_side.sql** | Dedup menciones, índice único, triggers en posts_db y comments_db para crear notificación MENTION. | ✅ Sí. |
| **fix_trigger_401.sql** | Mismo trigger push con placeholders para corregir 401/400. | ✅ Sí. Runbook (NOTIFICATIONS_RUNBOOK lo cita). |
| **push_troubleshooting.sql** | Consultas de diagnóstico (pg_net, trigger, tokens FCM). | ✅ Sí. Troubleshooting. |
| **push_e2e_verification.sql** | Script E2E para probar timeline + notificaciones + push (params editables). | ✅ Sí. Pruebas manuales / validación. |

### 3. Usuarios y borrado de cuenta

| Fichero | Uso | ¿Guardar? |
|---------|-----|-----------|
| **users_db_public_profiles_select.sql** | Política RLS para que autenticados lean perfiles públicos (avatares en listas). | ✅ Sí. |
| **users_timestamps.sql** | Columnas `created_at`/`updated_at` y trigger en `users_db`. | ✅ Sí. |
| **account_deletion_lifecycle.sql** | Columnas `account_status`, `deactivation_requested_at`, `scheduled_deletion_at`, `hard_delete_user_data()`. | ✅ Sí. Esquema y lógica de baja. |

### 4. Cafés (coffees) y RLS

| Fichero | Uso | ¿Guardar? |
|---------|-----|-----------|
| **coffees_rls.sql** | Políticas RLS para SELECT/INSERT/UPDATE/DELETE en `coffees` (custom + públicos). | ✅ Sí. |
| **coffees_rls_troubleshoot.sql** | Consultas para diagnosticar por qué un café custom no aparece (get_my_internal_id, políticas). | ✅ Sí. Diagnóstico. |

### 5. Despensa y diario

| Fichero | Uso | ¿Guardar? |
|---------|-----|-----------|
| **pantry_items.sql** | Migración a PK `id` UUID y RLS; comentarios de creación desde cero. | ✅ Sí. |
| **pantry_historical.sql** | Tabla `pantry_historical` (café terminado) y RLS. | ✅ Sí. |
| **diary_entry_size.sql** | Columna `size_label` en `diary_entries`. | ✅ Sí. ADD COLUMN IF NOT EXISTS. |
| **diary_entries_pantry_item_id.sql** | Columna `pantry_item_id` en `diary_entries` (vincular actividad a ítem de despensa). | ✅ Sí. |

### 6. Comentarios y timeline

| Fichero | Uso | ¿Guardar? |
|---------|-----|-----------|
| **comments.sql** | RPC `delete_comment` (SECURITY DEFINER) para borrado seguro con RLS. | ✅ Sí. |
| **post_coffee_tags.sql** | Tabla `post_coffee_tags` y RLS. | ✅ Sí. |

### 7. Deploy y cola de cambios

| Fichero | Uso | ¿Guardar? |
|---------|-----|-----------|
| **deploy_change_queue.sql** | Tabla `deploy_change_events` para cola de despliegue (Edge Functions). | ✅ Sí. |

### 8. Fixes puntuales y seguridad (referencia / ya aplicados)

| Fichero | Uso | ¿Guardar? |
|---------|-----|-----------|
| **fix_get_my_internal_id_no_500.sql** | `get_my_internal_id()` robusta + helpers `is_owner_of_list`/`is_member_of_list` + políticas para evitar 500 en listas. | ✅ Sí. Citado en CONEXION_SUPABASE_ANDROID y user_lists_500_troubleshooting. |
| **security_advisor_fixes_2026-03-02.sql** | Ajustes Security Advisor: `security_invoker` en vista, `search_path = public` en funciones. | ✅ Sí. Referencia de qué se aplicó; re-ejecutable en parte (ALTER). |

### 9. One-off / históricos (guardar como referencia)

| Fichero | Uso | ¿Guardar? |
|---------|-----|-----------|
| **diary_entries_backfill_grams_size_2026-03-06_dry_run.sql** | Solo SELECT (dry run) para backfill de `coffee_grams` y `size_label` en diary_entries. No es migración; es validación previa. | ✅ Sí. Documenta la lógica del backfill; si se repite en otro entorno, sirve de plantilla. |

---

## Orden sugerido para un entorno nuevo

Para un proyecto limpio que use listas, notificaciones, push y borrado de cuenta:

1. users_db (timestamps, public profiles) + get_my_internal_id (fix_get_my_internal_id_no_500 o equivalente)
2. notifications.sql → notifications_push.sql (y fix_trigger_401 si aplica)
3. mentions_server_side.sql
4. user_lists.sql → user_list_members_and_invitations.sql → user_lists_privacy_column.sql → user_lists_members_can_edit.sql → user_list_members_leave_policy.sql → user_lists_members_can_invite.sql → user_lists_join_by_link.sql
5. coffees_rls.sql
6. pantry_items.sql, pantry_historical.sql
7. diary_entry_size.sql, diary_entries_pantry_item_id.sql
8. comments.sql, post_coffee_tags.sql
9. account_deletion_lifecycle.sql
10. deploy_change_queue.sql (si se usan Edge Functions de deploy)
11. security_advisor_fixes (opcional, según advisor)

---

## Posibles mejoras (opcional)

- Añadir al inicio de cada `.sql` una línea tipo `-- Última revisión: YYYY-MM` o "Estado: referencia / aplicar en nuevos entornos".

**Hecho:** existe [README.md](./README.md) en `docs/supabase/` con índice y enlace a este documento.

No es necesario borrar ningún fichero; todos tienen sentido como documentación y referencia ejecutable.
