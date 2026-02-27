# Despliegue en Ionos

## Errores 500 en assets / registerSW.js / manifest

Si al desplegar en Ionos ves **500** en `registerSW.js`, `assets/index-*.js`, `index-*.css`, `logo.png` o `manifest.webmanifest`, suele deberse a que el servidor (Apache) no está sirviendo esos ficheros como estáticos y los reescribe o los pasa a un script que falla.

### Qué hacer

1. **Subir el contenido de `dist/`, no la carpeta `dist`**
   - En la raíz del sitio (p. ej. `public_html` o la carpeta que asigne Ionos a tu dominio) deben estar:
     - `index.html`
     - carpeta `assets/` (con los .js y .css con hash)
     - `logo.png`, `favicon.svg`
     - `sw.js`, `registerSW.js`, `manifest.webmanifest` (generados por Vite PWA)
     - `.htaccess` (incluido en este proyecto en `public/`, Vite lo copia a `dist/` al hacer build)

2. **Comprobar que `.htaccess` está en la raíz del sitio**
   - El `.htaccess` hace que las peticiones a ficheros que **existen** se sirvan como estáticos y que el resto vaya a `index.html` (SPA). Si no está, créalo desde `webApp/public/.htaccess` o vuelve a hacer build y subir.

3. **Build local**
   ```bash
   cd webApp
   npm ci
   npm run build
   ```
   Luego sube **todo** lo que haya dentro de `webApp/dist/` a la raíz del sitio en Ionos (FTP o gestor de archivos).

4. **Si la app está en un subdirectorio** (p. ej. `https://tudominio.com/app/`)
   - En `vite.config.ts` define `base: '/app/'`.
   - Vuelve a hacer `npm run build` y sube el contenido de `dist/` dentro de la carpeta `app/` en el servidor.

### CSP y fuente externa

Si en consola aparece un aviso de **Content Security Policy** sobre una fuente en `r2cdn.perplexity.ai`: suele ser una extensión del navegador (p. ej. Perplexity) que inyecta esa fuente. En este proyecto la CSP ya permite ese origen en `font-src` para evitar el aviso; la app no usa esa fuente.
