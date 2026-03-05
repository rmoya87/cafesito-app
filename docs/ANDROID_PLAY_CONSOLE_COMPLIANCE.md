# Cumplimiento recomendaciones Google Play Console (Android 15/16)

**Fecha:** 2026  
**Contexto:** Recomendaciones mostradas en Play Console sobre restricciones de orientación, APIs obsoletas de edge-to-edge y vista de extremo a extremo.

---

## 1. Restricciones de redimensionamiento y orientación (Android 16)

**Recomendación:** Quitar `screenOrientation` para compatibilidad con pantallas grandes (tablets, plegables).

### Cambios realizados

| Origen | Actividad | Acción |
|--------|-----------|--------|
| App | `MainActivity` | Eliminado `android:screenOrientation="portrait"` del `AndroidManifest.xml`. |
| App | `NativeBarcodeScannerActivity` | Eliminado `android:screenOrientation="portrait"`. |
| Dependencia ML Kit | `GmsBarcodeScanningDelegateActivity` | Anulación en nuestro manifest con `tools:node="merge"` y `android:screenOrientation="fullSensor"` para permitir rotación en tablets/plegables. |

**Archivos modificados:** `app/src/main/AndroidManifest.xml`.

A partir de Android 16 el sistema puede ignorar estas restricciones en pantallas grandes; al quitarlas o suavizarlas evitamos problemas de diseño y cumplimos con la recomendación.

---

## 2. APIs obsoletas para vista de extremo a extremo (Android 15)

**Recomendación de Play:** Dejar de usar `Window.setStatusBarColor`, `Window.setNavigationBarColor` y `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`.

### Estado en nuestra app

- **Código propio:** No usamos esas APIs. Usamos únicamente `enableEdgeToEdge()` con `SystemBarStyle.auto(TRANSPARENT, TRANSPARENT)` (API recomendada por Google).
- **Origen de las advertencias (según Play Console):** Las APIs obsoletas las usan **dependencias de terceros**, no nuestro código:
  - `com.google.android.material.datepicker.o.D` → Material Components (DatePicker u otros)
  - `d.r.b`, `d.t.b`, `d.w.b` → Código ofuscado de librerías
  - `androidx.credentials.playservices.controllers.CreatePublicKeyCredential.e.s` → Credentials / Passkey (Google)

**Acción:** No podemos modificar el código de esas librerías. Las advertencias se reducirán cuando actualicemos a versiones que migren a las nuevas APIs (p. ej. Material 1.14+ cuando esté estable). En nuestro código no usamos ni usaremos `setStatusBarColor`, `setNavigationBarColor` ni `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`.

---

## 3. Vista de extremo a extremo (edge-to-edge) en Android 15+

**Recomendación de Play:** Con target SDK 35 la app es edge-to-edge por defecto; hay que gestionar bajorrelieves (insets) y se puede llamar a `enableEdgeToEdge()` para retrocompatibilidad.

### Estado en nuestra app (cumplimiento)

- **Llamada explícita a `enableEdgeToEdge()`:** En `AppUiInitializer.setupEdgeToEdge()` se llama a `activity.enableEdgeToEdge()` con barras de sistema transparentes (`SystemBarStyle.auto(TRANSPARENT, TRANSPARENT)`). Se invoca desde `MainActivity.onCreate()` **antes** de `setContent`, tal como recomienda Google para retrocompatibilidad.
- **Gestión de insets (bajorrelieves):** En Compose se usan `Modifier.statusBarsPadding()`, `Modifier.navigationBarsPadding()`, `WindowInsets.statusBars` y `contentWindowInsets` en las pantallas que lo requieren (LoginScreen, SearchScreen, AppNavigation, DetailScreen, TimelineComponents, etc.), de modo que el contenido no quede bajo la barra de estado ni la de navegación.
- **Otras actividades:** `NativeBarcodeScannerActivity` también llama a `enableEdgeToEdge()` en su `onCreate` para consistencia.

**Archivos:** `AppUiInitializer.kt`, `MainActivity.kt`, `NativeBarcodeScannerActivity.kt`, y las pantallas que aplican padding de insets.

La app cumple con la recomendación de edge-to-edge; conviene seguir probando en Android 15+ con muescas y gestos de navegación.

---

## Resumen de cambios realizados

1. **AndroidManifest.xml**
   - Eliminado `android:screenOrientation="portrait"` de `MainActivity` y `NativeBarcodeScannerActivity`.
   - Añadida anulación de `GmsBarcodeScanningDelegateActivity` (ML Kit) con `tools:node="merge"` y `android:screenOrientation="fullSensor"`.

2. **Documentación**
   - Creado este documento (`docs/ANDROID_PLAY_CONSOLE_COMPLIANCE.md`) con el estado de cada recomendación y los archivos tocados.

---

## Pruebas recomendadas

- Probar la app en orientación vertical y horizontal (y en tablet/plegable si hay dispositivo).
- Comprobar que el escáner de códigos de barras (propio y el de ML Kit) se ve y usa bien en distintas orientaciones.
- Revisar que las pantallas con insets (login, búsqueda, detalle, timeline) sigan mostrando contenido correctamente en Android 15+ y con gestos de navegación.
