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

### 2.2 ❌ Quick Settings Tile

**Qué hace:** Una “tile” es un botón que aparece en el panel de ajustes rápidos (al bajar la barra de estado dos veces). La app implementa un `TileService` que define el icono, la etiqueta y la acción al pulsar (por ejemplo alternar estado o abrir una pantalla). Da acceso en un solo toque sin abrir la app.

**Ejemplos vistos en el proyecto:** Ninguno. No hay `TileService` ni declaración en el manifest.

**En Cafesito (propuesta):**
- Una tile “Cafesito” que alterna entre “Iniciar elaboración” y “Ver diario” según estado (por ejemplo si hay elaboración en curso → “Ver elaboración”), con un tap.

### 2.3 ✅ Timer de elaboración en primer plano

**Qué hace:** Un **Foreground Service** es un servicio que se considera “en primer plano”: el sistema no lo mata por ahorro de batería y debe mostrar una notificación ongoing (no descartable por el usuario). Se usa para tareas que el usuario percibe como activas (reproducción de audio, descarga importante, **timer**). La notificación suele mostrar estado (por ejemplo tiempo restante) y acciones (pausar, cancelar).

**Ejemplos vistos en el proyecto:** `BrewLabTimerService` (Foreground Service con `foregroundServiceType="specialUse"`). Al iniciar el timer en Brew Lab se arranca el servicio; notificación ongoing con título "Elaborando: [método]", tiempo restante, acciones Pausar/Reanudar y Cancelar. Al terminar se muestra "¿Registrar elaboración?" con enlace a la app. `BrewLabViewModel` inicia/controla el servicio; `BrewLabScreen` sincroniza estado desde SharedPreferences cada segundo.

**En Cafesito (propuesta):**
- Durante el timer de Brew Lab: **Foreground Service** con notificación ongoing (no descartable), título “Elaborando: [nombre receta]”, tiempo restante, y acciones “Pausar” / “Cancelar”. Al terminar, reemplazar por la notificación “¿Registrar elaboración?” con acciones.

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

1. ❌ **Timer en primer plano** (Foreground Service + notificación ongoing) — impacto directo en Brew Lab.
2. 🔶 **Notificaciones con acciones** para elaboración (“¿Registrar?”) y agua — ya hay base social.
3. ✅ **App Links** — ya declarados; verificar `assetlinks.json` y rutas.
4. ❌ **Quick Settings Tile** — un solo tap desde la barra de estado.
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
| **Quick Settings Tile** | ❌ | No hay `TileService` ni declaración en manifest. |
| **Timer en primer plano** | ✅ | `BrewLabTimerService`: notificación ongoing con tiempo restante, Pausar/Reanudar/Cancelar; al terminar "¿Registrar elaboración?". |
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
