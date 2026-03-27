# Internacionalización (i18n) — WebApp y Android

**Estado:** vivo  
**Última actualización:** 2026-03-27  
**Ámbito:** `webApp/src/i18n/`, `app/src/main/res/values*/strings.xml`, preferencia de idioma y contenido dinámico (Brew, diario).

**Propósito:** Una sola referencia para saber qué idiomas hay, cómo está montado el sistema técnico en cada plataforma y cómo añadir o cambiar textos sin hardcodear y sin romper paridad.

---

## 1. Idiomas soportados

| Código | Idioma | Notas |
|--------|--------|--------|
| `es` | Español | Idioma base de contenido en muchos flujos; en WebApp suele ser fallback de claves. |
| `en` | Inglés | Fallback cuando falta traducción o el sistema no coincide con un idioma soportado. |
| `fr` | Francés | |
| `pt` | Portugués | |
| `de` | Alemán | |

Además existe la opción **Sistema** (`system`):

- **Android:** `AppLanguageManager.SYSTEM` — la app sigue el locale del sistema (`LocaleListCompat` vacío = comportamiento por defecto del SO).
- **WebApp:** `LocalePreference "system"` — se resuelve con `navigator.language` contra la lista `SUPPORTED`; si no coincide, **inglés** (`en`).

**Regla de negocio común:** si el idioma del sistema/navegador no está en la tabla, se usa **inglés** como sustituto (Android: `resolveEffectiveLanguage`; Web: `detectSystemLocale`).

---

## 2. WebApp — técnica

### 2.1 Archivos clave

| Ruta | Rol |
|------|-----|
| `webApp/src/i18n/messages.ts` | Tabla `MESSAGES` por locale, tipo `I18nKey` (union de todas las claves). |
| `webApp/src/i18n/index.tsx` | `I18nProvider`, `useI18n()`, `t(key, vars?)`, persistencia en `localStorage`. |

### 2.2 API de traducción

- **`t("clave")`** — texto plano.
- **`t("clave", { nombre: valor })`** — interpolación: en el string del mensaje se usan placeholders `{nombre}` y se sustituyen en runtime.

Resolución de fallback en `t`:

1. `MESSAGES[localeActual][key]`
2. Si falta: `MESSAGES.es[key]`
3. Si sigue faltando: la propia `key` (evitar en producción: añadir siempre la clave en `es` como mínimo).

### 2.3 Preferencia de usuario

- Clave principal: `cafesito.locale` (valores: `system` | `es` | `en` | `fr` | `pt` | `de`).
- Compatibilidad: `cafesito.locale.value` (legacy).

Pantalla de perfil: `webApp/src/features/profile/ProfileLanguageView.tsx` (y rutas de perfil según `core/routing.ts`).

### 2.4 Cómo añadir o cambiar un texto (WebApp)

1. Añadir la clave al tipo **`I18nKey`** en `messages.ts` (TypeScript obligará a completar todos los locales en `MESSAGES`).
2. Rellenar el string en **los cinco bloques** de locale (`es`, `en`, `fr`, `pt`, `de`) dentro de `MESSAGES`.
3. Usar **`t("mi.clave")`** en el componente; no dejar literales de UI en español/inglés sueltos salvo datos de dominio (nombres de café, etc.).

### 2.5 Contenido dinámico (Brew, diario, etc.)

Si el motor compartido devuelve frases en español, la WebApp aplica funciones de traducción por locale (p. ej. en `BrewViews.tsx`: consejos de temporizador, tips de barista). Reglas:

- No duplicar lógica de negocio solo para i18n: preferir funciones **`translate*`** o mapas por `locale` que transformen el texto generado o etiquetas conocidas.
- Mantener **paridad conceptual** con Android (misma información, mismo orden de ideas).

---

## 3. Android — técnica

### 3.1 Archivos clave

| Ruta | Rol |
|------|-----|
| `app/src/main/res/values/strings.xml` | Español (o default del proyecto según convención actual; conviene mantener **todas** las claves aquí). |
| `app/src/main/res/values-en/strings.xml` | Inglés |
| `app/src/main/res/values-fr/strings.xml` | Francés |
| `app/src/main/res/values-pt/strings.xml` | Portugués |
| `app/src/main/res/values-de/strings.xml` | Alemán |
| `app/src/main/java/.../AppLanguageManager.kt` | Preferencia guardada, aplicación de locale, lista soportada. |
| `app/src/main/java/.../CafesitoApp.kt` | `AppLanguageManager.applySavedLanguage(this)` al arrancar. |

### 3.2 Preferencia y aplicación del locale

- SharedPreferences: `cafesito_prefs` → clave `app_language` (`system` | `es` | `en` | …).
- **`AppCompatDelegate.setApplicationLocales(...)`** — con `LocaleListCompat` vacío para “sistema”, o `forLanguageTags(effective)` para idioma fijo.
- Tras cambiar idioma en ajustes, se puede llamar a **`recreate()`** de la actividad para refrescar Compose de inmediato.

Pantalla de ajustes: `LanguageSettingsScreen.kt` (ruta de navegación bajo perfil).

### 3.3 Uso en UI

- **`stringResource(R.string.mi_clave)`** en composables.
- Cadenas con formato: `stringResource(R.string.mi_clave, arg1, arg2)` alineado con placeholders en XML (`%1$s`, `%d`, etc.).
- **`contentDescription`**: siempre que sea texto para el usuario o TalkBack, usar recursos, no literales (ver `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`).

### 3.4 Detección del idioma activo de la app

Para lógica que dependa del idioma (traducción de textos generados por el motor Brew, tips, etc.), usar el locale de la configuración de la app, no solo `Locale.getDefault()` del sistema:

- **`LocalConfiguration.current.locales[0]`** (o equivalente API 24+) para alinear con el idioma elegido en ajustes.

### 3.5 Cómo añadir o cambiar un texto (Android)

1. Añadir o editar la entrada en **`values/strings.xml`** (clave estable, nombre descriptivo).
2. Replicar la misma clave en **`values-en`**, **`values-fr`**, **`values-pt`**, **`values-de`** con la traducción.
3. Referenciar desde código con `R.string...` / `stringResource`.

Si se añade un **idioma nuevo** al producto: crear `values-xx/strings.xml`, actualizar `AppLanguageManager.SUPPORTED` y la UI de selección de idioma.

### 3.6 Contenido dinámico (Brew, etc.)

Parte del texto sale de **`shared`** / `BrewEngine` en español. Donde haga falta, existen capas **`translate*ByLanguage`** o equivalentes en componentes (p. ej. `BrewLabComponents.kt`). Al modificar el motor:

- Actualizar las funciones de traducción o mapas asociados.
- Probar en **inglés y al menos un idioma más** para evitar regresiones a español mezclado.

---

## 4. Paridad y qué no hacer

| Incorrecto | Correcto |
|------------|----------|
| `Text("Guardar")` en Compose | `Text(stringResource(R.string.common_save))` |
| `"Seleccionar periodo"` en JSX | `t("diary.selectPeriod")` |
| Añadir clave solo en `values-en` | Misma clave en los cinco `values*` |
| Añadir clave solo en `MESSAGES.en` | Completar los cinco locales en `MESSAGES` |
| Usar `Locale.getDefault()` para textos de Brew cuando el usuario eligió otro idioma en la app | `LocalConfiguration` / locale efectivo de la app |
| Contenido dinámico solo en español en UI | Traducir vía capa dedicada o strings por caso |

---

## 5. Checklist rápido (nueva pantalla o feature)

- [ ] Web: claves nuevas en `I18nKey` + `MESSAGES` para `es`, `en`, `fr`, `pt`, `de`.
- [ ] Android: mismas ideas en `strings.xml` en los cinco `values*`.
- [ ] Accesibilidad: `aria-label` / `contentDescription` también con recursos o `t()`.
- [ ] Probar cambio de idioma en **ajustes** sin reiniciar manualmente (Web: recarga si aplica; Android: `recreate` o recomposición).
- [ ] Si hay analytics con nombres de pantalla en string, revisar `docs/ANALITICAS.md` por si se documentan eventos nuevos.

---

## 6. Referencias cruzadas

- Accesibilidad: `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`
- Analíticas y rutas: `docs/ANALITICAS.md`
- Índice general de documentación: `docs/README.md` (sección 4)
- Paridad elaboración / motor: `docs/BREW_ENGINE_PARITY_CONTRACT.md` (cuando afecte a textos o fases)

---

## 7. Historial de decisiones (resumen)

- Selector de idioma en perfil (Web y Android) y opción “Sistema” documentada en `REGISTRO_DESARROLLO_E_INCIDENCIAS.md` (entrada 26).
- Fallback Web: `es` → clave; fallback Android: recursos por `values-*` del sistema de recursos.
