# Referencia del contenedor GTM Web (GTM-WLXN93VK)

Configuración completa para replicar o validar el contenedor **web** de Cafesito. ID de medición GA4 por defecto: **G-BMZEQNRKR4** (sustituir si usas otro).

---

## 0. Contrato dataLayer (WebApp → GTM)

La WebApp hace `dataLayer.push(...)` con estas formas. Las **claves** deben coincidir con los nombres de las variables DLV en GTM:

| Evento (`event`) | Claves en el push | Origen en código |
|------------------|-------------------|------------------|
| `page_view` | `page_path`, `page_location`, `screen_name`, `page_title` (opcional) | `gtm.ts` → `pushPageView()` |
| `set_user_id` | `user_id` (string; vacío en logout) | `gtm.ts` → `pushUserId()` |
| `gtm_platform_ready` | `platform`: `"web"` | `gtm.ts` → `initGtm()` |
| `modal_open` / `modal_close` | `modal_id` | `ga4.ts` → `sendEvent("modal_open"|"modal_close", { modal_id })` |
| `button_click` | `button_id` | `sendEvent("button_click", { button_id })` |
| `carousel_nav` | `carousel_id`, `direction` | `sendEvent("carousel_nav", { carousel_id, direction })` |

Tests que validan este contrato: **unit** `webApp/src/core/gtm.test.ts`; **e2e** `webApp/e2e/dataLayer.spec.ts` (comprueba en navegador que al cargar y al abrir/cerrar modal se hace push de `page_view` y `modal_open`/`modal_close`). Ejecutar e2e con `npm run test:e2e:dataLayer` (o con el dev server ya levantado: `npx playwright test dataLayer.spec.ts`).

---

## 1. Variables (Definidas por el usuario)

| Nombre            | Tipo                  | Configuración                          |
|-------------------|-----------------------|----------------------------------------|
| DLV - page_path   | Variable de capa de datos | Nombre de la clave: `page_path`   |
| DLV - screen_name | Variable de capa de datos | Nombre de la clave: `screen_name` (normalizado, paridad con Android) |
| DLV - page_title  | Variable de capa de datos | Nombre de la clave: `page_title`  |
| DLV - page_location | Variable de capa de datos | Nombre de la clave: `page_location` |
| DLV - user_id     | Variable de capa de datos | Nombre de la clave: `user_id`     |
| DLV - platform    | Variable de capa de datos | Nombre de la clave: `platform`    |
| DLV - carousel_id | Variable de capa de datos | Nombre de la clave: `carousel_id` |
| DLV - direction   | Variable de capa de datos | Nombre de la clave: `direction`   |
| DLV - modal_id    | Variable de capa de datos | Nombre de la clave: `modal_id`    |
| DLV - button_id   | Variable de capa de datos | Nombre de la clave: `button_id`   |

---

## 2. Disparadores (Triggers)

| Nombre            | Tipo                  | Configuración                          |
|-------------------|-----------------------|----------------------------------------|
| CE - page_view    | Evento personalizado  | Nombre del evento: `page_view`         |
| CE - set_user_id  | Evento personalizado  | Nombre del evento: `set_user_id`       |
| CE - gtm_platform_ready | Evento personalizado | Nombre del evento: `gtm_platform_ready` |
| CE - carousel_nav | Evento personalizado  | Nombre del evento: `carousel_nav`      |
| CE - modal_open   | Evento personalizado  | Nombre del evento: `modal_open`        |
| CE - modal_close  | Evento personalizado  | Nombre del evento: `modal_close`       |
| CE - button_click | Evento personalizado  | Nombre del evento: `button_click`      |

---

## 3. Etiquetas (Tags)

### 3.1 GA4 – Configuración

- **Tipo:** Google Analytics: configuración de GA4  
- **ID de medición:** G-BMZEQNRKR4  
- **User ID:** `{{DLV - user_id}}` (opcional, solo cuando existe)  
- **Propiedades de usuario:**  
  - Nombre: `platform` | Valor: `web` (o `{{DLV - platform}}`)  
- **Activación:** Disparador **All Pages** (o evento `gtm_platform_ready`)

### 3.2 GA4 – Evento page_view

- **Tipo:** Google Analytics: evento de GA4  
- **Configuración:** Referencia a la etiqueta "GA4 – Configuración"  
- **Nombre del evento:** `page_view`  
- **Parámetros de evento:**  
  - `page_path` = `{{DLV - page_path}}`  
  - `page_title` = `{{DLV - page_title}}`  
  - `page_location` = `{{DLV - page_location}}`  
- **Activación:** Disparador **CE - page_view**

### 3.2b GA4 – Evento screen_view (paridad con Android)

Para que GA4 reciba el mismo evento y los mismos nombres de parámetros que desde Android en navegación (p. ej. `screen_name` = "detail" en detalle café, no la URL completa):

- **Tipo:** Google Analytics: evento de GA4  
- **Configuración:** Referencia a "GA4 – Configuración"  
- **Nombre del evento:** `screen_view`  
- **Parámetros de evento:**  
  - `screen_name` = `{{DLV - screen_name}}` (la WebApp envía este valor normalizado en cada `page_view`)  
  - `screen_class` = `{{DLV - page_title}}`  
- **Activación:** Disparador **CE - page_view**

### 3.3 GA4 – set_user_id

- **Tipo:** Google Analytics: configuración de GA4 (o evento GA4 que envíe user_id)  
- **User ID:** `{{DLV - user_id}}` (establecer en el hit)  
- **Activación:** Disparador **CE - set_user_id**

### 3.4 GA4 – Evento carousel_nav

- **Tipo:** Google Analytics: evento de GA4  
- **Configuración:** Referencia a "GA4 – Configuración"  
- **Nombre del evento:** `carousel_nav`  
- **Parámetros:** `carousel_id` = `{{DLV - carousel_id}}`, `direction` = `{{DLV - direction}}`  
- **Activación:** **CE - carousel_nav**

### 3.5 GA4 – Evento modal_open

- **Tipo:** Google Analytics: evento de GA4  
- **Configuración:** Referencia a "GA4 – Configuración"  
- **Nombre del evento:** `modal_open`  
- **Parámetros:** `modal_id` = `{{DLV - modal_id}}`  
- **Activación:** **CE - modal_open**

### 3.6 GA4 – Evento modal_close

- **Tipo:** Google Analytics: evento de GA4  
- **Configuración:** Referencia a "GA4 – Configuración"  
- **Nombre del evento:** `modal_close`  
- **Parámetros:** `modal_id` = `{{DLV - modal_id}}`  
- **Activación:** **CE - modal_close**

### 3.7 GA4 – Evento button_click

- **Tipo:** Google Analytics: evento de GA4  
- **Configuración:** Referencia a "GA4 – Configuración"  
- **Nombre del evento:** `button_click`  
- **Parámetros:** `button_id` = `{{DLV - button_id}}`  
- **Activación:** **CE - button_click**

---

## 4. Paridad con Android (GA4)

En GA4 llegarán los **mismos nombres de evento y de parámetros** desde web y Android si el contenedor Web incluye la etiqueta **GA4 - screen_view** (apartado 3.2b):

| Concepto        | Android (dataLayer/Firebase) | Web (dataLayer + GTM)        |
|-----------------|------------------------------|-----------------------------|
| Navegación      | `screen_view` + `screen_name`, `screen_class` | `screen_view` (etiqueta en CE - page_view) + `screen_name` = page_path, `screen_class` = page_title |
| Usuario         | `set_user_id` + `user_id`    | Igual                      |
| Plataforma      | `platform`: android          | `platform`: web            |
| Modales         | `modal_open` / `modal_close` + `modal_id` | Igual |
| Botones         | `button_click` + `button_id` | Igual                      |
| Carrusel        | `carousel_nav` + `carousel_id`, `direction` | Igual |

Además, la web sigue enviando el evento estándar `page_view` con `page_path`, `page_title`, `page_location` para informes web.

---

## 5. Resumen de eventos enviados desde el código (WebApp)

| Evento              | Origen                    | Parámetros típicos                          |
|---------------------|---------------------------|---------------------------------------------|
| gtm_platform_ready  | initGtm()                 | platform: "web"                             |
| page_view           | sendPageView()            | page_path, page_title, page_location       |
| set_user_id         | pushUserId()              | user_id                                     |
| carousel_nav        | sendEvent("carousel_nav")  | carousel_id, direction                      |
| modal_open          | sendEvent("modal_open")   | modal_id                                    |
| modal_close         | sendEvent("modal_close")  | modal_id                                    |
| button_click        | sendEvent("button_click") | button_id                                   |

**Valores de `carousel_id` en web:** `brew_despensa`, `home_elaboration`, `home_despensa`, `home_recommendations`, `home_suggestions`.

**Valores de `modal_id` en web:**

| Contexto           | modal_id |
|--------------------|----------|
| Detalle café       | `add_to_list`, `create_list`, `review`, `stock_edit`, `sensory_profile` |
| Perfil / listas    | `profile_create_list`, `sensory_detail`, `list_edit`, `delete_confirm_list`, `leave_list_confirm` |
| TopBar / cuenta    | `profile_options`, `delete_confirm_account` |
| Login              | `login_sheet` |
| Diario             | `diary_barista_list`, `diary_pantry_options`, `diary_stock_edit`, `diary_finished_confirm`, `diary_delete_confirm_pantry` |
| Home / timeline    | `timeline_pantry_options`, `timeline_stock_edit`, `timeline_finished_confirm`, `timeline_delete_confirm_pantry` |
| Búsqueda           | `search_filter` |

**Valores de `button_id` en web:** `brew_select_coffee`, `brew_add_to_pantry`, `brew_create_coffee`, `brew_next_step`, `brew_save_to_diary`.
