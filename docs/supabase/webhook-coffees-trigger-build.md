# Deploy web: prerender de cafés, SEO y webhook

**Estado:** vigente  
**Última actualización:** 2026-03-04  
**Ámbito:** WebApp (Ionos), prerender, SEO, actualizar web sin Git.

Documento único para: comportamiento del prerender, requisitos en el servidor, y cómo disparar el build/deploy cuando cambian los cafés en Supabase (sin push a Git).

---

## Actualizar la web sin Git (altas/bajas/edición de cafés)

Los workflows generan **HTML estáticos por café** (prerender con caché) para SEO y previews. El despliegue ahora es **programado**:

1. El workflow **Release & Deploy** se ejecuta cada día a las **03:00 UTC** (04:00 en España peninsular en invierno).
2. La rama objetivo se define con `NIGHTLY_DEPLOY_BRANCH` (por defecto `Beta`).
3. Los cambios en Supabase se publican en ese siguiente ciclo nocturno.

La Edge Function `trigger-coffees-build` se mantiene por compatibilidad con webhooks, pero ya no dispara `repository_dispatch` inmediato.

---

## Comportamiento del prerender

- En cada deploy se calcula un **hash de la lista de cafés** en Supabase. Si ese hash (y el código de la webapp) no cambió, se reutiliza la caché de `dist/coffee` y solo se actualizan sitemap/robots. Si cambió, se ejecuta el prerender completo.
- Las rutas `/coffee/slug/` tienen HTML estático con `<title>`, meta description, og:image y JSON-LD para SEO y previews en redes.
- El servidor (Ionos) debe tener **SPA fallback** para rutas sin HTML estático (ej. `/profile/usuario`): devolver `index.html` cuando no exista archivo.

**Requisitos en el servidor (Ionos):**

- **Nginx:** `try_files $uri $uri/ /index.html;` (ajustar ruta base si aplica).
- **Apache:** `FallbackResource /index.html`.
- **Panel Ionos:** Si hay "Página 404" o "Rutas no encontradas", apuntarla a `index.html`.

---

## Comportamiento por evento (crear/eliminar café)

- **Crear café:** el siguiente deploy regenera el prerender e incluye la nueva página en `dist/coffee/<slug>/index.html`.
- **Eliminar café:** el siguiente deploy ya no genera esa página; la carpeta desaparece. Si alguien entra en la URL antigua, la app carga (SPA) y **redirige al buscador** (`/search`).

## Cómo disparar el workflow desde Supabase

### Opción 1: Edge Function + Database Webhook (recomendado)

1. Despliega la Edge Function `trigger-coffees-build` (ver `edge-functions/trigger-coffees-build/`).
2. En Supabase Dashboard → Database → Webhooks, crea un webhook en la tabla `public.coffees`:
   - Events: **Insert**, **Update**, **Delete**
   - Type: **Supabase Edge Functions**
   - Function: `trigger-coffees-build`
3. No necesitas configurar `GITHUB_PAT` ni `GITHUB_REPO` para despliegue inmediato.
4. Opcionalmente, puedes enviar `payload.branch` para trazabilidad; el deploy efectivo seguirá siendo nocturno.

### Opción 2: Configurar rama nocturna

Si quieres cambiar la rama que se despliega por la noche, define en GitHub Actions Variables:

- `NIGHTLY_DEPLOY_BRANCH=Alpha` (o `Beta`, `Producción`, `Interna`).

## Redirección cuando el café ya no existe

Si un usuario tiene guardada la URL de un café que luego se eliminó:

1. El servidor sirve `index.html` (SPA) para esa ruta.
2. La app arranca, resuelve la ruta y consulta si el slug existe en la lista de cafés.
3. Si el café no existe (eliminado o nunca existió), la app hace **redirección a la pestaña Buscador** (`/search`) con `window.location.replace`, para que no quede en una página vacía.

Esto está implementado en `useCoffeeRouteSync` (webApp).
