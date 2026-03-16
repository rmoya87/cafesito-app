# Guía de componentes UI — Inventario y uso

**Propósito:** Catálogo de todos los componentes de interfaz (WebApp y Android) definidos por **estilos**, **funcionalidad**, **dónde se usan** y **estado actual**, para reutilizarlos o evolucionarlos en nuevas pantallas y eliminar los que no se usen.

**Última actualización:** 2026-03-13  
**Ámbito:** WebApp (`webApp/src/`), Android (`app/.../ui/components/`).  
**Documentos relacionados:** `DESIGN_TOKENS.md`, `GUIA_UNIFICACION_COMPONENTES_UI.md`, `UX_EMPTY_AND_ERROR_STATES.md`, `ACCESIBILIDAD_WEBAPP_ANDROID.md`.

---

## Cómo usar esta guía

```mermaid
flowchart LR
  A[Nueva pantalla o flujo] --> B[Buscar en guía: mismo dato/jerarquía?]
  B -->|Sí| C[Reutilizar componente]
  B -->|Casi igual| D[Evolucionar: props/variantes]
  B -->|No existe| E[Crear nuevo + documentar aquí]
  F[Componente sin usos en "Dónde se usa"] --> G[Valorar eliminación]
  G --> H[Eliminar o reasignar + actualizar guía]
  I[Cambio de estilos/contrato] --> J[Actualizar este doc + DESIGN_TOKENS si aplica]
```

| Situación | Acción |
|-----------|--------|
| **Nueva pantalla o flujo** | Buscar en esta guía un componente que ya cubra el mismo dato/jerarquía visual → **reutilizarlo** (mismas props/clases). |
| **Componente casi igual** | **Evolucionar** el existente (props opcionales, variantes) en lugar de crear uno nuevo. |
| **Componente no usado en "Dónde se usa"** | Valorar **eliminación** o reasignación a un flujo concreto; si se confirma que no se usa, eliminar y actualizar esta guía. |
| **Cambio de estilos/contrato** | Actualizar este documento y, si afecta tokens, `DESIGN_TOKENS.md`. |

---

# Parte A — WebApp

**Ruta base de componentes:** `webApp/src/ui/components/`.  
**Estilos:** `webApp/src/styles/` (`tokens.css`, `components/`, `features.css`).  
**Export:** `webApp/src/ui/components/index.ts`.

---

## A.1 Componentes React (uno a uno)

### Button

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/Button.tsx` |
| **Estilos** | Clases: `action-button`, `action-button-ghost`, `text-button`, `chip-button`, `is-danger`; tamaños: `ui-button-sm`, `ui-button-md`, `ui-button-lg`. CSS en `components/buttons.css`. Tokens: usar `var(--space-*)`, min tap 44px. |
| **Funcionalidad** | Botón genérico. Variantes: `primary`, `ghost`, `text`, `chip`, `danger`, `plain`. Tamaños: `sm`, `md`, `lg`. Props estándar HTML button. |
| **Dónde se usa** | HistorialView (volver), CoffeeDetailView (múltiples: cerrar, añadir lista, compartir, opiniones, etc.), TimelineView (enlaces usuario, acciones), AppContainer (cancelar eliminar lista), MobileBarcodeScannerSheet (cerrar), NotificationsSheet (header). |
| **Estado actual** | En uso. Estándar para acciones primarias y secundarias; alinear nuevos botones a estas variantes. |

---

### IconButton

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/IconButton.tsx` |
| **Estilos** | Clases: `icon-button`, `topbar-icon-button`, `chip-button`. Área de tap ≥ 44px (WCAG). `components/buttons.css`, `features.css` (detalle café: `.coffee-detail-topbar-icon`). |
| **Funcionalidad** | Botón solo icono. Tono: `topbar`, `menu`, etc. Aria-label obligatorio para accesibilidad. |
| **Dónde se usa** | TopBar (volver, buscar usuarios, escanear, filtros chips, opciones perfil, notificaciones, favorito, stock), CoffeeDetailView (cerrar, favorito, stock), NotificationsSheet (espaciador), TimelineView (acciones). |
| **Estado actual** | En uso. Referencia para cualquier botón de icono en topbar o listas. |

---

### Chip

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/Chip.tsx` |
| **Estilos** | Estilo chip/filtro; puede incluir `.filter-chip-count` para badge. Tokens para bordes y fondo. |
| **Funcionalidad** | Chip seleccionable/filtro. Props: `active`, `onClick`; contenido hijos (ej. texto + contador). |
| **Dónde se usa** | TopBar (filtros búsqueda: PAIS, ESPECIALIDAD, TUESTE, FORMATO, NOTA). |
| **Estado actual** | En uso. Reutilizar para cualquier filtro tipo chip en barras de búsqueda o filtros. |

---

### Input

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/Input.tsx` |
| **Estilos** | `components/inputs.css`; tokens `var(--space-*)`, `--radius-*`. |
| **Funcionalidad** | Campo de texto estándar (controlado/no controlado). |
| **Dónde se usa** | ProfileView (edición perfil, hojas con Input/Textarea). |
| **Estado actual** | En uso. Usar para formularios; no duplicar con `<input>` crudo. |

---

### Select

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/Select.tsx` |
| **Estilos** | Alineado con Input; tokens en inputs.css. |
| **Funcionalidad** | Desplegable de opciones. |
| **Dónde se usa** | Donde se necesite un select en formularios (verificar en features: profile, diary, brew). |
| **Estado actual** | Exportado; confirmar usos en código y reutilizar en nuevos formularios. |

---

### Textarea

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/Textarea.tsx` |
| **Estilos** | Mismo sistema que Input. |
| **Funcionalidad** | Área de texto multilínea. |
| **Dónde se usa** | ComposerInputShell (opiniones, comentarios); ProfileView. |
| **Estado actual** | En uso. Reutilizar para cualquier texto largo (opinión, bio, etc.). |

---

### Switch

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/Switch.tsx` |
| **Estilos** | Tokens: `--ui-switch-track-*`, `--ui-switch-border-off` en `tokens.css`. |
| **Funcionalidad** | Toggle on/off. |
| **Dónde se usa** | Donde se necesite un switch (preferencias, Brew Lab, etc.). |
| **Estado actual** | Exportado; reutilizar para opciones booleanas. |

---

### Sheet (SheetOverlay, SheetCard, SheetHandle, SheetHeader)

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/Sheet.tsx` |
| **Estilos** | Overlay + card con handle; clases por contexto (`.coffee-detail-sheet`, `.diary-sheet`, `.notifications-panel`, `.barcode-scanner-sheet`). Tokens para superficie y sombra. |
| **Funcionalidad** | Modal tipo bottom sheet. `SheetOverlay` (backdrop + onDismiss), `SheetCard` (contenido), `SheetHandle`, `SheetHeader`. |
| **Dónde se usa** | CoffeeDetailView (perfil sensorial, compartir, opiniones, crear lista), AppContainer (opciones lista, editar lista, eliminar lista), TopBar (opciones perfil, eliminar cuenta), TimelineView (opciones despensa), CreateListSheet, EditListSheet, NotificationsSheet, MobileBarcodeScannerSheet. |
| **Estado actual** | Muy usado. Patrón estándar para modales y paneles deslizantes; no crear variantes ad hoc. |

---

### Tabs / TabButton

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/Tabs.tsx` |
| **Estilos** | Contenedor `.profile-tabs`; TabButton con estado `active`. Tokens para espaciado y borde. |
| **Funcionalidad** | Pestañas; `TabButton`: `active`, `role="tab"`, `aria-selected`, `onClick`. |
| **Dónde se usa** | ProfileView (Actividad, ADN, Listas). |
| **Estado actual** | En uso. Reutilizar para cualquier navegación por pestañas (máx. 4–5 tabs). |

---

### Topbar

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/Topbar.tsx` |
| **Estilos** | Barra superior; clases `topbar-timeline`, `notifications-header`; altura y padding con tokens. Iconos vía IconButton con tone `topbar`. |
| **Funcionalidad** | Barra de título con slot izquierdo (volver), centro (título/chips), derecho (iconos). |
| **Dónde se usa** | TopBar.tsx (contenedor principal de la barra según tab), NotificationsSheet (header). |
| **Estado actual** | En uso. Componente estructural de todas las pantallas con barra superior. |

---

### ComposerInputShell

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/ComposerInputShell.tsx` |
| **Estilos** | Campo de texto + botón de envío; hereda Textarea + Button. |
| **Funcionalidad** | Composición: texto + acción (ej. "Publicar opinión"). Usa Button y Textarea internamente. |
| **Dónde se usa** | CoffeeDetailView (añadir opinión, comentarios). |
| **Estado actual** | En uso. Reutilizar para cualquier "composer" (opinión, comentario, mensaje). |

---

### EmptyState

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/EmptyState.tsx` |
| **Estilos** | Clases: `timeline-empty`, `ui-empty-state`. Mensaje + subtítulo + CTA opcional. Ver `UX_EMPTY_AND_ERROR_STATES.md`. |
| **Funcionalidad** | Estado vacío unificado: `title`, `subtitle?`, `ctaText?`, `onCtaClick?`. Botón primario si hay CTA. |
| **Dónde se usa** | TimelineView (timeline vacío). |
| **Estado actual** | En uso. Debe usarse en todas las pantallas con lista vacía (diario, favoritos, búsqueda, etc.) en lugar de bloques ad hoc. |

---

### ErrorState

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/components/ErrorState.tsx` |
| **Estilos** | `timeline-empty`, `timeline-error`, `ui-error-state`. Mensaje + detalle opcional + botón "Reintentar". |
| **Funcionalidad** | Estado de error unificado: `message?`, `detail?`, `onRetry`. Usa strings de `core/emptyErrorStrings`. |
| **Dónde se usa** | TimelineView (error de carga). |
| **Estado actual** | En uso. Debe usarse en todas las pantallas con datos remotos que puedan fallar (timeline, perfil, búsqueda, detalle). |

---

## A.2 Otros elementos UI (no en components/)

### MentionText

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ui/MentionText.tsx` |
| **Estilos** | Texto con menciones @usuario enlazadas (color/estilo enlace). |
| **Funcionalidad** | Parsea texto y convierte @usuario en enlaces/buttons. |
| **Dónde se usa** | DiaryView (entradas con menciones). |
| **Estado actual** | En uso. Reutilizar en timeline, comentarios o cualquier texto con @. |

---

### Tarjetas (clases CSS)

| Clase | Estilos | Dónde se usa | Estado |
|-------|---------|--------------|--------|
| `.card` | Base: surface, borde, radio, sombra (`--surface-card-*`). Padding con `--space-*`. | Base para: profile-activity-card, diary-empty-card, diary-analytics-card, diary-stats-card. | Estándar; heredar en nuevas tarjetas. |
| `.coffee-card` | Igual que card; variante para listas de café. | coffee-detail-opinion-card, search-coffee-view, coffee-list, cafes-probados-coffee-list, favoritos. | Estándar para ítems de café. |
| `.config-card` | Card de configuración (brew, etc.). | Features de elaboración/config. | Reutilizar en flujos de configuración. |

---

# Parte B — Android

**Ruta base:** `app/src/main/java/com/cafesito/app/ui/components/`.  
**Tema:** `app/.../ui/theme/` (Color.kt, Spacing.kt, Shapes.kt, Dimens.kt).  
**Principio:** Composable reutilizable por contrato (parámetros); estilos vía `MaterialTheme`, `Shapes`, `Spacing`, `Dimens`.

---

## B.1 Componentes base (CafesitoUI.kt)

### PremiumCard

| Atributo | Valor |
|----------|--------|
| **Ruta** | `CafesitoUI.kt` |
| **Estilos** | `Surface` con `Shapes.shapePremium` (32.dp), `MaterialTheme.colorScheme.surface`, borde 1.dp outline. Parámetros: `modifier`, `shape`, `containerColor`, `content`. |
| **Funcionalidad** | Contenedor tipo card estándar para bloques de contenido (listas, perfiles, Brew Lab, detalle). |
| **Dónde se usa** | ProfileScreen (actividad, ADN con SensoryRadarChart), ProfileComponents (actividad, favoritos, listas, opciones), BrewLabCards (método, tamaño), BrewLabComponents (pasos, opciones, temporizador, resultados), TimelineScreen (RecommendationCarousel), TimelineComponents (tarjeta timeline), DetailScreen (BuyPremiumCard), DiaryScreen (CaffeinePremiumCard), AddDiaryEntryScreen (bloques), SearchScreen (PremiumCard en opciones). |
| **Estado actual** | Muy usado. No crear cards alternativos; evolucionar con `shape`/`containerColor` si hace falta. |

---

## B.2 Listas y tarjetas de contenido

### CoffeeListItem

| Atributo | Valor |
|----------|--------|
| **Ruta** | `CoffeeListItem.kt` |
| **Estilos** | `Shapes.card`, `Spacing.space3` padding, `MaterialTheme.colorScheme.surface`. Imagen con `imageSize` (48.dp o 60.dp), altura fila 72/86.dp. |
| **Funcionalidad** | Fila de café para listas: `coffee`, `subtitle?`, `secondLine?`, `imageSize`, `showChevron`, `onClick`. Imagen optimizada (Coil). |
| **Dónde se usa** | SearchScreen (resultados búsqueda), CafesProbadosScreen (listado por país). |
| **Estado actual** | En uso. Componente unificado para cualquier lista de cafés (buscador, probados, favoritos); no duplicar. |

---

### UserSuggestionCard

| Atributo | Valor |
|----------|--------|
| **Ruta** | `UserSuggestionCard.kt` |
| **Estilos** | Surface/card con avatar, nombre, botón seguir. |
| **Funcionalidad** | Tarjeta de sugerencia de usuario a seguir. |
| **Dónde se usa** | UserSuggestionCard.kt (uso interno en lista de sugerencias). TimelineScreen o búsqueda de usuarios. |
| **Estado actual** | En uso. Reutilizar para "sugerir usuario" en timeline o búsqueda. |

---

### UserReviewCard

| Atributo | Valor |
|----------|--------|
| **Ruta** | `UserReviewCard.kt` |
| **Estilos** | Card para mostrar reseña de usuario (nombre, nota, texto). |
| **Funcionalidad** | Muestra una reseña individual. |
| **Dónde se usa** | Donde se listen reseñas (DetailScreen, perfil, etc.). |
| **Estado actual** | En uso. Reutilizar para listas de opiniones/reseñas. |

---

## B.3 Estados vacío y error

### EmptyStateMessage

| Atributo | Valor |
|----------|--------|
| **Ruta** | `DiaryComponents.kt` |
| **Estilos** | Texto centrado; tipografía MaterialTheme. |
| **Funcionalidad** | `EmptyStateMessage(message: String)`. Solo mensaje, sin CTA. |
| **Dónde se usa** | DiaryScreen ("Sin café o agua registrada"). |
| **Estado actual** | En uso. Para listas simples; si se necesita CTA, usar patrón con Button como en TimelineEmptyState. |

---

### ErrorStateMessage

| Atributo | Valor |
|----------|--------|
| **Ruta** | `DiaryComponents.kt` |
| **Estilos** | Mensaje + botón "Reintentar". |
| **Funcionalidad** | `ErrorStateMessage(message: String, onRetry: () -> Unit)`. |
| **Dónde se usa** | SearchScreen, TimelineScreen, ProfileScreen, DetailScreen. |
| **Estado actual** | En uso. Estándar para fallos de carga; no crear variantes. |

---

### TimelineEmptyState

| Atributo | Valor |
|----------|--------|
| **Ruta** | `TimelineEmptyState.kt` |
| **Estilos** | Mensaje + CTA + sugerencias (TagChip). |
| **Funcionalidad** | Estado vacío enriquecido para timeline (mensaje, botón, temas sugeridos). |
| **Dónde se usa** | TimelineScreen (timeline vacío). |
| **Estado actual** | En uso. Reutilizar patrón (mensaje + CTA + chips) si se añaden más pantallas con sugerencias. |

---

## B.4 Chips y controles

### TagChip

| Atributo | Valor |
|----------|--------|
| **Ruta** | `TagChip.kt` |
| **Estilos** | Surface con `Shapes.cardSmall`, borde; label + value. |
| **Funcionalidad** | `TagChip(label: String, value: String)`. Chip de solo lectura (tema, categoría). |
| **Dónde se usa** | TimelineEmptyState (sugerencias de tema). |
| **Estado actual** | En uso. Reutilizar para etiquetas no interactivas. |

---

### SemicircleRatingBar

| Atributo | Valor |
|----------|--------|
| **Ruta** | `SemicircleRatingBar.kt` |
| **Estilos** | Barra semicircular para nota 0–5; colores tema y acento. |
| **Funcionalidad** | `SemicircleRatingBar(rating, onRatingChanged)`. Edición de valoración. |
| **Dónde se usa** | DetailScreen (editar nota de opinión). |
| **Estado actual** | En uso. Reutilizar para cualquier selector de valoración. |

---

### SensoryRadarChart

| Atributo | Valor |
|----------|--------|
| **Ruta** | `SensoryRadarChart.kt` |
| **Estilos** | Gráfico radar; ejes y rejilla con `RadarGridDark`, `LocalCaramelAccent`. |
| **Funcionalidad** | Muestra perfil sensorial (datos 0–10 por eje). |
| **Dónde se usa** | ProfileScreen (ADN), ProfileComponents (actividad ADN). |
| **Estado actual** | En uso. No duplicar; evolucionar con parámetros si se necesita en más pantallas. |

---

## B.5 Carruseles y bloques de feature

### RecommendationCarousel

| Atributo | Valor |
|----------|--------|
| **Ruta** | `RecommendationCarousel.kt` |
| **Estilos** | LazyRow de ítems (cafés o entradas); Shapes y Spacing. |
| **Funcionalidad** | Carrusel horizontal de recomendaciones. |
| **Dónde se usa** | TimelineScreen. |
| **Estado actual** | En uso. Reutilizar para cualquier carrusel horizontal (recomendaciones, recientes). |

---

### BrewLabCards / BrewLabComponents

| Atributo | Valor |
|----------|--------|
| **Ruta** | `BrewLabCards.kt`, `BrewLabComponents.kt` |
| **Estilos** | PremiumCard, Shapes, Spacing, Color.kt (PureBlack, PureWhite, switch/track). |
| **Funcionalidad** | Cards y pasos de elaboración (método, tamaño, configuración, temporizador, resultados). |
| **Dónde se usa** | Brew Lab (selección método, tamaño, pasos, timer, gusto). |
| **Estado actual** | En uso. Específicos de Brew Lab; no reutilizar fuera del flujo salvo componentes genéricos (PremiumCard, sliders). |

---

### DiaryComponents

| Atributo | Valor |
|----------|--------|
| **Ruta** | `DiaryComponents.kt` |
| **Estilos** | Cards, gráficos (eje DateMetaAxis*), EmptyStateMessage, ErrorStateMessage, quick actions. |
| **Funcionalidad** | Bloques del diario: entradas, analytics, gráfica, estados vacío/error. |
| **Dónde se usa** | DiaryScreen, AddDiaryEntryScreen y pantallas de diario. |
| **Estado actual** | En uso. Centraliza diario; nuevos bloques de diario añadir aquí. |

---

### ProfileComponents

| Atributo | Valor |
|----------|--------|
| **Ruta** | `ProfileComponents.kt` |
| **Estilos** | PremiumCard, SensoryRadarChart, actividad (nota, favoritos, listas). |
| **Funcionalidad** | Actividad de perfil, ADN, listas, opciones de perfil (añadir lista, etc.). |
| **Dónde se usa** | ProfileScreen. |
| **Estado actual** | En uso. Evolucionar con props si se reutiliza en otras pantallas (ej. actividad de otro usuario). |

---

### TimelineComponents

| Atributo | Valor |
|----------|--------|
| **Ruta** | `TimelineComponents.kt` |
| **Estilos** | PremiumCard para post, sliders (SliderTrackInactive*), chips, campos. |
| **Funcionalidad** | Post de timeline, acciones, comentarios, opciones despensa. |
| **Dónde se usa** | TimelineScreen. |
| **Estado actual** | En uso. Referencia para cualquier "post" o ítem de feed. |

---

## B.6 Utilidades y otros

### PhotoPicker

| Atributo | Valor |
|----------|--------|
| **Ruta** | `PhotoPicker.kt` |
| **Estilos** | Integrado con sistema de temas. |
| **Funcionalidad** | Selección de foto (galería/cámara). |
| **Dónde se usa** | Donde se sube imagen (perfil, café, etc.). |
| **Estado actual** | En uso. Reutilizar para cualquier picker de imagen. |

---

### RatingBar

| Atributo | Valor |
|----------|--------|
| **Ruta** | `RatingBar.kt` |
| **Estilos** | Barra o estrellas de valoración (solo lectura posible). |
| **Funcionalidad** | Mostrar valoración. |
| **Dónde se usa** | Verificar en DetailScreen, listas, cards. |
| **Estado actual** | Confirmar usos; si solo hay SemicircleRatingBar para edición, RatingBar puede ser solo lectura. |

---

### TextFormatters, SvgIcons

| Atributo | Valor |
|----------|--------|
| **Ruta** | `TextFormatters.kt`, `SvgIcons.kt` |
| **Estilos** | N/A (utilidades). |
| **Funcionalidad** | Formateo de texto; iconos SVG. |
| **Dónde se usa** | Varios composables. |
| **Estado actual** | En uso. Mantener como utilidades compartidas. |

---

# Parte C — Resumen y acciones

## C.1 Componentes no usados (candidatos a eliminar)

Ninguno en este momento. *CoffeeCard (Android) fue eliminado el 2026-03-14 por no estar referenciado en ninguna pantalla; las listas usan `CoffeeListItem`.*

## C.2 Componentes a reutilizar en nuevas pantallas

- **WebApp:** EmptyState, ErrorState (todas las listas/datos remotos); Sheet para modales; Button/IconButton/Chip según patrón existente; `.card`/`.coffee-card` para tarjetas.
- **Android:** PremiumCard para cualquier bloque tipo card; CoffeeListItem para listas de café; ErrorStateMessage/EmptyStateMessage para listas y fallos; TagChip para etiquetas; SemicircleRatingBar para valoraciones editables.

## C.3 Evolución sin duplicar

- Añadir **props opcionales** o **variantes** al componente existente antes de crear uno nuevo.
- Si se crea un componente nuevo reutilizable: añadirlo a esta guía (Parte A o B), indicar estilos, funcionalidad, dónde se usa y estado.
- Si se elimina un componente: marcarlo aquí y actualizar imports en el proyecto.

---

## C.4 Referencias cruzadas

| Documento | Uso |
|------------|-----|
| `DESIGN_TOKENS.md` | Colores, espaciado, radios; al cambiar estilos de un componente, comprobar tokens. |
| `GUIA_UNIFICACION_COMPONENTES_UI.md` | Unificación de estilos entre pantallas; checklist de tareas de estandarización. |
| `UX_EMPTY_AND_ERROR_STATES.md` | Patrón Empty/Error; textos por contexto. |
| `ACCESIBILIDAD_WEBAPP_ANDROID.md` | Área de tap, labels, contraste al añadir o modificar componentes. |
