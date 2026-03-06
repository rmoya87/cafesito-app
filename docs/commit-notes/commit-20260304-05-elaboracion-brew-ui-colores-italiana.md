# Cambios 2026-03-04 y 2026-03-05 — Elaboración (Brew), UI, colores, Italiana, tiempos

**Estado:** documentado para referencia en futuros cambios  
**Última actualización:** 2026-03-05

---

## Uso

Este documento recopila los cambios de ayer y hoy en **Elaboración (Brew Lab)**, **UI/colores** y **modo Italiana** para que se tengan en cuenta en futuras modificaciones (paridad Android/WebApp, temas, sliders, etc.).

---

## 1. Elaboración (Brew Lab) — Flujo y pantallas

### 1.1 Crear mi café desde “Elige tu café”
- **Android:** En paso “Elige tu café”, el botón **“Crear mi café”** navega a **formulario de crear café** (`addPantryItem?onlyActivity=true&origin=brewlab`), no a “añadir a despensa”.
- **Navegación:** `onCreateCoffeeClick` → pantalla crear café; `onAddToPantryClick` → añadir stock/despensa.
- **Formulario crear café (Brew Lab):** título “CREAR MI CAFÉ”; campo **“Cantidad del café (g)”** con slider 0–2000 g al final del formulario; se pasa `totalGrams` al guardar.

### 1.2 Configuración → Proceso en curso
- **Tiempo de extracción (espresso):** El tiempo configurado en “Configuración” se aplica al temporizador en “Proceso en curso”.
- **Android:** `phasesTimeline` en `BrewLabViewModel` debe incluir `_brewTimeSeconds` en el `combine` para que el timeline (y el timer) reaccionen al cambio de tiempo de extracción.
- **WebApp:** `brewTimeline` ya depende de `espressoTimeSeconds`; sin cambios adicionales.

### 1.3 Pantalla “Proceso en curso” (sin pantalla Resultado separada)
- **Resultado integrado:** No existe paso “Resultado” independiente. Al **terminar el temporizador**:
  - Se oculta el carrusel de consejos y se muestra la tarjeta **“¿QUÉ SABOR HAS OBTENIDO?”** (selección de sabor) en el mismo paso.
  - El botón **“Guardar”** aparece en la **topbar (derecha)**, solo texto, sin fondo; deshabilitado hasta elegir un sabor; al elegir sabor se habilita y guarda en diario.
- **Botones Iniciar/Pausar/Reiniciar:** Se muestran solo **mientras el temporizador no ha terminado**. Cuando el timer termina se ocultan todos. Para “reiniciar”, el usuario debe volver atrás (Configuración) y volver a “Proceso en curso”; entonces el temporizador vuelve a cero.
- **Android:** `BrewStep.RESULT` eliminado. `PreparationStep` muestra `PreparationTasteCard` cuando `timerEnded`; TopBar muestra “Guardar” cuando `step == BREWING && timerEnded`. `backStep()` en BREWING llama a `resetTimer()` antes de pasar a CONFIGURATION.
- **WebApp:** `useBrewTimer` no cambia a `brewStep("result")` al terminar; la tarjeta de sabor y el “Guardar” en topbar se controlan con `timerEnded` y `brewResultShowGuardar`. Evitar dependencias inestables en `useEffect` de `onBrewResultSaveState` (usar `useRef` para `onSaveResultToDiary` y valores actuales).

### 1.4 Sliders en Configuración (agua, ratio, tiempo)
- **Estilo:** Sliders continuos (`steps = 0`), sin marcas visibles. Barra activa con color temático (agua = azul; ratio/tiempo según apartado siguiente); barra inactiva gris (claro en día, oscuro en noche): `Color(0xFFE0E0E0)` / `Color(0xFF404040)` en Android; `--slider-track-inactive` en WebApp.
- **Colores de tiempo (número + slider):** Modo día **negro**, modo noche **blanco** (no marrón):
  - **Android:** Número “Tiempo (s)” y etiqueta “TIEMPO DE EXTRACCIÓN” + slider de tiempo: `timeColor` / `timeSliderColor` = `Color.White` (dark) / `Color.Black` (light). Temporizador grande y tiempo transcurrido en “Proceso en curso”: mismo criterio. Barra de fases (`BrewTimeline`): `timeBarColor` blanco/negro según tema.
  - **WebApp:** Clase `is-time` en el input de tiempo y `app-range--time` en el range; `.brew-prep-clock` y `.brew-prep-total strong` y barra de fases (`.brew-prep-bar i`): en tema base (noche) blanco; en `prefers-color-scheme: light` negro. Variables `--slider-color` para `app-range--time` y colores de texto correspondientes.

### 1.5 Modo Italiana (y Turco) — Slider de café
- **Problema:** En Italiana solo aparecía el slider de agua; el usuario no podía configurar el café desde un slider.
- **Solución (Android y WebApp):** Mostrar **slider “CANTIDAD DE CAFÉ (g)”** cuando `isWaterEditable && !isRatioEditable` (Italiana, Turco).
- **Android:** Bloque nuevo en `ConfigStep` con rango `coffeeMin = waterMinMl/ratioMax`, `coffeeMax = waterMaxMl/ratioMin` (entre 1 y 250). Color del slider: `LocalCaramelAccent.current` (marrón).
- **WebApp:** `isCoffeeSliderShown = isWaterEditable && !isRatioEditable`; slider con clase `app-range--coffee`; al mover se actualiza agua como `coffee × defaultRatio`. CSS: `.app-range--coffee` con `--slider-color` marrón (caramel-soft / caramel-accent según tema).

### 1.6 Número de café en Ajustes técnicos — Marrón
- **Android:** El valor del campo “Café (g)” usa **marrón**: `LocalCaramelAccent.current` (no `primary` ni negro).
- **WebApp:** Clase `is-coffee` en el input “Café (g)”. CSS: `.brew-tech-value-input.is-coffee` con `color: var(--caramel-soft)` (tema base) y `var(--caramel-accent)` en tema claro.

---

## 2. UI y temas (resumen para consistencia)

- **Botones primarios / chips de sabor activos:** Modo día texto blanco, modo noche texto negro; fondo marrón / marrón claro según tema.
- **Sliders:** Agua = azul; café/ratio = marrón (caramel); tiempo = negro (día) / blanco (noche). Barra inactiva siempre gris.
- **Temporizador y barras de tiempo:** Negro en día, blanco en noche (incl. número grande, tiempo transcurrido y barra de fases).
- **Dark mode (marrón):** Donde antes se usaba marrón oscuro fijo, usar `LocalCaramelAccent` (Android) o variables `--caramel-soft` / `--caramel-accent` (WebApp) para marrón claro en noche.

---

## 3. Archivos relevantes (referencia rápida)

| Área | Android | WebApp |
|------|---------|--------|
| Brew config / sliders | `BrewLabComponents.kt` (ConfigStep) | `BrewViews.tsx` (config), `features.css` (.app-range, .brew-tech-*) |
| Proceso en curso / timer | `BrewLabComponents.kt` (PreparationStep, BrewTimeline) | `BrewViews.tsx` (brewStep === "brewing"), `features.css` (.brew-prep-*) |
| TopBar Guardar | `BrewLabScreen.kt` (GlassyTopBar actions) | `TopBar.tsx`, `AppContainer.tsx` (brewResultSaveMeta, brewResultShowGuardar) |
| Timer / resultado integrado | `BrewLabViewModel.kt` (phasesTimeline, timerEnded, resetTimer) | `useBrewTimer.ts`, `useTopBarActions.ts` |
| Navegación crear café | `AppNavigation.kt`, `BrewLabScreen.kt` | — |
| Crear café (cantidad g) | `AddPantryItemScreen.kt`, `DiaryViewModel.kt` | — |
| Tema / colores | `Theme.kt` (LocalCaramelAccent), `Color.kt` | `features.css` (--caramel-*, --pure-white, --pure-black) |

---

## 4. Workflow y despliegue (recordatorio)

- **main** no dispara el workflow (no está en `branches` del push y el job `changes` tiene `if` que excluye push a main).
- **Push** a Interna, Interno, Alpha, Beta, Producción (y variantes en minúsculas) **sí** dispara el workflow.
- **workflow_dispatch** permite lanzar manualmente eligiendo rama y opciones (solo Android, solo web, etc.).
- **schedule** (03:00 UTC): despliegue nocturno según `NIGHTLY_DEPLOY_BRANCH`.
- Documentación detallada: `docs/RELEASE_DEPLOY_WORKFLOW.md` (actualizar si se cambia el comportamiento de triggers).

---

## 5. Otros cambios recientes (contexto)

- Símbolos nativos en release Android; notas de versión automáticas promocionales.
- Sincronización de favoritos en `SyncManager.syncAll()` para que los favoritos de web aparezcan en Android.
- Migración de `ClickableText` a `Text` con `LinkAnnotation` en `LoginScreen.kt`.
- Play Console: orientación, edge-to-edge, `assetlinks.json` y ASO; ver `docs/ANDROID_PLAY_CONSOLE_COMPLIANCE.md` y `docs/ASO_PLAY_STORE.md`.
- Diario: visibilidad de números de 3 dígitos en el gráfico; menos espacio bajo las tabs (Mi diario, Perfil, ADN).
- Revisión de crashes: proceso manual documentado en `docs/CRASH_FIX_WEEKLY.md` (sin automatización Git/OpenAI en CI).
