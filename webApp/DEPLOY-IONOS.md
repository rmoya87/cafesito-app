# Despliegue en Ionos

## Errores 500 en assets / registerSW.js / logo.png

Si al desplegar en Ionos ves **500** en `registerSW.js`, `assets/index-*.js`, `index-*.css` o `logo.png`, suele deberse a:

- El servidor reescribe todas las peticiones (incluidas las de estáticos) a un script que devuelve 500.
- La app está en un **subdirectorio** (p. ej. `https://cafesitoapp.com/app/`) pero los recursos se piden a la raíz (`/assets/`, `/logo.png`) y ahí no existen o pasan por un handler que falla.

### Solución aplicada en el proyecto

- **Base relativa (`base: "./"`)**: Los recursos (JS, CSS, logo, PWA) se piden con rutas relativas.
- **`.htaccess`**: Excluye `registerSW.js` y `assets/` de la reescritura para que el servidor no devuelva 500. Así, si la app está en `https://cafesitoapp.com/app/`, todo se carga desde `/app/` (p. ej. `/app/assets/...`, `/app/logo.png`) y el servidor sirve los ficheros que realmente subiste.
- **`.htaccess`**: Reglas para no reescribir `/assets/` ni peticiones a `.js`, `.css`, `.png`, etc., y que el resto vaya a `index.html` (SPA).

### Qué hacer al desplegar

1. **Build**
   ```bash
   cd webApp
   npm ci
   npm run build
   ```

2. **Subir todo el contenido de `dist/`**
   - Si la app va en la **raíz** del dominio: sube el contenido de `dist/` a la raíz del sitio en Ionos (p. ej. `public_html`).
   - Si la app va en un **subdirectorio** (p. ej. `https://cafesitoapp.com/app/`): sube el contenido de `dist/` **dentro** de la carpeta `app/` (así que en el servidor existan `app/index.html`, `app/assets/`, `app/logo.png`, `app/registerSW.js`, etc.).

3. **Incluir el `.htaccess`**
   - Vite ya copia `public/.htaccess` a `dist/`. Asegúrate de subir también el `.htaccess` que queda en `dist/` al mismo nivel que `index.html` (raíz del sitio o dentro de `app/`).

4. **Comprobar en Ionos**
   - Si Ionos desactiva `.htaccess` (AllowOverride), las reglas no se aplicarán. En ese caso, usa el panel de Ionos para configurar reescritura o contacta con soporte para que los estáticos se sirvan sin pasar por PHP.

### «Faltan VITE_SUPABASE_URL y/o VITE_SUPABASE_ANON_KEY»

Ese mensaje aparece cuando la app se ejecuta sin la configuración de Supabase.

- **En local (npm run dev)**  
  Asegúrate de tener `webApp/.env` con `VITE_SUPABASE_URL` y `VITE_SUPABASE_ANON_KEY`, de ejecutar desde la carpeta `webApp/` (`npm run dev`) y de **reiniciar el servidor** después de crear o cambiar el `.env`. Si usas `npm run preview`, estás sirviendo el build (que se generó sin .env si lo hiciste en CI); para probar con .env usa `npm run dev`.

- **En producción / CI**  
  Si despliegas con **GitHub Actions**, añade en el repo **Settings → Secrets and variables → Actions** las variables (o secrets) `VITE_SUPABASE_URL` y `VITE_SUPABASE_ANON_KEY`; el workflow las inyecta en el build. Si no están ahí, el build se genera sin ellas y el botón de Google mostrará ese error en producción.

**Configuración de Supabase en producción:** usa siempre variables de entorno en el pipeline de build (GitHub Actions → Secrets o Variables: `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`). El build las embebe; no añadas credenciales en el HTML ni en el repositorio.

En local, crea `webApp/.env` con `VITE_SUPABASE_URL` y `VITE_SUPABASE_ANON_KEY`, ejecuta desde `webApp/` y reinicia el servidor tras cambiar el `.env`.

### Login: video de fondo y botón «Continuar con Google»

- **Video de fondo**: Usa ruta relativa y reproducción programática (incl. iOS con `webkit-playsinline`). Si en iOS no se reproduce, el navegador puede estar bloqueando el autoplay hasta la primera interacción.
- **Botón Google (y PWA «Añadir a página de inicio» en iOS)**: El redirect tras el login va a **{VITE_SITE_URL}/timeline**. Por defecto la app está en la raíz del dominio.
  - En Supabase → Authentication → URL Configuration:
    - **Site URL**: la URL pública de la app, p. ej. `https://cafesitoapp.com` (raíz) o `https://cafesitoapp.com/cafesito-web/app` si está en subdirectorio.
    - **Redirect URLs**: añade la URL de timeline y la raíz de la app, p. ej. `https://cafesitoapp.com/timeline`, `https://cafesitoapp.com/` (si la app está en raíz).
  - En CI (GitHub Actions) `VITE_SITE_URL` por defecto es `https://cafesitoapp.com`. Si la app está en un subdirectorio, define la variable `VITE_SITE_URL` en el repo con la URL completa (ej. `https://cafesitoapp.com/cafesito-web/app`).

### Desarrollo local: configurar Supabase para que el login funcione

Aunque tu `webApp/.env` tenga los mismos datos que producción, **Supabase debe autorizar las URLs de localhost** como redirección tras el login. Si no, Google te redirige pero Supabase rechaza la URL y el login falla.

1. **Supabase Dashboard** → **Authentication** → **URL Configuration**.
2. En **Redirect URLs** añade (una línea por URL):
   - `http://localhost:4173/timeline`
   - `http://localhost:4173/`
   - `http://localhost:4174/timeline`
   - `http://localhost:4174/`
   (4173 es el puerto por defecto de la webapp; si Vite usa otro, añade ese puerto también.)
3. **Guarda** los cambios.
4. Opcional: en **Site URL** puedes dejar la de producción; Supabase usa la lista de Redirect URLs para aceptar a dónde redirigir.

**Clave anon**: La anon key de Supabase suele ser un JWT largo que empieza por `eyJ...`. Si en tu `.env` usas otro formato y el login falla, copia la clave desde **Supabase → Settings → API → Project API keys → anon public**.

### CSP y fuente externa

Si en consola aparece un aviso de **Content Security Policy** sobre una fuente en `r2cdn.perplexity.ai`: suele ser una extensión del navegador (p. ej. Perplexity) que inyecta esa fuente. En este proyecto la CSP ya permite ese origen en `font-src` para evitar el aviso; la app no usa esa fuente.
