# Commit 2026-03-04 — UI: swipe borrar, botones eliminar/primarios, Continuar explorando

**Estado:** documentado  
**Última actualización:** 2026-03-04

---

## Desplegado en
- **main** (push directo)

---

## Resumen
Ajustes de UI/UX en webapp y Android: corrección del rojo visible en los bordes del swipe de borrar, estandarización de botones de eliminar/borrar y botones primarios (marrón / marrón claro, texto blanco/negro), y botón «Continuar explorando» en modo noche con texto negro.

---

## Cambios detallados

### 1. Swipe de borrar: rojo no visible por los bordes (webapp)
- **Problema:** El fondo rojo del swipe de borrar se veía por los bordes del elemento que tiene encima (p. ej. en Perfil → Favoritos).
- **Solución:**
  - **Diario:** `.diary-card-swipe-wrap` con `border-radius: 16px` y `overflow: hidden`. `.diary-swipe-bg` con `inset: 3px` y `border-radius: 13px`.
  - **Favoritos (perfil):** `.profile-favorite-swipe-bg` con `inset: 3px`, `border-radius: 15px`, fondo `var(--electric-red)`, texto/icono negro en noche y blanco en día (override en bloque light).
  - **Notificaciones:** `.notifications-swipe-bg` (ambos bloques) con `inset: 3px`, `border-radius: 17px`, `var(--danger)`.
- **Archivos:** `webApp/src/styles/features.css`

### 2. Botón «Continuar explorando» — modo noche (webapp)
- **Problema:** En modo noche el texto del botón no seguía el estándar (debe ser negro con fondo marrón claro).
- **Solución:** Base en `profile-adn.css` = `var(--caramel-soft)` + `var(--pure-black)`. Override explícito en `features.css` dentro de `@media (prefers-color-scheme: dark)` para asegurar texto negro. En modo día se mantiene `var(--caramel-accent)` + blanco (ya definido en bloque light).
- **Archivos:** `webApp/src/styles/features/profile-adn.css`, `webApp/src/styles/features.css`

### 3. Botones de borrar/eliminar y swipe (webapp + Android)
- **Patrón:** Modo día: fondo rojo eléctrico, texto/icono blanco. Modo noche: fondo rojo eléctrico, texto/icono negro.
- **Webapp:** `.diary-delete-confirm-submit`, `.diary-entry-delete`, `.diary-swipe-bg`, `.coffee-detail-review-delete`, `.diary-sheet-action-pantry.is-delete`, `.comment-action-button.is-danger`, `.notifications-swipe-bg`; iconos de swipe con color según tema.
- **Android:** `DeleteConfirmationDialog` (fondo `ElectricRed`), icono de papelera en swipe en Timeline, Notificaciones, Favoritos (perfil), Detalle café, Diario; `ModalMenuOption` «Eliminar de la despensa» con icono negro en noche y blanco en día.
- **Archivos:** `webApp/src/styles/features.css`; Android: `TimelineComponents.kt`, `NotificationsScreen.kt`, `ProfileComponents.kt`, `DiaryScreen.kt`, `DetailScreen.kt`, `TimelineScreen.kt`

### 4. Botones primarios (Seguir, Iniciar, Guardar en diario, etc.)
- **Webapp:** Botones con fondo marrón en día (texto blanco) y marrón claro en noche (texto negro). Ajustes en `.brew-prep-action-primary`, `.brew-result-action-primary`, `.profile-adn-continue-button`, tabs activas sin capa negra, `.nav-desktop` con `--nav-rail-border`, eliminado `padding-top` de `.main-shell`.
- **Android:** `FollowButton`, `FollowItemModern` (FollowersScreen), Notificaciones «RESPONDER», BrewLab «INICIAR»/«PAUSAR» y «GUARDAR EN DIARIO» con `contentColor` según `isSystemInDarkTheme()`.

### 5. Botones detalle café (Volver, Favorito, Despensa)
- **Webapp + Android:** Estilo blanco (fondo blanco, icono negro) para coherencia visual.

### 6. Otros ajustes de sesión (referencia)
- Colores de fecha/meta: gris oscuro en día, gris claro en noche (webapp + Android).
- Recomendaciones «Recomendados para tu paladar»: lógica con favoritos, despensa y visitados; estilo de tarjetas en móvil (2 líneas, imagen más alta).
- Layout buscador usuarios y notificaciones en webapp; cabecera notificaciones y botón Volver circular; homogenización de botones Seguir/Siguiendo en buscador y perfiles.
- Google Sign-In (GIS), tokens y variables de tema en webapp y Android.

---

## Archivos tocados (resumen)
- **Webapp:** `webApp/src/styles/features.css`, `webApp/src/styles/features/profile-adn.css`, `webApp/src/styles/tokens.css`, `webApp/src/app/AppContainer.tsx`, `webApp/src/features/coffee/CoffeeDetailView.tsx`, `webApp/src/features/search/SearchView.tsx`, `webApp/src/features/timeline/NotificationsSheet.tsx`, componentes de auth y hooks.
- **Android:** `app/src/main/java/.../ui/theme/Color.kt`, `Theme.kt`, `ProfileComponents.kt`, `FollowersScreen.kt`, `DetailScreen.kt`, `TimelineComponents.kt`, `TimelineScreen.kt`, `NotificationsScreen.kt`, `DiaryScreen.kt`, `BrewLabComponents.kt`, `PostCard.kt`, `UserReviewCard.kt`, `RecommendationCarousel.kt`, `SensoryRadarChart.kt`, `AddPantryItemScreen.kt`, `AddPostScreen.kt`, `TimelineViewModel.kt`, etc.
- **Docs:** esta nota en `docs/commit-notes/`.

---

## Cómo verificar
- **Webapp:** Probar en modo claro y oscuro: swipe borrar en Diario, Favoritos (perfil) y Notificaciones; botón «Continuar explorando» en análisis de preferencias; botones Eliminar/Borrar y primarios.
- **Android:** Mismo flujo en tema claro/oscuro; compilar con `./gradlew :app:assembleDebug`.
