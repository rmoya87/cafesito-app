# Workflow Release & Deploy

Workflow único de GitHub Actions (`.github/workflows/release-deploy.yml`) que gestiona el release de Android en Google Play y el despliegue de la web en Ionos según la rama.

## Cuándo se ejecuta

- **Push** a las ramas: `Interna`, `Alpha`, `Beta`, `Producción` (no a `main`).
- **Ejecución manual**: Actions → **Release & Deploy** → **Run workflow** → elegir rama.

## Comportamiento por rama

| Rama         | Release Android (Play Console)     | Deploy web (Ionos)              |
|-------------|-------------------------------------|----------------------------------|
| **main**    | No se ejecuta el workflow           | —                                |
| **Interna** | Solo si hay cambios en `app/` o `shared/` → pruebas internas | No |
| **Alpha**   | Solo si hay cambios en `app/` o `shared/` → pruebas cerradas | No |
| **Beta**    | Solo si hay cambios en `app/` o `shared/` → pruebas abiertas | **Siempre** → `cafesito-web/app` |
| **Producción** | Solo si hay cambios en `app/` o `shared/` → producción | **Siempre** → `cafesito-web/app` |

- **Android**: no se sube por subir; solo se construye y se publica en Play cuando el push incluye cambios en `app/` o `shared/`.
- **Web**: en Beta y Producción el job de deploy web **siempre** se ejecuta y sube el build de `webApp` al servidor Ionos en la ruta `cafesito-web/app`.

## Jobs del workflow

1. **changes**  
   Detecta si en el push hay cambios en:
   - `app/` o `shared/` → para decidir si se ejecuta release Android.
   - `webApp/` → se sigue calculando pero ya no condiciona el deploy web en Beta/Producción (siempre se despliega).

2. **release-android**  
   - Condición: rama en `Interna` / `Alpha` / `Beta` / `Producción` y cambios en `app/` o `shared/`.
   - Configura keystore y `google-services.json`, hace bump de versión, build del AAB y subida a la pista de Play correspondiente.

3. **deploy-web**  
   - Condición: rama `Beta` o `Producción`.
   - Ejecuta `npm ci`, `npm test`, `npm run build` en `webApp` y sube el contenido de `webApp/dist/` por FTP a Ionos en `cafesito-web/app`.

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
- `ANDROID_KEYSTORE_BASE64` – Keystore en base64.
- `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- `GOOGLE_SERVICES_JSON` – Contenido de `google-services.json`.

### Web (Ionos)

- `IONOS_SSH_HOST` – Host/servidor FTP o SFTP (ej. `ftp.tudominio.com`).
- `IONOS_SSH_USER` – Usuario FTP/SFTP.
- `IONOS_SSH_PASSWORD` – Contraseña.
- `IONOS_SSH_PORT` – Puerto (opcional; si no se define, se usa el por defecto del protocolo).

El deploy web sube a la ruta remota **`cafesito-web/app`**.

## Resumen rápido

- **main**: no hace nada.
- **Interna / Alpha**: solo release Android si cambian `app/` o `shared/`.
- **Beta / Producción**: release Android si cambian `app/` o `shared/`; **deploy web siempre** a Ionos `cafesito-web/app`.
- Para forzar todo: **Run workflow** manual con la rama elegida.
