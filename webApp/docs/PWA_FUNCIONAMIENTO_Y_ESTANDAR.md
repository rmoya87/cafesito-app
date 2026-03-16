# PWA Cafesito — Funcionamiento y estándar

**Estado:** vivo  
**Última actualización:** 2026-03-13  
**Ámbito:** WebApp (Progressive Web App instalable en **Android**, **iOS** y escritorio)

---

## 1. Objetivo de la PWA

La WebApp debe comportarse como **PWA instalable** en **Android**, **iOS** y escritorio:

- **Instalabilidad**: En **Android** el usuario puede “Instalar” o “Añadir a la pantalla de inicio” (Chrome y otros). En **iOS** (Safari) debe usar “Compartir” → “Añadir a la pantalla de inicio”; no hay prompt automático como en Chrome. En escritorio, “Instalar” cuando el navegador lo ofrezca.
- **Experiencia tipo app**: En modo instalado (`display-mode: standalone`), sin barra de URL; en **Android** barra de estado con `theme_color`; en **iOS** barra de estado y notch respetados con `viewport-fit=cover` y `env(safe-area-inset-*)`.
- **Resiliencia offline**: Indicación clara cuando no hay red; shell y datos recientes cacheados cuando sea posible (SW en producción; iOS Safari soporta Service Worker).
- **Actualizaciones transparentes**: El Service Worker actualiza en segundo plano; el usuario ve la nueva versión al volver a la app o en la siguiente carga.

---

## 2. Requisitos técnicos (cómo debe funcionar)

### 2.1 Manifest (Web App Manifest)

- **Fuente de verdad**: La configuración del manifest en **producción** la genera `vite-plugin-pwa` desde `webApp/vite.config.ts` (bloque `VitePWA({ manifest: { ... } })`). No debe depender del archivo estático `public/manifest.webmanifest` en build, para evitar duplicados e inconsistencias.
- **Campos obligatorios** (aplican a **Android** y **iOS** cuando la PWA se instala desde Safari/Chrome):
  - `name`, `short_name`: "Cafesito".
  - `description`: Texto breve para instalación y listados.
  - `theme_color`: Color de la barra de estado (**Android**). En **iOS** Safari usa además los meta `apple-mobile-web-app-*` para la apariencia al abrir desde el icono.
  - `background_color`: Fondo de la splash al abrir la app (Android e iOS).
  - `display`: `"standalone"` (sin UI del navegador).
  - `orientation`: `"portrait"` (recomendado para uso móvil).
  - `start_url`: `"/"` (o `base + "/"` si la app se despliega en subruta).
  - `icons`: Al menos 192×192 y 512×512; `purpose: "any"` y `"maskable"` para **Android**. En **iOS** Safari usa sobre todo `apple-touch-icon` del HTML; el manifest se respeta para “Añadir a pantalla de inicio”.
- **Scope**: Por defecto el scope es el directorio de `start_url`; para SPA en raíz no hace falta definirlo.

### 2.2 Meta tags (HTML)

- **Viewport**: `width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover` — imprescindible en **iOS** para safe area (notch, barra de estado).
- **theme-color**: En `index.html` para la pestaña del navegador (modo claro/oscuro). El manifest `theme_color` aplica a la PWA instalada (Android barra de estado; iOS usa también los meta Apple).
- **iOS (Safari — “Añadir a pantalla de inicio”)**:
  - `apple-mobile-web-app-capable`: `yes` — permite modo pantalla completa sin chrome de Safari.
  - `apple-mobile-web-app-title`: "Cafesito" — nombre bajo el icono en el home.
  - `apple-mobile-web-app-status-bar-style`: `black-translucent` — contenido bajo la barra de estado; la app debe compensar con `env(safe-area-inset-top)` en la CSS.
- **Iconos y splash iOS**: `apple-touch-icon` (icono en home) y `apple-touch-startup-image` (splash al abrir) para varios tamaños de pantalla (iPhone); ver `index.html` y scripts de generación de splash.

### 2.3 Service Worker (Workbox)

- **Registro**: Solo en **producción** (`import.meta.env.PROD`). En desarrollo el SW no se registra (y se desregistra si existía) para evitar caché que entorpezca el desarrollo. **iOS** (Safari 11.1+) soporta Service Worker; el mismo SW y estrategias de caché aplican en Android e iOS.
- **Estrategia de actualización**: `registerType: "autoUpdate"` — el SW se actualiza en segundo plano; la siguiente carga usa la nueva versión (no prompt “Actualizar”).
- **Detección de nueva versión**: Tras registrar el SW, se comprueba actualización en `visibilitychange` (al volver a la pestaña) y con un intervalo razonable (p. ej. cada 5 minutos) para que el usuario reciba la nueva versión sin intervención.
- **Precache**: Solo shell de la app: `**/*.{js,css,html}`. No precachear todos los PNG/SVG para no alargar la instalación del SW.
- **Runtime caching**:
  - **API Supabase REST**: `NetworkFirst` con timeout corto (p. ej. 4 s), para que con red se use la red y sin red se use caché si está disponible.
  - **Supabase Storage**: `CacheFirst` con expiración (p. ej. 7 días, límite de entradas).
  - **Assets estáticos** (drawables, logo, splash, favicon): `CacheFirst` con expiración larga (p. ej. 30 días).

### 2.4 Detección de modo PWA (standalone)

- **Criterio**: `window.matchMedia("(display-mode: standalone)").matches` (Android, Chrome desktop) **o** en **iOS** el flag `navigator.standalone === true` (Safari cuando se abre desde “Añadir a pantalla de inicio”). Ambos se comprueban en `main.tsx` para añadir la clase `pwa-standalone`.
- **Clase en la raíz**: Cuando la app se abre instalada (Android o iOS), se añade `pwa-standalone` a `<html>`. La CSS debe:
  - Topbar con `padding-top: env(safe-area-inset-top)` y relleno para la barra de estado (notch en iPhone).
  - Evitar franjas o solapamientos con notch/barra de estado en **iOS** y Android.
  - Comportamiento de sheets y overlays similar a la app nativa.
- **Desarrollo**: En dev, `?pwa=1` simula `pwa-standalone` para probar estilos sin instalar en dispositivo.

### 2.5 Offline

- **Banner**: Cuando `navigator.onLine === false`, se muestra un banner fijo (p. ej. arriba) con el texto “Sin conexión”, accesible (`role="status"`, `aria-live="polite"`).
- **Comportamiento**: Las peticiones a la API siguen la estrategia del SW (NetworkFirst); el usuario puede seguir viendo datos cacheados. No se exige modo offline completo para todas las pantallas; sí indicación clara de estado.

### 2.6 Analíticas

- En PWA instalada se puede añadir un parámetro (p. ej. `?pwa=1`) a `page_path` / `page_location` en GA para segmentar tráfico PWA frente a uso en navegador.

### 2.7 Seguridad y headers

- **HTTPS**: La PWA y el Service Worker deben servirse solo por HTTPS (en producción).
- **CSP**: Si hay Content-Security-Policy, debe permitir el origen del SW y los recursos necesarios (script-src, worker-src si aplica).

---

## 3. Archivos y responsabilidades en el proyecto

| Elemento | Ubicación | Responsabilidad |
|---------|-----------|------------------|
| Manifest (generado) | `vite.config.ts` → VitePWA `manifest` | name, short_name, theme_color, background_color, display, start_url, icons |
| Manifest estático | `public/manifest.webmanifest` | Solo por si se sirve en dev o como fallback; debe alinearse con vite para evitar diferencias |
| Meta / links PWA | `index.html` | viewport, theme-color, apple-mobile-web-app-*, apple-touch-icon, apple-touch-startup-image, link manifest |
| Registro SW | `src/main.tsx` | registerSW (solo PROD), detección standalone, clase pwa-standalone, ?pwa=1 en dev |
| Offline banner | `src/app/OfflineBanner.tsx` | Mostrar “Sin conexión” cuando !navigator.onLine |
| Estilos PWA | `src/styles/base.css`, `src/styles/features.css` | .pwa-standalone (topbar, safe-area), .offline-banner |
| Analíticas PWA | `src/core/ga4.ts` | Añadir pwa=1 a page_path/page_location cuando sea standalone |

---

## 4. Checklist de cumplimiento y pendientes

Se ha revisado la WebApp frente a esta especificación. Resultado:

### 4.1 Cumplido

- [x] **Manifest en build**: Definido en `vite.config.ts` (name, short_name, description, theme_color, background_color, display, orientation, start_url, icons). Aplica a **Android** e **iOS** cuando se instala desde el navegador.
- [x] **Meta viewport**: `viewport-fit=cover` y opciones correctas en `index.html`; necesario para **iOS** (safe area / notch).
- [x] **Meta iOS**: `apple-mobile-web-app-capable`, `apple-mobile-web-app-title`, `apple-mobile-web-app-status-bar-style` en `index.html` para “Añadir a pantalla de inicio” en Safari.
- [x] **Iconos y splash**: **Android**: manifest icons. **iOS**: `apple-touch-icon` y `apple-touch-startup-image` con varias resoluciones en `index.html`; splash generado (scripts).
- [x] **Link al manifest**: `index.html` enlaza a `/manifest.webmanifest`.
- [x] **SW solo en producción**: Registro con `import.meta.env.PROD`; en dev se desregistran SW y se limpian caches. **iOS** Safari soporta Service Worker.
- [x] **Auto-update**: `registerType: "autoUpdate"` y comprobación en visibilitychange + intervalo 5 min.
- [x] **Precache**: Solo `**/*.{js,css,html}` en workbox.
- [x] **Runtime caching**: NetworkFirst para Supabase REST; CacheFirst para Storage y assets estáticos.
- [x] **Detección standalone**: `matchMedia("(display-mode: standalone)")` (Android/Chrome) y `navigator.standalone === true` (**iOS** Safari); clase `pwa-standalone` en `<html>`.
- [x] **Simulación en dev**: `?pwa=1` añade `pwa-standalone` en desarrollo.
- [x] **Offline banner**: Componente `OfflineBanner` con eventos online/offline y estilos; accesibilidad (role, aria-live). Visible en **Android** e **iOS** cuando no hay red.
- [x] **Estilos PWA**: Uso de `.pwa-standalone` para topbar, safe-area (notch **iOS**), sheets, overlays en `base.css` y `features.css`.
- [x] **Analíticas**: GA con parámetro `pwa=1` cuando es standalone (`ga4.ts`).
- [x] **HTTPS en producción**: Deploy en HTTPS; dev con mkcert para HTTPS local. Requerido para SW en **Android** e **iOS**.

### 4.2 Resuelto / notas

- [x] **Manifest estático vs generado**: Se mantiene `public/manifest.webmanifest` alineado con `vite.config.ts` (theme_color/background_color `#6f4e37`) como fallback para dev y preview. En build, vite-plugin-pwa genera su propio manifest; el estático evita 404 en entornos donde no se inyecta.
- [x] **theme-color en HTML vs manifest**: Documentado en `index.html`: los meta `theme-color` (#212121 / #f7f7f7) aplican a la **pestaña del navegador**; el manifest usa `#6f4e37` para la **PWA instalada** (barra de estado). Es intencional que difieran.
- [x] **Icono maskable**: El manifest usa el mismo `logo.png` para `purpose: "any"` y `"maskable"`. Si en **Android** el icono se recorta mal, crear una variante 512×512 con ~20% de padding seguro y declararla solo para `purpose: "maskable"`. En **iOS** el icono en home viene de `apple-touch-icon` (ya configurado).
- [x] **Lighthouse PWA**: Recomendado ejecutar auditoría PWA (Lighthouse) en producción (instalable, manifest válido, SW, HTTPS, viewport) y corregir avisos que afecten instalabilidad o experiencia.
- [x] **start_url con base**: Si la app se despliega en subruta (p. ej. `base: "/app/"`), actualizar `start_url` en el bloque `manifest` de `vite.config.ts` a `"/app/"` (o la raíz de la app) para que la PWA abra correctamente.
- [x] **Scope explícito**: No necesario con `start_url: "/"`. Si se usa subruta, definir `scope` en el manifest de forma coherente con `start_url`.

---

## 5. Referencias

- [Web App Manifest (W3C)](https://www.w3.org/TR/appmanifest/)
- [vite-plugin-pwa](https://vite-pwa-org.netlify.app/)
- [Workbox](https://developer.chrome.com/docs/workbox/)
- **iOS**: [Configuring Web Applications (Apple)](https://developer.apple.com/library/archive/documentation/AppleApplications/Reference/SafariWebContent/ConfiguringWebApplications/ConfiguringWebApplications.html) — meta `apple-mobile-web-app-*`, `apple-touch-icon`, `apple-touch-startup-image`, safe area.
- Documentación del proyecto: `docs/README.md`, `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`, `webApp/DEPLOY-IONOS.md` (base, rutas, redirects).
