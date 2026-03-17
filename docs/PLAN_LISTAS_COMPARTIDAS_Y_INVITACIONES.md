# Plan: listas compartidas, públicas e invitaciones con notificación push

**Estado:** implementado (backend, Edge Function, Android, WebApp)  
**Última actualización:** 2026-03-14  
**Ámbito:** WebApp, Android, iOS (futuro), Supabase (backend + push).

---

## 1. Objetivos

1. **Listas compartidas con personas concretas:** El dueño de una lista puede invitar a usuarios registrados de Cafesito; el invitado recibe notificación nativa (Android/iOS) y, desde la sección de notificaciones, puede **Añadir** (aceptar) o **Rechazar** la invitación. Al aceptar, la lista compartida aparece en sus listas y puede verla (y, si se define, editarla).
2. **Listas públicas:** Cualquier usuario puede descubrir listas públicas y **añadirse** para verlas (sin invitación previa).
3. **Paridad:** Mismo comportamiento en WebApp, Android e iOS (cuando exista).

---

## 2. Modelo de datos propuesto

### 2.1 Tablas existentes (sin cambios de esquema)

- **`user_lists`**: `id`, `user_id` (dueño), `name`, `is_public`, `created_at`. Se mantiene igual.
- **`user_list_items`**: `list_id`, `coffee_id`. Se mantiene igual.

### 2.2 Tablas nuevas

**`user_list_members`** — Quién puede ver (y opcionalmente editar) una lista.

| Columna     | Tipo        | Descripción |
|------------|-------------|-------------|
| `list_id`  | uuid        | FK a `user_lists.id` |
| `user_id`  | bigint      | Usuario que tiene acceso |
| `role`     | text        | `'viewer'` \| `'editor'` (el dueño no va aquí o va con `'owner'` en su lista) |
| `invited_by` | bigint    | Quien le dio acceso (null si se añadió por lista pública) |
| `created_at` | timestamptz | |

- PK: `(list_id, user_id)`.
- El **dueño** de la lista (`user_lists.user_id`) no necesita fila aquí: ya puede todo por ser owner. Las filas en `user_list_members` son para **otros** usuarios con acceso.

**`user_list_invitations`** — Invitaciones pendientes (compartir con alguien concreto).

| Columna     | Tipo        | Descripción |
|------------|-------------|-------------|
| `id`       | uuid        | PK, para usar en `notifications_db.related_id` y en acciones Añadir/Rechazar |
| `list_id`  | uuid        | FK a `user_lists.id` |
| `inviter_id` | bigint    | Quien invita (dueño de la lista) |
| `invitee_id` | bigint     | Usuario invitado |
| `status`   | text        | `'pending'` \| `'accepted'` \| `'declined'` |
| `created_at` | timestamptz | |

- Índice único: `(list_id, invitee_id)` para no duplicar invitaciones a la misma lista/persona.
- RLS: solo el inviter puede INSERT; solo el invitee puede UPDATE (accept/decline).

---

## 3. Flujos

### 3.1 Compartir lista con un usuario (invitación)

1. Dueño abre la lista → "Compartir" → elige usuario(s) de Cafesito (búsqueda por username o desde seguidores).
2. Cliente llama a RPC o inserta en `user_list_invitations` (una fila por invitado) y, por cada una, inserta en `notifications_db`:
   - `type = 'LIST_INVITE'`
   - `user_id = invitee_id`
   - `from_username = inviter username`
   - `message = "Te ha invitado a la lista «Nombre lista»"`
   - `related_id = invitation.id` (uuid)
3. El trigger existente en `notifications_db` dispara push (pg_net → Edge Function `send-notification`).
4. En el dispositivo del invitado: llega la notificación nativa; al pulsar se abre la app en la **sección de notificaciones**. Ahí la notificación LIST_INVITE se muestra con botones **Añadir** y **Rechazar**.

### 3.2 Aceptar invitación (desde notificación)

1. Usuario pulsa **Añadir** en la notificación (o en la pantalla de notificaciones).
2. Cliente llama a RPC `accept_list_invitation(invitation_id)`:
   - Comprueba que el usuario actual es el invitee y que status es `pending`.
   - Inserta en `user_list_members` (list_id, invitee_id, role = 'viewer', invited_by = inviter_id).
   - Actualiza `user_list_invitations.status = 'accepted'`.
3. La lista pasa a aparecer en "Mis listas" (o "Listas compartidas conmigo") del invitado.

### 3.3 Rechazar invitación

1. Usuario pulsa **Rechazar**.
2. Cliente llama a RPC `decline_list_invitation(invitation_id)` → `status = 'declined'`.
3. La notificación puede marcarse como leída o seguir mostrándose sin acciones.

### 3.4 Lista pública: añadirse sin invitación

1. Usuario descubre una lista pública (ej. explorar listas públicas o enlace).
2. Botón "Añadirme a esta lista" → cliente llama a RPC `join_public_list(list_id)`:
   - Comprueba que la lista existe y `is_public = true`.
   - Inserta en `user_list_members` (list_id, user_id actual, role = 'viewer', invited_by = null).
3. La lista aparece en sus listas.

### 3.5 Consultar "mis listas" (propias + compartidas)

- **Consultar:** `user_lists` donde `user_id = yo` **UNION** listas cuyo `id` está en `user_list_members` con `user_id = yo`. En el cliente (o vista SQL) se puede hacer: (1) listas propias; (2) listas donde soy miembro. Así se distingue "Mis listas" vs "Compartidas conmigo" si se desea en la UI.

### 3.6 Abrir enlace de lista en móvil (Android / iOS)

Cuando alguien abre la URL de una lista compartida (p. ej. `https://cafesitoapp.com/profile/list/{listId}`) en un dispositivo móvil:

- **Android:** El sistema abre la app nativa (App Links con `intent-filter` en `AndroidManifest.xml`). La app parsea el URI, extrae `listId` y:
  - Si el usuario **no está logado:** muestra la pantalla de login; tras iniciar sesión (o registrarse) navega a la lista y muestra el botón **Suscribirse** a la izquierda del icono de compartir para que decida si unirse.
  - Si **está logado:** navega directamente a la lista (resolviendo el dueño con `getUserListById(listId)`).
- **iOS:** Mismo flujo: si la app nativa está instalada y tiene configurados Universal Links para `cafesitoapp.com`, debe abrir la app y aplicar el mismo proceso (login si hace falta → lista → Suscribirse). Si no hay app o el enlace se abre en Safari, la WebApp ya aplica el mismo flujo (login → lista → Suscribirse).

---

## 4. Notificaciones y push

### 4.1 Tipo nuevo en `notifications_db`

- **`type = 'LIST_INVITE'`**
- **`related_id`** = UUID de la invitación (`user_list_invitations.id`), para que el cliente pueda llamar a accept/decline sin más datos.

### 4.2 Trigger de push (existente)

- El trigger `notify_fcm_on_notification_insert` ya se ejecuta en cada INSERT en `notifications_db`. No requiere cambios; solo hay que insertar filas con `type = 'LIST_INVITE'`.

### 4.3 Edge Function `send-notification`

- Añadir rama para `record.type === 'LIST_INVITE'`:
  - Título: p. ej. "Invitación a lista"
  - Cuerpo: `from_username + " te ha invitado a la lista «" + list_name + "»"` (list_name se puede obtener por `list_id` desde `user_lists` en la Edge Function o enviar en el mensaje).
  - En `data`: `type: 'LIST_INVITE'`, `related_id: invitation_id`, `list_id`, `invitation_id` (igual que related_id) para que el cliente abra notificaciones y muestre los botones.

### 4.4 Android: FCM y acciones

- **CafesitoFcmService:** Si `notificationType == "LIST_INVITE"`:
  - `contentIntent`: abrir MainActivity con `nav_type = "NOTIFICATIONS"` (o la ruta que lleve a la pestaña de notificaciones) para que al pulsar la notificación se vea la lista de notificaciones y ahí la LIST_INVITE.
  - Añadir dos acciones:
    - "Añadir" → `NotificationActionReceiver.ACTION_ACCEPT_LIST_INVITE` con `EXTRA_INVITATION_ID`.
    - "Rechazar" → `NotificationActionReceiver.ACTION_DECLINE_LIST_INVITE` con `EXTRA_INVITATION_ID`.
- **NotificationActionReceiver:** Nuevos `ACTION_ACCEPT_LIST_INVITE` y `ACTION_DECLINE_LIST_INVITE`. Al recibirlos, llamar a un repositorio/servicio que invoque la RPC correspondiente (o API Supabase) y, si hace falta, refrescar estado o navegar a listas.

### 4.5 iOS (cuando exista)

- Mismo flujo: notificación con category/actions "Añadir" y "Rechazar", abrir app en sección notificaciones; desde ahí o desde la acción, llamar accept/decline.

### 4.6 WebApp

- No hay push nativo; el usuario verá la notificación LIST_INVITE al abrir la app (en la hoja/panel de notificaciones).
- En la fila de notificación tipo LIST_INVITE: mostrar botones "Añadir" y "Rechazar" que llamen a las mismas RPC/API. Así la paridad es de flujo, no de canal (push solo donde el SO lo soporte).

---

## 5. RLS y seguridad

- **user_list_members:**  
  - SELECT: si soy el dueño de la lista (`user_lists.user_id`) o si tengo fila en `user_list_members` para esa lista.  
  - INSERT: solo el dueño de la lista (o RPC que valide inviter/invitee y lista pública para join).
- **user_list_invitations:**  
  - SELECT: el inviter o el invitee (para ver estado).  
  - INSERT: solo el dueño de la lista (inviter_id = get_my_internal_id() y list_id pertenece a ese user).  
  - UPDATE: solo el invitee (para accept/decline).
- **Lectura de listas e ítems:**  
  - Mantener política actual de `user_lists` (propias + is_public).  
  - Ampliar: también puede SELECT en `user_lists` / `user_list_items` si existe fila en `user_list_members` para ese list_id y user_id actual. Así el invitado (o quien se unió a lista pública) puede leer la lista y sus ítems.

---

## 6. Contratos recomendados (RPC / API)

| Función / uso | Descripción |
|---------------|-------------|
| `create_list_invitation(list_id, invitee_id)` | Solo dueño. Inserta en `user_list_invitations` y en `notifications_db` (LIST_INVITE). Devuelve invitation id. |
| `accept_list_invitation(invitation_id)` | Solo el invitee. Inserta en `user_list_members`, actualiza invitation a accepted. |
| `decline_list_invitation(invitation_id)` | Solo el invitee. Actualiza invitation a declined. |
| `join_public_list(list_id)` | Cualquier usuario autenticado. Comprueba is_public e inserta en `user_list_members`. |
| "Mis listas (incl. compartidas)" | Cliente: obtener listas propias + listas donde hay fila en `user_list_members` para el usuario actual; o una RPC que devuelva ambas. |

---

## 7. Resumen de cambios por capa

| Capa | Cambios |
|------|--------|
| **Supabase (SQL)** | Tablas `user_list_members`, `user_list_invitations`; RLS; RPCs `create_list_invitation`, `accept_list_invitation`, `decline_list_invitation`, `join_public_list`; política de SELECT en `user_lists`/`user_list_items` para miembros. |
| **Edge Function** | `send-notification`: soporte tipo `LIST_INVITE` (título, cuerpo, data con invitation_id/list_id). |
| **Android** | FCM: tipo LIST_INVITE, abrir notificaciones, acciones Añadir/Rechazar; NotificationActionReceiver: aceptar/rechazar llamando a RPC; SupabaseDataSource + posible ListRepository para invitaciones; UI: notificación LIST_INVITE con botones; "Mis listas" incluyendo compartidas; flujo "Compartir" en detalle de lista. |
| **WebApp** | API para create_list_invitation, accept, decline, join_public_list; NotificationsSheet: render LIST_INVITE con Añadir/Rechazar; listas: incluir compartidas en "Mis listas"; UI "Compartir lista" (selector de usuarios) y "Añadirme" en listas públicas. |
| **iOS** | Mismo flujo que Android cuando exista cliente: notificaciones con acciones, pantalla notificaciones con botones, RPCs. |
| **shared/** | Opcional: modelos o casos de uso de invitación si se quiere lógica común; si no, cada plataforma consume las RPC directamente. |

---

## 8. Orden sugerido de implementación

1. **Backend:** Tablas, RLS, RPCs y ampliación de políticas de lectura en listas/ítems.
2. **Edge Function:** LIST_INVITE en `send-notification`.
3. **Android:** Datasource/RPC, FCM LIST_INVITE + acciones, NotificationActionReceiver, UI notificaciones y "Compartir" / "Mis listas".
4. **WebApp:** API, NotificationsSheet LIST_INVITE, listas compartidas y públicas, "Compartir" y "Añadirme".
5. **iOS:** Replicar flujo cuando el cliente esté listo.
6. **Documentación:** Actualizar `ANALITICAS.md` y `ACCESIBILIDAD_WEBAPP_ANDROID.md` si se añaden pantallas o eventos (ej. evento `list_invite_accepted` en Android).

---

## 9. Notas

- **Favoritos:** La lista por defecto "Favoritos" (`local_favorites`) no se comparte; solo las listas personalizadas (`user_lists`).
- **Borrado de cuenta:** En la función de borrado de usuario, borrar filas en `user_list_invitations` (como inviter o invitee) y en `user_list_members` donde `user_id = p_user_id`.
- **Listas públicas:** Si se quiere "descubrir" listas públicas, hará falta una vista o endpoint que liste listas con `is_public = true` (con paginación); puede ser una fase posterior.

Si esta propuesta encaja con lo que quieres (listas para ti + compartir con quien quieras + listas públicas para que quien quiera se una + notificación nativa con Añadir/Rechazar que lleve a notificaciones), el siguiente paso sería implementar en el orden anterior y ajustar detalles (por ejemplo, rol `editor` vs solo `viewer` en la primera versión).
