# Estáticos (landing/legal), login SPA y CSS — Runbook para no repetir regresiones

**Estado:** vigente  
**Última actualización:** 2026-03-25  
**Ámbito:** `webApp/public/landing`, `webApp/public/legal`, pantalla de login (`LoginGate`), SEO runtime (`useCoffeeSeoMeta`), `AppContainer`  
**Relacionado:** `webApp/docs/SEO_WEBAPP_AVANZADO_2026-03.md`, `webApp/DEPLOY-IONOS.md`, `webApp/public/.htaccess`

---

## 1. Objetivo

Evitar que vuelvan a ocurrir estas regresiones:

1. **SEO `robots` / canonical en login:** `noindex` o canonical incorrecto cuando la URL visible es `/` y el SEO debe tratar la página como “login”.
2. **Login sin estilos / iconos rotos / banner de cookies roto:** falta de CSS al cargar solo `auth.css` o al no cargar `features.css` en la ruta de login.
3. **Landing “sin CSS ni imágenes”:** rutas relativas rotas cuando la URL canónica es `/landing` **sin barra final**.
4. **Legales:** favicon con ruta relativa frágil frente a `logo.png` en la raíz.

---

## 2. Login SPA: URL real `/` vs SEO `/login`

**Hecho:** La pantalla de login se muestra en la **raíz del SPA** (`/` o prefijo de app), no en una ruta dedicada `/login` en muchos navegaciones.

**Consecuencia:** `useCoffeeSeoMeta` usa `pathname`; si solo se considera “login” cuando el path coincide con `/login`, en `/` la política aplicaba `noindex, follow` como “resto SPA”.

**Solución implementada:** En `AppContainer`, después de `guestCanAccessCurrentTab`, se calcula `pathnameForSeo`: si el usuario está en la puerta de login (sin sesión, sin pestaña invitado, `authReady`), y la URL actual coincide con la raíz de la app (`getAppRootPath`), se pasa **`/login`** (o `/<prefijo>/login` en subpath) al hook `useCoffeeSeoMeta` para canonical, `robots`, OG y títulos coherentes con la página indexable de login.

**Archivos:** `webApp/src/app/AppContainer.tsx` (`pathnameForSeo`, `useCoffeeSeoMeta`).

**No revertir:** No sustituir solo por regex `/login` en el hook sin tener en cuenta la raíz; mantener el override desde el contenedor o documentar el mismo criterio en el hook.

---

## 3. LoginGate: importar `features.css`, no solo `auth.css`

**Hecho:** `features.css` importa `features/auth.css` y concentra muchas reglas que el login necesita:

- **Material Symbols:** `.material-symbol-icon`, variaciones FILL, etc. (los `UiIcon` con símbolos Material).
- **`.ui-icon` base** y reglas asociadas en features.
- **Banner de cookies:** `.cookie-consent-banner` y variantes.
- **Sheets móvil:** overlays y componentes relacionados con el flujo de login.

**Consecuencia:** Importar solo `auth.css` en `LoginGate` dejaba la maquetación parcial, iconos como texto o vacíos, y el consentimiento sin estilos.

**Solución:** `LoginGate` importa **`../../styles/features.css`** (una vez). El bundle inicial crece; es el trade-off correcto para paridad visual en la primera pintura.

**Archivo:** `webApp/src/features/auth/LoginGate.tsx`.

**Nota:** Las vistas lazy siguen importando `features.css`; Vite puede deduplicar en parte. No volver a “solo auth.css” en login sin extraer a un chunk mínimo compartido (trabajo futuro opcional).

---

## 4. Landing: rutas absolutas bajo `/landing/...`

**Hecho:** La URL canónica es `https://cafesitoapp.com/landing` **sin slash final**. Apache sirve `landing/index.html` para esa ruta.

**Consecuencia HTML:** Con el documento cargado como `.../landing` (sin `/` final), el navegador resuelve rutas relativas `css/styles.css` y `assets/logo.png` respecto al path “directorio” incorrecto, pidiendo por ejemplo **`/css/styles.css`** y **`/assets/...`** en lugar de `/landing/css/...` y `/landing/assets/...`.

**Solución:** En `webApp/public/landing/index.html`, usar **rutas absolutas desde la raíz del sitio:**

- `/landing/css/design-tokens.css`, `/landing/css/styles.css`
- `/landing/assets/...` (imágenes, favicon)
- `/landing/js/main.js`
- En estilos inline, `url('/landing/assets/...')` para avatares de testimonios.

**No depender** de “añadir slash final” o `<base href>` sin revisar anclas y enlaces externos.

---

## 5. Legales: favicon

**Hecho:** `href="../logo.png"` desde `/legal/*.html` suele resolver a `/logo.png`, pero es frágil si cambia el despliegue o la profundidad de URL.

**Solución:** Favicon con **`href="/logo.png"`** (logo en `public/logo.png` en raíz de dist).

**Archivos:** `webApp/public/legal/privacidad.html`, `condiciones.html`, `eliminacion-cuenta.html`.

---

## 6. Checklist antes de merge o deploy web

- [ ] **Login sin sesión:** DevTools → Network: `features.css` o chunk que incluya reglas de login; comprobar iconos Material y banner de cookies.
- [ ] **Meta:** En `/` con login visible, `meta robots` indexable según política y canonical acorde a `/login` si aplica.
- [ ] **Landing:** Abrir `https://<host>/landing` (sin barra final) → 200 en `/landing/css/styles.css`, `/landing/assets/logo.png`, `/landing/js/main.js`.
- [ ] **Legal:** Abrir cada HTML → favicon `/logo.png` con 200.
- [ ] **Apache:** Reglas `landing` en `webApp/public/.htaccess` sin cambios que rompan el paso de `landing/**` a ficheros estáticos.

---

## 7. Referencias rápidas de archivos

| Tema | Archivos |
|------|-----------|
| SEO runtime SPA | `webApp/src/hooks/domains/useCoffeeSeoMeta.ts` |
| Override pathname login | `webApp/src/app/AppContainer.tsx` |
| Login UI + CSS | `webApp/src/features/auth/LoginGate.tsx` |
| Landing estática | `webApp/public/landing/index.html`, `css/*`, `js/main.js`, `assets/*` |
| Legal estática | `webApp/public/legal/*.html` |
| Rewrite landing | `webApp/public/.htaccess` |
