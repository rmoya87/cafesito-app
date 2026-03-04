# Workflow Release & Deploy

**Estado:** vivo  
**Última actualización:** 2026-03-04  
**Fuente de verdad:** comportamiento por rama y despliegue (Android + Web).

Workflow único de GitHub Actions (`.github/workflows/release-deploy.yml`) que gestiona el release de Android en Google Play y el despliegue de la web en Ionos según la rama.

## Cuándo se ejecuta

- **Programado**: todos los días a las **03:00 UTC** (equivale a las **04:00** en España peninsular en invierno).
- El workflow ya no se dispara por `push`, ni por ejecución manual (`workflow_dispatch`), ni por `repository_dispatch` de Supabase.
- La rama objetivo del deploy nocturno se define con la variable de repositorio `NIGHTLY_DEPLOY_BRANCH` (por defecto: `Beta`).

### Si no quieres que se ejecute solo por cambios en cafés

- Entra en **Supabase Dashboard → Database → Webhooks**.
- Busca un webhook asociado a la tabla **`public.coffees`** que llame a la Edge Function **`trigger-coffees-build`**.
- **Opcional:** puedes dejarlo activo por trazabilidad, pero ya no lanza despliegues inmediatos.

Si lo dejas activo, la función responderá en modo diferido y el despliegue se aplicará en la ventana nocturna.

## Comportamiento por rama

| Rama         | Release Android (Play Console)     | Deploy web (Ionos)              |
|-------------|-------------------------------------|----------------------------------|
| **main**    | No se ejecuta el workflow           | —                                |
| **Interna** | Solo si hay cambios en `app/` o `shared/` → pruebas internas | No |
| **Alpha**   | Solo si hay cambios en `app/` o `shared/` → pruebas cerradas | **Siempre** → `/cafesito-web/app/` |
| **Beta**    | Solo si hay cambios en `app/` o `shared/` → pruebas abiertas | **Siempre** → `/cafesito-web/app/` |
| **Producción** | Solo si hay cambios en `app/` o `shared/` → producción | **Siempre** → `/cafesito-web/app/` |

- **Android**: se construye y publica dentro de la ventana nocturna programada.
- **Web**: en Alpha, Beta y Producción el job de deploy web se ejecuta en la misma ventana nocturna y sube el build de `webApp` a `/cafesito-web/app/`.

## Jobs del workflow

1. **changes**  
   Consulta `consume-deploy-changes` en Supabase y decide:
   - `web=true` solo si hay pendientes en `deploy_change_events`.
   - `android=false` por defecto (evita releases diarios de Play sin cambios).

2. **release-android**  
   - Condición: rama en `Interna` / `Alpha` / `Beta` / `Producción` (según `NIGHTLY_DEPLOY_BRANCH`).
   - Configura keystore y `google-services.json`, hace bump de versión, build del AAB y subida a la pista de Play correspondiente.
   - Sube también los **símbolos nativos** (`debugSymbols`) generados por el build para que Play Console pueda mostrar ANR y crashes de forma legible.
   - Las **notas de la versión** (What’s new) se generan de forma automática: se basan en la última versión anterior (último tag o último push), son promocionales y pensadas para el usuario que disfruta la app y el café, sin tecnicismos.

3. **deploy-web**  
   - Condición: rama `Alpha`, `Beta` o `Producción` (según `NIGHTLY_DEPLOY_BRANCH`).
   - Ejecuta `npm ci`, `npm test`, `npm run build` en `webApp` y sube el contenido de `webApp/dist/` por **SFTP** (SSH) a Ionos en `/cafesito-web/app/`.

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

### Cola de cambios Supabase (deploy nocturno)

- `SUPABASE_DEPLOY_QUEUE_URL` – URL de la Edge Function `consume-deploy-changes`.
- `SUPABASE_DEPLOY_QUEUE_TOKEN` – token compartido para proteger esa función (cabecera `x-deploy-token`).

### Revisión de crashes (manual, desde Cursor)

No hay workflow automático. Los crashes se revisan y resuelven **desde Cursor**: tú pones el informe en `.github/crash-reports/weekly.json` (o generas `docs/crash-fixes/pending-review.md` con el script), pides aquí que se revisen y resuelvan, y **tú subes a Git** cuando quieras. Ver `docs/CRASH_FIX_WEEKLY.md`.

## Resumen rápido

- **main**: no hace nada.
- **Interna / Alpha**: solo release Android si cambian `app/` o `shared/`.
- **Alpha / Beta / Producción**: release Android si cambian `app/` o `shared/`; **deploy web siempre** a Ionos `/cafesito-web/app/`.
- Para cambiar la rama nocturna, ajusta `NIGHTLY_DEPLOY_BRANCH` en Variables de GitHub Actions.

## Registro de cambios de despliegue

Se documentan aquí los despliegues relevantes (push a main + Alpha/Beta/Producción) para trazabilidad.

| Fecha       | Ramas        | Descripción |
|------------|--------------|-------------|
| 2026-02-28 | main, Alpha  | **Webapp**: redirect OAuth con path completo (fix 500 PWA iOS), topbar hide on scroll down / show on scroll up, .htaccess excluye `registerSW.js`. Ver `docs/commit-notes/commit-20260228-webapp-main-alpha.md`. |
