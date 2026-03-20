# Integraciones Android implementadas (Cafesito)

**Estado:** vivo  
**Última actualización:** 2026-03-19  
**Ámbito:** Android (Kotlin, Jetpack Compose). Registro de integraciones nativas ya implementadas en la app.

---

## 1. Objetivo

Este documento es la **fuente de verdad** de las integraciones Android que ya están en el proyecto. Para ideas o propuestas de **futuros** desarrollos (Direct Share, Bubbles, Health Connect, etc.) ver `FUTUROS_DESARROLLOS_ANDROID.md`.

---

## 2. Timer de elaboración en primer plano (Foreground Service)

**Estado:** ✅ Implementado.

- **Servicio:** `app/.../brewlab/BrewLabTimerService.kt`
  - Foreground Service con `foregroundServiceType="specialUse"`; notificación ongoing no descartable.
  - Título: "Estás elaborando: [método]"; contenido: tiempo **total** restante (suma de todas las fases); acciones Pausar / Reanudar / Cancelar.
  - Al llegar a 00:00: `stopForeground(STOP_FOREGROUND_REMOVE)`, muestra notificación "¿Registrar elaboración?" con texto "Abre Cafesito y guarda en tu diario. Continuar." y `PendingIntent` con `EXTRA_OPEN_BREWLAB_CONSUMO = true` para abrir la app directamente en la pantalla **Consumo** (tipo/tamaño/sabor, Guardar).
- **Persistencia:** SharedPreferences (`brew_timer_service`): `KEY_ELAPSED`, `KEY_TOTAL`, `KEY_METHOD`, `KEY_PAUSED`, `KEY_RUNNING`, `KEY_JUST_ENDED`.
- **ViewModel / UI:** `BrewLabViewModel` inicia/controla el servicio; `BrewLabScreen` sincroniza estado desde SharedPreferences cada segundo cuando el paso es BREWING. En pantalla: "El paso: [nombre del paso]" y tiempo grande (restante del paso); la notificación muestra tiempo total restante.
- **Restauración al reabrir app:** Si el usuario fuerza el cierre con el timer activo, el servicio sigue en primer plano. Al reabrir la app y entrar en Brew Lab, `BrewLabViewModel.restoreBrewingFromServiceIfNeeded()` restaura paso BREWING, método seleccionado y estado del timer desde SharedPreferences; `BrewLabScreen` muestra de nuevo la elaboración con tiempo y paso sincronizados.
- **Manifest:** `<service>` con `foregroundServiceType="specialUse"` y `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`; permiso `FOREGROUND_SERVICE_SPECIAL_USE`.

**Referencias:** `REGISTRO_DESARROLLO_E_INCIDENCIAS.md` §18; `FUTUROS_DESARROLLOS_ANDROID.md` (antes §2.3, ahora solo referencias).

---

## 3. Notificación «¿Registrar elaboración?» y navegación a Consumo

**Estado:** ✅ Implementado.

- **Texto:** `res/values/strings.xml`: `brew_timer_register_title` = "¿Registrar elaboración?", `brew_timer_register_text` = "Abre Cafesito y guarda en tu diario. Continuar."
- **Intent:** Al tocar la notificación se lanza `MainActivity` con `EXTRA_OPEN_BREWLAB_CONSUMO = true`.
- **Navegación:** `NotificationNavigation.fromIntent()` detecta el extra y devuelve tipo `OPEN_BREWLAB_CONSUMO`; `AppNavigation` navega a `brewlab?openConsumo=true`; `BrewLabScreen(openConsumoFromNotification = true)` llama a `viewModel.openConsumoFromNotification()` que pone `_currentStep = BrewStep.RESULT` (pantalla Consumo).

---

## 4. Quick Settings Tile «Cafesito»

**Estado:** ✅ Implementado.

- **Servicio:** `app/.../quicksettings/CafesitoTileService.kt`
  - En `onStartListening()`: lee `BrewLabTimerService.isRunning(this)` (SharedPreferences `KEY_RUNNING`). Si hay elaboración en curso: etiqueta "Ver elaboración", estado `Tile.STATE_ACTIVE`; si no: etiqueta "Cafesito", estado `Tile.STATE_INACTIVE`. Icono: `R.mipmap.ic_launcher`.
  - En `onClick()`: `startActivityAndCollapse(Intent(MainActivity).putExtra(EXTRA_OPEN_BREWLAB, true))` para abrir la app en Brew Lab (`brewlab?openConsumo=false`). Si hay timer en curso, `BrewLabScreen` restaura el estado desde prefs.
- **Manifest:** `<service android:name="...CafesitoTileService" android:exported="true"` con `intent-filter` `android.service.quicksettings.action.QS_TILE`; `android:icon` y `android:label` para el listado de tiles.
- **Strings:** `tile_label_brewing` = "Ver elaboración", `tile_content_description_default` / `tile_content_description_brewing`.

**Nota:** En algunos fabricantes (p. ej. Xiaomi/HyperOS) la lista de tiles puede no mostrar tiles de terceros; es limitación del OEM, no de la app.

---

## 5. App Links / Deep links (listas y unirse por enlace)

**Estado:** ✅ Implementado.

- **Manifest:** `MainActivity` con `intent-filter` `android:autoVerify="true"` para `https://cafesitoapp.com` (pathPrefix `/`).
- **Rutas URL:** `https://cafesitoapp.com/profile/list/{listId}` y `https://cafesitoapp.com/lists/join/{listId}` (`MainActivity.parseListIdFromIntent()`).
- **Usuario ya miembro o dueño:** `DeepLinkViewModel.getListOwnerId(listId)` vía `getUserListById` → `AppNavigation` navega a `profile/{ownerId}/list/{listId}`.
- **Usuario sin acceso (lista pública o por invitación):** `getListInfoForJoin(listId)` (RPC) → si hay datos, navegación a **`listJoin/{listId}`** (`ListJoinScreen`, `ListJoinViewModel`): pantalla "Unirse a la lista"; al confirmar, RPC `join_list_by_link` y navegación a `profile/{ownerId}/list/{listId}`.
- **Supabase:** `docs/supabase/user_lists_join_by_link.sql` (aplicar en el proyecto si no está desplegado).

**Paridad WebApp:** misma URL `/profile/list/{listId}`; si no es miembro, `JoinListView` en `AppContainer` y `joinListByLink`. Ver `OPCIONES_DE_LISTA_WEB_Y_ANDROID.md`.

---

## 6. Predictive app actions (atajos en recientes)

**Estado:** ✅ Implementado.

- **Código:** `PredictiveShortcutsHelper` con `ShortcutManager.setDynamicShortcuts()` (Elaboración, Mi diario, Busca café); `reportShortcutUsed(shortcutId)` en `AppNavigation` al entrar en brewlab, diary, search.
- **Manifest:** `meta-data` `android.app.shortcuts` con `@xml/shortcuts`.

---

## 7. Canales de notificaciones

**Estado:** ✅ Implementado.

- **Código:** `NotificationChannels` (general, social, menciones); `ensureCreated()` en FCM y TimelineNotificationSystem; canal específico para timer de elaboración (`CHANNEL_BREW_TIMER`).

---

## 8. Edge-to-edge y Predictive back

**Estado:** ✅ Implementado.

- **Edge-to-edge:** `AppUiInitializer.setupEdgeToEdge()` con `enableEdgeToEdge()`; invocado desde `MainActivity`.
- **Predictive back:** `android:enableOnBackInvokedCallback="true"` en manifest; `NavHost` con `popEnterTransition`/`popExitTransition` (scale + fade) para vista previa al gesto atrás.

---

## 9. Tabla resumen

| Integración              | Archivos principales                                                                 | Notas |
|--------------------------|---------------------------------------------------------------------------------------|-------|
| Timer en primer plano    | `BrewLabTimerService.kt`, `BrewLabViewModel.kt`, `BrewLabScreen.kt`, `BrewLabComponents.kt` | FGS, notificación ongoing, abrir Consumo al terminar, restauración al reabrir |
| Notificación Registrar  | `BrewLabTimerService.kt`, `AppNavigation.kt`, `strings.xml`                           | Texto y deep link a Consumo |
| Quick Settings Tile     | `CafesitoTileService.kt`, `AndroidManifest.xml`, `strings.xml`                         | Etiqueta dinámica según FGS |
| App Links                | `AndroidManifest.xml`, `MainActivity.kt`, `DeepLinkViewModel`, `AppNavigation.kt`     | Listas compartidas |
| Predictive shortcuts     | `PredictiveShortcutsHelper.kt`, `AppNavigation.kt`, `shortcuts.xml`                  | Recientes |
| Canales notificaciones   | `NotificationChannels.kt`, FCM, TimelineNotificationSystem                            | General, social, menciones, brew timer |
| Edge-to-edge / Predictive back | `AppUiInitializer.kt`, `MainActivity.kt`, `AppNavigation.kt`, manifest               | Insets y transiciones |

---

## 10. Referencias

- **Registro de desarrollo:** `REGISTRO_DESARROLLO_E_INCIDENCIAS.md` (§18, §19, §20)
- **Futuros desarrollos Android:** `FUTUROS_DESARROLLOS_ANDROID.md`
- **Documento Maestro:** `MASTER_ARCHITECTURE_GOVERNANCE.md`
