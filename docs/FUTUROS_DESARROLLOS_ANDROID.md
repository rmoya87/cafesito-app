# Futuros desarrollos — Integraciones Android (Cafesito)

**Estado:** vivo  
**Última actualización:** 2026-03-16  
**Ámbito:** Android (Kotlin, Jetpack Compose). Ideas y ejemplos para integrar la app con el ecosistema Android.

---

## 1. Objetivo

Este documento recoge integraciones Android (notificaciones con acciones, Quick Settings Tile, timer en primer plano, App Links, predictive actions, Direct Share, Bubbles, Health Connect, canales, edge-to-edge, predictive back) **con ejemplos reales aplicados a Cafesito**, y el **estado actual** de cada una en el proyecto.

---

## 2. Cómo aplicarlo en Cafesito (ejemplos reales)

**Leyenda:** ✅ Implementado | 🔶 Parcial / desactivado | ❌ No implementado

### 2.1 ✅ Notificaciones con acciones

**Qué hace:** Las notificaciones pueden incluir botones de acción (añadidos con `addAction()`). Al pulsar uno, el sistema envía un `PendingIntent` (por ejemplo a un `BroadcastReceiver`), de modo que el usuario puede actuar (aceptar, posponer, abrir una pantalla concreta) sin abrir la app a mano.

**Ejemplos vistos en el proyecto:** En `CafesitoFcmService` las notificaciones push llevan acciones según tipo: “Marcar leída” (todas); en FOLLOW, “Seguir”; en COMMENT/MENTION, “Ver” y “Guardar” (solo COMMENT); en LIST_INVITE, “Añadir” y “Rechazar”. `NotificationActionReceiver` recibe las pulsaciones (ACTION_MARK_READ, ACTION_FOLLOW_BACK, ACTION_SAVE_POST, ACTION_ACCEPT_LIST_INVITE, ACTION_DECLINE_LIST_INVITE). `TimelineNotificationSystem` construye notificaciones locales con acción “Marcar leída” y PendingIntent hacia el receiver.

**En Cafesito (propuesta):**
- **“¿Registrar elaboración?”:** al terminar un timer de elaboración, notificación con acciones “Sí, registrar” y “Más tarde” (abre Brew Lab o pospone).
- **Recordatorio agua:** notificación con “Ya bebí” (marca y cierra) y “Recordar en 15 min”.
- **Social:** ya existen acciones (marcar leída, seguir, guardar, añadir/rechazar); ampliar con “Responder” que abre el hilo o compose.

### 2.2 ✅ Quick Settings Tile

**Qué hace:** Una “tile” es un botón que aparece en el panel de ajustes rápidos (al bajar la barra de estado dos veces). La app implementa un `TileService` que define el icono, la etiqueta y la acción al pulsar (por ejemplo alternar estado o abrir una pantalla). Da acceso en un solo toque sin abrir la app.

**Ejemplos vistos en el proyecto:** Tile "Cafesito" implementada: `TileService` declarado en el manifest; si no hay elaboración en curso muestra etiqueta "Cafesito" y al pulsar abre Brew Lab; si hay elaboración en curso (FGS activo) muestra "Ver elaboración" y al pulsar abre Brew Lab con el timer sincronizado. El servicio lee SharedPreferences (flag de `BrewLabTimerService`) para el estado; el `PendingIntent` lleva extra para que la navegación abra `brewlab?openConsumo=false`.

**En Cafesito (implementado):**
- Tile "Cafesito" con icono de la app: si no hay elaboración en curso, etiqueta "Cafesito" y al pulsar se abre Brew Lab (elegir método); si hay elaboración en curso (FGS activo), etiqueta "Ver elaboración" y al pulsar se abre Brew Lab con el timer ya sincronizado. El `TileService` lee SharedPreferences de `BrewLabTimerService` (flag `KEY_RUNNING`) para elegir etiqueta y estado; el `PendingIntent` lleva un extra (p. ej. `open_brewlab=true`) para que `MainActivity`/`AppNavigation` navegue a `brewlab?openConsumo=false`.

**Ejemplos funcionales (flujos más complejos):**
- **Tile con estado dinámico:** El usuario añade la tile desde "Editar" en el panel de ajustes rápidos. La tile muestra el icono de Cafesito y la etiqueta "Cafesito". Al pulsar una vez: si no hay elaboración en curso, se abre la app en Brew Lab (pantalla de elegir método); si hay elaboración en curso (FGS activo), se abre la app en Brew Lab mostrando el timer y tiempo restante. Para saber si hay elaboración en curso, el `TileService` puede leer las mismas SharedPreferences que usa `BrewLabTimerService` (o comprobar si el servicio está en ejecución) y llamar a `tile.state = Tile.STATE_ACTIVE` y actualizar `tile.label` a "Ver elaboración" cuando corresponda; al pulsar, el `PendingIntent` lleva un extra (p. ej. `open_brewlab=true`) para que `MainActivity`/`AppNavigation` abra la ruta `brewlab?openConsumo=false` y `BrewLabScreen` restaure el estado desde prefs (igual que al reabrir la app).
- **Actualización de la tile (título/etiqueta):** Si se implementa estado activo durante el timer, la tile debería reflejar "Ver elaboración" y, opcionalmente, el tiempo restante en la subtítulo (si la API lo permite). Al terminar el timer, el servicio hace `stopSelf()` y ya no hay FGS; la próxima vez que el sistema llame a `onStartListening()` en el `TileService`, la tile puede volver a mostrar "Iniciar elaboración" o "Ver diario". Así el usuario ve en todo momento qué acción ejecutará el próximo tap.
- **Long-press y "Información":** En muchas ROMs, mantener pulsada la tile abre un menú (p. ej. "Quitar", "Información"). "Información" suele abrir la pantalla de ajustes de la app o de la tile. Se puede declarar `android:icon` y `android:label` en el `<service>` del manifest para que ese menú muestre un nombre y icono coherentes; no es necesario implementar lógica extra para el long-press, el sistema lo gestiona.
- **Varias tiles (avanzado):** Una variante más compleja sería exponer dos tiles: "Cafesito — Elaborar" (siempre abre Brew Lab) y "Cafesito — Diario" (abre Diario). Cada una sería un `TileService` distinto con su propio `<service>` en el manifest. Así el usuario elige qué atajo quiere en el panel sin depender del estado; la implementación es más simple (sin lectura de SharedPreferences en el TileService) pero ocupa dos huecos en el panel.
- **Tile + FGS:** Si hay timer en curso y el usuario pulsa la tile "Ver elaboración", la app se abre en primer plano en Brew Lab con el estado ya sincronizado desde SharedPreferences; no hace falta que el TileService se comunique con el FGS, basta con que la navegación abra `brewlab` y `BrewLabScreen` lea el estado como hace al reabrir la app. La tile solo necesita un indicador binario (¿está el FGS corriendo?) para mostrar "Ver elaboración" u otra etiqueta; puede obtenerse con `getRunningServices` (deprecado) o, de forma fiable, guardando un flag en SharedPreferences que `BrewLabTimerService` ponga a `true` al iniciar y a `false` al parar.

### 2.3 ✅ Timer de elaboración en primer plano

**Qué hace:** Un **Foreground Service** es un servicio que se considera “en primer plano”: el sistema no lo mata por ahorro de batería y debe mostrar una notificación ongoing (no descartable por el usuario). Se usa para tareas que el usuario percibe como activas (reproducción de audio, descarga importante, **timer**). La notificación suele mostrar estado (por ejemplo tiempo restante) y acciones (pausar, cancelar).

**Ejemplos vistos en el proyecto:** `BrewLabTimerService` (Foreground Service con `foregroundServiceType="specialUse"`). Al iniciar el timer en Brew Lab se arranca el servicio; notificación ongoing con título "Elaborando: [método]", tiempo restante, acciones Pausar/Reanudar y Cancelar. Al terminar se muestra "¿Registrar elaboración?" con enlace a la app. `BrewLabViewModel` inicia/controla el servicio; `BrewLabScreen` sincroniza estado desde SharedPreferences cada segundo.

**En Cafesito (propuesta):**
- Durante el timer de Brew Lab: **Foreground Service** con notificación ongoing (no descartable), título “Elaborando: [nombre receta]”, tiempo restante, y acciones “Pausar” / “Cancelar”. Al terminar, reemplazar por la notificación “¿Registrar elaboración?” con acciones.

**Ejemplos funcionales (flujos más complejos):**
- **Elaboración en segundo plano:** El usuario elige método V60, configura café y agua, pulsa Iniciar. El timer arranca y la notificación muestra “Estás elaborando: V60” y “04:00 restantes”. Minimiza la app (o cambia a otra); el servicio sigue en primer plano y la notificación se actualiza cada segundo. Al tocar **Pausar** en la notificación, el timer se pausa (texto “Pausado — XX:XX restantes”); al tocar **Reanudar** continúa. Al tocar **Cancelar** el servicio se detiene y se elimina la notificación.
- **Varias fases (p. ej. Bloom → Vertido):** La receta tiene fases con duración. En pantalla se muestra “El paso: Bloom” y el tiempo grande (restante del paso); la notificación muestra el tiempo total restante de la elaboración. Al cambiar de fase, la notificación sigue mostrando el total; el usuario puede abrir la app y ver el paso actual y el siguiente.
- **Timer termina con la app cerrada o en segundo plano:** El servicio detecta que el tiempo llegó a 00:00, hace `stopForeground`, muestra la notificación “¿Registrar elaboración?” con texto “Abre Cafesito y guarda en tu diario. Continuar.” y se detiene. El usuario toca la notificación → la app se abre **directamente en la pantalla Consumo** (tipo/tamaño/sabor, Guardar), no en la pantalla de elegir método, para que pueda registrar el resultado sin pasos extra.
- **App cerrada por el usuario con timer activo:** Si el usuario fuerza el cierre desde recientes, el proceso puede morir pero el **servicio en primer plano sigue vivo** en un proceso separado; la notificación sigue visible y el timer sigue contando (estado en SharedPreferences). Si el usuario reabre la app, `BrewLabScreen` lee SharedPreferences y muestra de nuevo la pantalla de elaboración con el tiempo restante y el paso actual sincronizados.
- **Reinicio del dispositivo (no cubierto):** Si el dispositivo se reinicia o el proceso del servicio se mata por condiciones extremas, el timer no se reanuda automáticamente; el usuario tendría que volver a Brew Lab y reiniciar. Un posible desarrollo futuro sería usar `WorkManager` o alarma para reanudar/recordar tras reinicio (con limitaciones de exactitud).

### 2.4 ✅ App Links / Deep links

**Qué hace:** Los **App Links** (y en general los deep links) hacen que una URL abra directamente la app en lugar del navegador. Con `android:autoVerify="true"` y un `assetlinks.json` correcto en el dominio, el sistema verifica que la app es propietaria de ese dominio y asocia los enlaces a la app sin preguntar al usuario. Así, un enlace a una lista compartida abre la app en esa pantalla.

**Ejemplos vistos en el proyecto:** En `AndroidManifest.xml`, `MainActivity` tiene un `intent-filter` con `android:autoVerify="true"` para `https://cafesitoapp.com` (pathPrefix `/`). `MainActivity.parseListIdFromIntent()` extrae el `listId` de la URI (p. ej. `profile/list/{listId}`). `DeepLinkViewModel.getListOwnerId(listId)` obtiene el `userId` del dueño de la lista vía `SupabaseDataSource.getUserListById(listId)`. En `AppNavigation`, un `LaunchedEffect(deepLinkListId)` navega a `profile/{ownerId}/list/{listId}` cuando hay sesión.

**En Cafesito (propuesta):**
- Enlaces web que abren la app (p. ej. `https://cafesitoapp.com/profile/list/{listId}`). Requiere `assetlinks.json` en el servidor y `intent-filter` con `android:autoVerify="true"`.

### 2.5 ✅ Predictive app actions

**Qué hace:** En la pantalla de aplicaciones recientes (y a veces en el launcher), Android puede mostrar **acciones sugeridas** por app (por ejemplo “Nueva nota”, “Enviar mensaje”). La app publica estas acciones mediante la API de ShortcutManager / App Actions; el sistema las muestra según uso reciente o contexto para que el usuario entre directamente en una tarea concreta.

**Ejemplos vistos en el proyecto:** `PredictiveShortcutsHelper` publica atajos dinámicos (Elaboración, Mi diario, Busca café) vía `ShortcutManager.setDynamicShortcuts()` al autenticarse el usuario. En `AppNavigation` se llama `reportShortcutUsed(shortcutId)` al entrar en brewlab, diary o search para que el sistema priorice esas acciones en la tarjeta de recientes.

**En Cafesito (propuesta):**
- En el selector de apps recientes: sugerir “Elaborar café” o “Añadir a diario” según contexto (por ejemplo última pantalla visitada o hora).

### 2.6 ❌ Direct Share

**Qué hace:** En el mismo diálogo de “Compartir”, además de elegir una app (Share target), el usuario puede ver **destinos directos**: contactos, chats o entidades concretas (por ejemplo “Compartir en lista X de Cafesito”). La app implementa un `ChooserTargetService` que devuelve esos destinos; así se evita abrir la app y elegir después dónde guardar.

**Ejemplos vistos en el proyecto:** Ninguno. No hay `ChooserTargetService` ni Direct Share.

**En Cafesito (propuesta):**
- Al compartir desde otra app, mostrar contactos o listas de Cafesito como objetivos directos (por ejemplo “Compartir en lista X”).

### 2.7 ❌ Bubbles

**Qué hace:** Las **burbujas** (Bubbles) son una vista flotante sobre otras apps: la notificación puede expandirse en una pequeña burbuja que el usuario puede mover. Al tocarla se abre un “bubble content” (por ejemplo la pantalla del timer). Sirve para mantener una tarea en curso visible (chat, llamada, **timer**) sin ocupar pantalla completa. Requiere que la notificación permita burbuja y que la app proporcione el contenido.

**Ejemplos vistos en el proyecto:** Ninguno. No hay notificaciones con burbuja ni `BubbleMetadata`.

**En Cafesito (propuesta):**
- Modo burbuja para el timer de elaboración: minimizar a burbuja flotante y seguir viendo tiempo restante; tap abre de nuevo la pantalla de Brew Lab.

### 2.8 ❌ Health Connect

**Qué hace:** **Health Connect** es la API de Android para leer y escribir datos de salud y bienestar en un almacén central (peso, actividad, sueño, nutrición, etc.). Las apps piden permisos por tipo de dato; el usuario da consentimiento. Permite unificar datos de varias apps y que Cafesito, si el usuario lo desea, registre consumo de café/cafeína para un perfil de salud más completo.

**Ejemplos vistos en el proyecto:** Ninguno. No hay integración con Health Connect.

**En Cafesito (propuesta):**
- Registrar “cafeína” o “café” como evento o dato si el usuario conecta su cuenta; opcional y con consentimiento explícito.

**Ejemplos funcionales (flujos más complejos):**
- **Activación y permisos:** En Ajustes de Cafesito (o en la pantalla de perfil), una opción "Conectar con Health Connect" abre la pantalla de permisos de Health Connect (`HealthConnectClient.createPermissionControllerContract()` o `PermissionController.createRequestPermissionResultContract()`). El usuario ve qué datos quiere leer/escribir Cafesito (p. ej. "Nutrición: cafeína" para escribir, "Actividad: sueño" para leer si se quisiera contextualizar). Al conceder, la app guarda en preferencias que Health Connect está vinculado y habilita la escritura automática al registrar elaboraciones.
- **Escribir cafeína al guardar en el diario:** Cuando el usuario guarda una entrada de café en el diario (pantalla Consumo, "Guardar"), además de persistir en la base local y/o backend, si Health Connect está conectado se llama a `HealthConnectClient.insertRecords()` con un registro de tipo adecuado (p. ej. `NutritionRecord` con `NutritionRecord.NUTRIENTS_CAFFEINE` en miligramos, o el tipo que exponga la API para "cafeína"). Se usa la cantidad de cafeína estimada que ya calcula Cafesito (p. ej. `BrewEngine` o lógica de elaboración) y la fecha/hora del consumo. Todo en segundo plano (coroutine o WorkManager) para no bloquear la UI.
- **Datos que se escriben:** Por cada entrada guardada: `timestamp` (cuando se consumió), `caffeine_mg` (estimado según método, gramos, tamaño de taza), opcionalmente `source` como "Cafesito". Health Connect permite registrar dosis de cafeína; si la API expone un tipo "Caffeine" o se usa nutrición, se inserta un registro por elaboración guardada. Si el usuario registra varias tazas en un mismo día, se escriben varios registros (uno por entrada), de modo que en Health Connect o en otras apps (p. ej. sueño) se pueda ver la evolución diaria.
- **Lectura (opcional) y coherencia:** Cafesito podría leer datos de cafeína desde Health Connect para mostrar un resumen "hoy has tomado X mg según Health Connect" y evitar duplicados si el usuario también registra en otra app. Implica permiso de lectura; el flujo sería: en pantalla de estadísticas o diario, si hay conexión, `HealthConnectClient.readRecords()` filtrando por fecha y tipo, y mostrar total o gráfica. Si no se implementa lectura, Cafesito solo escribe; el usuario ve los datos en la app de Salud/Health Connect.
- **Desconectar y privacidad:** En "Conectar con Health Connect" (o Ajustes > Salud), opción "Desvincular". No hace falta revocar permisos desde la app (el usuario puede revocarlos en Ajustes de Android); Cafesito deja de escribir y borra el flag de "vinculado". A partir de ahí no se insertan más registros; los ya escritos permanecen en Health Connect hasta que el usuario los borre desde la app de Salud.
- **Requisitos técnicos:** Health Connect está disponible desde Android 14 (API 34) como parte del sistema; en versiones anteriores puede estar como app instalable. Comprobar `HealthConnectClient.getSdkStatus()` antes de mostrar la opción; si no está disponible, ocultar "Conectar con Health Connect" o mostrar un mensaje. Dependencia `androidx.health.connect:connect-client`; declarar en el manifest los permisos de salud que se usen (p. ej. `android.permission.health.WRITE_NUTRITION`, `READ_NUTRITION` si se lee). Documentar en la política de privacidad que los datos de cafeína se comparten con Health Connect solo si el usuario lo activa.

### 2.9 ✅ Canales de notificaciones

**Qué hace:** Desde Android 8, cada notificación pertenece a un **canal**. El usuario puede silenciar, cambiar importancia o desactivar un canal completo desde ajustes (por ejemplo “Actividad social” en alto, “Marketing” en silencio). La app crea los canales al inicio y asigna cada notificación a uno; así se evita que todo sea “todo o nada” y se mejora el control del usuario.

**Ejemplos vistos en el proyecto:** `NotificationChannels` define tres canales: `CHANNEL_GENERAL` (“Cafesito general”, IMPORTANCE_DEFAULT), `CHANNEL_SOCIAL` (“Actividad social”, IMPORTANCE_HIGH), `CHANNEL_MENTIONS` (“Menciones y comentarios”, IMPORTANCE_HIGH). `resolveChannel(type)` asigna FOLLOW → social, MENTION/COMMENT → mentions, resto → general. Se llama `ensureCreated(context)` en `CafesitoFcmService` y en `TimelineNotificationSystem.ensureChannel()`.

**En Cafesito (propuesta):**
- Separar por tipo: general, social, recordatorios (agua, elaboración), marketing (si aplica). El usuario puede silenciar por canal.

### 2.10 ✅ Edge-to-edge

**Qué hace:** **Edge-to-edge** significa que la ventana de la app se dibuja detrás de la barra de estado y de la barra de navegación del sistema. La app aplica **insets** (padding o margin) para que el contenido no quede oculto bajo esas barras. El resultado es una interfaz más inmersiva y coherente con las guías de Material y Android.

**Ejemplos vistos en el proyecto:** En `AppUiInitializer.setupEdgeToEdge(activity)` se llama `activity.enableEdgeToEdge()` con `SystemBarStyle.auto(TRANSPARENT, TRANSPARENT)` para barra de estado y barra de navegación. `configure(activity)` se invoca desde `MainActivity` y aplica edge-to-edge además de la configuración de Coil.

**En Cafesito (propuesta):**
- Dibujar contenido hasta los bordes (status bar, navigation bar) con insets correctos para mejorar inmersión.

### 2.11 ✅ Predictive back

**Qué hace:** Con el **gesto de vuelta atrás**, Android puede mostrar una **vista previa** de la pantalla anterior mientras el usuario arrastra (predictive back). La app puede personalizar la animación y el contenido mostrado en esa transición. Mejora la sensación de control y la coherencia al navegar hacia atrás en elaboración, diario o listas.

**Ejemplos vistos en el proyecto:** `android:enableOnBackInvokedCallback="true"` en `AndroidManifest.xml`. En `AppNavigation.kt`, el `NavHost` define `popEnterTransition` y `popExitTransition` (scale + fade) para que al hacer el gesto atrás se vea la vista previa de la pantalla anterior (elaboración, diario, listas, etc.).

**En Cafesito (propuesta):**
- Transiciones de vuelta atrás con preview (gesto de vuelta) en pantallas de elaboración, diario y listas.

---

## 3. Orden sugerido de implementación

1. ✅ **Timer en primer plano** (Foreground Service + notificación ongoing) — impacto directo en Brew Lab.
2. ✅ **Notificaciones con acciones** para elaboración (“¿Registrar?”) y agua — ya hay base social.
3. ✅ **App Links** — ya declarados; verificar `assetlinks.json` y rutas.
4. ✅ **Quick Settings Tile** — un solo tap desde la barra de estado (implementado).
5. ✅ **Predictive app actions** — sugerencias en recientes (ya implementado).
6. ❌ **Burbuja** (opcional) — para timer en segundo plano.
7. ❌ **Health Connect** — según prioridad de producto.
8. ✅ **Edge-to-edge y predictive back** — pulido de UX (ya implementados).

---

## 4. Estado actual en Android

Revisión sobre el código y la configuración del proyecto Android (manifest, `app/`, `res/`).  
**Leyenda:** ✅ Implementado | 🔶 Parcial / desactivado | ❌ No implementado

| Integración | Estado | Notas |
|-------------|--------|--------|
| **Notificaciones con acciones** | ✅ | FCM + `CafesitoFcmService`: acciones “Marcar leída”, “Seguir”, “Guardar”, “Añadir”/“Rechazar”. `NotificationActionReceiver` para tramitar acciones. `TimelineNotificationSystem` con acciones locales. |
| **Quick Settings Tile** | ✅ | Tile "Cafesito": `TileService` en manifest; etiqueta "Cafesito" o "Ver elaboración" según estado (SharedPreferences/`BrewLabTimerService`); al pulsar abre Brew Lab (`brewlab?openConsumo=false`). Ver §2.2 ejemplos funcionales. |
| **Timer en primer plano** | ✅ | `BrewLabTimerService`: notificación ongoing “Estás elaborando: [método]”, tiempo restante, Pausar/Reanudar/Cancelar; al terminar “¿Registrar elaboración?” → abre pantalla Consumo. Persistencia en SharedPreferences; flujos: app en segundo plano, varias fases, tocar notificación a Consumo, reabrir app con timer activo (ver §2.3 ejemplos funcionales). |
| **App Links / Deep links** | ✅ | `intent-filter` con `android:autoVerify="true"` para `https://cafesitoapp.com`. `DeepLinkViewModel` para listas compartidas (listId → ownerId). `parseListIdFromIntent` en MainActivity. |
| **Predictive app actions** | ✅ | `PredictiveShortcutsHelper`: atajos dinámicos (brewlab, diary, search) y `reportShortcutUsed` al navegar; sugerencias en recientes. |
| **Direct Share** | ❌ | No implementado. |
| **Bubbles** | ❌ | No implementado. |
| **Health Connect** | ❌ | No implementado. |
| **Canales de notificaciones** | ✅ | `NotificationChannels`: general, social, menciones. `ensureCreated()` en FCM y TimelineNotificationSystem. |
| **Edge-to-edge** | ✅ | `AppUiInitializer.setupEdgeToEdge()` con `enableEdgeToEdge()`. |
| **Predictive back** | ✅ | `android:enableOnBackInvokedCallback="true"` en manifest; NavHost con `popEnterTransition`/`popExitTransition` (scale + fade) para vista previa al gesto atrás. |

---

## 5. Referencias

- Documento Maestro: `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`
- Registro de desarrollo: `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md`
- Accesibilidad: `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`
- Brew Lab y UI: `docs/commit-notes/commit-20260304-05-elaboracion-brew-ui-colores-italiana.md`
