# Workflow Release & Deploy

**Estado:** vivo  
**Última actualización:** 2026-03-04  
**Fuente de verdad:** comportamiento por rama y despliegue (Android + Web).

Workflow único de GitHub Actions (`.github/workflows/release-deploy.yml`) que gestiona el release de Android en Google Play y el despliegue de la web en Ionos según la rama.

## Cuándo se ejecuta

- **Push** a las ramas: `Interna`, `Alpha`, `Beta`, `Producción` (no a `main`).
- **Ejecución manual**: Actions → **Release & Deploy** → **Run workflow** → elegir rama.
- **Repository dispatch (Supabase):** el workflow también se dispara por el evento `supabase-coffees-changed`, que envía la Edge Function `trigger-coffees-build` cuando en Supabase está configurado un **Database Webhook** en la tabla `coffees` (Insert, Update, Delete). En ese caso el workflow se ejecuta **sin que hagas push**: cualquier alta, edición o baja de un café (desde la app, el dashboard de Supabase o cualquier cliente que escriba en esa tabla) activa el webhook y hace que se ejecute el job de deploy web (regenerar HTML estáticos y publicar en Ionos).

### Si no quieres que se ejecute solo por cambios en cafés

- Entra en **Supabase Dashboard → Database → Webhooks**.
- Busca un webhook asociado a la tabla **`public.coffees`** que llame a la Edge Function **`trigger-coffees-build`**.
- **Desactívalo** o **elimínalo**. A partir de ahí el workflow solo se ejecutará con push a las ramas de release o con "Run workflow" manual.

Si lo dejas activo, es normal que el workflow corra cada vez que alguien (o algo) cree, edite o borre un café en Supabase.

## Comportamiento por rama

| Rama         | Release Android (Play Console)     | Deploy web (Ionos)              |
|-------------|-------------------------------------|----------------------------------|
| **main**    | No se ejecuta el workflow           | —                                |
| **Interna** | Solo si hay cambios en `app/` o `shared/` → pruebas internas | No |
| **Alpha**   | Solo si hay cambios en `app/` o `shared/` → pruebas cerradas | **Siempre** → `/cafesito-web/app/` |
| **Beta**    | Solo si hay cambios en `app/` o `shared/` → pruebas abiertas | **Siempre** → `/cafesito-web/app/` |
| **Producción** | Solo si hay cambios en `app/` o `shared/` → producción | **Siempre** → `/cafesito-web/app/` |

- **Android**: no se sube por subir; solo se construye y se publica en Play cuando el push incluye cambios en `app/` o `shared/`.
- **Web**: en Alpha, Beta y Producción el job de deploy web **siempre** se ejecuta y sube el build de `webApp` al servidor Ionos en la ruta `/cafesito-web/app/`.

## Jobs del workflow

1. **changes**  
   Detecta si en el push hay cambios en:
   - `app/` o `shared/` → para decidir si se ejecuta release Android.
   - `webApp/` → se sigue calculando pero ya no condiciona el deploy web en Beta/Producción (siempre se despliega).

2. **release-android**  
   - Condición: rama en `Interna` / `Alpha` / `Beta` / `Producción` y cambios en `app/` o `shared/`.
   - Configura keystore y `google-services.json`, hace bump de versión, build del AAB y subida a la pista de Play correspondiente.
   - Sube también los **símbolos nativos** (`debugSymbols`) generados por el build para que Play Console pueda mostrar ANR y crashes de forma legible.
   - Las **notas de la versión** (What’s new) se generan de forma automática: se basan en la última versión anterior (último tag o último push), son promocionales y pensadas para el usuario que disfruta la app y el café, sin tecnicismos.

3. **deploy-web**  
   - Condición: rama `Alpha`, `Beta` o `Producción`.
   - Ejecuta `npm ci`, `npm test`, `npm run build` en `webApp` y sube el contenido de `webApp/dist/` por **SFTP** (SSH) a Ionos en `/cafesito-web/app/`.

## Forzar release sin push

Si quieres desplegar el estado actual de una rama sin tocar código:

1. Ve a **Actions** → **Release & Deploy**.
2. Pulsa **Run workflow**.
3. Elige la rama (Interna, Alpha, Beta, Producción).
4. En ejecución manual se asume que hay “cambios”, así que se ejecutan **release-android** y **deploy-web** (según la rama).

## Secretos necesarios

En **Settings → Secrets and variables → Actions**:

### Android / Play

- `GOOGLE_PLAY_JSON` – JSON del Service Account de Play.
- `ANDROID_KEYSTORE_BASE64` – Keystore en base64 (ver abajo).
- `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- `GOOGLE_SERVICES_JSON` – Contenido de `google-services.json`.

**Configuración en Google Play Console (una vez):**

1. Google Play Console → **Configuración → Acceso a API**.
2. Crear **Service Account** desde el enlace a Google Cloud.
3. En Google Cloud, crear una **Key** tipo JSON para el Service Account.
4. En Play Console, **otorgar permisos** al Service Account (ej. Release Manager).

**Keystore en base64:** `base64 -w 0 your-release.keystore` (o en PowerShell: codificar el binario). Pegar como una sola línea sin espacios ni saltos.

**Notas (Android):** El workflow sube el release en estado **draft** para revisión manual en Play Console. Añade mensaje promocional `whatsnew-es-ES`. El commit de bump de versión usa `[skip ci]` para evitar loops.

### Web (Ionos, SFTP)

El deploy usa **SFTP** (no FTP); Ionos suele ofrecer acceso por SSH/SFTP.

- `IONOS_SSH_HOST` – Host del servidor (ej. `ssh.tudominio.com` o la IP).
- `IONOS_SSH_USER` – Usuario SFTP/SSH.
- `IONOS_SSH_PASSWORD` – Contraseña.
- `IONOS_SSH_PORT` – Puerto (opcional; por defecto 22).

El deploy web sube a la ruta remota **`/cafesito-web/app/`**.

### Revisión de crashes (manual, desde Cursor)

No hay workflow automático. Los crashes se revisan y resuelven **desde Cursor**: tú pones el informe en `.github/crash-reports/weekly.json` (o generas `docs/crash-fixes/pending-review.md` con el script), pides aquí que se revisen y resuelvan, y **tú subes a Git** cuando quieras. Ver `docs/CRASH_FIX_WEEKLY.md`.

## Resumen rápido

- **main**: no hace nada.
- **Interna / Alpha**: solo release Android si cambian `app/` o `shared/`.
- **Alpha / Beta / Producción**: release Android si cambian `app/` o `shared/`; **deploy web siempre** a Ionos `/cafesito-web/app/`.
- Para forzar todo: **Run workflow** manual con la rama elegida.

## Registro de cambios de despliegue

Se documentan aquí los despliegues relevantes (push a main + Alpha/Beta/Producción) para trazabilidad.

| Fecha       | Ramas        | Descripción |
|------------|--------------|-------------|
| 2026-02-28 | main, Alpha  | **Webapp**: redirect OAuth con path completo (fix 500 PWA iOS), topbar hide on scroll down / show on scroll up, .htaccess excluye `registerSW.js`. Ver `docs/commit-notes/commit-20260228-webapp-main-alpha.md`. |
