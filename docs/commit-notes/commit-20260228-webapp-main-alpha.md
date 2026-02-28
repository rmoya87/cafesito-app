# Commit 20260228 - Webapp: redirect OAuth, topbar scroll, .htaccess

## Desplegado en
- **main** (push directo)
- **Alpha** (merge desde main + push)

## Resumen
Ajustes de la webapp para corregir el error "Internal Server Error" al iniciar sesión con Google desde PWA en iOS ("Añadir a página de inicio") y mejorar el comportamiento del topbar al hacer scroll.

## Cambios

### 1. Redirect OAuth después del login (PWA / iOS)
- **Problema**: Al abrir la app desde "Añadir a página de inicio" en iOS, el redirect tras Google apuntaba a la raíz (`/timeline`) y el servidor devolvía 500.
- **Solución**: `VITE_SITE_URL` se usa como URL base completa (incl. path). En CI el valor por defecto es `https://cafesitoapp.com/cafesito-web/app`. El redirect va siempre a la URL correcta de timeline.
- **Archivos**: `webApp/src/hooks/domains/useAuthSession.ts`, `.github/workflows/release-deploy.yml`, `webApp/DEPLOY-IONOS.md`

### 2. Topbar: ocultar al bajar, mostrar al subir
- **Comportamiento**: Scroll down → topbar se oculta; scroll up → topbar se muestra. Cerca del tope (24 px) siempre visible.
- **Mejora**: Delta mínimo de 12 px para evitar parpadeos.
- **Archivo**: `webApp/src/app/AppContainer.tsx`

### 3. .htaccess
- Exclusión de `registerSW.js` en la reescritura para evitar 500 en Ionos.
- **Archivo**: `webApp/public/.htaccess`

## Archivos tocados
- `.github/workflows/release-deploy.yml`
- `webApp/DEPLOY-IONOS.md`
- `webApp/public/.htaccess`
- `webApp/src/app/AppContainer.tsx`
- `webApp/src/hooks/domains/useAuthSession.ts`

## Nota
El deploy efectivo a Ionos se dispara al hacer push a **Alpha** (y Beta/Producción). El push a **main** solo actualiza la rama; el push a **Alpha** ejecuta el workflow y sube `webApp/dist/` a `/cafesito-web/app/`.
