# SEO WebApp avanzado — Marzo 2026

**Estado:** vigente  
**Ultima actualizacion:** 2026-03-25  
**Ambito:** `webApp/` (SPA + paginas estaticas + build/deploy)  
**Fuente unica SEO WebApp:** este documento (evitar duplicados en otros docs; en release solo mantener checklist operativo y enlace aqui).

---

## 1) Objetivo

Documentar todos los cambios SEO avanzados aplicados recientemente en la WebApp:

- canonicals y normalizacion de URL
- indexacion por tipo de ruta
- metadatos OG/Twitter y JSON-LD
- sitemaps (incluyendo gzip, facetas e imagenes)
- OG images generadas en build
- estrategia de cache en Apache (`.htaccess`)
- mejoras de Core Web Vitals (CSS split, critical CSS, content-visibility, network hints)
- separacion explicita SPA vs estaticas (`public/landing` y `public/legal/*.html`)

---

## 2) Resumen ejecutivo (implementado)

1. **Canonical sin slash final** y redirecciones 301 server-side.
2. **Indexacion controlada**:
   - indexables: `search`, `coffee/:slug`, `login`, `landing`, `legal/*.html`
   - no indexables SPA internas: `noindex, follow`
3. **Metadatos SEO/SMO ampliados**:
   - `og:locale`, `og:image:width/height`, `twitter:site`, `twitter:creator`
   - `og:image` y `twitter:image` para search/login/coffee
4. **JSON-LD `WebSite + SearchAction`** para buscador.
5. **Sitemaps avanzados** con `lastmod` y compresion `.xml.gz`.
6. **Image SEO**:
   - `image-sitemap.xml.gz`
   - OG images 1200x630 generadas en build (genericas + por cafe)
   - variantes WebP derivadas
7. **Cache SEO-first en Apache** por tipo de recurso/ruta.
8. **CWV**:
   - CSS dividido (`index` + `features` lazy)
   - critical CSS minimo inline
   - `content-visibility` en listas largas
   - `preconnect` selectivo por consentimiento

---

## 3) Cambios por area

## 3.1 Canonicalizacion y rutas

### 3.1.1 Apache (`.htaccess`)

Archivo: `webApp/public/.htaccess`

- Redirect 301 a URLs sin slash final (excepto raiz).
- Reglas especificas para:
  - `/landing/ -> /landing`
  - `/coffee/<slug>/ -> /coffee/<slug>`
- Mantiene fallback SPA a `index.html`.

### 3.1.2 Rutas de facetas SEO (limpias)

Archivo: `webApp/src/core/routing.ts`

Se soportan como rutas conocidas:

- `/search/origen/<valor>`
- `/search/tueste/<valor>`
- `/search/especialidad/<valor>`
- `/search/formato/<valor>`
- `/search/nota/<valor>`

Tambien se actualizo `buildRoute()` para generar rutas canonicas sin slash final en detalle cafe.

---

## 3.2 Metadatos SEO (runtime SPA)

Archivo: `webApp/src/hooks/domains/useCoffeeSeoMeta.ts`

Implementado:

- canonical por ruta (sin slash final)
- robots por contexto:
  - index/follow: search, facetas, coffee, login
  - noindex/follow: resto SPA
- OG/Twitter completos en SPA:
  - `og:locale=es_ES`
  - `og:image:width=1200`, `og:image:height=630`
  - `twitter:site`, `twitter:creator`
- Search y facetas:
  - OG/Twitter con imagen dedicada (`/og/search.jpg`)
  - `WebPage` JSON-LD
- Coffee detail:
  - OG/Twitter apuntando a OG generada por cafe (`/og/coffee/<id>.jpg`)
  - `Product` JSON-LD (con rating agregado cuando aplica)
- `WebSite + SearchAction` JSON-LD global.

Nota: se retiro el bloque UI de “Explorar relacionados” en detalle cafe por decision de producto.

---

## 3.2.b Metadatos SEO en paginas estaticas (landing/legal)

Archivos:

- `webApp/public/landing/index.html`
- `webApp/public/legal/privacidad.html`
- `webApp/public/legal/condiciones.html`
- `webApp/public/legal/eliminacion-cuenta.html`

Implementado en estaticas (sin hooks React):

- `canonical` explicito y uniforme sin slash final donde aplica:
  - `https://cafesitoapp.com/landing`
  - `https://cafesitoapp.com/legal/*.html`
- `hreflang` (`es`, `en`, `x-default`) en landing y legales.
- JSON-LD especifico tipo `WebPage` por pagina.
- En landing se mantiene adicionalmente `SoftwareApplication` (contexto de producto/app).

**Runbook anti-regresión (rutas absolutas en landing, login SPA vs `/login`, CSS de `LoginGate`, favicon legal):** `webApp/docs/ESTATICOS_LANDING_LEGAL_LOGIN_WEB.md`.

---

## 3.3 Parametros tracking y URL limpia

Archivo: `webApp/src/hooks/domains/useRouteSync.ts`

Se normalizan y eliminan parametros de tracking en URL canonicamente:

- `utm_*`
- `gclid`
- `fbclid`
- `ref`, `ref_src`
- `mc_cid`, `mc_eid`

Objetivo: evitar duplicados SEO por parametros.

---

## 3.4 OG images generadas en build

Archivo: `webApp/scripts/generate-og-images.mjs`

Genera en `dist/og/`:

- genericas:
  - `search.jpg` + `search.webp`
  - `login.jpg` + `login.webp`
  - `default.jpg` + `default.webp`
- por cafe (best effort):
  - `og/coffee/<id>.jpg`
  - `og/coffee/<id>.webp`

Las OG por cafe usan composicion 1200x630 con imagen de producto (si disponible) y fallback seguro.

---

## 3.5 Prerender de detalle cafe

Archivo: `webApp/scripts/prerender-coffee-pages.mjs`

Para cada `dist/coffee/<slug>/index.html`:

- canonical sin slash final
- `og:url` canonic
- `og:image` y `twitter:image` a `/og/coffee/<id>.jpg`
- `twitter:card=summary_large_image`

---

## 3.6 Sitemaps avanzados + gzip

Archivo: `webApp/scripts/update-sitemap-from-supabase.mjs`

Se generan:

- `sitemap-pages.xml.gz` (landing/search/login/legal)
- `sitemap-facets.xml.gz` (rutas limpias de facetas)
- `sitemap-coffee-*.xml.gz` + `sitemap-coffee.xml.gz`
- `image-sitemap.xml.gz`
- `sitemap.xml.gz` (index)

Y ademas:

- `lastmod` en cafes desde `updated_at` (fallback seguro si no existe columna)
- `lastmod` en estaticas con fecha de build
- `robots.txt` apunta a `sitemap.xml.gz`

---

## 3.7 Cache strategy SEO-first (Apache)

Archivo: `webApp/public/.htaccess`

Politica aplicada:

- `sw/register/workbox`: `no-cache, no-store`
- `/` e `/index.html`: cache corto
- `/coffee/<slug>`: cache largo + `stale-while-revalidate`
- `/search` y `/login`: cache medio
- `/assets/*.{js,css,woff2}`: cache largo immutable
- `/og/*.{jpg,jpeg,png,webp}`: cache largo immutable

---

## 3.8 CWV (render y red)

Archivos:

- `webApp/src/styles.css`
- `webApp/src/app/lazyViews.tsx`
- `webApp/index.html`
- `webApp/src/main.tsx`
- `webApp/src/styles/features.css`

Aplicado:

- **CSS split**:
  - `features.css` fuera del CSS inicial y cargado lazy con vistas
- **Critical CSS inline minimo** en `index.html`
- **`content-visibility: auto`** en listas largas
- **preconnect/dns-prefetch selectivo por consentimiento**:
  - Supabase: siempre (funcional)
  - GA/GTM/accounts: solo consentimiento `all`

---

## 4) Que aplica a SPA vs estaticas

- **SPA (`index.html` + React):**
  - hooks SEO runtime, robots dinamico, canonical dinamico, JSON-LD dinamico
- **Estaticas (`landing`, `legal`):**
  - SEO definido en HTML estatico: canonical, hreflang y JSON-LD `WebPage`
  - se benefician ademas de `.htaccess`, sitemaps, robots, cache strategy
  - no usan hooks runtime React (`useCoffeeSeoMeta`, limpieza de params, OG dinamico por ruta)

---

## 5) Pipeline de build/deploy

Archivo: `webApp/package.json`

Script recomendado para despliegue SEO completo:

- `npm run build:full`

Incluye:

1. `vite build`
2. `generate-og-images.mjs`
3. `prerender-coffee-pages.mjs`
4. `update-sitemap-from-supabase.mjs`

---

## 6) Validacion realizada

- `npm run build` OK
- `npm run build:full` OK
- generacion de OG por cafe verificada (centenares de ficheros)
- sitemaps `.xml.gz` generados y referenciados desde `sitemap.xml.gz`

---

## 7) Riesgos / observaciones

- En entorno local dev (`vite dev`), rutas `dist/og/*` pueden no estar disponibles.
- Las OG de produccion dependen de ejecutar `build:full` en pipeline de deploy.
- Si `coffees.updated_at` no existe, se usa fallback para no romper sitemap.

---

## 8) Checklist rapido post-deploy (PRO)

- [ ] `https://cafesitoapp.com/robots.txt` contiene `Sitemap: .../sitemap.xml.gz`
- [ ] `https://cafesitoapp.com/sitemap.xml.gz` responde 200
- [ ] `https://cafesitoapp.com/image-sitemap.xml.gz` responde 200
- [ ] `https://cafesitoapp.com/og/search.jpg` responde 200
- [ ] `https://cafesitoapp.com/og/coffee/<id>.jpg` responde 200 (para un id valido)
- [ ] `curl -I https://cafesitoapp.com/coffee/<slug>` devuelve cache-control esperado
- [ ] compartir un cafe en redes muestra OG image correcta

---

## 9) Relacion con release/deploy

- El checklist operativo de despliegue SEO vive en `docs/RELEASE_DEPLOY_WORKFLOW.md`.
- Este documento sigue siendo la referencia funcional y tecnica SEO de WebApp.

### Validacion rapida (resumen): Local vs PRO

| Item SEO | Local (build/dist) | PRO (dominio) |
|---|---|---|
| `build:full`, sitemaps y robots | Si | Si |
| Canonical/hreflang/JSON-LD en estaticas (`landing/legal`) | Si | Si |
| Metas SEO runtime SPA (hooks React) | Parcial | Si |
| OG images (`dist/og/*` y URL publicas) | Si (ficheros) | Si (HTTP 200) |
| `Cache-Control`, redirecciones 301, preview social | Limitado | Obligatorio |

