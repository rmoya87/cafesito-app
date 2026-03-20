# Opciones de lista: backend, WebApp y Android

**Estado:** vigente  
**Última actualización:** 2026-03-19  
**Ámbito:** Supabase, WebApp, Android.

---

## 1. Resumen

La funcionalidad **Opciones de lista** permite al usuario (dueño o miembro) gestionar una lista personalizada: privacidad, miembros, invitaciones, editar nombre, eliminar lista o abandonarla. En WebApp está implementada como **página completa** (`/profile/list/:listId/options`). En Android debe ofrecerse como **pantalla** equivalente con la misma lógica de negocio y APIs.

---

## 2. Backend (Supabase)

### 2.1 Tablas y columnas

- **`user_lists`**
  - `id`, `user_id`, `name`, `is_public`, `created_at`
  - **`privacy`** (text): `'public' | 'invitation' | 'private'`. Ver `docs/supabase/user_lists_privacy_column.sql`.
  - **`members_can_edit`** (boolean): si los miembros pueden añadir/quitar cafés. Ver `docs/supabase/user_lists_members_can_edit.sql`.

- **`user_list_members`**: miembros con acceso (además del dueño).
  - `list_id`, `user_id`, `role` ('viewer'|'editor'), `invited_by`, `created_at`.
  - RLS: SELECT si soy dueño, miembro o inviter; INSERT/DELETE solo dueño; DELETE también si `user_id = get_my_internal_id()` (abandonar lista). Ver `user_list_members_and_invitations.sql` y `user_list_members_leave_policy.sql`.

- **`user_list_invitations`**: invitaciones pendientes.
  - `id`, `list_id`, `inviter_id`, `invitee_id`, `status` ('pending'|'accepted'|'declined'), `created_at`.
  - RLS: SELECT inviter o invitee; INSERT solo dueño; UPDATE solo invitee (aceptar/rechazar).

### 2.2 APIs utilizadas por Opciones de lista

| Acción | API / tabla | Descripción |
|--------|-------------|-------------|
| Cargar lista (con privacidad) | `user_lists` SELECT `id, user_id, name, is_public, privacy, members_can_edit, created_at` | Por listId; RLS según propio/miembro/público. |
| Actualizar privacidad y edición | `user_lists` UPDATE `name`, `is_public`, `privacy`, `members_can_edit` | Solo dueño (RLS update_own). |
| Listar miembros | `user_list_members` SELECT por `list_id` | Solo dueño ve miembros (o RLS select con is_owner). |
| Listar invitaciones | `user_list_invitations` SELECT por `list_id` | Solo inviter (dueño). |
| Invitar usuario | RPC `create_list_invitation(p_list_id, p_invitee_id)` | Crea fila en `user_list_invitations` y notificación. |
| Eliminar miembro / Abandonar lista | `user_list_members` DELETE donde `list_id` y `user_id` | Dueño borra a cualquier miembro; cualquier usuario puede borrarse a sí mismo (leave). |
| Eliminar lista | `user_lists` DELETE | Solo dueño; cascada a items e invitaciones. |

### 2.3 RPCs

- **`create_list_invitation(p_list_id, p_invitee_id)`**: comprueba dueño, inserta en `user_list_invitations`, crea notificación LIST_INVITE.
- **`accept_list_invitation(p_invitation_id)`**: invitee acepta; inserta en `user_list_members`, actualiza status.
- **`decline_list_invitation(p_invitation_id)`**: invitee rechaza; actualiza status.
- **`join_public_list(p_list_id)`**: usuario se une a lista pública; inserta en `user_list_members`.
- **`get_list_info_for_join(p_list_id)`** y **`join_list_by_link(p_list_id)`**: ver `docs/supabase/user_lists_join_by_link.sql`. Permiten a un usuario autenticado **ver nombre y dueño** de una lista y **unirse por enlace** si la lista es **pública** o **`privacy = 'invitation'`** (sin invitación previa en `user_list_invitations`). La WebApp usa `/profile/list/{listId}`; si el visitante no es miembro, muestra "Unirse a la lista". Android abre la misma URL o `/lists/join/{listId}` y, si no hay acceso RLS, la pantalla `listJoin/{listId}`.

### 2.4 Unirse por enlace (WebApp + Android)

| Plataforma | Comportamiento |
|------------|----------------|
| **WebApp** | `fetchListInfoForJoin` / `joinListByLink` en `supabaseApi.ts`; UI `JoinListView`; flujo en `AppContainer` cuando la ruta es lista sin ser miembro. |
| **Android** | `SupabaseDataSource.getListInfoForJoin`, `joinListByLink`; `ListJoinScreen` + ruta `listJoin/{listId}`; deep link en `MainActivity` + `AppNavigation`. |

---

## 3. Frontend WebApp

### 3.1 Rutas

- Detalle de lista: `/profile/list/:listId` (panel `profile`, sección list, `profileListId`).
- **Opciones de lista:** `/profile/list/:listId/options` (mismo panel, `listOptionsView: true`).
- En `parseRoute` / `buildRoute`: cuarto segmento `"options"` activa la vista de opciones.

### 3.2 Componentes

- **`ListOptionsPage`** (`webApp/src/features/lists/ListOptionsPage.tsx`): página completa con:
  - **Privacidad** (solo dueño): opciones Pública / Por invitación / Privada; switch "Permitir editar lista" (solo si pública o por invitación).
  - **Miembros** (solo dueño): bloque `ListOptionsMembersBlock` con buscador, copiar enlace, listado de miembros (orden: dueño primero) y eliminar miembro (swipe o botón).
  - **General**: Editar lista, Eliminar lista (dueño); Abandonar lista (no dueño).

- **`ListOptionsMembersBlock`** (`ListOptionsMembersBlock.tsx`):
  - Buscador de usuarios (excluye actual, miembros e invitados pendientes).
  - Botón "Invitar" por usuario sugerido; llama `createListInvitation(listId, inviteeId)`.
  - "Copiar enlace": URL de la lista al portapapeles; chip "Enlace copiado" temporal.
  - Lista de miembros con avatar, nombre, rol (Admin./Miembro); swipe o botón Eliminar para no-dueños (llama `leaveList(listId, userId)`).

### 3.3 Estado y datos en AppContainer

- `listOptionsView`: si se muestra la página de opciones.
- `listOptionsMembers`, `listOptionsMemberUsers`, `listOptionsInvitations`: cargados cuando `listOptionsView && profileListId`.
- `listOptionsInvitingId`, `showCopyChip`, `copyChipExiting`: UI de invitación y copia.
- Handlers: `updateUserListWithPrivacy`, `createListInvitation`, `leaveList` (para quitar miembro o abandonar), `fetchListMembersByListId`, `fetchListInvitationsByListId`, `fetchUsersByIds` para avatares/nombres.

### 3.4 Acceso desde el detalle de lista

- En la TopBar del detalle: icono de opciones (3 puntos) o avatares + contador de miembros (si pública/invitación) abren la **página** de opciones navegando a `/profile/list/:listId/options`.
- El título de la TopBar en opciones es "Opciones"; botón atrás vuelve al detalle de la lista.

### 3.5 Edición de lista y permisos de ítems

- **Editar lista**: abre sheet/modal de edición (nombre, privacidad, permitir editar); guarda con `updateUserListWithPrivacy(listId, name, privacy, membersCanEdit)`.
- En el detalle de lista, si `isOwner || (list.members_can_edit && !isOwner)` se permite añadir/quitar cafés; si no, solo lectura.

---

## 4. Paridad Android (qué implementar)

### 4.1 Capa de datos

- **Modelos**: extender `UserListRow` con `privacy` (String?) y `membersCanEdit` (Boolean?). Incluir en selects de `user_lists`.
- **ListMemberRow** completo: `listId`, `userId`, `role`, `invitedBy` (para listado de miembros).
- **ListInvitationRow**: `id`, `listId`, `inviterId`, `inviteeId`, `status` (para filtrar invitados pendientes en el buscador).
- **SupabaseDataSource**:
  - `getUserLists` / `getUserListById` / `getSharedWithMeLists`: incluir `privacy`, `members_can_edit`.
  - `updateUserListWithPrivacy(listId, name, privacy, membersCanEdit)`.
  - `leaveList(listId, userId)`: DELETE en `user_list_members` (abandonar o que el dueño elimine a un miembro).
  - `fetchListMembersByListId(listId)`, `fetchListInvitationsByListId(listId)`.
  - `createUserList` con `privacy` y `membersCanEdit` si se crea lista desde la app.

### 4.2 Navegación

- Ruta nueva: `profile/{userId}/list/{listId}/options` (y opcional `?listName=...`).
- Desde **ListDetailScreen**: el menú de 3 puntos (y, si aplica, avatares/miembros) navega a esta pantalla en lugar de abrir el bottom sheet de opciones.
- ListOptionsScreen: título "Opciones", botón atrás vuelve al detalle de la lista.

### 4.3 Pantalla ListOptionsScreen

- **Privacidad** (solo si dueño): tres opciones (Pública, Por invitación, Privada) + switch "Permitir editar lista" si pública o por invitación. Al cambiar, llamar `updateUserListWithPrivacy`.
- **Miembros** (solo si dueño):
  - Buscador de usuarios (excluir usuario actual, miembros e invitados pendientes); botón Invitar → `createListInvitation`.
  - Botón Copiar enlace (URL de la lista); feedback tipo "Enlace copiado".
  - Lista de miembros (dueño primero); para cada no-dueño, acción Eliminar → `leaveList(listId, userId)`.
- **General**:
  - Dueño: Editar lista (abre sheet/modal existente; actualizar con `updateUserListWithPrivacy` si se usa privacidad), Eliminar lista (confirmación existente).
  - No dueño: Abandonar lista (confirmación) → `leaveList(listId, currentUserId)` y volver atrás o a listas.

### 4.4 Detalle de lista (ListDetailScreen / ViewModel)

- Usar `privacy` y `membersCanEdit` para mostrar/ocultar botón "Añadirme" (solo lista pública) y para permitir o no añadir/quitar cafés según permisos.
- Cargar lista con `getUserListById` o desde lista ya en memoria incluyendo `privacy` y `members_can_edit`.

### 4.5 Reutilización de UI existente

- Mantener bottom sheets o diálogos de **Editar lista**, **Eliminar lista** y **Abandonar lista** (confirmación); invocarlos desde ListOptionsScreen en lugar de desde el detalle.
- El ShareListBottomSheet actual puede quedar solo como flujo alternativo o eliminarse si toda la gestión de invitaciones y enlace está en ListOptionsScreen.

---

## 5. Referencia de archivos

| Ámbito | Archivo |
|--------|---------|
| Backend SQL | `docs/supabase/user_lists.sql`, `user_list_members_and_invitations.sql`, `user_lists_privacy_column.sql`, `user_lists_members_can_edit.sql`, `user_list_members_leave_policy.sql` |
| WebApp API | `webApp/src/data/supabaseApi.ts` (updateUserListWithPrivacy, leaveList, createListInvitation, fetchListMembersByListId, fetchListInvitationsByListId, fetchUsersByIds) |
| WebApp UI | `webApp/src/features/lists/ListOptionsPage.tsx`, `ListOptionsMembersBlock.tsx`, `listPrivacyOptions.ts` |
| WebApp estado/ruta | `webApp/src/app/AppContainer.tsx`, `webApp/src/core/routing.ts`, `webApp/src/hooks/domains/useAppNavigationDomain.ts` |
| Android (a implementar) | `app/.../data/Entities.kt`, `SupabaseDataSource.kt`, `ui/profile/ListOptionsScreen.kt`, `ListOptionsViewModel.kt`, `navigation/AppNavigation.kt`, `ListDetailScreen.kt` |
