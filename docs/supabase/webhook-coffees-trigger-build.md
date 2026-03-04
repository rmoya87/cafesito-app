# Deploy web: prerender de cafés, SEO y webhook

**Estado:** vigente  
**Última actualización:** 2026-03-04  
**Ámbito:** WebApp (Ionos), prerender, SEO, actualizar web sin Git.

Documento único para: comportamiento del prerender, requisitos en el servidor, y cómo disparar el build/deploy cuando cambian los cafés en Supabase (sin push a Git).

---

## Actualizar la web sin Git (altas/bajas/edición de cafés)

Los workflows generan **HTML estáticos por café** (prerender con caché) para SEO y previews. Para que un alta, baja o edición de café en Supabase actualice la web **sin hacer push a Git**:

1. **Configura en Supabase** un Database Webhook en la tabla `coffees` (Insert, Update, Delete) que llame a la Edge Function `trigger-coffees-build`.
2. Esa Edge Function hace `repository_dispatch` a GitHub con la rama elegida (ej. Beta o Alpha).
3. Se ejecuta el workflow **Release & Deploy** en esa rama: build, obtiene la lista de cafés, detecta cache miss si cambió, regenera HTMLs y despliega.

Ver sección **Cómo disparar el workflow desde Supabase** más abajo para el detalle.

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
3. En Supabase → Edge Functions → `trigger-coffees-build` → Secrets, configura:
   - `GITHUB_PAT`: token de GitHub con scope `repo` (o al menos `workflow`).
   - `GITHUB_REPO`: `owner/repo` (ej. `tu-usuario/cafesito-app-android`).

La función llamará a la API de GitHub para disparar el evento `repository_dispatch` con `event_type: supabase-coffees-changed` y `client_payload: { branch: "Beta" }`, de modo que se ejecute el job de deploy web sobre la rama Beta.

### Opción 2: Llamar a la API de GitHub desde tu backend

Si tienes otro servicio que deba reaccionar a cambios en `coffees`, puedes hacer un `POST` a:

```
https://api.github.com/repos/OWNER/REPO/dispatches
```

Headers:

- `Authorization: token TU_GITHUB_PAT`
- `Accept: application/vnd.github.v3+json`

Body:

```json
{
  "event_type": "supabase-coffees-changed",
  "client_payload": {
    "branch": "Beta"
  }
}
```

Usa `"Producción"` en `branch` si quieres desplegar desde la rama Producción.

### Opción 3: Disparo manual

En GitHub → Actions → "Release & Deploy" → "Run workflow", elige la rama (por ejemplo Beta) y ejecuta. Regenerará y desplegará la web igual que el webhook.

## Redirección cuando el café ya no existe

Si un usuario tiene guardada la URL de un café que luego se eliminó:

1. El servidor sirve `index.html` (SPA) para esa ruta.
2. La app arranca, resuelve la ruta y consulta si el slug existe en la lista de cafés.
3. Si el café no existe (eliminado o nunca existió), la app hace **redirección a la pestaña Buscador** (`/search`) con `window.location.replace`, para que no quede en una página vacía.

Esto está implementado en `useCoffeeRouteSync` (webApp).
