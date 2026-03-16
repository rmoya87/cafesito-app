# Documento funcional — Cafesito

**Propósito:** Especificación funcional y técnica de los flujos principales de la app para desarrollo, pruebas y alineación entre plataformas (Android, WebApp). Fuente de verdad para criterios de aceptación y comportamiento esperado.

**Estado:** vivo  
**Última actualización:** 2026-03-17  
**Ámbito:** Android, WebApp (paridad funcional).

---

## 1. Alcance y pantallas principales

| Área | Descripción breve |
|------|-------------------|
| **Inicio / Home** | Timeline de actividad, acceso a Tu despensa, añadir café/agua. |
| **Explorar** | Búsqueda de cafés, listas, detalle de café, reseñas. |
| **Elaboración (BrewLab)** | Configurar y ejecutar una elaboración (método, café, dosis, tiempo); "Selecciona café" como página/modal; guardar resultado en diario. |
| **Diario** | Entradas de café y agua por fecha; analíticas; Cafés probados; calendario. |
| **Perfil** | Usuario, actividad, listas, opciones, cerrar sesión. |
| **Despensa** | "Tu despensa": ítems por café (stock, restante); añadir a despensa desde Home o desde Elaboración; crear café nuevo. |

Navegación: pestañas o drawer (Inicio, Explorar, Elabora, Diario, Perfil). Paridad entre Android y WebApp en flujos y resultados de usuario.

---

## 2. Flujos por área

### 2.1 Despensa

#### 2.1.1 Ver "Tu despensa"

- **Dónde:** Home (bloque Tu despensa), Elaboración (pantalla/página "Selecciona café").
- **Comportamiento:** Lista de ítems de despensa (café + cantidad total y **stock restante real**). Orden: último uso primero. Cada ítem muestra café, total (g) y restante (g).
- **Criterios de aceptación:**
  - Se muestra `remaining` / `total` (stock real), no "restante tras esta elaboración" cuando el contexto es listado general.
  - En "Selecciona café" (elaboración), al elegir un café se asocia a la elaboración y se cierra la vista; si se crea un ítem nuevo (Guardar en despensa), el nuevo ítem queda seleccionado para la elaboración.

#### 2.1.2 Añadir a despensa (desde Home o desde Elaboración)

- **Entrada:** Home → "Añadir a despensa" o Elaboración → "Selecciona café" → "Crear mi café" / elegir café y guardar en despensa.
- **Comportamiento:** El usuario elige o crea un café; indica cantidad (g); confirma. Se inserta un ítem en `pantry_items` con `id` (UUID) generado en cliente si aplica. En WebApp el botón "Guardar" está habilitado cuando hay café seleccionado (incluido si el café está en `customCoffees` recién creado). En Android, si falla `addToPantry` se muestra estado de error y se deja de bloquear la UI (`isSaving = false`).
- **Criterios de aceptación:**
  - Insert en `pantry_items` incluye `id` (p. ej. `crypto.randomUUID()` en WebApp) para no recibir 400.
  - Tras guardar desde Elaboración, la elaboración usa el café/ítem recién creado (WebApp: `setBrewPantryItemId` + `setBrewCoffeeId`; Android: `savedStateHandle` del entry "brewlab").

#### 2.1.3 Guardar en despensa desde "Selecciona café"

- **Comportamiento:** Si el usuario está en "Selecciona café" (elaboración) y guarda un nuevo ítem de despensa, la elaboración debe quedar con ese café y ese ítem asociado.
- **Criterios de aceptación:** Mismo que 2.1.2; además, al cerrar "Selecciona café" la configuración de elaboración muestra el café y el ítem correctos.

---

### 2.2 Diario

#### 2.2.1 Añadir entrada (café o agua)

- **Entrada:** Diario → añadir café o agua (modal/sheet o pantalla).
- **Comportamiento:** Usuario elige café (de despensa o creación rápida), cantidad (ml/g), opcionalmente cafeína y tipo de preparación; o añade agua (ml). Se guarda una entrada en diario para la fecha seleccionada.
- **Criterios de aceptación:** Entrada visible en lista/calendario del diario; datos coherentes con lo introducido.

#### 2.2.2 Cafés probados

- **Comportamiento:** Vista de cafés probados (con o sin mapa por país). Listado por país; al pulsar un café se abre detalle. Al volver desde detalle se mantiene la vista (Cafés probados).
- **Criterios de aceptación:** Todos los cafés con al menos una entrada en diario aparecen; navegación de vuelta a Cafés probados correcta (WebApp: `returnDiarySubViewRef` / ruta `/diary/cafes-probados`).

#### 2.2.3 Modal "Café terminado" (Android)

- **Comportamiento:** Al marcar un café como "terminado" se muestra un diálogo de confirmación con botón de texto "CONFIRMAR" (no "ELIMINAR").
- **Criterios de aceptación:** El diálogo usa `confirmButtonText = "CONFIRMAR"` en DiaryScreen y TimelineScreen para este caso.

---

### 2.3 Elaboración (BrewLab)

#### 2.3.1 Selecciona café

- **Comportamiento:** "Selecciona café" es una **página** (Android: pantalla completa; WebApp: página con TopBar "Selecciona café" y flecha atrás). Contenido: Tu despensa (orden por último uso), buscador, sugerencias (sin cafés ya en despensa), "Crear mi café". Al elegir un café se asigna a la elaboración y se cierra la vista.
- **Criterios de aceptación:** Paridad Android/WebApp; despensa ordenada por último uso; al crear y guardar ítem desde aquí, la elaboración queda con ese café/ítem.

#### 2.3.2 Método espresso — UI

- **Comportamiento:** Fila de títulos con "Café (g)" y "RATIO 1:2.0 - CONCENTRADO" (o equivalente) en la misma línea. Etiqueta "Café (g)" reutilizada donde aplique en otros métodos.
- **Criterios de aceptación:** Títulos visibles en una sola línea; sin solapamientos ni saltos innecesarios.

#### 2.3.3 Guardar elaboración en diario

- **Comportamiento:** Al finalizar la elaboración el usuario puede guardar el resultado en el diario (café, cantidad, tipo, etc.). Flujo sin pantalla "Resultado" separada: todo en "Proceso en curso" con tarjeta de sabor y botón Guardar en topbar.

---

### 2.4 Auth y sesión

- **Comportamiento:** Login (p. ej. Google); sesión persistente; cierre de sesión. Sin forzar re-login innecesario (manejo de refresh token según diseño).
- **Criterios de aceptación:** Usuario no autenticado ve pantalla de login; usuario autenticado accede a las áreas descritas sin errores 401 no manejados.

---

### 2.5 Perfil y actividad

- **Comportamiento:** Perfil con pestañas (p. ej. Actividad); actividad con estado de carga ("Recargando…") mientras se obtienen datos; listado de actividad cuando hay datos.
- **Criterios de aceptación:** No se muestra "Tu actividad está vacía" mientras se está cargando; al terminar la carga se muestra vacío o listado según datos.

---

## 3. Dependencias técnicas (referencia)

| Recurso | Uso en flujos |
|---------|----------------|
| **Tabla `pantry_items`** | Despensa: id (UUID), coffee_id, user_id, cantidad, etc. RLS por usuario. |
| **Tabla `diary_entries`** | Diario: entradas por usuario y fecha. |
| **Supabase Auth** | Login, sesión, user_id. |
| **APIs / RPC** | Inserts y selects según docs de Supabase y `webApp/src/data/`, `app/` (Android). |

Consultar `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`, `docs/SHARED_BUSINESS_LOGIC.md` y `docs/supabase/` para contratos y políticas.

---

## 4. Mantenimiento del documento

- Al añadir o cambiar un flujo funcional relevante, actualizar la sección correspondiente y los criterios de aceptación.
- Mantener paridad descrita entre Android y WebApp; si una plataforma difiere de forma intencionada, indicarlo explícitamente.
- **Última actualización:** actualizar la cabecera al modificar el documento.

---

## 5. Referencias

- **Registro de incidencias y cambios:** `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md`
- **Tests de humo:** `docs/SMOKE_TESTS.md`
- **Testing pre/post desarrollo:** `docs/TESTING_PRE_POST_DESARROLLO.md`
- **Arquitectura:** `docs/MASTER_ARCHITECTURE_GOVERNANCE.md`
