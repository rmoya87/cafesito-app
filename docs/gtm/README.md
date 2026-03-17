# Referencia de contenedores GTM — Cafesito

**Fuente de verdad sobre analíticas (flujo, código, GA4, checklists):** [docs/ANALITICAS.md](../ANALITICAS.md).

En esta carpeta está la **referencia de configuración** de los contenedores GTM (Web y Android) para replicar o validar la instalación. El export real en formato JSON se hace desde la interfaz de Google Tag Manager.

## Cómo obtener un export real

1. **Web (GTM-WLXN93VK):** Entra en [Tag Manager](https://tagmanager.google.com/) → elige el contenedor **Web** → **Admin** → **Export Container** → elige la versión publicada o la del workspace actual → descarga el `.json`.
2. **Android (GTM-T9WDBCBR):** Mismo flujo en el contenedor **Android** (móvil).

Ese JSON es el que puedes **importar** en otro contenedor (Admin → Import Container) o guardar en control de versiones.

## Configurar el contenedor Web a mano (paso a paso)

**Sigue esta guía en orden:** [**GUIA_PASO_A_PASO_WEB.md**](GUIA_PASO_A_PASO_WEB.md) — Variables de capa de datos → Disparadores (evento personalizado) → Etiquetas GA4 → Publicar. Contenedor: **GTM-WLXN93VK**.

## Configurar el contenedor Android a mano (paso a paso)

**Sigue esta guía en orden:** [**GUIA_PASO_A_PASO_ANDROID.md**](GUIA_PASO_A_PASO_ANDROID.md) — Variables → Disparadores → Etiquetas → Publicar. No hace falta importar ningún JSON.

---

## Qué hay en esta carpeta

- **GUIA_PASO_A_PASO_WEB.md** — Guía paso a paso para configurar el contenedor Web (GTM-WLXN93VK) a mano: variables, disparadores y etiquetas GA4.
- **GUIA_PASO_A_PASO_ANDROID.md** — Guía paso a paso para configurar el contenedor Android (GTM-T9WDBCBR) a mano en la interfaz de GTM.
- **CONTAINER_REFERENCE_WEB.md** — Listado completo de variables, disparadores y etiquetas del contenedor **web** para replicar a mano o comprobar que no falta nada.
- **CONTAINER_REFERENCE_ANDROID.md** — Equivalente para el contenedor **Android** (móvil).
- **GTM-T9WDBCBR-android-import.json** — JSON para **importar** en el contenedor Android (GTM-T9WDBCBR). Incluye la estructura del contenedor y **cuatro variables** de capa de datos (DLV - screen_name, screen_class, user_id, platform). Los disparadores se crean a mano. Si al importar aparece *«Tipo de entidad desconocido (ID público de plantilla: j)»*, ver nota más abajo para obtener el tipo correcto.

### Cómo importar el JSON en el contenedor Android

1. Entra en [Tag Manager](https://tagmanager.google.com/) y abre el contenedor **Android** (GTM-T9WDBCBR).
2. **Admin** → **Import Container** → elige el archivo **GTM-T9WDBCBR-android-import.json**.
3. Elige **Combinar** (Merge) o **Sobrescribir** (Overwrite).
4. Confirma. Si la importación incluyó las variables, solo queda **crear los disparadores a mano** (Evento personalizado: screen_view, set_user_id, etc.) y las etiquetas Firebase/GA4 según **CONTAINER_REFERENCE_ANDROID.md**. Si no se importaron las variables por el error de plantilla "j", créalas a mano también.

Si en el futuro GTM cambia la estructura o los IDs, exporta de nuevo el contenedor desde GTM y sustituye el bloque `containerVersion` por el tuyo. Si prefieres no importar JSON, crea variables y disparadores a mano siguiendo **CONTAINER_REFERENCE_ANDROID.md**.

### Si aparece «Tipo de entidad desconocido (ID público de plantilla: j)»

En contenedores **web** la "Variable de capa de datos" usa el tipo **`j`**; en Android (ANDROID_SDK_5) ese ID puede no existir. Si al importar ves ese error: (1) Crea **una sola** variable de tipo "Variable de capa de datos" en el contenedor Android (clave por ejemplo `screen_name`), guarda y publica o guarda en workspace. (2) **Export Container** y abre el JSON. (3) En `containerVersion.variable` busca esa variable y copia el valor de **`type`**. (4) Sustituye en **GTM-T9WDBCBR-android-import.json** todos los `"type": "j"` por ese valor, vuelve a importar. Los disparadores siguen creándose a mano (el tipo de activador del export web no es compatible con Android).

### Valores de enum que exige la importación GTM

| Dónde | Valor correcto | Evitar |
|-------|----------------|--------|
| `container.usageContext` (Android) | `["ANDROID_SDK_5"]` | `["ANDROID"]`, `["android"]` |
| `customEventFilter[].type` | `"EQUALS"` | `"eq"`, `"equals"` |
| `parameter[].type` | `"TEMPLATE"` | `"template"` |
| `trigger.type` | `"CUSTOM_EVENT"` | minúsculas |

La configuración GA4 (User-ID, plataforma, etc.) y el contexto general están en **[docs/ANALITICAS.md](../ANALITICAS.md)**.
