# Estados vacío y error — Patrón unificado

**Propósito:** Definir un patrón común para listas vacías y errores de red en WebApp y Android, para que la experiencia sea coherente entre plataformas.

**Última actualización:** 2026-03-04  
**Ámbito:** WebApp (`webApp/`), Android (`app/`).

---

## 1. Lista vacía

### 1.1 Estructura

- **Icono** (opcional): ilustración o icono representativo del contenido (café, agua, publicaciones, etc.).
- **Mensaje principal:** una línea clara que explique por qué está vacío.
- **CTA (opcional):** botón o enlace para la acción principal (ej. "Añadir café", "Publicar tu primer café").

### 1.2 Jerarquía visual

- Título/mensaje: tipografía destacada (titleMedium / titleLarge), color principal (`onSurface`).
- Subtítulo (opcional): bodyMedium, color secundario (`onSurfaceVariant`).
- Botón: estilo primario (fondo marrón / marrón claro según tema), texto blanco (día) o negro (noche). Ver `docs/DESIGN_TOKENS.md`.

### 1.3 Textos por contexto (referencia)

| Contexto | Mensaje | CTA (si aplica) |
|----------|---------|------------------|
| Diario — sin entradas | "Sin café o agua registrada" | Añadir entrada (desde FAB/acción existente) |
| Diario — despensa vacía | "No hay café en tu despensa" | Añadir a despensa |
| Timeline sin posts | "Tu timeline está vacío" / "No hay publicaciones disponibles" | "Publica tu primer café" |
| Comentarios | "No hay comentarios todavía" | — |
| Favoritos perfil | "No hay cafés favoritos" | Explorar / Añadir |
| Búsqueda / despensa Brew | "No hay coincidencias en tu despensa" / "Tu despensa está vacía" | — |
| Opiniones detalle café | "No hay opiniones aún. ¡Sé el primero!" | Añadir opinión |
| Notificaciones | Mensaje vacío coherente con el anterior | — |

Al añadir un nuevo estado vacío, reutilizar estos textos o variantes coherentes y el mismo patrón (mensaje + CTA si procede).

### 1.4 Implementación

- **Web:** clases tipo `.timeline-empty`, `.diary-empty-card`; contenedor centrado, padding generoso (p. ej. 48px), texto centrado.
- **Android:** composables `EmptyStateMessage(message)` (solo texto) o patrones como `TimelineEmptyState` (mensaje + CTA + sugerencias). Para listas simples usar un bloque centrado con `Text` + opcional `Button`.

---

## 2. Error de red (o fallo de carga)

### 2.1 Estructura

- **Mensaje:** una línea que indique que algo ha fallado (ej. "No se han podido cargar los datos").
- **Botón "Reintentar":** misma jerarquía que un botón secundario o terciario (borde, no lleno), que dispare de nuevo la carga o refresco.

### 2.2 Jerarquía visual

- Mensaje: bodyLarge o titleSmall, color `onSurfaceVariant` o `onSurface`.
- Botón "Reintentar": contorno (outline) o estilo secundario; no rojo (reservar rojo para acciones destructivas). Ver `DESIGN_TOKENS.md`.

### 2.3 Texto

- Mensaje: **"No se han podido cargar los datos."** (o variante por pantalla: "No se han podido cargar las notificaciones.", etc.).
- Botón: **"Reintentar"**.

### 2.4 Implementación

- **Web:** bloque con clase tipo `.timeline-error` o `.error-state`; mensaje + `<button>` "Reintentar" que llame a la función de recarga (refetch).
- **Android:** composable `ErrorStateMessage(message, onRetry)` o equivalente; `Button` con `onClick = onRetry` y texto "Reintentar".

Evitar mostrar lista vacía cuando en realidad hay un error; distinguir en el estado de la pantalla entre "vacío" y "error" y mostrar el patrón correspondiente.

---

## 3. Resumen

| Estado | Elementos | Objetivo |
|--------|-----------|----------|
| **Vacío** | Mensaje (+ icono opcional) + CTA opcional | Explicar y guiar la siguiente acción. |
| **Error** | Mensaje + botón "Reintentar" | Explicar el fallo y ofrecer recuperación. |

Al diseñar una nueva pantalla con lista o datos remotos, definir qué se muestra en vacío y en error y alinear textos y jerarquía con este documento y con `DESIGN_TOKENS.md`.
