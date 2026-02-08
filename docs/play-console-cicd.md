# CI/CD para Google Play (GitHub Actions)

## Resumen
Este repositorio incluye un workflow que hace lo siguiente al hacer _merge_ a `main`:

1. Incrementa `versionCode` automáticamente.
2. Genera `versionName` con formato `YYYY.M.VV` (año.mes.version-del-día).
3. Compila un `AAB` de release.
4. Publica el release en Google Play (track configurable).
5. Genera el changelog a partir del PR asociado al merge.

## Configuración en Google Play Console

1. Ve a **Google Play Console → Configuración → Acceso a API**.
2. Crea un **Service Account** desde el enlace de Google Cloud.
3. En Google Cloud, crea una **Key** tipo JSON para el Service Account.
4. Vuelve a Play Console y **otorga permisos** al Service Account (por ejemplo, Release Manager).

## Secretos necesarios en GitHub

Configura estos secretos en GitHub (**Settings → Secrets and variables → Actions**):

- `GOOGLE_PLAY_JSON`: contenido completo del JSON de la Service Account.
- `ANDROID_KEYSTORE_BASE64`: keystore codificado en base64.
- `ANDROID_KEYSTORE_PASSWORD`: password del keystore.
- `ANDROID_KEY_ALIAS`: alias del key.
- `ANDROID_KEY_PASSWORD`: password del key.
- `GOOGLE_SERVICES_JSON`: contenido completo del archivo `google-services.json` (Firebase).

> Para convertir el keystore a base64:
>
> ```bash
> base64 -w 0 your-release.keystore
> ```
>
> Si copias el valor manualmente, asegúrate de pegarlo como una sola línea sin espacios ni saltos de línea.

## Variables opcionales

- `PLAY_TRACK`: define el track de Google Play (por defecto `beta`).
  - Valores típicos: `beta` (closed testing), `alpha`, `internal`, `production`.

## Notas

- El workflow sube el release en estado **draft** para que puedas revisarlo y publicarlo manualmente desde Play Console.
- También agrega un mensaje promocional en español (`whatsnew-es-ES`) que verán los usuarios en Google Play.
- El commit de bump de versión se hace con `[skip ci]` para evitar loops.
