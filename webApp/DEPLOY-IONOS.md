# Despliegue en Ionos

## Errores 500 en assets / registerSW.js / logo.png

Si al desplegar en Ionos ves **500** en `registerSW.js`, `assets/index-*.js`, `index-*.css` o `logo.png`, suele deberse a:

- El servidor reescribe todas las peticiones (incluidas las de estáticos) a un script que devuelve 500.
- La app está en un **subdirectorio** (p. ej. `https://cafesitoapp.com/app/`) pero los recursos se piden a la raíz (`/assets/`, `/logo.png`) y ahí no existen o pasan por un handler que falla.

### Solución aplicada en el proyecto

- **Base relativa (`base: "./"`)**: Los recursos (JS, CSS, logo, PWA) se piden con rutas relativas. Así, si la app está en `https://cafesitoapp.com/app/`, todo se carga desde `/app/` (p. ej. `/app/assets/...`, `/app/logo.png`) y el servidor sirve los ficheros que realmente subiste.
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

### Login: video de fondo y botón «Continuar con Google»

- **Video de fondo**: Usa ruta relativa (`base`) y reproducción programática al cargar, para que funcione en el servidor y en móviles.
- **Botón Google**: Si al pulsar no ocurre nada, revisa:
  1. **Variables de entorno en el build**: La build debe tener `VITE_SUPABASE_URL` y `VITE_SUPABASE_ANON_KEY`. Si faltan, se mostrará el mensaje de error bajo el botón.
  2. **URL de redirección en Supabase**: En el proyecto de Supabase → Authentication → URL Configuration, añade en **Redirect URLs** la URL exacta de tu app en producción, por ejemplo:
     - `https://cafesitoapp.com/`
     - o `https://cafesitoapp.com/app/` si la app está en un subdirectorio.
     Sin esa URL, tras iniciar sesión con Google el usuario no podrá volver correctamente.

### CSP y fuente externa

Si en consola aparece un aviso de **Content Security Policy** sobre una fuente en `r2cdn.perplexity.ai`: suele ser una extensión del navegador (p. ej. Perplexity) que inyecta esa fuente. En este proyecto la CSP ya permite ese origen en `font-src` para evitar el aviso; la app no usa esa fuente.
