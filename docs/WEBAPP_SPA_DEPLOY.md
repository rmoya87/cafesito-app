# Webapp: prerender de cafés, SEO y despliegue

## No depender de Git para altas/bajas de cafés

Los workflows **generan HTML estáticos por café** (prerender con caché) para buen SEO y previews al compartir. Para que un **alta, baja o edición de café en Supabase** actualice la web **sin hacer push a Git**:

1. **Configura en Supabase** un **Database Webhook** en la tabla `coffees` (eventos Insert, Update, Delete) que llame a la **Edge Function** `trigger-coffees-build`.
2. Esa Edge Function hace `repository_dispatch` a GitHub con la rama que quieras (p. ej. Beta o Alpha).
3. Se ejecuta el workflow **Release & Deploy** en esa rama: build, obtiene la lista de cafés de Supabase (ya con el cambio), detecta que la lista cambió (cache miss), regenera los HTMLs y despliega.

Así, al dar de alta o borrar un café en Supabase, en unos minutos la web queda actualizada sin tocar Git.

## Comportamiento del prerender

- En cada deploy se calcula un **hash de la lista de cafés** en Supabase. Si ese hash (y el código de la webapp) no cambió, se **reutiliza la caché** de `dist/coffee` y solo se actualizan sitemap/robots. Si cambió, se ejecuta el prerender completo.
- Las rutas `/coffee/slug/` tienen HTML estático con `<title>`, meta description, og:image y JSON-LD para SEO y previews en redes.
- El servidor (Ionos) debe tener **SPA fallback** para rutas que no tengan HTML estático (p. ej. `/profile/usuario`).

## Requisito en el servidor (Ionos)

Para rutas sin HTML estático y para refrescar en cliente, el servidor debe devolver `index.html` cuando no exista archivo (SPA fallback):

- **Nginx:** `try_files $uri $uri/ /index.html;` (ajustar ruta base si aplica).
- **Apache:** `FallbackResource /index.html`.
- **Panel Ionos:** Si hay "Página 404" o "Rutas no encontradas", apuntarla a `index.html`.

## Workflows (GitHub Actions)

- **Deploy Webapp** y **Release & Deploy**: build, hash de cafés, caché de `dist/coffee`, prerender solo si cache miss, actualización de sitemap/robots si cache hit, deploy.
- **Webhook**: Supabase (tabla `coffees`) → Edge Function `trigger-coffees-build` → `repository_dispatch` → Release & Deploy en la rama configurada. Así los cambios de contenido disparan el deploy sin Git.
