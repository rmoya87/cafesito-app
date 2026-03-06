# App Links (Android)

El fichero `assetlinks.json` permite que los enlaces a `https://cafesitoapp.com` abran la app Android cuando esté instalada.

## Cómo completar

1. Entra en **Google Play Console** → tu app → **Configuración** → **Integridad de la app**.
2. En **Clave de firma de la aplicación**, copia el **SHA-256** del certificado.
3. Sustituye en `assetlinks.json` la línea `"REPLACE_WITH_SHA256_FROM_PLAY_CONSOLE"` por ese valor.
   - Si Play te muestra el fingerprint con dos puntos (ej. `AA:BB:CC:...`), puedes dejarlo con dos puntos o quitarlos; ambos formatos son válidos.
4. Despliega la web para que `https://cafesitoapp.com/.well-known/assetlinks.json` devuelva este JSON con `Content-Type: application/json`.

Ver también: `docs/ASO_PLAY_STORE.md` (sección 3.2).
