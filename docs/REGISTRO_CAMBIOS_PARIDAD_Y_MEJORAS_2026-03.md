# Registro de cambios: paridad Web/Android y mejoras (marzo 2026)

**Propósito:** Documentar todos los cambios, eliminaciones y correcciones realizados en local para paridad entre WebApp y Android y para futuras modificaciones y mejoras.  
**Última actualización:** 2026-03-12  
**Ámbito:** Android (Kotlin/Compose), WebApp (React/TypeScript), Supabase (SQL/RLS).

---

## Índice

1. [Actividad de perfil (homogeneización)](#1-actividad-de-perfil-homogeneización)
2. [ADN / Gráfica radar](#2-adn--gráfica-radar)
3. [Despensa (pantry)](#3-despensa-pantry)
4. [Listas e iconos](#4-listas-e-iconos)
5. [Mi Diario (gráfica y filtros)](#5-mi-diario-gráfica-y-filtros)
6. [Elaboración (BrewLab) WebApp](#6-elaboración-brewlab-webapp)
7. [Auth y rendimiento](#7-auth-y-rendimiento)
8. [Supabase (esquema y RLS)](#8-supabase-esquema-y-rls)
9. [Resumen por archivo](#9-resumen-por-archivo)

---

## 1. Actividad de perfil (homogeneización)

### 1.1 Objetivo

Que la pestaña **Actividad** del perfil muestre los mismos tipos de ítem, mensajes y tiempos relativos en WebApp y Android.

### 1.2 Cambios Android

| Tema | Cambio | Archivos |
|------|--------|----------|
| **Nombres de café** | Si un café no está en la caché `allCoffees`, se resuelve con `CoffeeRepository.getCoffeeById(id)` (Supabase) para no mostrar "Café" genérico. | `CoffeeRepository.kt` (nuevo `getCoffeeById`), `ProfileViewModel.kt` (`resolveCoffeeName`, `listItemsForUser`, `firstTimeFromDiary` suspend) |
| **Perfiles sensoriales en ADN** | Inclusión de `coffee_sensory_profiles` del usuario en el cálculo del radar (véase [ADN](#2-adn--gráfica-radar)). | `SupabaseDataSource.kt` (`getSensoryProfilesByUserId`), `ProfileViewModel.kt` |
| **Tiempo relativo** | Formato igual que web: "X MIN", "X H", "X D". | `ProfileComponents.kt` (`formatRelativeTime`), cards de actividad |
| **Orden** | Actividad ya ordenada por timestamp descendente; se mantiene. | `ProfileViewModel.kt` |
| **Card opinión** | Estilo web: avatar del autor, línea de label ("Opinaste sobre un café" / "X opinó sobre un café"), nombre del café, valoración y comentario, tiempo relativo. | `ProfileComponents.kt` (`ProfileActivityReviewCard` con `isCurrentUser`) |
| **Card "primera vez"** | Texto "probó por primera vez" + nombre café + tiempo relativo. | `ProfileComponents.kt` |
| **Card "añadido a lista"** | "añadió un café a la lista" + nombre café + "Ver lista: {nombreLista}" + tiempo relativo. | `ProfileComponents.kt` |
| **Actividad de seguidos** | En mi perfil se muestra también la actividad de usuarios que sigo; se usa `getFollowingIdsForUser` y carga de usuario por ID si falta en el mapa. | `UserRepository.kt` (`getFollowingIdsForUser`), `ProfileViewModel.kt` (`refreshProfileActivity`) |
| **Combine de flujos** | Se añadió `profileUserSensoryProfilesFlow`; al no existir `combine` de 6 flows en stdlib, se anidó con `ProfileCombineState`. | `ProfileViewModel.kt` |

### 1.3 Cambios WebApp

| Tema | Cambio | Archivos |
|------|--------|----------|
| **Estado vacío (mi perfil)** | Texto: "Comienza a seguir **a** otras personas y descubre nuevos cafés". | `ProfileView.tsx` |
| **Card "primera vez"** | Label unificado: "probó por primera vez" (tercera persona). | `useAppDerivedData.ts` |
| **Card lista/favorito** | Label: "añadió un café a la lista"; enlace: "Ver lista: {nombreLista}". | `useAppDerivedData.ts`, `ProfileView.tsx` |

---

## 2. ADN / Gráfica radar

### 2.1 Datos y fórmulas

- **Fuentes de datos (igual que web):** Cafés relevantes (reseñados, favoritos, listas, diario) con al menos un valor sensorial > 0, más **perfiles sensoriales del usuario** (`coffee_sensory_profiles`).
- **Fórmulas:** `startAngle = -π/2`, `angleStep = 2π/5`, factor por eje = `value / 10` (escala 0–10). Misma lógica en WebApp y Android.

### 2.2 Cambios Android

| Tema | Cambio | Archivos |
|------|--------|----------|
| **Perfiles sensoriales** | Nuevo flujo `profileUserSensoryProfilesFlow` que obtiene `getSensoryProfilesByUserId(targetUser.id)`; se combina con el resto del estado y se usan en el cálculo de `sensoryProfile`. | `SupabaseDataSource.kt`, `ProfileViewModel.kt` |
| **Muestras con ceros** | Solo se añade una muestra (café o perfil) si al menos uno de aroma/sabor/cuerpo/acidez/dulzura es > 0. Valores acotados en 0–10. | `ProfileViewModel.kt` (data class `SensorySample`, `addSample`) |
| **Puntos en el radar** | Se dibujan círculos en cada vértice del polígono (radio 5.dp), relleno con `lineColor` y borde con `labelColor` para que se vean en modo claro y oscuro. | `SensoryRadarChart.kt` |
| **Ejes y orden** | Orden fijo: Aroma, Sabor, Cuerpo, Acidez, Dulzura (`RADAR_LABELS`). | `SensoryRadarChart.kt` |
| **Modo noche** | Rejilla en gris visible; contenedor del radar con fondo negro. | `SensoryRadarChart.kt`, `ProfileComponents.kt`, `ProfileScreen.kt` |

### 2.3 Cambios WebApp

- Sin cambios de fórmula; ya usaba `coffeeSensoryProfiles` y filtrado por valor > 0. Se unificaron textos (véase Actividad).

---

## 3. Despensa (pantry)

### 3.1 Múltiples registros por café

- **Antes:** Un solo registro por (coffee_id, user_id); al añadir de nuevo se actualizaba cantidad.
- **Ahora:** Cada ítem tiene `id` (UUID) como clave primaria; se puede añadir el mismo café tantas veces como se quiera como registros distintos.

| Plataforma | Cambios principales |
|------------|---------------------|
| **Supabase** | Migración: columna `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`, políticas RLS por `id`. Ver `docs/supabase/pantry_items.sql`. |
| **Android** | `PantryItemEntity` con `id: String` como PK. `addToPantry` siempre inserta con nuevo UUID. `updatePantryStockById`, `deletePantryItemById`, `markCoffeeAsFinished(pantryItemId)`. UI y ViewModels pasan `pantryItemId` en lugar de `coffeeId` para editar/eliminar/marcar terminado. | `Entities.kt`, `Daos.kt`, `DiaryRepository.kt`, `SupabaseDataSource.kt`, `DiaryViewModel.kt`, `TimelineViewModel.kt`, `DetailViewModel.kt`, pantallas (Timeline, Diary, Detail, BrewLab) |
| **WebApp** | `PantryItemRow` con `id`. `insertPantryItem`, `updatePantryItem(id, ...)`, `deletePantryItemById(id)`. Hooks y vistas usan `pantryItemId` en estados y callbacks. | `types.ts`, `supabaseApi.ts`, `useDiaryActions.ts`, `useDiarySheetActions.ts`, `useCreateCoffeeDomain.ts`, `useCoffeeDetailActions.ts`, `DiaryView.tsx`, `TimelineView.tsx`, `AppContainer.tsx` |

### 3.2 Independencia de actividad y listas

- **Regla:** Eliminar una entrada de actividad (diario) o una lista **no** debe borrar ítems de despensa.
- **Comprobado:** No hay cascadas ni lógica que elimine pantry al borrar diario o lista. Documentado en `docs/supabase/pantry_independent_of_diary_and_lists.md` y comentarios en código.

### 3.3 Visibilidad cuando falta el café en caché

- **Android:** Si un ítem de despensa existe pero su café no está aún en `coffeeDao.getAllCoffeesWithDetails()`, se muestra con un café "stub" (id + nombre "Café") para no ocultar el ítem. | `DiaryRepository.kt` (`getPantryItems`, `stubCoffeeForId`)

### 3.4 Modal "Añadir a despensa"

- Sustitución del icono de rayo por el texto **"Crea tu café"** (misma acción que antes). Android: `AddStockScreen.kt`, `AddDiaryEntryScreen.kt`, `BrewLabComponents.kt`. WebApp: `DiarySheets.tsx`; en layout, botón en una sola línea (grid `auto`, `white-space: nowrap`).

---

## 4. Listas e iconos

### 4.1 Iconos de listas

- **Genérico (nombre de lista):** `list_alt` (WebApp: inline SVG; Android: drawable/asset).
- **Estado inactivo (añadir):** `list_alt_add`.
- **Estado activo (en lista):** `list_alt_check`; **color activo:** verde eléctrico (`#00E676`), no rojo.

| Plataforma | Dónde |
|------------|--------|
| **Android** | `Color.kt` (`ElectricGreen`), `DetailScreen.kt`, `ProfileComponents.kt`; iconos SVG/Vector en drawable o vía Coil. |
| **WebApp** | `tokens.css` (`--electric-green`), `features.css`, `theme-forced.css` para `.is-active` y favorito activo. |

---

## 5. Mi Diario (gráfica y filtros)

### 5.1 Gráfica de líneas

- **Recorte en 0:** La curva (Catmull-Rom en Android, Chart.js en Web) puede bajar ligeramente por debajo de 0. Se añadió margen inferior (padding interno y/o `scales.y.min` en web) para que no se corte. Android: `CHART_INTERNAL_BOTTOM_DP`, `CHART_PADDING_BOTTOM_DP`; WebApp: `layout.padding.bottom`, `scales.y.min: -maxVal * 0.06`.
- **Altura:** Gráfica más alta manteniendo márgenes (Android: `CHART_MIN_HEIGHT_DP`, `CHART_CANVAS_HEIGHT_DP` en `DiaryComponents.kt`).

### 5.2 Eje X (día actual)

- Etiqueta del día actual en **negro** (modo día) o **blanco** (modo noche); el resto en gris. Día actual en negrita. Filtro "Hoy" se mantiene. Android: `DiaryComponents.kt`. WebApp: `DiaryLineChart.tsx` (ticks.color, font).

### 5.3 "Primera vez" y estadísticas

- **Primera vez:** Solo se considera "probó por primera vez" un café que tiene **exactamente una** entrada en el diario (excl. agua). Android: `DiaryViewModel.kt` (baristaStats), `ProfileViewModel.kt` (firstTimeFromDiary). WebApp: `useAppDerivedData.ts` (`firstTimeCoffeeFromDiary`), `DiaryView.tsx`.

---

## 6. Elaboración (BrewLab) WebApp

- **Continuar sin café:** Seleccionar café no es obligatorio para pasar a configuración/resultado; condición `brewCanGoToConfig` basada en `brewMethod` (y no en `brewCoffeeId`) para métodos distintos de "Agua". | `AppContainer.tsx`
- **Página Resultado:** Guardar es opcional; se puede guardar sin elegir sabor; si no hay sabor se envía "—". | `BrewViews.tsx`
- **Dosis/cantidad:** La cantidad de café no se cambia automáticamente por tamaño; el usuario la edita; relación consistente con ratio y agua. | `AppContainer.tsx`, `BrewViews.tsx`, `useDiaryActions.ts`
- **Modo noche:** Fondos y textos en elaboración y recomendaciones corregidos (`--surface-1`, reglas en `theme-forced.css` y `features.css` para dark). | `tokens.css`, `features.css`, `theme-forced.css`

---

## 7. Auth y rendimiento

### 7.1 Refresh token

- Si la sesión no tiene refresh token (p. ej. Google Sign-In solo con ID token), no se fuerza re-login; se registra un aviso y en `syncPendingFcmTokenIfAny` se trata como advertencia, no error. | `UserRepository.kt`

### 7.2 Trabajo pesado en hilo principal

- El refresh de datos en Timeline (sync, etc.) se ejecuta en `Dispatchers.IO` dentro de `refreshData()` para no bloquear el main thread. | `TimelineViewModel.kt`

---

## 8. Supabase (esquema y RLS)

### 8.1 Despensa

- `docs/supabase/pantry_items.sql`: esquema con `id UUID PRIMARY KEY`, migración desde clave (coffee_id, user_id), políticas por `id`.
- `docs/supabase/pantry_independent_of_diary_and_lists.md`: regla de no borrar pantry al eliminar diario o lista; consulta para comprobar triggers.

### 8.2 Cafés (RLS)

- `docs/supabase/coffees_rls.sql`: políticas opcionales para lectura (públicos + propios custom; o "todos" para autenticados).
- `docs/supabase/coffees_rls_troubleshoot.sql`: consultas para diagnosticar RLS y `get_my_internal_id()` cuando un café custom no aparece.

---

## 9. Resumen por archivo

### Android (app/)

| Archivo | Cambios / eliminaciones |
|---------|-------------------------|
| `data/CoffeeRepository.kt` | `getCoffeeById(id)` (caché + Supabase). |
| `data/SupabaseDataSource.kt` | `getSensoryProfilesByUserId(userId)`; `getListItemsWithMetaForUser` (created_at → ms); pantry por `id`. |
| `data/DiaryRepository.kt` | `addToPantry` con UUID; `updatePantryStockById`, `deletePantryItemById`, `markCoffeeAsFinished(pantryItemId)`; `getPantryItems` con stub de café; sync pantry por `id`. |
| `data/Entities.kt` | `PantryItemEntity` con `id` como PK. |
| `data/Daos.kt` | Queries de pantry por `id`; `getPantryItemById`, `deletePantryItemById`. |
| `data/UserRepository.kt` | `getFollowingIdsForUser(userId)`; manejo de excepción "no refresh token". |
| `data/AppDatabase.kt` | Versión incrementada por cambio de PK en pantry. |
| `ui/profile/ProfileViewModel.kt` | `profileUserSensoryProfilesFlow`, `ProfileCombineState`, cálculo ADN con muestras filtradas y perfiles sensoriales; `resolveCoffeeName`, `firstTimeFromDiary`/`listItemsForUser` suspend con resolución de nombre. |
| `ui/profile/ProfileScreen.kt` | Paso de `isCurrentUser` a cards de actividad; ADN en negro en modo noche. |
| `ui/components/ProfileComponents.kt` | `formatRelativeTime`; cards de actividad (review, primera vez, lista) con tiempo relativo y estilo unificado; iconos list_alt; verde eléctrico para lista activa; texto ADN. |
| `ui/components/SensoryRadarChart.kt` | Puntos en vértices (círculo + borde); fórmulas alineadas con web; modo noche (grid). |
| `ui/components/DiaryComponents.kt` | Gráfica más alta y márgenes; etiquetas eje X (día actual negro/blanco); `PantryPremiumMiniCard` con `pantryItemId`. |
| `ui/detail/DetailScreen.kt` | Iconos lista SVG; verde eléctrico activo. |
| `ui/diary/DiaryViewModel.kt` | BaristaStats "primera vez" solo con count == 1; pantry por `pantryItemId`. |
| `ui/diary/AddStockScreen.kt`, `AddDiaryEntryScreen.kt` | "Crea tu café" en lugar de icono rayo. |
| `ui/timeline/TimelineViewModel.kt` | `refreshData` en `Dispatchers.IO`; pantry por `pantryItemId`. |
| `ui/timeline/TimelineScreen.kt` | Estados y callbacks por `pantryItemId`. |
| `ui/theme/Color.kt` | `ElectricGreen`. |
| `ui/components/BrewLabComponents.kt` | "Crea tu café"; paso de `reduceFromPantryItemId` al guardar. |

### WebApp (webApp/)

| Archivo | Cambios / eliminaciones |
|---------|-------------------------|
| `src/data/supabaseApi.ts` | `insertPantryItem`, `updatePantryItem(id, ...)`, `deletePantryItemById(id)`; fetch pantry con `id`; comentarios sobre no borrar pantry. |
| `src/hooks/domains/useDiaryActions.ts` | `handleUpdatePantryStock(pantryItemId, ...)`, `handleRemovePantryItem(pantryItemId)`; labels actividad "añadió un café a la lista". |
| `src/hooks/domains/useDiarySheetActions.ts` | `savePantry` con `insertPantryItem`; estado actualizado añadiendo nueva fila. |
| `src/hooks/domains/useCreateCoffeeDomain.ts` | Añadir a pantry con `insertPantryItem`. |
| `src/hooks/domains/useCoffeeDetailActions.ts` | `detailPantryStock`; `saveDetailStock` con `updatePantryItem` o `insertPantryItem` según tenga `id`. |
| `src/hooks/domains/useAppDerivedData.ts` | Labels "probó por primera vez", "añadió un café a la lista"; primera vez solo con count === 1. |
| `src/app/AppContainer.tsx` | `brewCanGoToConfig` sin exigir café; `handleMarkPantryCoffeeFinished(pantryItemId)`; paso de `profileDiaryCoffeeIds`/`profileListCoffeeIds` a perfil; `detailPantryStock` a detail actions. |
| `src/features/brew/BrewViews.tsx` | Guardar resultado opcional; `canSave` siempre true; sabor "—" si vacío. |
| `src/features/diary/DiaryView.tsx` | Estados por `pantryItemId`; callbacks con `pantryItemId`. |
| `src/features/diary/DiarySheets.tsx` | Botón "Crea tu café" (sin rayo). |
| `src/features/diary/DiaryLineChart.tsx` | Eje X (día actual negro/blanco); padding y `scales.y.min` para no cortar curva. |
| `src/features/profile/ProfileView.tsx` | Estado vacío "a otras personas"; enlace lista "Ver lista: {nombre}"; ADN con `profileDiaryCoffeeIds`/`profileListCoffeeIds`; modo noche ADN. |
| `src/features/timeline/TimelineView.tsx` | Opciones y edición de pantry por `pantryItemId`. |
| `src/styles/tokens.css` | `--electric-green`, `--surface-1` en dark. |
| `src/styles/features.css` | Verde eléctrico activo; modo noche elaboración y ADN; layout botón "Crea tu café". |
| `src/styles/theme-forced.css` | Verde eléctrico y modo noche ADN/elaboración. |
| `src/ui/iconography.tsx` | Iconos list_alt, list_alt_add, list_alt_check (inline SVG). |
| `src/mappers/supabaseMappers.ts` | `mapPantryItemRow` con `id`. |

### Documentación (docs/)

| Archivo | Contenido |
|---------|-----------|
| `supabase/pantry_items.sql` | Esquema y migración pantry con `id` UUID. |
| `supabase/pantry_independent_of_diary_and_lists.md` | Regla y comprobaciones. |
| `supabase/coffees_rls.sql` | Políticas RLS cafés (incl. opción "todos"). |
| `supabase/coffees_rls_troubleshoot.sql` | Diagnóstico RLS y `get_my_internal_id()`. |
| `supabase/user_lists.sql` | Nota de no borrar pantry al eliminar lista. |

---

*Documento vivo: actualizar al aplicar nuevos cambios de paridad o mejoras en WebApp, Android o Supabase.*
