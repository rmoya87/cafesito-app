# Workflow Release & Deploy

**Estado:** vivo  
**Última actualización:** 2026-03-05  
**Fuente de verdad:** comportamiento por rama y despliegue (Android + Web).

Workflow único de GitHub Actions (`.github/workflows/release-deploy.yml`) que gestiona el release de Android en Google Play y el despliegue de la web en Ionos según la rama.

## Cuándo se ejecuta

- **Push** a ramas: `Interna`, `Interno`, `alpha`, `beta`, `Producción`, `Produccion` (las ramas canónicas son **alpha** y **beta** en minúsculas). **main** no está en la lista: un push a main no dispara el workflow (y el job `changes` tiene `if` que excluye push a main).
- **workflow_dispatch**: ejecución manual desde Actions → "Run workflow", eligiendo rama y opciones (solo Android, solo web, etc.).
- **Programado (schedule)**: todos los días a las **03:00 UTC** (deploy nocturno). La rama objetivo se define con la variable de repositorio `NIGHTLY_DEPLOY_BRANCH` (por defecto: `beta`).

### Si no quieres que se ejecute solo por cambios en cafés

- Entra en **Supabase Dashboard → Database → Webhooks**.
- Busca un webhook asociado a la tabla **`public.coffees`** que llame a la Edge Function **`trigger-coffees-build`**.
- **Opcional:** puedes dejarlo activo por trazabilidad, pero ya no lanza despliegues inmediatos.

Si lo dejas activo, la función responderá en modo diferido y el despliegue se aplicará en la ventana nocturna.

## Comportamiento por rama y por ficheros (push)

En **push a alpha o beta**: siempre se ejecutan **release-android** y **deploy-web** (así, al hacer merge de main en beta se despliega aunque el último commit solo toque docs o workflow).

En **push a otras ramas** (Interna, Producción): solo se ejecuta cada job si hay **ficheros modificados** que impactan a ese target (detección con `dorny/paths-filter`):

- **Release Android** se ejecuta solo si cambian ficheros de Android: `app/**`, `shared/**`, `gradle/**`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle-wrapper.properties`, `libs.versions.toml`.
- **Deploy web** se ejecuta solo si cambian ficheros de la webapp: `webApp/**`.

En **workflow_dispatch** (manual) y en **schedule** (nocturno) no se usa filtro por ficheros: el usuario elige “solo Android” / “solo web” en manual, y en schedule la cola de Supabase decide si hay deploy web.

| Rama         | Release Android (Play Console)     | Deploy web (Ionos)              |
|-------------|-------------------------------------|----------------------------------|
| **main**    | No se ejecuta el workflow           | —                                |
| **Interna** | Solo si hay ficheros que impactan Android → pruebas internas | No (rama no incluida en deploy web) |
| **alpha**   | **Siempre** en push → pruebas cerradas | **Siempre** en push → `/cafesito-web/app/` |
| **beta**    | **Siempre** en push → pruebas abiertas | **Siempre** en push → `/cafesito-web/app/` |
| **Producción** | Solo si hay ficheros que impactan Android → producción | Solo si hay ficheros que impactan webapp → `/cafesito-web/app/` |

- **alpha/beta**: en cada push se despliegan Android y web para que un merge desde main dispare el despliegue completo.
- **Otras ramas**: Android y web solo si el push incluye ficheros que impactan a cada uno (o en manual/schedule según configuración).

## Jobs del workflow

1. **changes**  
   Decide qué jobs ejecutar (`android` / `web`):
   - **Push a alpha o beta:** siempre `android=true` y `web=true` (siempre se despliegan ambos).
   - **Push a otras ramas:** usa `dorny/paths-filter` sobre los ficheros modificados: `android=true` si hay ficheros que impactan Android, `web=true` si hay ficheros que impactan `webApp/`.
   - **workflow_dispatch:** el usuario elige “solo Android”, “solo web” o ambos.
   - **schedule:** consulta `consume-deploy-changes` en Supabase; `web=true` solo si hay pendientes en la cola; `android=false` en schedule (evita releases diarios de Play sin push).

2. **release-android**  
   - Condición: rama en `Interna` / `alpha` / `beta` / `Producción` (según `NIGHTLY_DEPLOY_BRANCH`).
   - Configura keystore y `google-services.json`, hace bump de versión, build del AAB y subida a la pista de Play correspondiente.
   - Sube también los **símbolos nativos** (`debugSymbols`) generados por el build para que Play Console pueda mostrar ANR y crashes de forma legible.
   - Las **notas de la versión** (What’s new) se generan de forma automática: se basan en la última versión anterior (último tag o último push), son promocionales y pensadas para el usuario que disfruta la app y el café, sin tecnicismos.

3. **deploy-web**  
   - Condición: rama `alpha`, `beta` o `Producción` (según `NIGHTLY_DEPLOY_BRANCH`).
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
- **Push a Interna / alpha / beta / Producción**: release Android **solo si** hay ficheros que impactan Android; deploy web **solo si** hay ficheros que impactan la webapp (`webApp/`). Si cambian ficheros de ambos, se despliegan ambos.
- **workflow_dispatch**: eliges manualmente “solo Android”, “solo web” o ambos (sin filtro por ficheros).
- **schedule**: deploy web según cola Supabase; Android no se publica en schedule.
- Para cambiar la rama nocturna, ajusta `NIGHTLY_DEPLOY_BRANCH` en Variables de GitHub Actions.

## Registro de cambios de despliegue

Se documentan aquí los despliegues relevantes (push a main + Alpha/Beta/Producción) para trazabilidad.

| Fecha       | Ramas        | Descripción |
|------------|--------------|-------------|
| 2026-02-28 | main, Alpha  | **Webapp**: redirect OAuth con path completo (fix 500 PWA iOS), topbar hide on scroll down / show on scroll up, .htaccess excluye `registerSW.js`. Ver `docs/commit-notes/commit-20260228-webapp-main-alpha.md`. |
