# Despliegue en Ionos

## Configuración del hosting

- **En el servidor:** los ficheros de la web están en la ruta **/cafesito-web/app** (SFTP: `remote_path` del deploy).
- **Dominio:** cafesitoapp.com tiene esa carpeta (**app**) como document root: la web se sirve desde la carpeta app (las URLs son `/`, `/coffee/xxx`, `/timeline`, etc., no `/cafesito-web/app/...`).
- El `.htaccess` debe usar **RewriteBase /** (la raíz ya es la carpeta app). El workflow no parchea el `.htaccess`; se sube con `RewriteBase /`.

## 500 al hacer Ctrl+F5 (recarga forzada) en una ruta SPA

Si al hacer **Ctrl+F5** en una URL como `/coffee/achicoria-expres-el-chimbo` o `/timeline` ves **500** en la primera recarga (y al hacer F5 otra vez funciona), la causa suele ser el fallback SPA.

- El `.htaccess` incluye **ErrorDocument 404 index.html** para que, cuando no exista fichero, se sirva la SPA en lugar del handler por defecto del host (que a veces devuelve 500).
- Debe tener **RewriteBase /** (correcto cuando el dominio apunta a la carpeta app como document root). Si en el servidor hubiera `RewriteBase /cafesito-web/app/`, las reescrituras irían mal y podrías ver 500.
- **Comprobar en el servidor:** Por SFTP, abre `/cafesito-web/app/.htaccess` y verifica que tenga `RewriteBase /` y `ErrorDocument 404 index.html`.

## 500 en /profile/usuario o /favicon.ico

Si ves **500 (Internal Server Error)** al abrir `https://cafesitoapp.com/profile/ramonmoyaromero` o `https://cafesitoapp.com/favicon.ico`, la causa está en la **configuración del servidor**, no en el código:

1. **La raíz del dominio no es la carpeta de la app**  
   El workflow sube la webapp a `/cafesito-web/app/`. Si el **document root** de `cafesitoapp.com` es otra carpeta (p. ej. `public_html`), las peticiones a `/profile/xxx` o `/favicon.ico` no llegan al `index.html` de la SPA y el servidor puede devolver 500.

2. **Qué hacer en Ionos**
   - **Opción A (recomendada):** Que el **document root** del dominio (o del subdominio) apunte a la carpeta donde está la app, es decir donde están `index.html` y el `.htaccess` (p. ej. la carpeta que contiene `cafesito-web/app/` o la propia `app/` si reestructuras). Así el `.htaccess` del proyecto se aplica y las rutas sin fichero físico devuelven `index.html` (SPA fallback) con 200.
   - **Opción B:** Si la raíz no puede ser esa carpeta, en la raíz debe haber un **fallback SPA**: que cualquier ruta que no sea un fichero estático devuelva **200** con el contenido de `index.html` de la app (no 500). En Apache suele ser `FallbackResource /cafesito-web/app/index.html` (o la ruta correcta). En el panel de Ionos, revisar "Página de error 404" / "Rutas no encontradas" y que no apunte a un script que falle (eso genera 500).

3. **favicon.ico**  
   La app usa `./logo.png` como icono. Algunos navegadores piden igualmente `/favicon.ico`. Si el document root es la carpeta de la app, puedes añadir un `favicon.ico` ahí (o un redirect en el servidor a `logo.png`) para evitar 500 en esa petición.

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
- **Botón Google (y PWA «Añadir a página de inicio» en iOS)**: El redirect tras el login va a la **raíz de la app** (`{VITE_SITE_URL}/`). Así no depende de que el servidor resuelva `/timeline` como SPA fallback.
  - En Supabase → Authentication → URL Configuration:
    - **Site URL**: la URL pública de la app, p. ej. `https://cafesitoapp.com` (raíz) o `https://cafesitoapp.com/cafesito-web/app` si está en subdirectorio.
    - **Redirect URLs**: añade como mínimo la raíz de la app, p. ej. `https://cafesitoapp.com/` (y opcionalmente `https://cafesitoapp.com/timeline` si tu hosting soporta fallback SPA correctamente).
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

### Google Analytics 4 (GA4): que registre datos

Si en GA4 no aparece ningún evento o página:

1. **ID de medición**  
   La app usa la variable de entorno `VITE_GA4_MEASUREMENT_ID`. Si no está definida, no se carga el script ni se envía nada a GA4.

2. **Dónde está el ID en GA4**  
   En **Google Analytics** → **Admin** → **Flujo de datos** (o **Data streams**) → tu flujo web → **ID de medición**. Tiene el formato `G-XXXXXXXXX`.

3. **Configuración**  
   - **En local:** en `webApp/.env` añade por ejemplo `VITE_GA4_MEASUREMENT_ID=G-XXXXXXXXX` (sustituye por tu ID). Reinicia `npm run dev`.  
   - **En producción (GitHub Actions):** en el repo **Settings → Secrets and variables → Actions** añade una **Variable** (o Secret) `VITE_GA4_MEASUREMENT_ID` con el valor `G-XXXXXXXXX`. En el siguiente deploy el build incluirá ese ID y GA4 empezará a recibir eventos.

4. **Comprobar**  
   Con el ID configurado, abre la web, cambia de sección (timeline, explorar, etc.) y en GA4 → **Informes** → **Tiempo real** deberían aparecer usuarios y páginas vistas al cabo de unos segundos.

### CSP y fuente externa

Si en consola aparece un aviso de **Content Security Policy** sobre una fuente en `r2cdn.perplexity.ai`: suele ser una extensión del navegador (p. ej. Perplexity) que inyecta esa fuente. En este proyecto la CSP ya permite ese origen en `font-src` para evitar el aviso; la app no usa esa fuente.
