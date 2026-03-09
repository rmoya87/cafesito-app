# Registro de desarrollo e incidencias

**Propósito:** Documentar cambios, correcciones y decisiones recientes para tenerlos en cuenta en próximos desarrollos o incidencias.  
**Última actualización:** 2026-03-04

---

## Índice rápido

1. [Elaboración (Brew), UI, colores, Italiana](#1-elaboración-brew-ui-colores-italiana)
2. [Incidencias CI/CD y TypeScript (deploy-web)](#2-incidencias-cicd-y-typescript-deploy-web)
3. [Ramas y entornos](#3-ramas-y-entornos)
4. [Reglas Cursor por plataforma](#4-reglas-cursor-por-plataforma)
5. [Webapp — Diario: rejilla de meta y máscara de scroll](#5-webapp--diario-rejilla-de-meta-y-máscara-de-scroll)
6. [Referencias a otros documentos](#6-referencias-a-otros-documentos)
7. [Resumen de cambios — accesibilidad, diario, documentación (03–04 mar 2026)](#7-resumen-de-cambios--accesibilidad-diario-documentación-0304-mar-2026)
8. [Release notes Android desde última publicación (git)](#8-release-notes-android-desde-última-publicación-git)
9. [Resumen de cambios — historial, detalle café (04 mar 2026)](#9-resumen-de-cambios--historial-detalle-café-04-mar-2026)

---

## 1. Elaboración (Brew), UI, colores, Italiana

Detalle completo en: **`docs/commit-notes/commit-20260304-05-elaboracion-brew-ui-colores-italiana.md`**.

Resumen para futuras modificaciones:

- **Crear mi café:** Botón “Crear mi café” en “Elige tu café” va al formulario de crear café (`addPantryItem?onlyActivity=true&origin=brewlab`). Formulario incluye “Cantidad del café (g)” (slider 0–2000 g) y título “CREAR MI CAFÉ”.
- **Tiempo de extracción (espresso):** Debe aplicarse al temporizador en “Proceso en curso”. En Android, `phasesTimeline` debe incluir `_brewTimeSeconds` en el `combine`.
- **Sin pantalla Resultado:** Todo queda en “Proceso en curso”: al terminar el timer se muestra la tarjeta “¿QUÉ SABOR HAS OBTENIDO?” y el “Guardar” en la topbar (solo texto). No hay botón Reiniciar al terminar; para repetir se vuelve atrás a Configuración.
- **Colores:**
  - **Tiempo (número y slider):** Modo día negro, modo noche blanco (no marrón). Incluye temporizador grande, tiempo transcurrido y barra de fases (`BrewTimeline`).
  - **Café (número en Ajustes técnicos):** Siempre marrón (`LocalCaramelAccent` / `--caramel-soft` / `--caramel-accent`).
  - **Sliders:** Agua = azul; ratio/café = marrón; tiempo = negro/blanco según tema; barra inactiva gris.
- **Modo Italiana (y Turco):** Mostrar slider “CANTIDAD DE CAFÉ (g)” cuando `isWaterEditable && !isRatioEditable`. Rango derivado de `waterMinMl/ratioMax` a `waterMaxMl/ratioMin`.

---

## 2. Incidencias CI/CD y TypeScript (deploy-web)

### 2.1 Error: `BrewBaristaTip[]` vs `{ label, value, icon: string }[]`

**Síntoma:** El job `deploy-web` fallaba con:

```text
Argument of type '{ label: string; value: string; icon: string; }[]' is not assignable to parameter of type 'BrewBaristaTip[]'.
Type '{ label: string; value: string; icon: string; }' is not assignable to type 'BrewBaristaTip'.
  Types of property 'icon' are incompatible.
  Type 'string' is not assignable to type '"coffee" | "grind" | "thermostat" | "water" | "clock"'.
```

**Causa:** En `webApp/src/core/brew.ts`, la variable `baseTips` se infiere por la cadena de ternarios y TypeScript acaba infiriendo `icon` como `string`.

**Solución:** Declarar explícitamente el tipo de `baseTips`:

```ts
const baseTips: BrewBaristaTip[] = !key ? defaults : key.includes("espresso") ? [ ... ] : ...
```

**Archivo:** `webApp/src/core/brew.ts` (función `getBrewBaristaTipsForMethod`).

---

### 2.2 Error: comparación `"coffee"` con tipo sin overlap

**Síntoma:** El job `deploy-web` fallaba con:

```text
This comparison appears to be unintentional because the types '"search" | "timeline" | "brewlab" | "diary" | "profile"' and '"coffee"' have no overlap.
```

**Causa:** En `AppContainer.tsx`, el `TopBar` se renderiza dentro de `{guardedActiveTab !== "coffee" ? ( <TopBar ... /> ) : ...}`. En ese bloque TypeScript restringe `guardedActiveTab` a los otros tabs, por lo que comparar de nuevo con `"coffee"` genera el error.

**Soluciones aplicadas:**

1. **Props del TopBar en la rama no-coffee:** En esa rama nunca estamos en `"coffee"`, así que se pasan valores fijos en lugar de depender de `guardedActiveTab === "coffee"`:
   - `coffeeTopbarFavoriteActive={false}`
   - `coffeeTopbarStockActive={false}`

2. **Clase `is-coffee` en el main shell:** Donde se usaba `guardedActiveTab === "coffee"` para la clase `main-shell-scroll is-coffee`, se cambió a `activeTab === "coffee"` (sin restricción de tipo).

**Archivos:** `webApp/src/app/AppContainer.tsx` (aprox. líneas 1760–1768).

---

## 3. Ramas y entornos

### 3.1 Ramas que se mantienen

Solo se usan estas ramas para entornos y despliegue:

| Rama     | Uso / entorno |
|----------|----------------|
| **main** | Desarrollo principal; no dispara el workflow de deploy. |
| **beta** | Pruebas abiertas (Play Console, web). |
| **alpha** | Pruebas cerradas. |
| **Interna** | Pruebas internas (solo Android en el workflow). |

Cualquier otra rama (codex/*, fix/*, webapp, webap, Alpha/Beta con mayúscula, etc.) se considera prescindible y puede eliminarse para no acumular ramas activas.

### 3.2 Acciones realizadas (limpieza de ramas)

- **Remoto (origin):** Se eliminaron las ramas `Alpha`, `Beta`, `webap`, `webapp`, `fix/alpha-consolidacion-webapp-webap-20260225`, `fix/widgets-diary-and-deprecations` y un gran número de ramas `codex/*`. Se mantienen solo `main`, `beta`, `alpha`, `Interna`. (Nota: la eliminación masiva de `codex/*` se lanzó en segundo plano; si alguna sigue existiendo en origin, se puede borrar con `git push origin --delete <nombre>`.)
- **Local:** Se eliminaron las ramas locales `Alpha`, `webap`, `beta` (luego se recreó `beta` desde `main`), `alpha-merge`, `beta-play-fix`, `webapp`. Se mantienen `main` y `beta` (y opcionalmente `alpha` si se trabaja con esa pestaña).

### 3.3 Flujo habitual: subir main y actualizar beta

1. Subir cambios a `main`:  
   `git push origin main`
2. Dejar `beta` igual que `main`:  
   `git push origin main:beta`

Si en remoto ya no existe `beta`, `main:beta` la crea. Si existe, la actualiza (puede requerir `--force` si la historia diverge y se quiere que beta sea exactamente main).

---

## 4. Reglas Cursor por plataforma

En `.cursor/rules/` hay reglas específicas que se aplican al editar código de cada plataforma:

| Plataforma | Archivo | Cuándo se aplica |
|------------|---------|-------------------|
| **WebApp** | `webapp-react-typescript.mdc` | Al editar `webApp/**/*.ts`, `*.tsx`, `*.css`, `*.mjs` |
| **Android (app)** | `android-kotlin-compose.mdc` (siempre) + `android-app-especifico.mdc` | General siempre; contexto de `app/` al editar `app/**/*.kt`, `*.xml` |
| **iOS** | `ios-swiftui.mdc` | Al editar `iosApp/**/*.swift`, `Package.swift` |

Además: `docs-before-code.mdc` (siempre) — consultar docs antes de actuar.

---

## 5. Webapp — Diario: rejilla de meta y máscara de scroll

**Fecha:** 2026-03-04. **Ámbito:** solo webapp (`webApp/`).

### 5.1 Rejilla de meta (`.diary-entry-meta-grid`)

- **Base (todas las anchuras):** `gap: 8px 30px` (antes `24px`). Archivo: `webApp/src/styles/features.css`.
- **En `@media (max-width: 899px)`:** En los dos bloques que afectan a `.diary-entry-meta-grid`, el `gap` se unificó a `8px 25px` (uno tenía `20px`, otro `8px 10px`). Así en móvil/tablet la rejilla usa 8px entre filas y 25px entre columnas.

### 5.2 Máscara de scroll (`.diary-entry-meta-scroll`)

- **Comportamiento:** Por defecto no se muestra máscara (`mask-image: none`). La máscara izquierda solo se muestra cuando hay scroll horizontal y hay contenido que ha “desaparecido” a la izquierda (`scrollLeft > 0`). La máscara derecha solo cuando hay más contenido a la derecha (`scrollLeft + clientWidth < scrollWidth`).
- **Implementación:**
  - **CSS:** Clases `.has-scroll-left` y `.has-scroll-right` aplican los gradientes correspondientes; si ambas están presentes se usa un único gradiente que difumina ambos lados. Archivo: `webApp/src/styles/features.css`.
  - **Lógica:** En `DiaryView.tsx` se añadieron estado `metaScrollLeft` y `metaScrollRight`, callback `updateMetaScrollEdges()` (actualiza esos estados según `scrollLeft` y dimensiones del nodo), y suscripción a `scroll` y `ResizeObserver` en el contenedor `.diary-entry-meta-scroll`. Las clases `has-scroll-left` / `has-scroll-right` se aplican al mismo div según ese estado.

---

## 6. Referencias a otros documentos

| Tema | Documento |
|------|-----------|
| Tokens de diseño (colores, espaciados, radios) Web + Android | `docs/DESIGN_TOKENS.md` |
| Estados vacío y error (patrón unificado) | `docs/UX_EMPTY_AND_ERROR_STATES.md` |
| Lógica de negocio compartida (diario, brew, recomendaciones) | `docs/SHARED_BUSINESS_LOGIC.md` |
| Tests de humo (flujo crítico) | `docs/SMOKE_TESTS.md` |
| Accesibilidad mínima (aria, 44px, WCAG) | `docs/ACCESSIBILITY_MINIMA.md` |
| Workflow release y deploy (triggers, ramas, jobs) | `docs/RELEASE_DEPLOY_WORKFLOW.md` |
| Changelog detallado Brew/UI/colores/Italiana (04–05 mar) | `docs/commit-notes/commit-20260304-05-elaboracion-brew-ui-colores-italiana.md` |
| Compliance Play Console, orientación, edge-to-edge | `docs/ANDROID_PLAY_CONSOLE_COMPLIANCE.md` |
| ASO y App Links (assetlinks.json) | `docs/ASO_PLAY_STORE.md` |
| Revisión manual de crashes (sin automatización en CI) | `docs/CRASH_FIX_WEEKLY.md` |

---

## 7. Resumen de cambios — accesibilidad, diario, documentación (03–04 mar 2026)

Recopilación de lo modificado en las sesiones recientes (diario webapp, accesibilidad Android y webapp, documentación de diseño y pruebas).

### 7.1 Webapp

| Ámbito | Archivo(s) | Cambio |
|--------|------------|--------|
| **Diario — rejilla de meta** | `webApp/src/styles/features.css` | `.diary-entry-meta-grid`: `gap: 8px 30px` (base); en `@media (max-width: 899px)` → `gap: 8px 25px`. |
| **Diario — máscara de scroll** | `webApp/src/styles/features.css` | `.diary-entry-meta-scroll`: por defecto `mask-image: none`; clases `.has-scroll-left` y `.has-scroll-right` aplican gradientes solo cuando hay contenido oculto a ese lado. |
| **Diario — lógica máscara** | `webApp/src/features/diary/DiaryView.tsx` | Estado `metaScrollLeft` / `metaScrollRight`, callback `updateMetaScrollEdges()`, suscripción a `scroll` y `ResizeObserver` en el contenedor de meta; clases dinámicas en el div. |
| **Diario — alt en imágenes** | `webApp/src/features/diary/DiarySheets.tsx` | Imágenes de café en despensa y sugerencias: `alt={row.coffee.nombre}` y `alt={coffee.nombre}`; imagen placeholder taza con `alt=""` y `aria-hidden="true"`. |
| **Tests** | `webApp/src/App.test.tsx` | Test de humo: navegar a Diario y comprobar que se muestra contenido del diario (texto "mi diario" o "sin café o agua registrada"). |

### 7.2 Android — accesibilidad

Se sustituyeron **todos** los `contentDescription = null` e `Icon(..., null)` por descripciones útiles para TalkBack y se aseguró área táctil mínima en un botón crítico.

| Ámbito | Archivo(s) | Cambio |
|--------|------------|--------|
| **Diario** | `EditNormalStockScreen.kt`, `AddDiaryEntryScreen.kt`, `DiaryComponents.kt`, `AddPantryItemScreen.kt`, `AddStockScreen.kt` | Imágenes de café → nombre del café; iconos WaterDrop, LocalCafe, Search, AddCircle, CoffeeMaker → "Añadir agua", "Añadir café", "Buscar", "Crear café", "Método de elaboración"; portafiltro → "Gramos"; MoreHoriz → "Opciones"; chips → `label`; ChevronRight → "Abrir". |
| **BrewLab** | `BrewLabCards.kt`, `BrewLabComponents.kt` | Imágenes de café → nombre; Add → "Añadir café a la despensa"; AddCircle, Search, Play/Pause → "Crear mi café", "Buscar café", "Iniciar"/"Pausar"; CoffeeMaker, AutoAwesome, ChevronRight → descripciones acordes; tips → `tip.label`. |
| **Valoraciones** | `RatingBar.kt`, `SemicircleRatingBar.kt`, `UserReviewCard.kt` | Estrellas → "Estrella i de n" / "Valoración i de 5"; imagen reseña y café → "Imagen de la reseña" / nombre; estrella → "Valoración". |
| **Búsqueda / perfil** | `SearchScreen.kt`, `SearchUsersScreen.kt`, `CompleteProfileScreen.kt`, `UserSuggestionCard.kt`, `ProfileComponents.kt`, `FollowingScreen.kt`, `FollowersScreen.kt` | Imágenes de café y favoritos → nombre / "Quitar de favoritos" | "Añadir a favoritos"; Search → "Buscar" o "Buscar usuarios"; foto perfil → "Foto de perfil" / "Añadir foto de perfil"; avatar sugerencias → "Avatar de {username}"; IconButton favoritos en SearchScreen → `minimumInteractiveComponentSize()` (área táctil ≥ 48 dp). |
| **Timeline / publicaciones** | `TimelineComponents.kt`, `PostCard.kt`, `AddPostScreen.kt` | Imágenes de post y café → "Imagen del post" / nombre; iconos PhotoCamera, Close, Schedule, CoffeeMaker → "Cámara", "Cerrar", "Hora", `option.label`; DetailPremiumBlock → `label`; Coffee, ArrowForwardIos, AddPhotoAlternate → "Añadir café", "Seleccionar café", "Añadir foto". |
| **Detalle / acceso** | `DetailScreen.kt`, `LoginScreen.kt`, `RecommendationCarousel.kt` | Editar/Añadir reseña → "Editar reseña" / "Añadir reseña"; icono opción login → `title`; imagen recomendación → nombre del café. |
| **Diario — máscara de scroll** | `TimelineComponents.kt` (`DiaryEntryItem`) | Máscara condicional en la fila de meta (LazyRow): gradientes izquierda/derecha según `hasScrollLeft` / `hasScrollRight` derivados del estado de scroll, alineado con el comportamiento de la webapp. |

### 7.3 Documentación creada o actualizada

| Documento | Descripción |
|-----------|-------------|
| **`docs/DESIGN_TOKENS.md`** | Tokens de diseño (colores día/noche, espaciados, radios) para Web y Android; variables CSS y uso en Compose. |
| **`docs/UX_EMPTY_AND_ERROR_STATES.md`** | Patrón unificado para listas vacías (mensaje, icono, CTA) y errores de red (mensaje + "Reintentar"). |
| **`docs/SHARED_BUSINESS_LOGIC.md`** | Qué lógica es compartida (Kotlin shared), replicada (Web TS) o por plataforma; diario, brew, recomendaciones, fechas. |
| **`docs/SMOKE_TESTS.md`** | Flujo crítico (login → diario → detalle/añadir) y dónde implementar tests de humo en Web (Vitest/RTL) y Android (instrumented/Compose). |
| **`docs/ACCESSIBILITY_MINIMA.md`** | Criterios mínimos: aria-label / contentDescription, área de tap ≥ 44px / 48 dp, contraste WCAG. |
| **`docs/README.md`** | Enlaces a los cinco documentos anteriores en la sección "Arquitectura y gobernanza". |
| **`docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md`** | Sección 5 (diario webapp), sección 6 (referencias), esta sección 7 (resumen). |

### 7.4 Resumen por tipo de cambio

- **UI/UX:** Rejilla y máscara de scroll en diario (webapp y Android); área táctil mínima en botón favoritos (Android).
- **Accesibilidad:** contentDescription/aria-label y alt en iconos, imágenes y controles; documento de criterios mínimos.
- **Calidad:** Test de humo web (navegación a Diario); documentos de smoke tests, estados vacío/error y lógica compartida.
- **Mantenibilidad:** DESIGN_TOKENS, README y registro actualizados para no desalinear Web y Android.

---

## 8. Release notes Android desde última publicación (git)

**Fecha:** 2026-03-05. **Ámbito:** workflow Release & Deploy (Android).

### Objetivo

Que las notas “Qué hay de nuevo” en Play Store reflejen **todos los cambios desde la última versión que el usuario tenía instalada**, usando el historial de git en lugar de textos genéricos.

### Cambios en `.github/workflows/release-deploy.yml`

1. **Checkout:** Se añade `fetch-tags: true` para que el job tenga los tags `deploy/android/<pista>/*` creados en despliegues anteriores.
2. **Generar release notes:**
   - Se busca el último tag `deploy/android/<track>/<versionCode>` que sea ancestro de HEAD (por pista: internal, alpha, beta, production).
   - Se ejecuta `git log TAG..HEAD --no-merges --pretty=format:%s` y se formatea como lista de viñetas.
   - Se filtran mensajes `chore(release):` y `[skip ci]`; se eliminan duplicados; se limita a 500 caracteres (límite de Play).
   - Si no hay tag previo (primera publicación), se usa el texto genérico: "Mejoras y correcciones. ¡Gracias por usar Cafesito!".
3. **Tag tras subida a Play:** Después de “Upload to Google Play” y antes de “Save last deployed version”, se crea el tag anotado `deploy/android/<track>/<versionCode>` en el commit actual (el del bump) y se hace push. Así la próxima ejecución puede tomar ese tag como “última versión desplegada”.

### Documentación

- **`docs/RELEASE_DEPLOY_WORKFLOW.md`:** Actualizada la descripción del job `release-android` (notas desde git, tag de deploy).

### Primera ejecución

En la primera vez que corra el workflow **no** existirán tags `deploy/android/*`; las release notes serán el texto genérico. A partir del primer despliegue exitoso se creará el tag y las siguientes releases mostrarán los commits desde esa versión.

---

## 9. Resumen de cambios — historial, detalle café (04 mar 2026)

Cambios de UI/UX en webapp: página historial (sin scroll innecesario en desktop) y botones del detalle de café (favorito, despensa, compartir) con fondo y borde siempre blancos.

### 9.1 Webapp — Historial (desktop): sin scroll innecesario

- **Problema:** En versión desktop, la página de historial mostraba barra de scroll aunque no hubiera suficientes ítems para justificarla.
- **Causa:** `.content.content-profile` y `.historial-view` tenían `min-height: 100%` y `min-height: 100dvh`, forzando una altura mínima de viewport y provocando overflow.
- **Solución:** Se eliminaron esas propiedades de `min-height` en ambos bloques. La altura del contenido depende solo de los ítems; el fondo de la zona vacía lo aporta `.main-shell-scroll` (tokens).
- **Archivos:** `webApp/src/styles/features.css` (`.content.content-profile`, `.historial-view`).

### 9.2 Webapp — Detalle café: botones favorito, despensa y compartir

- **Objetivo:** Los botones del hero del detalle de café (favorito, despensa, compartir) deben tener **siempre** fondo blanco y borde blanco/claro (día y noche), estén activos o no. Solo el **icono** puede cambiar de color (p. ej. rojo cuando favorito está activo).
- **Cambios:**
  - Se forzó `background: var(--pure-white) !important` y `border: 1px solid rgba(0, 0, 0, 0.12) !important` para `.coffee-detail-topbar-icon` y `.coffee-detail-topbar-icon.is-active`, de modo que las reglas genéricas de `topbar-icon-button` (p. ej. `background: transparent` en modo oscuro) no los sobrescriban.
  - El estado activo solo modifica el color del icono: `color: var(--electric-red)` en `.is-active` y en `.is-active .ui-icon`.
  - Misma lógica aplicada en `@media (prefers-color-scheme: dark)`, en bloques desktop y en `theme-forced.css` para `html.theme-dark` y `html.theme-light`.
- **Terminología:** En comentarios del CSS se sustituyó "me gusta" por "favorito" en los bloques del detalle de café.
- **Archivos:** `webApp/src/styles/features.css`, `webApp/src/styles/theme-forced.css`.

### 9.3 Resumen por archivo

| Archivo | Cambio |
|---------|--------|
| `webApp/src/styles/features.css` | `.content.content-profile` y `.historial-view` sin `min-height`; `.coffee-detail-topbar-icon` / `.is-active` con bg y borde blancos `!important`; comentarios "favorito" en lugar de "me gusta". |
| `webApp/src/styles/theme-forced.css` | `html.theme-dark` y `html.theme-light`: mismo bg y borde blancos para los iconos del detalle café; solo color del icono en rojo cuando `.is-active`. |

---

*Este registro debe actualizarse cuando se resuelvan incidencias relevantes o se tomen decisiones que afecten a ramas, CI o comportamiento de la app (Android/WebApp).*
