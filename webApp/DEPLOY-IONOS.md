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

Tienes **dos opciones** para configurar Supabase:

1. **Build con variables de entorno (recomendado)**  
   Crea en `webApp/` un archivo `.env` con:
   ```
   VITE_SUPABASE_URL=https://TU_PROYECTO.supabase.co
   VITE_SUPABASE_ANON_KEY=eyJ...tu_anon_key...
   ```
   Luego ejecuta `npm run build` en esa carpeta. Las variables se embeben en el build y no hace falta tocar el HTML en el servidor.

2. **Config en runtime (sin volver a hacer build)**  
   Si no puedes pasar env en el build, la app puede leer la config desde `window.__SUPABASE_CONFIG__`. En el **index.html desplegado** (el que está en `dist/` o en el servidor), añade **antes** del `<script>` que carga la app algo así:
   ```html
   <script>
   window.__SUPABASE_CONFIG__ = {
     url: "https://TU_PROYECTO.supabase.co",
     anonKey: "eyJ...tu_anon_key..."
   };
   </script>
   ```
   Así el login con Google funcionará sin recompilar.

### Login: video de fondo y botón «Continuar con Google»

- **Video de fondo**: Usa ruta relativa y reproducción programática (incl. iOS con `webkit-playsinline`). Si en iOS no se reproduce, el navegador puede estar bloqueando el autoplay hasta la primera interacción.
- **Botón Google (y PWA «Añadir a página de inicio» en iOS)**: El redirect tras el login va a **timeline**. Si la app está en un subdirectorio (p. ej. `/cafesito-web/app/`), **VITE_SITE_URL debe ser la URL base completa incluyendo ese path** (p. ej. `https://cafesitoapp.com/cafesito-web/app`). Si no, al abrir desde «página de inicio» en iOS el redirect puede ir a `https://cafesitoapp.com/timeline` y el servidor devolverá 404/500.
  - En Supabase → Authentication → URL Configuration:
    - **Site URL**: la URL pública de la app (con path si aplica), p. ej. `https://cafesitoapp.com/cafesito-web/app`.
    - **Redirect URLs**: añade la URL de timeline y la raíz de la app, p. ej. `https://cafesitoapp.com/cafesito-web/app/timeline`, `https://cafesitoapp.com/cafesito-web/app/`.
  - En CI (GitHub Actions) se usa `VITE_SITE_URL` (por defecto `https://cafesitoapp.com/cafesito-web/app`). Define la variable `VITE_SITE_URL` en el repo si tu despliegue usa otro path.

### CSP y fuente externa

Si en consola aparece un aviso de **Content Security Policy** sobre una fuente en `r2cdn.perplexity.ai`: suele ser una extensión del navegador (p. ej. Perplexity) que inyecta esa fuente. En este proyecto la CSP ya permite ese origen en `font-src` para evitar el aviso; la app no usa esa fuente.
