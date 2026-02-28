# Webhook: cambios en cafés → disparar build y deploy web

Cuando se **crea**, **actualiza** o **elimina** un café en Supabase, puedes disparar automáticamente el workflow de GitHub Actions para regenerar las páginas estáticas y desplegar de nuevo la web.

## Comportamiento

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
