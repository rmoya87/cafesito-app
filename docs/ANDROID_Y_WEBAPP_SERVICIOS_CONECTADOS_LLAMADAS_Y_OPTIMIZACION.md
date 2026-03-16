# Android y WebApp: servicios conectados, llamadas y optimización

**Estado:** vivo  
**Última actualización:** 2026-03-15  
**Ámbito:** App Android y WebApp — servicios externos, capa de datos y posibles optimizaciones.

---

## Checklist de cambios a realizar

### Android (prioridad alta)

**Requisito previo:** Toda solución debe ser **robusta** y mantener **Supabase/BDD como fuente de verdad**. En caso de error, expiración de caché o inconsistencia, se debe llamar a Supabase para refrescar datos; la caché es capa de rendimiento, no sustituye al backend.

- [x] Centralizar syncs: que las pantallas no llamen `syncCoffees()` / `syncUsers()` directamente; usar `SyncManager` con intervalo mínimo o flags "dirty" por recurso. En fallo de red o error, reintentar contra Supabase y no depender solo de datos locales. **(Hecho: `SyncManager.syncCoffeesIfNeeded` / `syncUsersIfNeeded` con TTL 10 min y reintento; ProfileViewModel, TimelineViewModel, SearchViewModel, DiaryViewModel usan SyncManager.)**
- [x] Caché para listas: introducir Room (o flujo único) para `user_lists` e ítems con TTL corto; evitar refetch en cada vista. **Robusto:** en error al leer caché, expiración de TTL o petición explícita de refresco, llamar siempre a Supabase y actualizar caché; la BDD es la fuente de verdad. **(Hecho: caché en memoria en `SupabaseDataSource` con TTL 5 min, `getCachedUserLists` / `getCachedSharedWithMeLists`, reintento en error; invalidación al crear/editar/borrar/salir/invitar.)**
- [x] Caché para recomendaciones: cachear RPC `get_coffee_recommendations` en memoria o Room con TTL (p. ej. 1 h); invalidar al cambiar preferencias o tras sync. **Robusto:** en error o caché expirada, llamar a Supabase; no servir solo datos obsoletos sin intentar refresco. **(Hecho: `getRecommendationsWithCache(userId)` en `SupabaseDataSource` con TTL 1 h y reintento; `invalidateRecommendationsCache` disponible.)**

### Android (prioridad media)
- [x] Realtime: valorar un solo canal con filtro por tabla o suscripción solo a tablas de la pantalla visible (reducir 4 canales). **(Hecho: eliminados canales likes, comments y posts; solo queda `subscribeToNotifications(userId)` para `notifications_db`. SocialRepository ya no suscribe a Realtime.)**
- [x] Realtime: documentar política de reintento y backoff; exponer estado conectado/desconectado si afecta a la UI. **(Hecho: política documentada en §3.2.1 — reintento 3 veces con backoff 1s/2s/4s en `UserRepository.getNotificationsForUser`; polling 3s como respaldo. Estado conectado/desconectado no expuesto a la UI; doc indica cómo hacerlo si se necesita.)**
- [x] Sync tras login: valorar ejecutar solo lo imprescindible para la pantalla inicial y diferir el resto (WorkManager o al cambiar de pestaña). **(Hecho: `syncEssentialForLaunch()` tras login — usuarios, follows, cafés, favoritos; `syncDeferred()` a los 5 s — social, despensa, diario pendiente. Ver §6.)**

### Android (prioridad baja)
- [x] Asegurar que todas las pantallas que usan catálogo usen Paging o rangos acotados (no `getAllCoffees` con range 0–9999). **(Hecho: sync de cafés usa `fetchAllCoffeesPaginated()` en páginas de 500; `getAllCoffees` acotado a `MAX_COFFEES_SINGLE_FETCH` (2000). Ver §3.2 y SupabaseDataSource.)**
- [x] Revisar que todos los flujos que hacen fetch comprueben conectividad (ConnectivityObserver). **(Hecho: `syncCoffees()` y `getNotificationsForUser` comprueban conectividad antes de llamar a Supabase; networkBoundResource ya la usa. Ver §3.4.)**
- [x] Timeouts: valorar timeout mayor solo para subida de imagen u operaciones lentas concretas. **(Hecho: 30 s global en SupabaseModule; comentario en código y doc para valorar 60 s en subidas si hay fallos. Ver §3.4.)**
- [x] Documentar política de Realtime (reconexión, backoff). **(Hecho: §3.2.1 y §3.2.2 — reintento 3× con backoff 1s/2s/4s, polling 3s respaldo.)**

### WebApp (prioridad alta)

**Requisito previo:** Solución **robusta**; **Supabase/BDD como fuente de verdad**. En error o inconsistencia, llamar a Supabase para refrescar; la caché no sustituye al backend.

- [x] Revisar límites de `fetchInitialData` (1500 users, 2000 coffees, 3000 reviews, etc.); valorar reducir o paginación por sección. En fallo, reintentar contra Supabase y no quedarse solo con caché antigua. **(Hecho: constantes `INITIAL_DATA_LIMITS` — users 1200, coffees 1500, reviews/sensory 2000, etc. En fallo se limpia caché y se reintenta una vez tras 2 s. Ver §4.6.)**
- [x] Revisar número de peticiones en carga post-login (fetchUserData + 4 llamadas + fetchCoffeeIdsInLists); documentar o agrupar. En error de alguna petición, refrescar desde Supabase para mantener coherencia. **(Hecho: flujo documentado en §4.6; `useUserDataLoader` reintenta una vez (2 s) en error. Fuente de verdad: Supabase.)**

### WebApp (prioridad media)
- [x] Caché para listas: estado global o en memoria con TTL corto; evitar refetch innecesario al navegar. **Robusto:** en error o TTL expirado, llamar a Supabase y actualizar; BDD como fuente de verdad. **(Hecho: caché en memoria por userId, TTL 5 min; `fetchUserLists`/`fetchSharedWithMeLists` la usan; reintento en error; `invalidateUserListsCache(userId?)` en crear/editar/borrar/añadir/quitar ítem. Ver §4.6.)**
- [x] Caché `fetchInitialData`: considerar TTL configurable o invalidación por pestaña (mantener 90 s como base). **Robusto:** en error al cargar, reintentar Supabase; no servir solo datos obsoletos sin intentar refresco. **(Hecho: TTL exportado `INITIAL_DATA_CACHE_TTL_MS` (90 s); en error se limpia caché y se reintenta una vez. Ver §4.6.)**

### WebApp (prioridad baja)
- [x] Valorar un RPC "user session bundle" para reducir round-trips tras login (opcional). **(Valorado en §4.7: 1 RPC que devuelva diario, despensa, favoritos, listas, notificaciones, etc.; reduciría 1+4+1 a 1 llamada; opcional, requiere función en Supabase.)**
- [x] Confirmar que Realtime hace `unsubscribe` al desmontar en `useUserDataLoader` (ya implementado). **(N/A: la suscripción Realtime fue eliminada de `useUserDataLoader`; no hay canal que desuscribir. Ver §4.4.)**

---

## 1. Android — Servicios conectados

| Servicio | Uso | Configuración | Módulo DI |
|----------|-----|----------------|-----------|
| **Supabase** | Backend principal: API REST (PostgREST), Auth, Realtime, Storage | `BuildConfig.SUPABASE_URL`, `BuildConfig.SUPABASE_PUBLISHABLE_KEY` | `SupabaseModule` |
| **Firebase Analytics** | Eventos de uso (pantallas, acciones) | Google Services / `google-services.json` | `AnalyticsModule` |
| **Firebase Cloud Messaging (FCM)** | Push: notificaciones (likes, comentarios, invitaciones a listas) y registro de token | Idem | Uso directo + `UserRepository.updateFcmToken` → Supabase |

**Nota:** Las notificaciones push se envían desde **Supabase** (Edge Function `send-notification` invocada por trigger en BD), no desde el cliente. El cliente solo recibe el mensaje vía FCM y registra/actualiza el token en Supabase.

---

## 2. WebApp — Servicios conectados

| Servicio | Uso | Configuración | Dónde |
|----------|-----|----------------|-------|
| **Supabase** | Backend: PostgREST, Auth (Google OAuth / PKCE), Realtime (notificaciones), Storage | `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY` (o `__SUPABASE_URL__`/`__SUPABASE_ANON_KEY__` en build) | `webApp/src/supabase.ts` |
| **Google Analytics 4 (GA4)** | Eventos de página y uso (page_view por ruta SPA) | `VITE_GA4_MEASUREMENT_ID` (por defecto `G-BMZEQNRKR4`) | `webApp/src/core/ga4.ts` |

La WebApp **no** usa Firebase Cloud Messaging (es web; las notificaciones push nativas son solo Android/iOS). El envío de notificaciones a otros usuarios se hace vía RPC `create_notification` o insert en `notifications_db`; la Edge Function envía el push a FCM para clientes móviles.

---

## 3. Android — Supabase: detalle de conexión y llamadas

### 3.1 Configuración del cliente

- **Origen:** `app/di/SupabaseModule.kt`
- **Componentes instalados:** PostgREST, Auth (Google ID Token), Realtime, Storage.
- **Timeouts HTTP (Ktor):** 30 s (request, connect, socket).
- **Serialización:** KotlinX JSON con `ignoreUnknownKeys`, `coerceInputValues`, `encodeDefaults`.

La app usa un único `SupabaseClient` en modo singleton. Todas las llamadas remotas pasan por `SupabaseDataSource` (y en shared por `SupabaseRemoteDataSource` para reseñas).

### 3.2 Llamadas por ámbito (SupabaseDataSource)

Resumen por tabla/RPC y si hay caché local (Room).

| Ámbito | Tabla / RPC | Operaciones | ¿Caché Room? |
|--------|-------------|-------------|--------------|
| **Storage** | `storage.from(bucket)` | `uploadImage` (avatars, fotos) | No |
| **Realtime** | canal postgres | `subscribeToNotifications(userId)` (tabla `notifications_db`). Likes/comments/posts eliminados; solo notificaciones. | No (eventos en tiempo real) |
| **Usuarios** | `users_db` | `getAllUsers`, `getUserById`, `getUserByUsername`, `getUserByUsernameInsensitive`, `getUserByGoogleId`, `upsertUser`, `touchUserLastInteraction`, `requestAccountDeletion`, `cancelAccountDeletion`, `getAccountLifecycleInfo`, `hardDeleteAccountData` | Sí (UserDao) |
| **FCM tokens** | `user_fcm_tokens` | `insertUserToken` (upsert por `user_id`) | No |
| **Seguimientos** | `follows` | `getAllFollows`, `insertFollow`, `deleteFollow` | Sí |
| **Cafés** | `coffees` | `fetchAllCoffeesPaginated` (sync en páginas de 500), `getAllCoffees` (rango acotado 0–1999), `getCoffeesPaginated`, `upsertCoffees`, `getCoffeesByIds`, `getCoffeeByBarcode`, `getCustomCoffees`, etc. | Sí (CoffeeDao) |
| **Recomendaciones** | RPC `get_coffee_recommendations` | `getRecommendationsRpc(userId)` | No (se pide bajo demanda) |
| **Publicaciones** | `posts_db` | Ya no se cargan. `insertPost`, `deletePost` y borrado en `hardDeleteAccountData` se mantienen. | Sí (Room; vacío, no se hace fetch) |
| **Tags café en posts** | `post_coffee_tags` | Ya no se cargan. `upsertPostCoffeeTag`, `deletePostCoffeeTag` y borrado en cuenta se mantienen. | Sí (Room; vacío) |
| **Comentarios** | `comments_db` | Ya no se cargan. `insertComment`, `upsertComment`, `deleteComment`, `updateComment` y borrado en cuenta se mantienen. | Sí (Room; vacío) |
| **Likes** | `likes_db` | Ya no se cargan. `insertLike`, `deleteLike` y borrado en cuenta se mantienen. | Sí (Room; vacío) |
| **Reseñas** | `reviews_db` | `getAllReviews`, `getReviewsByCoffeeId`, `upsertReview`, `deleteReview` | Sí |
| **Perfil sensorial** | `coffee_sensory_profiles` | `getSensoryProfilesByCoffeeId`, `getSensoryProfilesByUserId`, `upsertSensoryProfile` | Sí (vía ReviewRepository/shared) |
| **Favoritos** | `local_favorites` | `getFavoritesByUserId`, `insertFavorite`, `deleteFavorite` | Sí |
| **Diario** | `diary_entries` | `getDiaryEntries`, `insertDiaryEntry`, `upsertDiaryEntry`, `deleteDiaryEntry` | Sí (DiaryRepository + Room) |
| **Despensa** | `pantry_items` | `getPantryItems`, `upsertPantryItem`, `deletePantryItemById` | Sí |
| **Historial** | `pantry_historical` | `getFinishedCoffees`, `insertFinishedCoffee` | Sí |
| **Notificaciones** | `notifications_db` + RPCs | `insertNotification` (RPC `create_notification` o insert), `getNotificationsForUser` (RPC `get_notifications_for_user` o select), `markNotificationRead`, `markAllNotificationsRead`, `deleteNotification` | Sí (UserRepository/notifications) |
| **Listas** | `user_lists`, `user_list_items`, `user_list_members`, `user_list_invitations` | `getUserLists`, `getUserListById`, `getSharedWithMeLists`, `createUserList`/`createUserListWithPrivacy`, `getUserListItems`, `getListItemsWithMetaForUser`, `addUserListItem`, `removeUserListItem`, `updateUserList`/`updateUserListWithPrivacy`, `leaveList`, `fetchListMembersByListId`, `fetchListInvitationsByListId`, `deleteUserList`, `getCoffeeIdsInUserLists` | No (solo remoto o en memoria) |
| **Invitaciones listas** | RPCs | `createListInvitation`, `acceptListInvitation`, `declineListInvitation`, `joinPublicList` | No |

### 3.2.1 Realtime (Android) — solución unificada

- **Un solo uso:** notificaciones. Canal `notifications-{userId}`, tabla `notifications_db`.
- **API:** `SupabaseDataSource.subscribeToNotifications(userId)`; usado en `UserRepository.getNotificationsForUser(userId)` (Flow que combina Realtime + polling de respaldo).
- **Eliminado:** suscripciones a `likes_db`, `comments_db` y `posts_db`. Esa funcionalidad (timeline de posts/likes/comentarios) ya no se usa en la app; los canales Realtime de social se quitaron y `SocialRepository` ya no inicia canales Realtime.

**Política de reintento y backoff (Realtime notificaciones):**

- **Reintento:** Si el canal Realtime falla o se cae, el Flow en `UserRepository.getNotificationsForUser` hace hasta **3 reintentos** con backoff exponencial: 1 s, 2 s, 4 s. Tras agotar reintentos, el flujo sigue vivo gracias al **polling cada 3 s**, que actúa como respaldo y garantiza que la lista de notificaciones se actualice aunque Realtime no esté conectado.
- **Backoff:** Delays 1 s → 2 s → 4 s entre intentos (sin jitter adicional). El cliente Supabase Realtime puede además reconectar por su cuenta; nuestro retry cubre fallos puntuales al suscribir o al recibir eventos.
- **Estado conectado/desconectado:** No se expone actualmente a la UI. Si en el futuro se quiere mostrar un indicador (“en tiempo real” vs “actualizando cada 3 s”), puede añadirse un `StateFlow` derivado del flujo (p. ej. último evento Realtime recibido vs solo polling).

### 3.2.2 Política de Realtime (reconexión, backoff)

- **Reconexión:** El canal de notificaciones se suscribe una vez por `getNotificationsForUser(userId)`. Si el canal falla, el Flow aplica **retry** con backoff (véase §3.2.1); no hay reconexión explícita de canal — se depende del retry y del polling cada 3 s.
- **Backoff:** 1 s, 2 s, 4 s entre reintentos (hasta 3). Tras 3 fallos se deja de reintentar y el flujo sigue con polling.
- **Resumen:** Política documentada en §3.2.1; este apartado la referencia como “política de Realtime” del proyecto.

### 3.3 Auth (Supabase Auth)

- **Login:** `supabaseClient.auth.signInWith(IDToken) { provider = Google }` (en `UserRepository.signInWithSupabase`).
- **Logout:** `supabaseClient.auth.signOut()` (solo si hay red).
- **Sesión:** Se restaura al arrancar; el JWT se usa en todas las peticiones PostgREST/Storage/Realtime.

### 3.4 Conectividad y timeouts (Android)

- **ConnectivityObserver:** Los flujos que hacen fetch usan `connectivityObserver.observe().first()` antes de llamar a Supabase cuando no pasan por `networkBoundResource` (que ya lo comprueba). Ejemplos: `CoffeeRepository.syncCoffees()`, `UserRepository.getNotificationsForUser()`; UserRepository, SocialRepository, DiaryRepository y CoffeeRepository usan el observer en networkBoundResource y en operaciones suspend que escriben en remoto.
- **Timeouts (Ktor):** En `SupabaseModule` se configuran 30 s para request, connect y socket (global). Para **subida de imagen** (Storage) en redes lentas, si se observan timeouts, valorar aumentar a 60 s en la config global o en un cliente dedicado a Storage; hay comentario en código apuntando a este doc.

---

## 4. WebApp — Supabase y GA4: detalle

### 4.1 Configuración del cliente Supabase

- **Origen:** `webApp/src/supabase.ts`
- **Cliente:** `createClient(url, anonKey, { auth: { flowType: "pkce", persistSession: true, autoRefreshToken: true, detectSessionInUrl: true } })`. No se configuran timeouts explícitos (usa los por defecto del cliente JS).
- **Punto de uso:** `getSupabaseClient()`. Todas las llamadas de datos pasan por `webApp/src/data/supabaseApi.ts`.

### 4.2 Llamadas por ámbito (supabaseApi.ts)

| Ámbito | Tabla / RPC | Operaciones principales | ¿Caché? |
|--------|-------------|-------------------------|---------|
| **Datos iniciales** | Varias | `fetchInitialData`: users_db, coffees, reviews_db, coffee_sensory_profiles, follows (Promise.all, 5 peticiones). posts_db, likes_db, comments_db, post_coffee_tags ya no se cargan. | Sí: caché en memoria 90 s |
| **Usuario/diario/despensa** | diary_entries, pantry_items, local_favorites, pantry_historical, coffees (custom) | `fetchUserData`: diario, despensa, favoritos, historial, cafés custom (paralelo + 1 extra) | No |
| **Actividad listas** | user_lists, user_list_items | `fetchAllListItemsForActivity`, `fetchProfileUserActivityData` (diario + favoritos + listItems) | No |
| **Timeline (legado)** | likes_db, comments_db, posts_db, users_db | `toggleLike`, `createComment`, `updateComment`, `deleteComment`, `updatePost`, `deletePost`, `addPostCoffeeTag`, `notifyMentionsForText`. Solo para borrado de cuenta y APIs residuales; la timeline ya no carga posts/likes/comments. | No |
| **Usuarios** | users_db | `fetchUserById`, `fetchUsersByIds`, `updateUserProfile` | No |
| **Seguimientos** | follows | `toggleFollow` (select + insert/delete) | No |
| **Favoritos** | local_favorites | `toggleFavoriteCoffee` | No |
| **Listas** | user_lists, user_list_items, user_list_members, user_list_invitations | `fetchUserListById`, `fetchUserLists`, `fetchSharedWithMeLists`, `createUserList`, `updateUserList`/`updateUserListWithPrivacy`, `fetchUserListItems`, `addUserListItem`, `removeUserListItem`, `fetchListMembersByListId`, `fetchListInvitationsByListId`, `deleteUserList`, `fetchCoffeeIdsInUserLists`, `fetchCoffeeIdsInLists` | No |
| **Invitaciones** | RPCs | `createListInvitation`, `acceptListInvitation`, `declineListInvitation`, `joinPublicList`, `leaveList` | No |
| **Reseñas y sensorial** | reviews_db, coffee_sensory_profiles | `upsertCoffeeReview`, `deleteCoffeeReview`, `upsertCoffeeSensoryProfile` | No |
| **Despensa** | pantry_items, pantry_historical | `insertPantryItem`, `updatePantryItem`, `deletePantryItemById`, `insertFinishedCoffee` | No |
| **Cafés** | coffees | `upsertCustomCoffee` (insert/upsert) | No |
| **Diario** | diary_entries, pantry_items | `createDiaryEntry`, `deleteDiaryEntry`, `updateDiaryEntry` | No |
| **Notificaciones** | notifications_db | `fetchNotifications`, `markNotificationsAsRead`, `deleteNotification` | No |
| **Cuenta** | users_db | `requestAccountDeletion`, `syncAccountLifecycleAfterLogin`; borrado completo: post_coffee_tags, comments_db, likes_db, local_favorites, reviews_db, coffee_sensory_profiles, diary_entries, pantry_items, notifications_db, user_list_invitations, user_list_members, follows, posts_db, users_db | No |
| **Storage** | Storage | `uploadImageFile` (bucket, path, file) | No |

### 4.3 Auth (WebApp)

- **Login:** Supabase Auth con proveedor Google (OAuth / PKCE). Sesión persistida y auto-refresh.
- **Logout:** `supabase.auth.signOut()`.

### 4.4 Realtime y llamadas periódicas (WebApp)

- **Sin Realtime ni polling:** No hay suscripción Realtime a `notifications_db` ni refetch al volver a la pestaña. Los datos se cargan **una sola vez** al cargar la app (y al hacer login). Para actualizar, el usuario recarga la página. Así se evitan llamadas periódicas.

### 4.5 GA4 (WebApp)

- **Inicialización:** `initGa4()` en `main.tsx` (no se carga en localhost ni sin red).
- **Page views:** `sendPageView()` en cada cambio de ruta (SPA) desde `AppContainer`.
- **Eventos:** Ver `docs/ANALITICAS_WEB_Y_ANDROID.md`.

### 4.6 WebApp — Límites, post-login, caché listas y TTL

- **Límites `fetchInitialData`:** Constantes `INITIAL_DATA_LIMITS` en `supabaseApi.ts`: users 1200, coffees 1500, reviews 2000, sensoryProfiles 2000, follows 1500. posts_db, likes_db, comments_db y post_coffee_tags ya no se cargan (código residual eliminado). En fallo se limpia la caché y se reintenta una vez tras 2 s; no se sirve solo caché antigua.
- **Carga post-login:** En `useUserDataLoader`: (1) `fetchUserData(userId)` (diario, despensa, favoritos, historial, cafés custom); (2) en paralelo `fetchNotifications`, `fetchUserLists`, `fetchSharedWithMeLists`, `fetchAllListItemsForActivity` (4 llamadas); (3) `fetchCoffeeIdsInLists(mergedListIds)`. Total: 1 + 4 + 1 bloques lógicos. En error se reintenta una vez (2 s) para mantener coherencia desde Supabase.
- **Caché listas:** Caché en memoria por `userId`, TTL 5 min. `fetchUserLists` y `fetchSharedWithMeLists` leen/escriben esa caché; en error se reintenta contra Supabase. `invalidateUserListsCache(userId?)` se llama al crear, actualizar, borrar lista y al añadir/quitar ítem. BDD como fuente de verdad.
- **Caché `fetchInitialData`:** TTL exportado `INITIAL_DATA_CACHE_TTL_MS` (90 s). En error al cargar se limpia caché y se reintenta una vez; no se sirven solo datos obsoletos.
- **Sin llamadas periódicas:** No hay `setInterval` para refetch de datos ni actualización de `recommendationDateKey` cada minuto; la clave de recomendación del día queda fija hasta recarga. El único intervalo que permanece es el de comprobación de actualización del Service Worker en `main.tsx` (cada 5 min), que no es una llamada a API.

### 4.7 WebApp — Valoración RPC "user session bundle" (opcional)

- **Objetivo:** Reducir round-trips tras login. Hoy `useUserDataLoader` hace: (1) `fetchUserData(userId)`; (2) en paralelo `fetchNotifications`, `fetchUserLists`, `fetchSharedWithMeLists`, `fetchAllListItemsForActivity`; (3) `fetchCoffeeIdsInLists(mergedListIds)`. Total 1 + 4 + 1 bloques de red.
- **Propuesta:** Un RPC en Supabase (p. ej. `get_user_session_bundle(p_user_id)`) que devuelva en una sola respuesta: diario, despensa, favoritos, historial, cafés custom, notificaciones, listas propias, listas compartidas, ítems de listas para actividad, y los `coffee_id` en listas (o las listas ya con ítems). El cliente haría una sola llamada y mapearía el resultado al estado actual.
- **Ventajas:** Menos latencia (1 ida y vuelta en lugar de varias), menos conexiones, más simple en el cliente si se mantiene un único contrato.
- **Inconvenientes:** Requiere crear y mantener la función en Supabase/Postgres; payload puede ser grande; hay que definir el formato (JSON) y actualizarlo si se añaden campos. La caché de listas (TTL 5 min) y la lógica de invalidación seguirían aplicando en cliente.
- **Conclusión:** Opcional. Si en el futuro se quiere optimizar aún más el tiempo de carga post-login, implementar este RPC y sustituir en `useUserDataLoader` las 6 fuentes por una llamada a `get_user_session_bundle(userId)`.

---

## 5. Android — Firebase

### 5.1 Analytics

- **Inyección:** `AnalyticsModule` → `FirebaseAnalytics`.
- **Uso:** `AnalyticsHelper` (eventos de pantalla, login, listas, notificaciones, etc.). Ver `docs/ANALITICAS_WEB_Y_ANDROID.md`.

### 5.2 Cloud Messaging

- **Servicio:** `CafesitoFcmService` (FirebaseMessagingService).
- **Flujo:**  
  - `onNewToken` → `UserRepository.updateFcmToken(token)` → Supabase `user_fcm_tokens` (upsert).  
  - `onMessageReceived` → muestra notificación local y, según `type`, navega a notificaciones/post/comentario/invitación.
- **Registro de token:** También en `AppSessionCoordinator` (al autenticar y al pasar a primer plano, con throttling de 60 s).

---

## 6. Android — Sincronización (SyncManager) y cuándo se llama

- **Definición:** `app/data/SyncManager.kt`.
- **Sync tras login (prioridad pantalla inicial):**
  - **Esencial:** `syncEssentialForLaunch()` — usuarios, seguimientos, cafés, favoritos (lo mínimo para Timeline/Home). Se ejecuta en `AppSessionCoordinator.onAuthenticated` justo tras login para que la primera pantalla tenga datos sin esperar al sync completo.
  - **Diferido:** `syncDeferred()` — social, despensa, diario pendiente. Se lanza **5 segundos después** del esencial en el mismo scope, para no bloquear la UI; el usuario ya está viendo la pantalla inicial.
- **Social (SocialRepository.syncSocialData):** Ya no se cargan posts, likes, comentarios ni post_coffee_tags desde Supabase; solo se sincronizan reseñas y se vacía la caché local de posts/likes/comments/tags. Equivalente al cambio en WebApp (fetchInitialData sin posts/likes/comments/postCoffeeTags).
- **Sync completo:** `syncAll(force)` ejecuta en paralelo: coffees, favorites, users, follows, social, pantry, diary pending. Intervalo mínimo 10 min entre ejecuciones (salvo `force = true`).
- **Dónde se invoca:**
  - Tras login: `AppSessionCoordinator.onAuthenticated` → `syncEssentialForLaunch()` y, con delay 5 s, `syncDeferred()`.
  - Otras pantallas (pull-to-refresh o entrada): `ProfileViewModel`, `TimelineViewModel`, `SearchViewModel`, `DiaryViewModel` llaman a `syncManager.syncCoffeesIfNeeded(force)`, `syncUsersIfNeeded(force)`, etc., centralizado en SyncManager con intervalo y reintento.
- **Protecciones:** Mutex para evitar sincronizaciones simultáneas; reintento en error en sync esencial y en sync completo.

---

## 7. WebApp — Carga inicial y datos de usuario

- **Al arrancar (sin usuario):** Se llama `fetchInitialData()` (9 peticiones en paralelo; resultado cacheado 90 s). Origen típico: hook o efecto que carga el bundle inicial.
- **Tras login (usuario activo):** En `useUserDataLoader` se llama `fetchUserData(userId)` (diario, despensa, favoritos, historial, cafés custom) y en paralelo `fetchNotifications`, `fetchUserLists`, `fetchSharedWithMeLists`, `fetchAllListItemsForActivity`; luego `fetchCoffeeIdsInLists(mergedListIds)`. No hay un "SyncManager" único; cada pestaña o flujo puede disparar sus propias cargas.

---

## 8. Posibles optimizaciones

### 8.1 Android — Reducción de llamadas y duplicados

| Área | Situación | Optimización sugerida |
|------|-----------|------------------------|
| **Sync tras login** | `syncAll()` lanza 7 flujos en paralelo (cafés, favoritos, usuarios, follows, social, despensa, diario pendiente). | Mantener paralelismo; valorar ejecutar solo lo imprescindible para la pantalla inicial y diferir el resto (p. ej. WorkManager o al cambiar de pestaña). |
| **Sync desde pantallas** | Varios ViewModels llaman `syncCoffees()` o `syncUsers()` al entrar/refrescar. | Centralizar más en `SyncManager` con intervalo mínimo y/o "dirty" flags por recurso; evitar varios `syncCoffees()` seguidos en distintas pantallas. |
| **Listas (user_lists)** | Sin caché Room; cada vista de listas/perfil puede pedir `getUserLists` + `getSharedWithMeLists` y luego ítems/miembros. | Introducir caché en Room para listas e ítems (y TTL corto) o un único flujo que rellene caché y el resto lea desde ahí. |
| **Recomendaciones** | RPC `get_coffee_recommendations` cada vez que se muestra la sección. | Cachear resultado en memoria o Room con TTL (p. ej. 1 h) e invalidar al cambiar preferencias o tras sync. |
| **getAllCoffees / getCoffeesPaginated** | ~~Catálogo con `range(0, 9999)`~~ | **Hecho:** sync usa `fetchAllCoffeesPaginated()` (páginas de 500); `getAllCoffees` acotado a 2000. CoffeePagingSource para buscador. |

### 8.2 Android — Realtime

| Área | Situación | Optimización sugerida |
|------|-----------|------------------------|
| **Canales** | 4 canales: likes, comments, posts, notifications (por usuario). | Valorar un solo canal con filtro por tabla o suscribirse solo a las tablas de la pantalla visible para reducir conexiones y mensajes. |
| **Reconexión** | Realtime puede desconectarse. | Comprobar política de reintento y backoff del cliente Supabase/Realtime y documentarla; exponer estado "conectado/desconectado" si afecta a la UI. |

### 8.3 Android — Timeouts y red

| Área | Situación | Optimización sugerida |
|------|-----------|------------------------|
| **Timeouts** | 30 s global (request, connect, socket). | Aceptable para móvil; para operaciones muy lentas (p. ej. subida de imagen) valorar timeout mayor solo en esa llamada. |
| **ConnectivityObserver** | Ya se usa para no lanzar peticiones sin red en flujos críticos. | Revisar que todos los flujos que hacen fetch comprueben conectividad (o pasen por un único punto que lo haga). |

### 8.4 Android — Worker y notificaciones

| Área | Situación | Optimización sugerida |
|------|-----------|------------------------|
| **TimelineNotificationWorker** | Obtiene usuario activo, lista de usuarios y notificaciones; ya no llama a `syncUsers()`. | Mantener así; si en el futuro se añaden más datos, seguir evitando syncs pesados en el worker. |

### 8.5 WebApp — Optimizaciones sugeridas

| Área | Situación | Optimización sugerida |
|------|-----------|------------------------|
| **fetchInitialData** | 9 peticiones en paralelo; caché 90 s. Límites altos (1500 users, 2000 coffees, 3000 reviews, etc.). | Valorar reducir límites o paginación por sección; mantener caché y considerar TTL configurable o invalidación por pestaña. |
| **fetchUserData + listas** | Tras login: fetchUserData (varias tablas) + 4 llamadas (notifications, ownedLists, sharedLists, listItemsForActivity) + fetchCoffeeIdsInLists. | Agrupar en un "user session bundle" opcional (un RPC o menos round-trips) para reducir latencia; o mantener paralelo y documentar orden de carga. |
| **Listas** | Varias llamadas por vista (getUserLists, getSharedWithMeLists, ítems, miembros, invitaciones). | Caché en memoria o en estado global con TTL corto; evitar refetch innecesario al navegar entre listas. |
| **Realtime** | Un canal por usuario (notificaciones). | Adecuado; asegurar unsubscribe al desmontar (ya implementado en useUserDataLoader). |
| **GA4** | Script solo si no es localhost y hay red. | Mantener; no cargar en dev evita ruido y errores de red. |

### 8.6 Resumen de prioridad (ambas plataformas)

**Android:**  
1. **Alta:** Centralizar y acotar syncs desde pantallas; considerar caché para listas y recomendaciones.  
2. **Media:** Revisar uso de Realtime (canales por pantalla).  
3. **Baja:** Timeouts por operación; documentar política de Realtime.

**WebApp:**  
1. **Alta:** Revisar límites y número de peticiones en `fetchInitialData` y carga post-login.  
2. **Media:** Caché o bundle para datos de listas y usuario.  
3. **Baja:** Un único "session bundle" RPC opcional para reducir round-trips tras login.

---

## 9. Referencias

- Conexión y problemas típicos: `docs/supabase/CONEXION_SUPABASE_ANDROID.md`
- Listas 500 / RLS: `docs/supabase/user_lists_500_troubleshooting.md`
- Analíticas: `docs/ANALITICAS_WEB_Y_ANDROID.md`
- **Android:** Cliente Supabase `app/di/SupabaseModule.kt`; llamadas `app/data/SupabaseDataSource.kt`; sincronización `app/data/SyncManager.kt`.
- **WebApp:** Cliente `webApp/src/supabase.ts`; API `webApp/src/data/supabaseApi.ts`; GA4 `webApp/src/core/ga4.ts`; carga usuario `webApp/src/hooks/domains/useUserDataLoader.ts`.
