# Idioma de aplicación (WebApp + Android)

**Estado:** vivo  
**Última actualización:** 2026-03-26  
**Ámbito:** `webApp/` + `app/`

---

## 1. Objetivo funcional

- Exponer una opción `Idioma` en el perfil de usuario (entre `Editar perfil` y `Eliminar mi cuenta y mis datos`) en WebApp y Android.
- Al pulsar, abrir una página de selección con estos idiomas:
  - Sistema (siempre primera opción)
  - Español
  - Inglés
  - Francés
  - Portugués
  - Alemán
- Regla de fallback: si el idioma del sistema no está soportado por Cafesito, usar inglés (`en`).

---

## 2. WebApp

### 2.1 Gestión de idioma

- Archivo: `webApp/src/i18n/index.tsx`
- Se añade `LocalePreference = "system" | Locale`.
- Persistencia: `localStorage` en `cafesito.locale`.
- Resolución efectiva:
  - Si la preferencia es `system`, se detecta `navigator.language`.
  - Si no coincide con `es | en | fr | pt | de`, se aplica `en`.
- Se mantiene compatibilidad con `setLocale(locale)` para no romper llamadas previas.

### 2.2 UI en perfil

- Nueva vista: `webApp/src/features/profile/ProfileLanguageView.tsx`.
- Nueva entrada en opciones de perfil (`TopBar`), entre editar y eliminar:
  - `top.language`.
- Navegación:
  - nueva sección de perfil: `language`
  - ruta: `/profile/language`
- Archivos impactados:
  - `webApp/src/features/topbar/TopBar.tsx`
  - `webApp/src/app/AppContainer.tsx`
  - `webApp/src/core/routing.ts`
  - `webApp/src/i18n/messages.ts`

---

## 3. Android

### 3.1 Gestión de idioma

- Nuevo gestor: `app/src/main/java/com/cafesito/app/ui/theme/AppLanguageManager.kt`
- Persistencia en `SharedPreferences` (`cafesito_prefs`, clave `app_language`).
- Preferencias soportadas: `system`, `es`, `en`, `fr`, `pt`, `de`.
- Resolución efectiva:
  - `system` intenta idioma del dispositivo.
  - si no está soportado, fuerza `en`.
- Aplicación del idioma:
  - `AppCompatDelegate.setApplicationLocales(...)`
  - inicialización en arranque en `CafesitoApp.onCreate()`.

### 3.2 UI en perfil

- En `SettingsBottomSheet` se añade la opción `Idioma` entre editar y eliminar.
- Nueva pantalla:
  - `app/src/main/java/com/cafesito/app/ui/profile/LanguageSettingsScreen.kt`
- Nueva ruta de navegación:
  - `profile/{userId}/language`
  - registrada en `app/src/main/java/com/cafesito/app/navigation/AppNavigation.kt`
- Integración desde perfil:
  - `ProfileScreen` recibe `onLanguageClick`.

### 3.3 Recursos string

- Se añaden claves en:
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-en/strings.xml`
  - `app/src/main/res/values-fr/strings.xml`
  - `app/src/main/res/values-pt/strings.xml`
  - `app/src/main/res/values-de/strings.xml`

---

## 4. Verificación mínima

1. **WebApp**
   - `cd webApp && npm run build`
   - Abrir perfil > opciones > idioma.
   - Cambiar idioma y comprobar persistencia al recargar.
   - Seleccionar `Sistema` con idioma del navegador no soportado y comprobar fallback a inglés.

2. **Android**
   - `./gradlew :app:compileDebugKotlin`
   - Perfil > opciones > idioma.
   - Cambiar idioma y comprobar refresco de textos.
   - Seleccionar `Sistema` con locale de dispositivo no soportado y comprobar fallback a inglés.

