# Plan: Offline-First por pantalla, galería/cámara y permisos

**Estado:** vigente (plan de referencia).  
**Última actualización:** 2026-03-04  

## 1. Contexto actual

- **Sincronización**: En MainActivity se llama `syncManager.syncAll()` al autenticarse. TimelineViewModel y SearchViewModel hacen `refreshData()` en `init`, provocando precarga global al arrancar.
- **Repositorios**: RepositoryUtils define `networkBoundResource` con `resourceKey` y `CACHE_TIMEOUT`; la precarga se dispara cuando hay suscriptor al Flow y `needsFetch` es true.
- **Permisos**: En AndroidManifest **no** están declarados `CAMERA`, `READ_MEDIA_IMAGES` ni `READ_EXTERNAL_STORAGE`.
- **Galería en AddPost**: AddPostScreen usa Accompanist para permisos y `loadGalleryImages()`; sin permisos en manifest la galería queda vacía.
- **Cámara**: Se usa `TakePicturePreview()` sin permiso CAMERA; en algunos dispositivos la app se sale del flujo al volver de la cámara.

---

## 2. Objetivos

1. **Offline-First por pantalla**: Precarga inicial **solo** al acceder a cada pantalla concreta; refrescar **solo el componente afectado** tras acciones del usuario, consolidando en Supabase.
2. **Todas las pantallas**: Aplicar el mismo patrón (precarga al entrar + refresh tras acción) en Timeline, Buscador, Perfil, Notificaciones, Buscador de usuarios, BrewLab, Diario, Stock, Detalle de café, Seguidores/Siguiendo, Nueva publicación, etc.
3. **Nueva publicación**: Galería visible (permisos) y flujo de cámara estable (permiso CAMERA + TakePicture con URI).
4. **Verificación**: Compilación y pruebas manuales.

---

## 3. Listado de pantallas y estrategia Offline-First

| Pantalla | Ruta | ViewModel | Repos / datos | Precarga al entrar | Refresh tras acción |
|----------|------|-----------|----------------|--------------------|---------------------|
| **Onboarding** | `onboarding` | — | — | No (sin datos remotos) | — |
| **Login** | `login` | LoginViewModel | UserRepository (auth) | No (solo al enviar login) | — |
| **Completar perfil** | `completeProfile` | CompleteProfileViewModel | UserRepository | No (formulario) | Tras guardar → triggerRefresh usuarios |
| **Timeline** | `timeline` | TimelineViewModel | SocialRepository, UserRepository, CoffeeRepository | Sí: loadInitialIfNeeded() en LaunchedEffect(Unit) | Tras like/comentar/crear post/eliminar → socialRepository.triggerRefresh() (y ya hace write-through Supabase) |
| **Notificaciones** | `notifications` | TimelineViewModel | UserRepository (notifications, follows) | Sí: mismo VM que Timeline; datos ya se precargan si se entró antes a Timeline. Si se entra directo: LaunchedEffect(Unit) loadInitialIfNeeded() | Tras marcar leído/eliminar → userRepository.triggerRefresh() si aplica |
| **Buscador de usuarios** | `searchUsers` | FollowViewModel | UserRepository (getAllUsersFlow, followingMap) | Sí: LaunchedEffect(Unit) → userRepository.triggerRefresh() o loadInitial en FollowViewModel | Tras seguir/dejar de seguir → userRepository.toggleFollow ya actualiza; triggerRefresh() para refrescar lista |
| **Buscador (cafés)** | `search` | SearchViewModel | CoffeeRepository | Sí: loadInitialIfNeeded() en LaunchedEffect(Unit) (quitar refreshData() del init) | Tras favorito/filtrar → coffeeRepository.triggerRefresh() si aplica |
| **BrewLab** | `brewlab` | BrewLabViewModel | DiaryRepository (pantry, entries) | Sí: LaunchedEffect(Unit) → diaryRepository.triggerRefresh() (o loadInitial en BrewLabViewModel) | Tras añadir café/elaboración → diaryRepository.triggerRefresh() |
| **Diario** | `diary` | DiaryViewModel | DiaryRepository, CoffeeRepository | Sí: LaunchedEffect(Unit) → diaryRepository.triggerRefresh(), coffeeRepository.triggerRefresh() (quitar o mover refreshData() del DiaryScreen init) | Tras añadir entrada/stock/editar → diaryRepository.triggerRefresh() |
| **Añadir stock** | `addStock` | DiaryViewModel | DiaryRepository, CoffeeRepository | Los datos ya vienen del Diario; opcional loadInitial al entrar | Tras guardar → triggerRefresh y volver |
| **Editar café custom** | `editCustomCoffee/{id}` | DiaryViewModel | DiaryRepository | Precarga implícita por navegación desde Diario/Stock | Tras guardar → diaryRepository.triggerRefresh() |
| **Editar stock normal** | `editNormalStock/{id}` | DiaryViewModel | DiaryRepository | Idem | Tras guardar → diaryRepository.triggerRefresh() |
| **Añadir entrada diario** | `addDiaryEntry` | DiaryViewModel | DiaryRepository, CoffeeRepository | Idem Diario | Tras guardar → diaryRepository.triggerRefresh() |
| **Añadir ítem despensa** | `addPantryItem` | DiaryViewModel | DiaryRepository, CoffeeRepository | Idem | Tras guardar → diaryRepository.triggerRefresh() |
| **Perfil** | `profile/{userId}` | ProfileViewModel | UserRepository, SocialRepository, CoffeeRepository, DiaryRepository | Sí: LaunchedEffect(Unit) o LaunchedEffect(userId) → refreshData() / loadInitialIfNeeded() | Tras editar perfil/follow/like/comentar/post → userRepository/socialRepository.triggerRefresh() |
| **Seguidores** | `profile/{userId}/followers` | FollowViewModel | UserRepository | Sí: LaunchedEffect(Unit) → userRepository.triggerRefresh() | Tras seguir/dejar de seguir → userRepository.triggerRefresh() |
| **Siguiendo** | `profile/{userId}/following` | FollowViewModel | UserRepository | Idem Seguidores | Idem |
| **Detalle café** | `detail/{coffeeId}` | DetailViewModel | CoffeeRepository, SocialRepository, DiaryRepository | Sí: LaunchedEffect(coffeeId) → coffeeRepository.triggerRefresh() (o getCoffeeWithDetailsById ya hace networkBoundResource) | Tras favorito/reseña/stock → coffeeRepository.triggerRefresh(), socialRepository.triggerRefresh() |
| **Nueva publicación** | `addPost` | AddPostViewModel | SocialRepository, CoffeeRepository, UserRepository | No precarga de timeline; solo galería (permisos + loadGalleryImages) y lista de cafés si va a reseña | Tras publicar/reseña → socialRepository.triggerRefresh() (y createPost/insert ya hace write-through) |

---

## 3.5 Pull-to-refresh e infinite scroll

**Requisito**: (1) Si el usuario **tira desde arriba** (pull-to-refresh), la pantalla debe **actualizar** los datos (refresco desde red/caché). (2) Si el usuario **va bajando** en la lista, la pantalla debe **seguir actualizando o cargando más información** (infinite scroll donde aplique).

| Pantalla | Pull-to-refresh | Cargar más al bajar |
|----------|-----------------|---------------------|
| **Timeline** | Sí: `PullToRefreshBox` + `viewModel.refreshData()` | Lista completa desde Flow; solo pull-to-refresh. Si en el futuro Supabase devuelve paginado, añadir `onItemDisplayed` para cargar siguiente página. |
| **Notificaciones** | Sí: `PullToRefreshBox` + `viewModel.refreshData()` (TimelineViewModel) | Pull-to-refresh suficiente; opcional "cargar más" si el backend expone paginación. |
| **Buscador (cafés)** | Sí: `PullToRefreshBox` + `viewModel.refreshData()` | Sí: infinite scroll con `onItemDisplayed(index)` y `_displayLimit += PAGE_SIZE` (SearchViewModel). |
| **Buscador de usuarios** | Sí: `PullToRefreshBox` + `viewModel.loadInitialIfNeeded()` | Pull-to-refresh; opcional "cargar más" si hay paginación en API. |
| **Perfil** | Sí: `PullToRefreshBox` + `viewModel.refreshData()` | Pull-to-refresh; opcional "cargar más" para posts/reseñas si hay paginación. |
| **Seguidores / Siguiendo** | Sí: `PullToRefreshBox` + `viewModel.loadInitialIfNeeded()` | Pull-to-refresh; opcional "cargar más" si el backend lo soporta. |
| **Diario** | Sí: `PullToRefreshBox` + `viewModel.refreshData()` | Pull-to-refresh; listas normalmente acotadas. |
| **Detalle café** | Sí (opcional): `PullToRefreshBox` + `viewModel.loadInitialIfNeeded()` | Refresca reseñas/detalle al tirar. |

**Integración con Offline-First**: La precarga al entrar sigue siendo `loadInitialIfNeeded()` o `refreshData()` en `LaunchedEffect(Unit)`. El pull-to-refresh reutiliza el mismo refresh para actualizar datos y consolidar en Supabase cuando hay red. El infinite scroll (Buscador) se mantiene con `onItemDisplayed` y límite progresivo en UI.

**Archivos afectados (implementado)**: `TimelineScreen.kt` (ya tenía pull-to-refresh), `NotificationsScreen.kt`, `SearchScreen.kt`, `SearchUsersScreen.kt`, `ProfileScreen.kt`, `FollowersScreen.kt`, `FollowingScreen.kt`, `DiaryScreen.kt`, `DetailScreen.kt`; ViewModels: `FollowViewModel.loadInitialIfNeeded()`, `DetailViewModel.loadInitialIfNeeded()`.

---

## 4. Cambios propuestos por área

### 4.1 Permisos en el manifest

- **Archivo**: `app/src/main/AndroidManifest.xml`
- Añadir: `CAMERA`, `READ_MEDIA_IMAGES`, `READ_EXTERNAL_STORAGE` (con `maxSdkVersion="32"` si se desea para &lt; API 33).

### 4.2 MainActivity y sincronización global

- **MainActivity**: Quitar o reducir `syncManager.syncAll()` al autenticarse. La precarga pasará a ser por pantalla (cada pantalla dispara su precarga al entrar).

### 4.3 ViewModels: quitar init refresh, exponer loadInitialIfNeeded

- **TimelineViewModel**: Quitar `refreshData()` del `init`. Añadir `fun loadInitialIfNeeded()` que llame a `userRepository.syncUsers()` y `socialRepository.syncSocialData()` (o triggerRefresh de los recursos que alimentan el timeline).
- **SearchViewModel**: Quitar `refreshData()` del `init`. Añadir `fun loadInitialIfNeeded()` que llame a `repository.triggerRefresh()`.
- **ProfileViewModel**: Ya no hace refresh en init; añadir `fun loadInitialIfNeeded()` y llamarlo desde la pantalla al entrar.
- **DiaryViewModel**: Si hay refresh en init o en DiaryScreen al entrar, unificar en `loadInitialIfNeeded()` y llamarlo desde DiaryScreen en LaunchedEffect(Unit).
- **BrewLabViewModel**: Añadir `fun loadInitialIfNeeded()` que llame a `diaryRepository.triggerRefresh()` (para pantry/entries).
- **FollowViewModel**: No tiene init con refresh; añadir `fun loadInitialIfNeeded()` que llame a `userRepository.triggerRefresh()` para que SearchUsersScreen, FollowersScreen y FollowingScreen precarguen al entrar.
- **DetailViewModel**: El detalle se alimenta de Flows (getCoffeeWithDetailsById ya usa networkBoundResource). Opcional: en DetailScreen LaunchedEffect(coffeeId) llamar a `coffeeRepository.triggerRefresh()` o al recurso del detalle para forzar precarga al entrar.

### 4.4 Pantallas: LaunchedEffect(Unit) o clave estable para precarga

- **TimelineScreen**: `LaunchedEffect(Unit) { viewModel.loadInitialIfNeeded() }`.
- **SearchScreen**: `LaunchedEffect(Unit) { viewModel.loadInitialIfNeeded() }`.
- **ProfileScreen**: `LaunchedEffect(Unit) { viewModel.loadInitialIfNeeded() }` (o `LaunchedEffect(userId)` si el userId viene por argumentos).
- **DiaryScreen**: `LaunchedEffect(Unit) { viewModel.loadInitialIfNeeded() }`.
- **BrewLabScreen**: `LaunchedEffect(Unit) { viewModel.loadInitialIfNeeded() }`.
- **SearchUsersScreen**: `LaunchedEffect(Unit) { viewModel.loadInitialIfNeeded() }`.
- **FollowersScreen / FollowingScreen**: `LaunchedEffect(Unit) { viewModel.loadInitialIfNeeded() }`.
- **DetailScreen**: Opcional `LaunchedEffect(coffeeId) { viewModel.loadInitialIfNeeded() }` si se añade en DetailViewModel.
- **Notifications**: Usa TimelineViewModel; si el usuario entra primero a Notificaciones sin haber entrado a Timeline, conviene que NotificationsScreen también llame a `viewModel.loadInitialIfNeeded()` en LaunchedEffect(Unit) (o que el VM ya tenga los datos por compartir con Timeline).

Las pantallas de formulario (addStock, editCustomCoffee, editNormalStock, addDiaryEntry, addPantryItem, addPost, completeProfile, login, onboarding) no necesitan precarga de “lista” salvo addPost (galería + cafés para reseña), que ya se maneja con permisos y loadGalleryImages.

### 4.5 Refresh tras acciones del usuario

- Revisar que tras cada escritura (crear post, like, comentar, follow, editar perfil, añadir entrada, favorito, reseña, etc.) se llame al `triggerRefresh()` del repositorio afectado para que el componente se actualice y, con red, se consolide en Supabase. Muchos ya lo hacen (ej. socialRepository.createPost hace insert local + sync Supabase; toggleLike idem). Asegurar que no falte en ninguna pantalla del listado anterior.

### 4.6 Galería y cámara en AddPost

- Permisos en manifest (READ_MEDIA_IMAGES, READ_EXTERNAL_STORAGE, CAMERA).
- AddPostScreen: solicitar permiso CAMERA antes de abrir la cámara; usar `TakePicture(uri)` con URI temporal (FileProvider) en lugar de `TakePicturePreview()` para evitar cierre del flujo; callback con `setCapturedImage(uri)`.
- AddPostViewModel: `setCapturedImage(uri: Uri?)`; en createPost/submitReview subir imagen real con `socialRepository.uploadImage` y usar la URL devuelta.

### 4.7 Verificación final

- `./gradlew assembleDebug` y pruebas manuales por flujo (Timeline, Buscador, Perfil, Diario, BrewLab, Detalle, Notificaciones, Seguidores/Siguiendo, Nueva publicación con galería y cámara).

---

## 5. Archivos a tocar (resumen)

| Área | Archivos |
|------|----------|
| Permisos | `app/src/main/AndroidManifest.xml` |
| Sincronización global | `app/src/main/java/.../MainActivity.kt` |
| Timeline | `TimelineViewModel.kt`, `TimelineScreen.kt` |
| Buscador cafés | `SearchViewModel.kt`, `SearchScreen.kt` |
| Perfil | `ProfileViewModel.kt`, `ProfileScreen.kt` |
| Notificaciones | `MainActivity.kt` (composable notifications) y/o `TimelineViewModel.kt` |
| Buscador usuarios / Seguidores / Siguiendo | `FollowViewModel.kt`, `SearchUsersScreen.kt`, `FollowersScreen.kt`, `FollowingScreen.kt` |
| Diario y pantallas derivadas | `DiaryViewModel.kt`, `DiaryScreen.kt`, `AddStockScreen.kt`, `AddDiaryEntryScreen.kt`, `AddPantryItemScreen.kt`, `EditNormalStockScreen.kt` |
| BrewLab | `BrewLabViewModel.kt`, `BrewLabScreen.kt` |
| Detalle café | `DetailViewModel.kt`, `DetailScreen.kt` (opcional) |
| Nueva publicación (galería/cámara) | `AddPostScreen.kt`, `AddPostViewModel.kt` |
| Subida imagen | `AddPostViewModel.kt`, `SupabaseDataSource.kt` si falta path/bucket |

---

## 6. Orden sugerido de implementación

1. Añadir permisos en el manifest.
2. MainActivity: quitar o reducir syncAll al login.
3. ViewModels: quitar refresh del init donde aplique; añadir loadInitialIfNeeded() en Timeline, Search, Profile, Diary, BrewLab, Follow (y opcional Detail).
4. Pantallas: añadir LaunchedEffect(Unit) (o clave estable) que llame a loadInitialIfNeeded() en Timeline, Search, Profile, Diary, BrewLab, SearchUsers, Followers, Following, Notifications (y opcional Detail).
5. Revisar que tras cada acción de escritura se llame triggerRefresh() del recurso afectado.
6. AddPost: permiso CAMERA, TakePicture(uri), setCapturedImage(Uri), subir imagen real en createPost/submitReview.
7. Pruebas y compilación final.
