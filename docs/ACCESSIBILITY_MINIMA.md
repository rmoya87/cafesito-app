# Accesibilidad mínima

**Propósito:** Criterios mínimos para WebApp y Android (botones/iconos descriptivos, tamaño de tap, contraste) y revisión en paralelo para que el nivel sea similar.

**Última actualización:** 2026-03-04  
**Ámbito:** WebApp (`webApp/`), Android (`app/`).

---

## 1. Botones e iconos

### 1.1 Descripción para lectores de pantalla

- **Web:** Todo botón o control que solo muestre un icono debe tener `aria-label` descriptivo (ej. "Cerrar", "Añadir entrada", "Buscar", "Notificaciones", "Volver").
- **Android:** Todo `Icon`, `IconButton` o `FloatingActionButton` que sea accionable debe tener `contentDescription` (ej. "Añadir entrada", "Buscar usuarios", "Notificaciones"). No usar `null` en iconos interactivos.

Así se mantiene paridad: mismo nivel de información para usuarios con lectores de pantalla en ambas plataformas.

### 1.2 Dónde revisar

- Navegación inferior (tabs): cada tab con etiqueta o `aria-label` / `contentDescription`.
- TopBar: iconos de atrás, favorito, despensa, notificaciones, búsqueda, menú.
- FAB y botones de acción primaria (añadir post, añadir entrada al diario).
- Listas con acciones (eliminar, editar, seguir): el botón/icono debe ser descriptivo.
- Modales y sheets: botón de cierre y acciones principales.

---

## 2. Tamaño de área de tap

- **Mínimo recomendado:** 44 x 44 px (dp en Android). WCAG 2.5.5 (Level AAA) recomienda al menos 44 x 44 CSS pixels.
- **Web:** Botones e iconos clicables con `min-width` y `min-height` de 44px, o padding que garantice ese tamaño. En móvil, evitar zonas clicables muy pequeñas.
- **Android:** `Modifier.size(48.dp)` para `IconButton` (Material recomienda 48.dp mínimo); o `minimumInteractiveComponentSize()` para que el área de toque sea al menos 48.dp. Comprobar que los chips y botones de texto tengan altura suficiente (p. ej. 48.dp).

Revisar en paralelo: si en web un botón es 44px, en Android el equivalente no debería ser mucho menor (evitar 32.dp para acciones críticas).

---

## 3. Contraste de texto

- **Estándar:** WCAG 2.1 nivel AA: relación de contraste al menos 4.5:1 para texto normal, 3:1 para texto grande.
- **Modo día:** Texto principal sobre fondo claro (`#f7f7f7`, blanco): usar color oscuro (p. ej. `#1a120b`, `#3c2a21`) para el cuerpo de texto.
- **Modo noche:** Texto claro sobre fondo oscuro (`#212121`, negro): usar blanco o gris claro (`#bdb7b2`) según jerarquía. Evitar grises muy claros sobre fondos no del todo negros que bajen el contraste.
- **Tokens:** Seguir `docs/DESIGN_TOKENS.md` para colores de texto (`onSurface`, `onSurfaceVariant`) y asegurarse de que las combinaciones fondo/texto cumplan el ratio en ambos temas.

Revisar en paralelo: los mismos textos (títulos, mensajes vacío, fechas) no deben tener peor contraste en una plataforma que en la otra.

---

## 4. Resumen de checklist

| Criterio | Web | Android |
|----------|-----|---------|
| Iconos/botones sin texto con etiqueta | `aria-label` en controles interactivos | `contentDescription` en Icon/IconButton |
| Área de tap mínima | ≥ 44px (min-width/height o padding) | ≥ 48.dp (IconButton / minimumInteractiveComponentSize) |
| Contraste texto/fondo | WCAG AA (4.5:1 normal, 3:1 grande) en día y noche | Mismo criterio con MaterialTheme y tokens |

Al añadir una nueva pantalla o componente con botones/iconos, aplicar este checklist y documentar aquí cualquier excepción acordada.
