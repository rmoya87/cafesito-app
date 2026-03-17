# Contenido público (webApp/public)

Este directorio se copia a la raíz del build. Todo lo que se coloque aquí es **accesible públicamente** (sin autenticación).

## Páginas estáticas públicas

- **`/landing/`** — Landing de presentación (index en `landing/`).
- **`/legal/privacidad.html`** — Política de privacidad.
- **`/legal/condiciones.html`** — Condiciones del servicio.
- **`/legal/eliminacion-cuenta.html`** — Procedimiento de eliminación de cuenta y datos.

Cualquier usuario puede acceder a estas URLs. No requieren inicio de sesión.

Al añadir nuevas páginas HTML públicas:

1. Mantener **accesibilidad** (ver `docs/ACCESIBILIDAD_WEBAPP_ANDROID.md`, sección 2.1).
2. Añadir **analíticas** vía GTM: snippet en head + noscript en body + script que haga `dataLayer.push({ event: 'page_view', page_path, page_title, page_location, screen_name })` (ver `docs/ANALITICAS.md`).
