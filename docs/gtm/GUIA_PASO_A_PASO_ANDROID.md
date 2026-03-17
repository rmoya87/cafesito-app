# Guía paso a paso: configurar el contenedor GTM Android (GTM-T9WDBCBR) a mano

Sigue estos pasos en orden en [Tag Manager](https://tagmanager.google.com/), con el contenedor **Android** (GTM-T9WDBCBR) abierto.

**Nota:** La app Android ya envía **modal_open** / **modal_close** y **button_click** en todas las pantallas con modales y botones, incluida la **Home** (Inicio). En Home se envían los `modal_id`: `timeline_pantry_options`, `timeline_stock_edit`, `timeline_delete_confirm_pantry`, `timeline_finished_confirm`. Ver lista completa en `docs/gtm/CONTAINER_REFERENCE_ANDROID.md`.

---

## Parte 1: Variables

**Importante:** La lista con "App ID", "App Name", "Event Name", "Platform", "Container ID", etc. son **variables integradas** (para activar/desactivar). **No uses esa lista** para nuestras variables. Las nuestras son **definidas por el usuario** y se crean más abajo en la misma página.

### Paso 1.1 — Ir a Variables y crear una nueva (definida por el usuario)

1. En el menú izquierdo, haz clic en **Variables**.
2. **Baja en la página** hasta la sección **Variables definidas por el usuario** (en inglés: *User-Defined Variables*).
3. Ahí haz clic en **Nueva** (o **New**).

### Paso 1.2 — Variable «DLV - screen_name»

1. Haz clic en el cuadro **Configuración de la variable** (el recuadro grande, "Elegir un tipo de variable").
2. En el menú, entra en **Firebase** y elige **Parámetro de evento**.
3. En la configuración del parámetro de evento:
   - **Event Type** / **Tipo de evento**: elige **Custom Parameter** / **Parámetro personalizado** (no uses "Suggested Firebase Parameter" ni "Evento sugerido", porque `screen_name` no está en la lista de parámetros sugeridos).
   - Donde pida el **nombre del parámetro personalizado** (o un campo para escribir el nombre), escribe exactamente: `screen_name`.
   - Si hay "Definir valor predeterminado", puedes dejarlo vacío.
4. Arriba, en **Nombre** de la variable, escribe: `DLV - screen_name`.
5. **Guardar**.

### Paso 1.3 — Variable «DLV - screen_class»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Configuración de la variable** → **Firebase** → **Parámetro de evento**.
3. **Event Type**: **Custom Parameter** / **Parámetro personalizado**. Nombre del parámetro: `screen_class` (no "DLV - screen_class").
4. **Nombre** de la variable (arriba): `DLV - screen_class`. **Guardar**.

### Paso 1.4 — Variable «DLV - user_id»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Firebase** → **Parámetro de evento** → **Custom Parameter**. Nombre del parámetro: `user_id`.
3. Nombre de la variable: `DLV - user_id`. **Guardar**.

### Paso 1.5 — Variable «DLV - platform»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Firebase** → **Parámetro de evento** → **Custom Parameter**. Nombre del parámetro: `platform`.
3. Nombre de la variable: `DLV - platform`. **Guardar**.

### Paso 1.6 — Variable «DLV - modal_id»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Configuración de la variable** → **Firebase** → **Parámetro de evento**.
3. **Event Type**: **Custom Parameter** / **Parámetro personalizado**. Nombre del parámetro: `modal_id`.
4. **Nombre** de la variable: `DLV - modal_id`. **Guardar**.

### Paso 1.7 — Variable «DLV - button_id»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Configuración de la variable** → **Firebase** → **Parámetro de evento**.
3. **Event Type**: **Custom Parameter** / **Parámetro personalizado**. Nombre del parámetro: `button_id`.
4. **Nombre** de la variable: `DLV - button_id`. **Guardar**.

### Paso 1.8 (opcional) — Variable «DLV - carousel_id»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Firebase** → **Parámetro de evento** → **Custom Parameter**. Nombre del parámetro: `carousel_id`.
3. **Nombre** de la variable: `DLV - carousel_id`. **Guardar**.

### Paso 1.9 (opcional) — Variable «DLV - direction»

1. **Variables** → **Variables definidas por el usuario** → **Nueva**.
2. **Firebase** → **Parámetro de evento** → **Custom Parameter**. Nombre del parámetro: `direction`.
3. **Nombre** de la variable: `DLV - direction`. **Guardar**.

Al terminar la Parte 1 tendrás 4 variables obligatorias (screen_name, screen_class, user_id, platform). Si haces también 1.6–1.9 tendrás 8 variables en total (o 6 si omites carrusel).

---

## Parte 2: Disparadores

En el contenedor Android el disparador es de tipo **Personalizado** y se configura con una condición: «cuando la variable **Nombre del evento** sea igual a» el nombre del evento (p. ej. `screen_view`).

### Paso 2.1 — Ir a Disparadores

1. En el menú izquierdo, haz clic en **Disparadores**.
2. Haz clic en **Nuevo**.

### Paso 2.2 — Disparador «CE - screen_view»

1. Arriba, **Nombre del disparador**: `CE - screen_view` (con guión bajo, no espacio).
2. Haz clic en **Configuración del activador**.
3. **Tipo de activador**: deja **Personalizado** (Custom).
4. **Este activador se activa en**: elige **Algunos eventos** (Some events).
5. En la fila de condición, rellena:
   - **Variable** (Seleccionar variable): elige la variable integrada **Nombre del evento** / **Event Name** (suele estar en **Utilidades**).
   - **Operador**: **igual a** (equals), no "contiene".
   - **Valor**: escribe exactamente `screen_view` (con guión bajo).
6. **Guardar**.

### Paso 2.3 — Disparador «CE - set_user_id»

1. **Disparadores** → **Nuevo**. Nombre: `CE - set_user_id`.
2. **Configuración del activador** → **Personalizado** → **Algunos eventos**.
3. Condición: Variable **Nombre del evento** / **Event Name** → **igual a** → Valor `set_user_id`.
4. **Guardar**.

### Paso 2.4 — Disparador «CE - gtm_platform_ready»

1. **Disparadores** → **Nuevo**. Nombre: `CE - gtm_platform_ready`.
2. **Personalizado** → **Algunos eventos**. Condición: **Nombre del evento** **igual a** `gtm_platform_ready`.
3. **Guardar**.

### Paso 2.5 — Disparador «CE - login_success»

1. **Disparadores** → **Nuevo**. Nombre: `CE - login_success`.
2. **Personalizado** → **Algunos eventos**. Condición: **Nombre del evento** **igual a** `login_success`.
3. **Guardar**.

### Paso 2.6 — Disparador «CE - profile_completed»

1. **Disparadores** → **Nuevo**. Nombre: `CE - profile_completed`.
2. **Configuración del activador** → **Personalizado** → **Algunos eventos**.
3. Condición: Variable **Nombre del evento** → **igual a** → Valor `profile_completed`.
4. **Guardar**.

### Paso 2.7 — Disparador «CE - modal_open»

1. **Disparadores** → **Nuevo**. **Nombre del disparador**: `CE - modal_open`.
2. **Configuración del activador** → **Personalizado** → **Algunos eventos**.
3. Condición: **Variable** = **Nombre del evento** (Event Name), **Operador** = **igual a**, **Valor** = `modal_open`.
4. **Guardar**.

### Paso 2.8 — Disparador «CE - modal_close»

1. **Disparadores** → **Nuevo**. Nombre: `CE - modal_close`.
2. **Personalizado** → **Algunos eventos**. Condición: **Nombre del evento** **igual a** `modal_close`.
3. **Guardar**.

### Paso 2.9 — Disparador «CE - button_click»

1. **Disparadores** → **Nuevo**. Nombre: `CE - button_click`.
2. **Personalizado** → **Algunos eventos**. Condición: **Nombre del evento** **igual a** `button_click`.
3. **Guardar**.

### Paso 2.10 (opcional) — Disparador «CE - carousel_nav»

1. **Disparadores** → **Nuevo**. Nombre: `CE - carousel_nav`.
2. **Personalizado** → **Algunos eventos**. Condición: **Nombre del evento** **igual a** `carousel_nav`.
3. **Guardar**.

Al terminar la Parte 2 tendrás 9 disparadores (o 10 si añades carousel_nav).

---

## Parte 3: Etiquetas en GTM

En GTM: menú izquierdo **Etiquetas** → **Nueva**. En cada etiqueta rellenarás **Configuración de la etiqueta** (tipo y campos) y **Activación** (qué disparador la ejecuta). Arriba pondrás el **Nombre** de la etiqueta. Al final, **Guardar**.

### Paso 3.1 — Etiqueta de configuración (Firebase o GA4)

En algunos contenedores Android, bajo **Google Analytics (Firebase)** solo aparece una etiqueta cuyo **Acción** es «Añadir evento», «Modificar evento» o «Bloquear evento»; esa etiqueta no sirve para la configuración (ID de medición, User ID). Comprueba si al hacer clic en **Google Analytics (Firebase)** se despliega más de una opción (p. ej. «Configuración» y «Evento»); si hay una de **configuración**, úsala y rellena ID de medición, User ID = `{{DLV - user_id}}`, propiedad de usuario `platform` = `android`, y **Activación** = **CE - gtm_platform_ready**. Si **no** existe esa etiqueta de configuración, puedes omitir este paso: la app ya envía datos a Firebase; User ID y plataforma se pueden gestionar en el código (p. ej. `AnalyticsHelper`) o en el proyecto de Firebase/GA4. Pasa al paso 3.2.

### Paso 3.2 — Etiqueta para el evento screen_view

1. **Etiquetas** → **Nueva**.
2. Arriba, **Nombre de la etiqueta**: `FA - screen_view` (o `GA4 - screen_view`).
3. **Configuración de la etiqueta** → **Google Analytics (Firebase)**.
4. **Acción**: elige **Añadir evento** (no «Modificar evento» ni «Bloquear evento»).
5. Rellena:
   - **Anular el nombre del evento**: escribe `screen_view`.
   - **Parámetros que se añadirán/editarán** → **Añadir fila** y añade dos filas:
     - Nombre de la clave: `screen_name` → Valor: `{{DLV - screen_name}}` (icono de bloques para elegir la variable).
     - Nombre de la clave: `screen_class` → Valor: `{{DLV - screen_class}}`.
   - «Parámetros que se deben ignorar»: déjalo vacío.
6. **Activación** → disparador **CE - screen_view**.
7. **Guardar**.

### Paso 3.3 — Etiqueta para set_user_id (actualizar User ID)

Si en el paso 3.1 no encontraste etiqueta de configuración, puedes omitir este paso o usar una etiqueta que envíe el User ID (si tu plantilla lo permite en «Añadir evento» con algún parámetro especial). Si sí tienes configuración en 3.1: **Etiquetas** → **Nueva** → **Nombre**: `FA - set_user_id` → misma etiqueta de **configuración** que 3.1 → **User ID** = `{{DLV - user_id}}` → **Activación**: **CE - set_user_id** → **Guardar**.

### Paso 3.4 (opcional) — Eventos login_success y profile_completed

1. **Etiquetas** → **Nueva** → **Nombre**: `FA - login_success`.
2. **Configuración de la etiqueta** → **Google Analytics (Firebase)** → **Acción**: **Añadir evento** → **Anular el nombre del evento**: `login_success`.
3. **Activación** → **CE - login_success** → **Guardar**.
4. **Etiquetas** → **Nueva** → **Nombre**: `FA - profile_completed`.
5. **Google Analytics (Firebase)** → **Acción**: **Añadir evento** → **Anular el nombre del evento**: `profile_completed`.
6. **Activación** → **CE - profile_completed** → **Guardar**.

### Paso 3.5 — Etiqueta «FA - modal_open»

1. **Etiquetas** → **Nueva**. **Nombre de la etiqueta**: `FA - modal_open`.
2. **Configuración de la etiqueta** → **Google Analytics (Firebase)** → **Acción**: **Añadir evento**.
3. **Anular el nombre del evento**: `modal_open`.
4. **Parámetros que se añadirán/editarán** → **Añadir fila** → Nombre de la clave: `modal_id` → Valor: `{{DLV - modal_id}}` (elegir la variable desde el icono de bloques).
5. **Activación** → disparador **CE - modal_open**.
6. **Guardar**.

### Paso 3.6 — Etiqueta «FA - modal_close»

1. **Etiquetas** → **Nueva**. **Nombre de la etiqueta**: `FA - modal_close`.
2. **Configuración de la etiqueta** → **Google Analytics (Firebase)** → **Acción**: **Añadir evento**.
3. **Anular el nombre del evento**: `modal_close`.
4. **Parámetros que se añadirán/editarán** → **Añadir fila** → Clave: `modal_id` → Valor: `{{DLV - modal_id}}`.
5. **Activación** → **CE - modal_close**.
6. **Guardar**.

### Paso 3.7 — Etiqueta «FA - button_click»

1. **Etiquetas** → **Nueva**. **Nombre de la etiqueta**: `FA - button_click`.
2. **Configuración de la etiqueta** → **Google Analytics (Firebase)** → **Acción**: **Añadir evento**.
3. **Anular el nombre del evento**: `button_click`.
4. **Parámetros que se añadirán/editarán** → **Añadir fila** → Clave: `button_id` → Valor: `{{DLV - button_id}}`.
5. **Activación** → **CE - button_click**.
6. **Guardar**.

### Paso 3.8 (opcional) — Etiqueta «FA - carousel_nav»

1. **Etiquetas** → **Nueva**. **Nombre de la etiqueta**: `FA - carousel_nav`.
2. **Google Analytics (Firebase)** → **Acción**: **Añadir evento**.
3. **Anular el nombre del evento**: `carousel_nav`.
4. **Parámetros** → Añadir: `carousel_id` = `{{DLV - carousel_id}}`, `direction` = `{{DLV - direction}}`.
5. **Activación** → **CE - carousel_nav**.
6. **Guardar**.

---

## Parte 4: Publicar

1. Arriba a la derecha, haz clic en **Enviar** (o **Submit**).
2. Pon **Nombre de la versión** (ej. «Variables, disparadores y etiquetas Android»).
3. **Publicar**.

---

## Resumen de lo que has creado

| Paso | Tipo      | Nombre              | Uso |
|------|-----------|---------------------|-----|
| 1.2  | Variable  | DLV - screen_name   | Pantalla actual |
| 1.3  | Variable  | DLV - screen_class  | Clase de pantalla |
| 1.4  | Variable  | DLV - user_id       | Usuario logueado |
| 1.5  | Variable  | DLV - platform      | Plataforma (android) |
| 1.6  | Variable  | DLV - modal_id      | ID del modal (abrir/cerrar) |
| 1.7  | Variable  | DLV - button_id     | ID del botón pulsado |
| 1.8  | Variable  | DLV - carousel_id   | (Opcional) ID del carrusel |
| 1.9  | Variable  | DLV - direction     | (Opcional) Dirección carrusel |
| 2.2  | Disparador| CE - screen_view    | Evento `screen_view` |
| 2.3  | Disparador| CE - set_user_id    | Evento `set_user_id` |
| 2.4  | Disparador| CE - gtm_platform_ready | Al iniciar |
| 2.5  | Disparador| CE - login_success  | Tras login correcto |
| 2.6  | Disparador| CE - profile_completed | Tras completar perfil |
| 2.7  | Disparador| CE - modal_open     | Al abrir un modal |
| 2.8  | Disparador| CE - modal_close    | Al cerrar un modal |
| 2.9  | Disparador| CE - button_click   | Al pulsar botón trackeado |
| 2.10 | Disparador| CE - carousel_nav   | (Opcional) Navegación carrusel |
| 3.1  | Etiqueta  | Configuración       | User ID, platform (si existe) |
| 3.2  | Etiqueta  | FA - screen_view    | Envío pantalla a GA4/Firebase |
| 3.3  | Etiqueta  | FA - set_user_id    | Actualizar User ID |
| 3.4  | Etiqueta  | FA - login_success, FA - profile_completed | Eventos de flujo |
| 3.5  | Etiqueta  | FA - modal_open     | Evento modal abierto |
| 3.6  | Etiqueta  | FA - modal_close    | Evento modal cerrado |
| 3.7  | Etiqueta  | FA - button_click   | Evento clic en botón |
| 3.8  | Etiqueta  | FA - carousel_nav   | (Opcional) Evento carrusel |

**Si configuraste la Parte 2B**, la app Android envía **button_click** (Brew Lab: `brew_select_coffee`, `brew_add_to_pantry`, `brew_create_coffee`, `brew_next_step`, `brew_save_to_diary`) y **modal_open** / **modal_close** en todas las pantallas con modales (detalle, perfil, listas, diario, inicio, añadir stock, añadir café, búsqueda, login). Ver `docs/gtm/CONTAINER_REFERENCE_ANDROID.md` para la lista completa de `modal_id`.

La app Android ya envía estos eventos al dataLayer cuando `GTM_CONTAINER_ID` está definido en `gradle.properties` (GTM-T9WDBCBR). Para que la app use el contenedor publicado, puede ser necesario tener un contenedor por defecto en `res/raw` y cargarlo con `TagManager.loadContainerPreferNonDefault`; ver [docs/ANALITICAS.md](../ANALITICAS.md) §5.

---

## Eventos adicionales en la app Android (carruseles, modales, botones)

La app ya envía **modal_open** y **modal_close** (con `modal_id`) y **button_click** (con `button_id`) en: Detalle, Perfil, Opciones de lista, Diario, Inicio (Home), Añadir stock, Añadir café/despensa, Búsqueda y Login. Ver la tabla de `modal_id` en `docs/gtm/CONTAINER_REFERENCE_ANDROID.md`.

| Evento         | Parámetros              | Estado en la app |
|----------------|------------------------|-------------------|
| **modal_open** / **modal_close** | `modal_id` | Implementado en todas las pantallas con modales. |
| **button_click** | `button_id` | Implementado en Brew Lab y donde aplique. |
| **carousel_nav** | `carousel_id`, `direction` | Opcional: añadir en carruseles (p. ej. Brew Lab, Home) si se desea. |

Ejemplo para carrusel (si se añade):

- `analyticsHelper.trackEvent("carousel_nav", bundleOf("carousel_id" to "brew_despensa", "direction" to "next"))`

`AnalyticsHelper` se inyecta en `AppNavigation` y se pasa `onTrackEvent` a cada pantalla que registra estos eventos.
