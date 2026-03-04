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

**Recomendación:** Dejar de usar `Window.setStatusBarColor`, `Window.setNavigationBarColor` y `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`.

### Estado en nuestra app

- **Código propio:** No usamos esas APIs directamente. Usamos `enableEdgeToEdge()` con `SystemBarStyle.auto(TRANSPARENT, TRANSPARENT)` en `AppUiInitializer.setupEdgeToEdge()` (API recomendada).
- **Dependencias que las usan (según Play Console):**
  - `com.google.android.material.datepicker` (Material DatePicker)
  - `androidx.credentials.playservices.controllers.CreatePublicKeyCredential` (Credentials / Passkey)
  - Posible código ofuscado de librerías (d.r.b, d.t.b, d.w.b).

**Acción:** Sin cambios en nuestro código. Las advertencias provienen de dependencias; se irán reduciendo al actualizar esas librerías. No usar en código nuevo `setStatusBarColor`, `setNavigationBarColor` ni modos de cutout obsoletos.

---

## 3. Vista de extremo a extremo (edge-to-edge) en Android 15+

**Recomendación:** Con target SDK 35, la app se muestra edge-to-edge por defecto; hay que gestionar insets (bajorrelieves) y, si se quiere, llamar a `enableEdgeToEdge()` para retrocompatibilidad.

### Estado en nuestra app

- **Edge-to-edge:** En `AppUiInitializer.setupEdgeToEdge()` se llama a `activity.enableEdgeToEdge()` con barras de sistema transparentes. Se invoca desde `MainActivity.onCreate()` antes de `setContent`.
- **Insets en Compose:** Se usan `Modifier.statusBarsPadding()`, `Modifier.navigationBarsPadding()`, `WindowInsets.statusBars` y `contentWindowInsets` donde hace falta (p. ej. `LoginScreen`, `SearchScreen`, `AppNavigation`, `DetailScreen`, `TimelineComponents`, etc.).

**Archivos relevantes:** `app/src/main/java/com/cafesito/app/startup/AppUiInitializer.kt`, `MainActivity.kt`, y pantallas que aplican padding de insets.

No se requieren cambios adicionales por esta recomendación; conviene seguir probando en dispositivos con muescas y gestos de navegación.

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
