# Changelog: cambios WebApp y guía de traslado a Android

**Estado:** vivo  
**Última actualización:** 2026-03-19  
**Ámbito:** WebApp (React) → Android (Kotlin/Compose) — paridad funcional y UX.

---

## 1. Resumen ejecutivo

Este documento recoge **todos los cambios realizados en la WebApp** (timeline/home, diario, perfil, elaboración/BrewLab, UI global) y sirve como **guía de traslado a Android** de forma nativa, manteniendo la misma lógica de negocio y flujos de usuario.

---

## 2. Cambios en la WebApp (inventario)

### 2.1 Navegación y nombres

| Cambio | Descripción |
|--------|-------------|
| Timeline → Home | Todas las referencias "timeline" pasan a "home": rutas (`/home`), tab ID, clases CSS, títulos. |
| Título página Home | De "Inicio" a "CAFESITO" en topbar y título del documento. |

### 2.2 Home (antes Timeline)

| Cambio | Descripción |
|--------|-------------|
| Hero eliminado | Se eliminó el hero; el topbar usa los mismos colores que el resto de la app. |
| Topbar estándar | Título "CAFESITO", sin avatar/greeting en hero. |
| Recomendaciones del día | Lista horizontal de **cards**: cada card tiene **3 cafés** en columna (imagen izquierda, nombre + marca derecha). **9 recomendaciones** en total (3 cards). Nombres de café hasta **2 líneas**. Línea gris suave entre filas. |
| Métodos de elaboración | Carousel con **orden por uso reciente** (más reciente a la izquierda). Métodos no usados en orden alfabético. Se añaden **"Otros"** (icono rayo) y **"Agua"** (icono gota) al final si no usados. |
| Tu despensa | Click en card de café en "Tu despensa" **navega al detalle del café** (ya no inicia elaboración). |
| Sin posts ni comentarios | Eliminados: cards de posts, botón "Añadir nuevo post", modal de comentarios, FAB de nuevo post. |
| Buscar usuarios | El botón "Buscar usuarios" está en el **perfil del usuario propio**, no en el topbar del home. |

### 2.3 Elaboración (BrewLab)

| Cambio | Descripción |
|--------|-------------|
| Un solo paso de selección | Pantalla única: carousel de **métodos** + componente **"Selecciona café"** (rectángulo tipo temporizador con flecha). Sin paso intermedio "Elige tu café" como pantalla separada. |
| Sin pantalla Configuración | La pantalla "Configuración" se eliminó. Los parámetros (agua, ratio, tiempo, café) se editan en la misma pantalla de elaboración, debajo de "Selecciona café". |
| Flecha siguiente en TopBar | Flecha a la derecha en el topbar: si **temporizador ON** → va a "Proceso en curso"; si **temporizador OFF** → va directo a "¿Qué sabor has obtenido?" (Resultado). El botón se muestra siempre pero **inactivo** (gris) hasta que método y café estén elegidos. |
| Selecciona café | Un rectángulo (estilo temporizador) que abre la **misma modal de selección de café que Mi Diario** (no la de "añadir a despensa"). Al elegir café, se cierra la modal y el nombre del café aparece en el rectángulo a la izquierda de la flecha. |
| Tipo y Tamaño | Tras "Selecciona café" se añaden dos carruseles: **Tipo** (tipos de café, estilo círculos como método) y **Tamaños** (tamaños de taza, icono izquierda + label + rangeLabel). Opciones compartidas con el flujo "añadir café" de Mi Diario. |
| Método "Otros" | Si se elige "Otros": no se muestra "Configura tu XXX" ni "Temporizador". |
| Método "Agua" | Solo se muestra "Configura tu Agua" (cantidad de agua). Al pulsar siguiente se **guarda directamente en actividad como agua** y se navega a Mi Diario (sin pantalla de resultado). Icono gota girada 180° y en actividad con fill azul. |
| Configuración en pantalla | "Configura tu [método]" con parámetros editables (agua, ratio, tiempo, café) en la misma pantalla. Dos columnas en desktop (parámetros | consejos barista), apilado en móvil. |
| Temporizador | Switch "Temporizador" (por defecto OFF) debajo de la configuración. Misma línea visual que las cajas de configuración. |
| Persistencia y café | Método, tipo, agua, ratio, temporizador se **persisten en localStorage** (no el café). El **café seleccionado se borra** cada vez que se **acede** a la pestaña Elaboración (al entrar en la tab). No hay fallback al primer café del catálogo. |
| Guardar sin café | Se puede guardar elaboración **sin café seleccionado**: en actividad se muestra título "Café" con icono genérico. |
| preparation_type | Formato guardado: `Lab: Método (Sabor)|Tipo` (ej. `Lab: Aeropress (Dulce)|Espresso`). Para "Agua" se guarda tipo "water" en actividad. |

### 2.4 Mi Diario

| Cambio | Descripción |
|--------|-------------|
| Sin pestaña Despensa | La pestaña "Despensa" se eliminó. El contenido de "Actividad" aparece directamente tras la tarjeta de analytics, sin tabs. |
| Actividad sin tabs | Una sola vista: analytics + lista de actividad. |
| Sin botón "Más" | Eliminados el botón "más" del topbar y el modal "Nuevo registro" (agua, café, despensa). |
| Editar actividad: Método y Tipo | En el formulario de edición: sección **Método** (carousel de métodos de elaboración) **antes** de la sección **Tipo** (antes "Preparación"). Se guarda `preparation_type` como `Lab: Método (Sabor)|Tipo`. |
| Visualización en actividad | **MÉTODO** y **TIPO** se muestran por separado cuando aplica. Si método y preparación coinciden, no se duplica. Icono de **PREPARACIÓN** según método (ej. rayo para "Otros", imagen del método para el resto). Nuevo meta **RESULTADO** con el sabor (ej. "Dulce") e icono. |
| Tamaños y ortografía | "Pequeño" y "Tazón XL" con ñ y acento correctos en etiquetas. |
| Entrada agua | Icono gota de agua rotado 180° y relleno azul (#2196f3) en la lista de actividad. |

### 2.5 Perfil

| Cambio | Descripción |
|--------|-------------|
| Tab Posts → Actividad | La primera pestaña pasa de "Posts" a **Actividad**. Muestra lo que han hecho las personas que sigues: guardar café, review, tomar café/agua. |
| Stats | La primera estadística del header es **ACTIVIDAD** (cuenta de ítems de actividad). |
| Sin posts | Eliminados: cards de posts, menú de post, editar/eliminar post, like, comentarios. |
| Actividad como cards | Los ítems son **cards** con el mismo estilo que ADN/Favoritos (borde, sombra, radio). Sin flecha (chevron). El enlace al café dentro de la card sigue abriendo detalle. |
| Solo actividad de otros | En el perfil **propio**, la pestaña Actividad muestra solo actividad de **personas que sigues** (no la del usuario). Si no hay: "Comienza a seguir otras personas y descubre nuevos cafés". |
| Perfil ajeno | En perfil de **otro usuario**, Actividad muestra solo la **actividad de ese usuario** (reviews, etc.). Vacío: "Sin actividad reciente". |
| Buscar usuarios | Botón "Buscar usuarios" en el topbar del **perfil propio**. |

### 2.6 UI global

| Cambio | Descripción |
|--------|-------------|
| Cards unificadas | Bordes y sombras unificados con variables `--surface-card-border`, `--surface-card-shadow`, `--surface-card-radius` (12px). Aplicado a temporizador, configuración, cards de perfil (ADN, Favoritos, Actividad), etc. |
| Switch inactivo | Colores del switch en estado OFF visibles en fondos claros y oscuros. |
| Títulos | Títulos de sección en color de texto principal (no gris claro). |

### 2.7 SEO y técnico (solo Web)

| Cambio | Descripción |
|--------|-------------|
| Sitemap | Sitemap de cafés fragmentado en varios archivos con índice. |
| index.html | No indexar `index.html` en robots.txt. |
| Canonical | Dominio canónico `https://cafesitoapp.com`. |
| Meta robots | `index, follow` explícito en entrada SPA, legales, prerender de cafés y rutas públicas en `useCoffeeSeoMeta`; `noindex, nofollow` en el resto de pestañas SPA para evitar meta obsoleta. |

---

## 3. Traslado a Android (guía nativa)

### 3.1 Principios

- **Lógica en shared:** Todo lo que sea dominio (orden de métodos, formato `preparation_type`, perfiles de método, tamaños, tipos) debe vivir o reutilizarse desde `shared` (Kotlin Multiplatform).
- **UX nativa:** Mismos flujos y datos que la web, con componentes Material 3 y navegación Android (NavController, back).
- **Paridad:** Mismo comportamiento: qué se persiste, qué se borra al entrar en elaboración, qué se muestra en actividad/perfil.

### 3.2 Mapeo por área

#### Navegación

| Web | Android nativo |
|-----|----------------|
| Tab "home" | Mantener o renombrar ruta/destino a "home" si se usa "timeline" en código. Título "CAFESITO" en TopBar cuando la pantalla sea Home. |
| `/home` | Ruta `"home"` o equivalente en `NavHost`. |

#### Home

| Web | Android nativo |
|-----|----------------|
| Recomendaciones: 9 ítems, 3 cards de 3 | `LazyRow` o horizontal pager de 3 cards; cada card es una `Column` de 3 filas (imagen + nombre/marca). `lineClamp = 2` para nombre. |
| Métodos con Otros y Agua, orden por uso | En `shared` o en app: lista de métodos que incluya "Otros" (icono bolt) y "Agua" (icono drop). Orden: `getOrderedBrewMethods(diaryEntries)` (más reciente primero, luego alfabético, Otros/Agua al final). |
| Click despensa → detalle café | En el carousel de despensa, `onClick` en la card → `onCoffeeClick(coffee.id)` (navegar a detalle), no abrir BrewLab. |
| Buscar usuarios en perfil | Botón solo en `ProfileScreen` cuando es el perfil del usuario actual. |

#### Elaboración (BrewLab)

| Web | Android nativo |
|-----|----------------|
| Un solo paso: método + selecciona café + tipo + tamaño + config + temporizador | Una sola pantalla con `LazyColumn` o `Column`: carousel métodos, rectángulo "Selecciona café", carousel Tipo, carousel Tamaño, bloque "Configura tu [método]", fila Temporizador. Ocultar config y temporizador si método = "Otros". Solo agua si método = "Agua". |
| Sin pantalla Configuración | Eliminar `BrewStep.CONFIGURATION`; la configuración es un bloque dentro del paso único (equivalente a `BrewStep.CHOOSE_METHOD` ampliado o un nuevo `BrewStep.METHOD` que contenga todo). |
| Siguiente: temporizador ON → Proceso en curso; OFF → Resultado | TopBar: una flecha "siguiente". Si `brewTimerEnabled`: navegar a pantalla de proceso en curso (timer). Si no: navegar a pantalla "¿Qué sabor has obtenido?" (resultado). Habilitar flecha solo cuando `selectedMethod != null && selectedCoffee != null` (o para Agua solo método y agua configurada). |
| Selecciona café abre sheet de selección (como Mi Diario) | Un `ModalBottomSheet` o pantalla modal con la misma lista de cafés que en "añadir café" del diario. Al seleccionar, cerrar y actualizar `selectedCoffee`; mostrar nombre en el rectángulo. |
| Tipo y Tamaño | Listas fijas desde `shared` o `diaryBrewOptions`: tipos (Espresso, etc.) y tamaños (label + rangeLabel). UI: carruseles horizontales; Tipo como círculos con texto; Tamaño como fila (icono + 2 líneas). |
| Método Agua: solo agua, guardar y ir a Diario | Si método = "Agua": mostrar solo input agua; al pulsar siguiente llamar `saveWaterWithAmount(amountMl)` y navegar a Diario. |
| Borrar café al entrar en elaboración | Al navegar a BrewLab (o al `onResume`/`LaunchedEffect(Unit)` de la pantalla), ejecutar `setBrewCoffeeId("")` / `clearSelectedCoffee()`. No persistir café en `SharedPreferences`. |
| Persistir método, tipo, agua, ratio, temporizador | Guardar en `DataStore` o `SharedPreferences` y rehidratar al abrir la app o la pantalla. No guardar `selectedCoffeeId`. |
| Guardar sin café | `saveBrewToDiary` debe aceptar `coffeeId == null`, `coffeeName = "Café"` y guardar en backend. En actividad, mostrar "Café" con icono genérico. |
| preparation_type | Formato `Lab: Método (Sabor)|Tipo` (ej. `Lab: Aeropress (Dulce)|Espresso`). Incluir `drinkType` (Tipo) al guardar. |

#### Mi Diario

| Web | Android nativo |
|-----|----------------|
| Sin pestaña Despensa | Si existe tab Despensa, quitarla. Una sola vista: analytics + lista de actividad. |
| Sin botón "Más" | Quitar FAB o botón "más" del topbar de Diario y el diálogo/sheet de "Nuevo registro". |
| Editar: Método + Tipo | En `EditDiaryEntry` o equivalente: carousel de métodos antes del carousel de tipo (preparación). Guardar `preparationType` como `Lab: Método (Sabor)|Tipo`. |
| Actividad: MÉTODO, TIPO, RESULTADO | Parsear `preparation_type`: extraer método (regex `Lab: X (Y)`), tipo (parte tras `|`), sabor (entre paréntesis). Mostrar MÉTODO solo si hay método; TIPO con su icono; RESULTADO con sabor e icono. |
| Icono preparación por método | "Otros" → icono bolt; "Agua" → gota rotada y azul; resto → drawable del método. |
| Pequeño / Tazón XL | En `BrewEngine.cupSizeLabelForAmountMl` y etiquetas: "Pequeño", "Tazón XL" (ñ y acento). |

#### Perfil

| Web | Android nativo |
|-----|----------------|
| Tab Actividad en lugar de Posts | Sustituir contenido de la primera tab por lista de actividad (reviews, cafés guardados, café/agua del día de los seguidos). Estadística "ACTIVIDAD" con count. |
| Cards sin chevron | Items de actividad como `Card` con mismo estilo que ADN/Favoritos. Sin `IconButton` de flecha. |
| Solo otros (perfil propio) / Solo ese usuario (perfil ajeno) | Datos: perfil propio → `profileFollowedActivity`; perfil ajeno → `profileUserActivity`. Mensajes vacío distintos. |
| Buscar usuarios en perfil propio | `IconButton` o `Button` en TopBar de `ProfileScreen` cuando `userId == currentUserId`. |

#### Shared (KMP)

| Cambio | Android (shared) |
|--------|-----------------|
| Orden de métodos por uso | Función `getOrderedBrewMethods(diaryEntries: List<DiaryEntryDto>): List<BrewMethodItem>` en `shared`. Incluir "Otros" y "Agua" en la lista. |
| Perfil "Agua" | En `BrewEngine` (o equivalente), `methodProfileFor("agua")` y `timeProfileFor("agua")` si se usan; para Agua solo se usa cantidad de agua. |
| Tamaños y tipos | Constantes o datos `COFFEE_TIPO_OPTIONS`, `COFFEE_SIZE_OPTIONS` (o equivalente) compartidos entre Diario y BrewLab. |
| cupSizeLabelForAmountMl | Corregir "Pequeno" → "Pequeño", "Tazon XL" → "Tazón XL". |

### 3.3 Archivos Android a tocar (resumen)

- **shared:** `BrewEngine.kt` (cupSize, perfiles agua si aplica), nuevo módulo o archivo para `getOrderedBrewMethods` y lista de métodos con Otros/Agua.
- **app:** `BrewLabViewModel.kt`, `BrewLabScreen.kt` (flujo único, sin paso Config, resultado/timer según switch, limpiar café al entrar, guardar sin café, preparation_type con Tipo).
- **app:** `DiaryViewModel.kt`, `DiaryScreen.kt` (quitar pestaña Despensa y botón Más si existen; actividad sin tabs).
- **app:** `DiaryComponents.kt` / componentes de actividad (parsear preparation_type, mostrar MÉTODO/TIPO/RESULTADO, icono agua azul).
- **app:** Pantalla o sheet de edición de entrada (Método + Tipo, guardar formato `Lab: X (Y)|Tipo`).
- **app:** `ProfileScreen.kt`, `ProfileViewModel.kt` (tab Actividad, datos seguidos/propio usuario, cards, buscar usuarios).
- **app:** `TimelineScreen.kt` / Home (recomendaciones en 3 cards de 3, métodos ordenados con Otros/Agua, click despensa → detalle).
- **app:** Navegación: ruta home, título CAFESITO.

---

## 4. Orden sugerido de implementación

1. **Shared:** `cupSizeLabelForAmountMl` (Pequeño, Tazón XL), perfiles "agua" y "otros" si faltan, `getOrderedBrewMethods` y lista de métodos con Otros/Agua.
2. **BrewLab:** Eliminar paso CONFIGURATION; unificar en un solo paso con bloques (método, selecciona café, tipo, tamaño, config, temporizador). Flecha siguiente → proceso o resultado según temporizador. Limpiar café al entrar; persistir el resto; guardar con `Lab: X (Y)|Tipo` y permitir café null.
3. **Diario:** Quitar tab Despensa y botón Más; edición con Método + Tipo; visualización MÉTODO/TIPO/RESULTADO e icono agua.
4. **Perfil:** Tab Actividad con datos correctos, cards, buscar usuarios en perfil propio.
5. **Home:** Recomendaciones 3x3, métodos ordenados, click despensa → detalle, título CAFESITO.

---

## 5. Estado del port Android (implementado en este ciclo)

- **Shared (KMP):**
  - `BrewEngine.kt`: corrección "Pequeño" y "Tazón XL" en `cupSizeLabelForAmountMl`; perfiles `methodProfileFor` y `timeProfileFor` para método "agua".
  - `BrewConfig.kt` (nuevo): `BREW_METHOD_NAMES`, `BREW_METHOD_OTROS`, `BREW_METHOD_AGUA`, `getOrderedBrewMethods(diaryEntries)` para orden por uso reciente.
- **BrewLab (app):**
  - Paso **RESULT** (pantalla "¿Qué sabor has obtenido?" + Guardar cuando no se usa temporizador).
  - Métodos **Otros** (icono rayo) y **Agua** (icono gota) en el carrusel; orden por uso reciente desde diario.
  - **clearSelectedCoffeeOnEnter()**: se llama al entrar en la pantalla Elaboración (café no persistido).
  - **Guardar sin café**: `saveToDiary` con `coffeeId == null`, `coffeeName = "Café"`; `preparationType` con formato `Lab: Método (Sabor)|Tipo` (incluye `drinkType`).
  - **Método Agua**: paso Configuración solo agua; botón siguiente guarda entrada tipo WATER y navega a Mi Diario; `DiaryRepository.TYPE_WATER`.
  - **Siguiente desde Configuración**: si temporizador activo → Proceso en curso; si no → Resultado. `goToNextFromConfig()`.
  - **ResultStep** en UI; TopBar con siguiente/Guardar según paso y método.
- **Diario:** Implementado (sin tab Despensa, sin botón Más; edición con Método + Tipo; visualización MÉTODO/TIPO/RESULTADO e icono agua).
- **Perfil:** Implementado (tab Actividad con reviews, buscar usuarios en perfil propio).
- **Home (TimelineScreen):** Implementado: **Recomendaciones del día** (carousel 3x3), **Métodos de elaboración** (carousel horizontal ordenado por uso reciente, con Otros/Agua; tap → navega a BrewLab), **Tu despensa** (carousel; tap en café → detalle). Sin posts, sin FAB de nuevo post, sin botón Buscar en topbar (buscar usuarios solo en Perfil).

---

## 6. Referencias

- Web: `webApp/src/features/brew/BrewViews.tsx`, `webApp/src/features/diary/DiaryView.tsx`, `webApp/src/features/profile/ProfileView.tsx`, `webApp/src/features/timeline/TimelineView.tsx`, `webApp/src/app/AppContainer.tsx`, `webApp/src/config/brew.ts`, `webApp/src/hooks/domains/useDiaryActions.ts`.
- Android: `app/.../brewlab/`, `app/.../diary/`, `app/.../profile/`, `app/.../timeline/`, `shared/.../brew/`.
- Documento maestro: `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`.
- Tokens de diseño: `docs/DESIGN_TOKENS.md`.
- Estado del port: sección 5 de este documento.
