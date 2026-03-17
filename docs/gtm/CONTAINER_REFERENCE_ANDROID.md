# Referencia del contenedor GTM Android (GTM-T9WDBCBR)

Configuración de referencia para el contenedor **móvil** (Android) de Cafesito. Los eventos llegan al dataLayer desde `AnalyticsHelper.kt` cuando `GTM_CONTAINER_ID` está definido. Para que el contenedor se ejecute en la app hay que cargarlo (p. ej. con `loadContainerPreferNonDefault` y un recurso por defecto); ver [docs/ANALITICAS.md](../ANALITICAS.md) §5 y §9.

---

## 1. Eventos en el dataLayer (Android)

| Evento              | Cuándo                          | Parámetros / datos                         |
|---------------------|----------------------------------|--------------------------------------------|
| gtm_platform_ready  | Inicio de AnalyticsHelper       | platform: "android"                        |
| screen_view         | Cada cambio de pantalla         | screen_name, screen_class                   |
| set_user_id         | Login / logout                  | user_id                                    |
| login_success       | Tras login correcto             | is_new_user (Boolean)                      |
| profile_completed   | Tras completar perfil           | —                                          |
| notification_action_* | Acciones en notificaciones   | —                                          |
| carousel_nav        | (Si se añade trackEvent en app) | carousel_id, direction                      |
| modal_open / modal_close | (Si se añade trackEvent en app) | modal_id                             |
| button_click        | Clics en botones (ej. elaboración) | button_id                               |

En la app Android se envían a Firebase y al dataLayer: `screen_view`, `set_user_id`, `login_success`, `profile_completed`, `notification_action_*`, **button_click** (Brew Lab y otras pantallas) y **modal_open** / **modal_close** en todas las pantallas con modales. Opcional: `carousel_nav` en carruseles.

**Valores de `modal_id` enviados por la app:**

| Pantalla / contexto | modal_id |
|---------------------|----------|
| Detalle café        | `review`, `stock_edit`, `sensory_profile`, `create_list`, `add_to_list` |
| Perfil              | `sensory_detail`, `profile_create_list`, `settings`, `delete_confirm_account`, `delete_confirm_review` |
| Opciones de lista   | `list_edit`, `delete_confirm_list`, `leave_list_confirm` |
| Diario              | `diary_stock_edit`, `diary_pantry_options`, `diary_delete_confirm_pantry`, `diary_finished_confirm`, `diary_period`, `diary_entry_edit`, `diary_date_picker` |
| Home / Inicio (timeline) | `timeline_stock_edit`, `timeline_pantry_options`, `timeline_delete_confirm_pantry`, `timeline_finished_confirm` |
| Añadir stock        | `add_stock_confirm` |
| Añadir café/despensa| `add_pantry_image_picker`, `add_pantry_option_picker` |
| Búsqueda            | `search_filter` |
| Login               | `login_sheet` |

**Valores de `button_id` (ejemplos):** `brew_select_coffee`, `brew_add_to_pantry`, `brew_create_coffee`, `brew_next_step`, `brew_save_to_diary`.

---

## 2. Variables (contenedor móvil)

En el contenedor Android GTM no existe «Variable de capa de datos»; se usa **Firebase → Parámetro de evento** con el nombre del parámetro que envía la app:

| Nombre           | Tipo en GTM Android      | Nombre del parámetro |
|------------------|--------------------------|------------------------|
| DLV - screen_name | Firebase → Parámetro de evento | screen_name |
| DLV - screen_class | Firebase → Parámetro de evento | screen_class |
| DLV - user_id    | Firebase → Parámetro de evento | user_id  |
| DLV - platform   | Firebase → Parámetro de evento | platform |
| DLV - carousel_id | Firebase → Parámetro de evento | carousel_id |
| DLV - direction  | Firebase → Parámetro de evento | direction |
| DLV - modal_id   | Firebase → Parámetro de evento | modal_id |
| DLV - button_id  | Firebase → Parámetro de evento | button_id |

---

## 3. Disparadores

| Nombre        | Tipo                  | Configuración                 |
|---------------|-----------------------|-------------------------------|
| CE - screen_view | Evento personalizado | Nombre del evento: `screen_view` |
| CE - set_user_id  | Evento personalizado | Nombre del evento: `set_user_id`  |
| CE - gtm_platform_ready | Evento personalizado | Nombre del evento: `gtm_platform_ready` |
| CE - login_success | Evento personalizado | Nombre del evento: `login_success` |
| CE - profile_completed | Evento personalizado | Nombre del evento: `profile_completed` |
| CE - carousel_nav | Evento personalizado | Nombre del evento: `carousel_nav` |
| CE - modal_open   | Evento personalizado | Nombre del evento: `modal_open` |
| CE - modal_close  | Evento personalizado | Nombre del evento: `modal_close` |
| CE - button_click | Evento personalizado | Nombre del evento: `button_click` |

---

## 4. Etiquetas típicas (Firebase / GA4)

En un contenedor móvil GTM suele haber:

1. **Firebase Analytics – Configuración** (o GA4 según plantillas disponibles en el contenedor móvil): User ID = `{{DLV - user_id}}`, user property `platform` = `android`.
2. **Etiqueta de evento** que se dispare con **CE - screen_view** y envíe a Firebase/GA4 el evento `screen_view` con `screen_name` y `screen_class`.
3. **Etiqueta** que reaccione a **CE - set_user_id** y actualice el User ID en los siguientes hits.
4. Etiquetas opcionales para `login_success`, `profile_completed`, etc., que envíen esos eventos a GA4/Firebase.

La documentación oficial de GTM para Android indica cómo mapear eventos del dataLayer a Firebase; el contenedor debe estar publicado y, en la app, cargado (p. ej. con recurso por defecto en `res/raw`).

---

## 5. Export real

El export en JSON del contenedor Android se obtiene desde GTM: **Admin** → **Export Container** → elegir versión y descargar. Ese archivo es el que se puede importar en otro contenedor o conservar en el repositorio.
