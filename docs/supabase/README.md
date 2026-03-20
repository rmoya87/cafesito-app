# Supabase — scripts y documentación

Scripts SQL, Edge Functions y guías para el backend Cafesito en Supabase.

---

## Revisión de ficheros SQL

**→ [REVISION_SQL_FICHEROS.md](./REVISION_SQL_FICHEROS.md)** — Revisión de todos los `.sql`: uso de cada uno, si conviene conservarlos y **orden sugerido para un entorno nuevo**.

---

## Índice rápido

### Scripts SQL (ejecutar en SQL Editor de Supabase)

| Categoría | Ficheros |
|-----------|----------|
| **Listas** | [user_lists.sql](./user_lists.sql), [user_list_members_and_invitations.sql](./user_list_members_and_invitations.sql), [user_lists_privacy_column.sql](./user_lists_privacy_column.sql), [user_lists_members_can_edit.sql](./user_lists_members_can_edit.sql), [user_list_members_leave_policy.sql](./user_list_members_leave_policy.sql), [user_lists_members_can_invite.sql](./user_lists_members_can_invite.sql), [user_lists_join_by_link.sql](./user_lists_join_by_link.sql) |
| **Notificaciones y push** | [notifications.sql](./notifications.sql), [notifications_push.sql](./notifications_push.sql), [mentions_server_side.sql](./mentions_server_side.sql), [fix_trigger_401.sql](./fix_trigger_401.sql), [push_troubleshooting.sql](./push_troubleshooting.sql), [push_e2e_verification.sql](./push_e2e_verification.sql) |
| **Usuarios y baja de cuenta** | [users_db_public_profiles_select.sql](./users_db_public_profiles_select.sql), [users_timestamps.sql](./users_timestamps.sql), [account_deletion_lifecycle.sql](./account_deletion_lifecycle.sql) |
| **Cafés (RLS)** | [coffees_rls.sql](./coffees_rls.sql), [coffees_rls_troubleshoot.sql](./coffees_rls_troubleshoot.sql) |
| **Despensa y diario** | [pantry_items.sql](./pantry_items.sql), [pantry_historical.sql](./pantry_historical.sql), [diary_entry_size.sql](./diary_entry_size.sql), [diary_entries_pantry_item_id.sql](./diary_entries_pantry_item_id.sql) |
| **Timeline** | [comments.sql](./comments.sql), [post_coffee_tags.sql](./post_coffee_tags.sql) |
| **Deploy** | [deploy_change_queue.sql](./deploy_change_queue.sql) |
| **Fixes / seguridad** | [fix_get_my_internal_id_no_500.sql](./fix_get_my_internal_id_no_500.sql), [security_advisor_fixes_2026-03-02.sql](./security_advisor_fixes_2026-03-02.sql) |
| **Referencia (one-off)** | [diary_entries_backfill_grams_size_2026-03-06_dry_run.sql](./diary_entries_backfill_grams_size_2026-03-06_dry_run.sql) |

### Edge Functions

| Función | Descripción |
|---------|-------------|
| [edge-functions/send-notification.ts](./edge-functions/send-notification.ts) | Envía push (FCM) al insertar en `notifications_db`. |
| [edge-functions/trigger-coffees-build/index.ts](./edge-functions/trigger-coffees-build/index.ts) | Dispara build de catálogo de cafés. |
| [edge-functions/consume-deploy-changes/index.ts](./edge-functions/consume-deploy-changes/index.ts) | Consume cola `deploy_change_events`. |
| [edge-functions/process-pending-account-deletions/index.ts](./edge-functions/process-pending-account-deletions/index.ts) | Procesa bajas de cuenta programadas. |

### Documentación (runbooks, guías)

| Documento | Contenido |
|------------|-----------|
| [CONEXION_SUPABASE_ANDROID.md](./CONEXION_SUPABASE_ANDROID.md) | Conexión Android–Supabase y problemas típicos. |
| [user_lists_500_troubleshooting.md](./user_lists_500_troubleshooting.md) | Resolución de 500 en listas (get_my_internal_id, RLS). |
| [NOTIFICATIONS_RUNBOOK_2026-03-04.md](./NOTIFICATIONS_RUNBOOK_2026-03-04.md) | Runbook notificaciones y push. |
| [webhook-coffees-trigger-build.md](./webhook-coffees-trigger-build.md) | Webhook para trigger de build de cafés. |
| [pantry_independent_of_diary_and_lists.md](./pantry_independent_of_diary_and_lists.md) | Regla: despensa independiente de diario y listas. |

---

## Orden para un entorno nuevo

Resumen; detalle y dependencias en [REVISION_SQL_FICHEROS.md](./REVISION_SQL_FICHEROS.md#orden-sugerido-para-un-entorno-nuevo).

1. Usuarios: `users_timestamps.sql`, `users_db_public_profiles_select.sql`, `fix_get_my_internal_id_no_500.sql`
2. Notificaciones: `notifications.sql` → `notifications_push.sql` (y `fix_trigger_401.sql` si aplica) → `mentions_server_side.sql`
3. Listas: `user_lists.sql` → `user_list_members_and_invitations.sql` → `user_lists_privacy_column.sql` → `user_lists_members_can_edit.sql` → `user_list_members_leave_policy.sql` → `user_lists_members_can_invite.sql` → `user_lists_join_by_link.sql`
4. Cafés: `coffees_rls.sql`
5. Despensa y diario: `pantry_items.sql`, `pantry_historical.sql`, `diary_entry_size.sql`, `diary_entries_pantry_item_id.sql`
6. Timeline: `comments.sql`, `post_coffee_tags.sql`
7. Borrado de cuenta: `account_deletion_lifecycle.sql`
8. Opcional: `deploy_change_queue.sql`, `security_advisor_fixes_2026-03-02.sql`
