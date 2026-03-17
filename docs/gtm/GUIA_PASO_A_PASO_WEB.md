# Guía paso a paso: configurar el contenedor GTM Web (GTM-WLXN93VK)

Sigue estos pasos en orden en [Tag Manager](https://tagmanager.google.com/), con el contenedor **Web** (GTM-WLXN93VK) abierto.

La WebApp envía a `dataLayer`: `gtm_platform_ready`, `page_view`, `set_user_id`, `modal_open`, `modal_close`, `button_click` y `carousel_nav`. Con esta guía configuras variables, disparadores y etiquetas GA4 para que esos eventos lleguen a tu propiedad GA4 (ID de medición por defecto: **G-BMZEQNRKR4**).

**Paridad con Android:** En Android se envía `screen_view` con `screen_name` y `screen_class`; en web se envía `page_view` con `page_path`, `page_title`, `page_location`. Para que GA4 reciba el **mismo nombre de evento y los mismos nombres de parámetros** en ambos canales (informes unificados), además de la etiqueta `page_view` se configura una etiqueta **GA4 - screen_view** que envía el evento `screen_view` con `screen_name` y `screen_class` cuando la web dispara `page_view` (paso 3.2b).

---

## Parte 1: Variables

En el contenedor **Web** las variables que leen el dataLayer son de tipo **Variable de capa de datos** (Data Layer Variable). La clave es el nombre que usa la WebApp al hacer `dataLayer.push({ event: "page_view", page_path: "...", ... })`.

### Paso 1.1 — Ir a Variables

1. Menú izquierdo → **Variables**.
2. En **Variables definidas por el usuario**, haz clic en **Nueva**.

### Paso 1.2 — Variable «DLV - page_path»

1. **Configuración de la variable** → **Variable de capa de datos** (en **Variables de capa de datos** o *Data Layer Variable*).
2. **Nombre de la clave de la capa de datos**: `page_path` (tal cual, la WebApp envía este nombre).
3. **Nombre** de la variable (arriba): `DLV - page_path`.
4. **Guardar**.

### Paso 1.3 — Variable «DLV - screen_name»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Variable de capa de datos** → **Nombre de la clave**: `screen_name` (la WebApp lo envía normalizado en cada `page_view` para paridad con Android; p. ej. "detail" en detalle café).
3. **Nombre** de la variable: `DLV - screen_name`. **Guardar**.

### Paso 1.4 — Variable «DLV - page_title»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Variable de capa de datos** → **Nombre de la clave**: `page_title`.
3. **Nombre** de la variable: `DLV - page_title`. **Guardar**.

### Paso 1.5 — Variable «DLV - page_location»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Variable de capa de datos** → **Nombre de la clave**: `page_location`.
3. **Nombre** de la variable: `DLV - page_location`. **Guardar**.

### Paso 1.6 — Variable «DLV - user_id»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Variable de capa de datos** → **Nombre de la clave**: `user_id`.
3. **Nombre** de la variable: `DLV - user_id`. **Guardar**.

### Paso 1.7 — Variable «DLV - platform»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Variable de capa de datos** → **Nombre de la clave**: `platform`.
3. **Nombre** de la variable: `DLV - platform`. **Guardar**.

### Paso 1.8 — Variable «DLV - modal_id»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Variable de capa de datos** → **Nombre de la clave**: `modal_id`.
3. **Nombre** de la variable: `DLV - modal_id`. **Guardar**.

### Paso 1.9 — Variable «DLV - button_id»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Variable de capa de datos** → **Nombre de la clave**: `button_id`.
3. **Nombre** de la variable: `DLV - button_id`. **Guardar**.

### Paso 1.10 (opcional) — Variable «DLV - carousel_id»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Variable de capa de datos** → **Nombre de la clave**: `carousel_id`.
3. **Nombre** de la variable: `DLV - carousel_id`. **Guardar**.

### Paso 1.11 (opcional) — Variable «DLV - direction»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Variable de capa de datos** → **Nombre de la clave**: `direction`.
3. **Nombre** de la variable: `DLV - direction`. **Guardar**.

Al terminar la Parte 1 tendrás 9 variables (o 11 si añades carrusel).

---

## Parte 2: Disparadores

En GTM Web los disparadores para eventos del dataLayer son de tipo **Evento personalizado** (Custom Event). El **nombre del evento** debe coincidir exactamente con el que envía la WebApp en `event: "page_view"`, `event: "modal_open"`, etc.

### Paso 2.1 — Ir a Disparadores

1. Menú izquierdo → **Disparadores** (Triggers).
2. **Nuevo**.

### Paso 2.2 — Disparador «CE - page_view»

1. **Nombre del disparador**: `CE - page_view`.
2. **Configuración del disparador** → **Evento personalizado** (Custom Event).
3. **Nombre del evento**: `page_view` (tal cual).
4. **Este disparador se activa en**: **Todos los eventos** (o **Algunos eventos** si quieres filtrar; para page_view suele bastar todos).
5. **Guardar**.

### Paso 2.3 — Disparador «CE - set_user_id»

1. **Disparadores** → **Nuevo**. Nombre: `CE - set_user_id`.
2. **Evento personalizado** → **Nombre del evento**: `set_user_id`.
3. **Guardar**.

### Paso 2.4 — Disparador «CE - gtm_platform_ready»

1. **Disparadores** → **Nuevo**. Nombre: `CE - gtm_platform_ready`.
2. **Evento personalizado** → **Nombre del evento**: `gtm_platform_ready`.
3. **Guardar**.

### Paso 2.5 — Disparador «CE - modal_open»

1. **Disparadores** → **Nuevo**. Nombre: `CE - modal_open`.
2. **Evento personalizado** → **Nombre del evento**: `modal_open`.
3. **Guardar**.

### Paso 2.6 — Disparador «CE - modal_close»

1. **Disparadores** → **Nuevo**. Nombre: `CE - modal_close`.
2. **Evento personalizado** → **Nombre del evento**: `modal_close`.
3. **Guardar**.

### Paso 2.7 — Disparador «CE - button_click»

1. **Disparadores** → **Nuevo**. Nombre: `CE - button_click`.
2. **Evento personalizado** → **Nombre del evento**: `button_click`.
3. **Guardar**.

### Paso 2.8 (opcional) — Disparador «CE - carousel_nav»

1. **Disparadores** → **Nuevo**. Nombre: `CE - carousel_nav`.
2. **Evento personalizado** → **Nombre del evento**: `carousel_nav`.
3. **Guardar**.

Al terminar la Parte 2 tendrás 7 disparadores (u 8 con carousel_nav).

---

## Parte 3: Etiquetas (GA4)

Necesitas una **etiqueta de configuración de GA4** (para el ID de medición y User ID) y una etiqueta de **evento** por cada tipo de evento que quieras enviar a GA4.

### Paso 3.1 — Etiqueta de configuración GA4

1. **Etiquetas** → **Nueva**. **Nombre**: `GA4 - Configuración`.
2. **Configuración de la etiqueta** → haz clic en el cuadro y elige **Google Analytics: configuración de GA4** (en inglés: *Google Analytics: GA4 Configuration*).  
   ⚠️ No elijas «Google Analytics: **evento** de GA4»; ese tipo es para eventos concretos (pasos 3.2, 3.4, etc.).
3. **ID de medición**: `G-BMZEQNRKR4` (o el de tu propiedad).
4. **user_id:** Si la etiqueta muestra un campo **«ID de usuario»** / **User ID** en la parte superior, rellénalo con `{{DLV - user_id}}`. Si no existe, búscalo en «Más ajustes» → «Campos para configurar» (añadir campo `user_id`, valor `{{DLV - user_id}}`).
5. **Plataforma:** Expande la sección **«Propiedades de usuario»** (User Properties) → **Añadir fila** → **Nombre de la propiedad:** `platform` → **Valor:** `web` (o `{{DLV - platform}}`).
6. **Activación**: disparador **CE - gtm_platform_ready** (o **All Pages** si prefieres que cargue en toda la web).
7. **Guardar**.

### Paso 3.2 — Etiqueta «GA4 - page_view»

1. **Etiquetas** → **Nueva**. **Nombre**: `GA4 - page_view`.
2. **Configuración de la etiqueta** → **Google Analytics: evento de GA4** (GA4 Event).
3. **Configuración de la etiqueta**: selecciona la etiqueta **GA4 - Configuración** (referencia).
4. **Nombre del evento**: `page_view`.
5. **Parámetros de evento** → Añadir filas:  
   - `page_path` = `{{DLV - page_path}}`  
   - `page_title` = `{{DLV - page_title}}`  
   - `page_location` = `{{DLV - page_location}}`
6. **Activación**: **CE - page_view**.
7. **Guardar**.

### Paso 3.2b — Etiqueta «GA4 - screen_view» (paridad con Android)

Para que en GA4 llegue el mismo evento y los mismos nombres de parámetros que desde Android (`screen_view`, `screen_name`, `screen_class`), añade una etiqueta que se dispare con cada `page_view` y envíe ese evento:

1. **Etiquetas** → **Nueva**. **Nombre**: `GA4 - screen_view`.
2. **Configuración de la etiqueta** → **Google Analytics: evento de GA4**.
3. **Configuración de la etiqueta**: selecciona **GA4 - Configuración**.
4. **Nombre del evento**: `screen_view` (igual que en Android).
5. **Parámetros de evento** → Añadir:  
   - `screen_name` = `{{DLV - screen_name}}` (la WebApp envía este valor normalizado en cada page_view; p. ej. "detail" en detalle café, paridad con Android).  
   - `screen_class` = `{{DLV - page_title}}` (si no hay título, en GA4 puede verse vacío).
6. **Activación**: **CE - page_view** (la misma que page_view).
7. **Guardar**.

Así, en GA4 podrás usar el evento **screen_view** y las dimensiones **screen_name** / **screen_class** tanto para tráfico Android como para web.

### Paso 3.3 — Etiqueta «GA4 - set_user_id»

Para que el User ID se aplique a los hits siguientes cuando el usuario inicia sesión:

1. **Etiquetas** → **Nueva**. **Nombre**: `GA4 - set_user_id`.
2. **Configuración de la etiqueta** → **Google Analytics: configuración de GA4**.
3. **ID de medición**: mismo que en 3.1 (o referencia a GA4 - Configuración si tu versión lo permite).
4. **user_id**: `{{DLV - user_id}}`.
5. **Activación**: **CE - set_user_id**.
6. **Guardar**.

### Paso 3.4 — Etiqueta «GA4 - modal_open»

1. **Etiquetas** → **Nueva**. **Nombre**: `GA4 - modal_open`.
2. **Google Analytics: evento de GA4** → Referencia a **GA4 - Configuración**.
3. **Nombre del evento**: `modal_open`.
4. **Parámetros de evento**: `modal_id` = `{{DLV - modal_id}}`.
5. **Activación**: **CE - modal_open**.
6. **Guardar**.

### Paso 3.5 — Etiqueta «GA4 - modal_close»

1. **Etiquetas** → **Nueva**. **Nombre**: `GA4 - modal_close`.
2. **Google Analytics: evento de GA4** → Referencia a **GA4 - Configuración**.
3. **Nombre del evento**: `modal_close`.
4. **Parámetros de evento**: `modal_id` = `{{DLV - modal_id}}`.
5. **Activación**: **CE - modal_close**.
6. **Guardar**.

### Paso 3.6 — Etiqueta «GA4 - button_click»

1. **Etiquetas** → **Nueva**. **Nombre**: `GA4 - button_click`.
2. **Google Analytics: evento de GA4** → Referencia a **GA4 - Configuración**.
3. **Nombre del evento**: `button_click`.
4. **Parámetros de evento**: `button_id` = `{{DLV - button_id}}`.
5. **Activación**: **CE - button_click**.
6. **Guardar**.

### Paso 3.7 (opcional) — Etiqueta «GA4 - carousel_nav»

1. **Etiquetas** → **Nueva**. **Nombre**: `GA4 - carousel_nav`.
2. **Google Analytics: evento de GA4** → Referencia a **GA4 - Configuración**.
3. **Nombre del evento**: `carousel_nav`.
4. **Parámetros de evento**: `carousel_id` = `{{DLV - carousel_id}}`, `direction` = `{{DLV - direction}}`.
5. **Activación**: **CE - carousel_nav**.
6. **Guardar**.

---

## Parte 4: Publicar

1. Arriba a la derecha → **Enviar** (Submit).
2. **Nombre de la versión**: p. ej. «Variables, disparadores y etiquetas Web».
3. **Publicar**.

---

## Resumen

| Paso | Tipo       | Nombre              | Uso |
|------|------------|---------------------|-----|
| 1.2  | Variable   | DLV - page_path     | Ruta de página |
| 1.3  | Variable   | DLV - page_title    | Título de página |
| 1.4  | Variable   | DLV - page_location | URL completa |
| 1.5  | Variable   | DLV - user_id       | Usuario logueado |
| 1.6  | Variable   | DLV - platform      | Plataforma (web) |
| 1.7  | Variable   | DLV - modal_id      | ID del modal |
| 1.8  | Variable   | DLV - button_id     | ID del botón |
| 1.9  | Variable   | DLV - carousel_id   | (Opcional) Carrusel |
| 1.10 | Variable   | DLV - direction     | (Opcional) Dirección |
| 2.2  | Disparador | CE - page_view      | Evento page_view |
| 2.3  | Disparador | CE - set_user_id    | Evento set_user_id |
| 2.4  | Disparador | CE - gtm_platform_ready | Al cargar GTM |
| 2.5  | Disparador | CE - modal_open     | Abrir modal |
| 2.6  | Disparador | CE - modal_close    | Cerrar modal |
| 2.7  | Disparador | CE - button_click   | Clic en botón |
| 2.8  | Disparador | CE - carousel_nav   | (Opcional) Carrusel |
| 3.1  | Etiqueta   | GA4 - Configuración | ID medición, user_id, platform |
| 3.2  | Etiqueta   | GA4 - page_view     | Evento page_view a GA4 |
| 3.2b | Etiqueta   | GA4 - screen_view   | Mismo evento/params que Android (screen_view, screen_name, screen_class) |
| 3.3  | Etiqueta   | GA4 - set_user_id   | Actualizar user_id |
| 3.4  | Etiqueta   | GA4 - modal_open    | Evento modal_open a GA4 |
| 3.5  | Etiqueta   | GA4 - modal_close   | Evento modal_close a GA4 |
| 3.6  | Etiqueta   | GA4 - button_click  | Evento button_click a GA4 |
| 3.7  | Etiqueta   | GA4 - carousel_nav  | (Opcional) Evento carousel_nav |

La WebApp envía estos eventos cuando `VITE_GTM_CONTAINER_ID` está definido (GTM-WLXN93VK). Lista de `modal_id` y `button_id` en `docs/gtm/CONTAINER_REFERENCE_WEB.md`.
