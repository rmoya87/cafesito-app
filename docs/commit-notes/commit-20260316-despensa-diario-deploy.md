# Nota de commit — Despensa, diario, deploy y CI (15–16 mar 2026)

**Fecha:** 2026-03-16  
**Ámbito:** WebApp (despensa, diario, elaboración), Android (despensa, modal café terminado), CI (deploy-web, job changes)

---

## Resumen

Correcciones en flujos de despensa (guardar/añadir desde elaboración y Home), stock restante en UI, método espresso, error 400 al insertar en despensa, modal "Café terminado" en Android, y correcciones de CI/deploy-web (TypeScript y git exit 128).

---

## WebApp — Despensa y elaboración

### Guardar en despensa desde "Selecciona café"

- `savePantry` devuelve el ítem creado; en el handler (p. ej. `handleSavePantry`) se llama a `setBrewPantryItemId(newRow.id)` además de `setBrewCoffeeId` cuando la modal se abrió desde elaboración.
- **Archivos:** `useDiarySheetActions.ts`, lógica de brew en `AppContainer` o equivalente.

### Stock restante en "Tu despensa"

- Se mostraba "restante tras esta elaboración" en lugar del stock real. Se pasó a mostrar siempre `row.remaining` / `row.total` en **BrewSelectCoffeePage** y **BrewViews**.

### Añadir a despensa (Home y Selecciona café)

- En `useDiarySheetActions.ts` se usa `customCoffees` y `getLastSelectedPantryCoffee` para resolver el café en `savePantry`.
- En **AppContainer.tsx:** `lastSelectedPantryCoffeeRef` y `onSelectPantryCoffee`; en **DiarySheets.tsx** se llama a `onSelectPantryCoffee(coffee)` al elegir un café y el botón Guardar pasa a `disabled={!diaryPantryCoffeeIdDraft}`.

### Error 400 al guardar en despensa

- **Causa:** Columna `id` de `pantry_items` obligatoria; el insert no enviaba valor.
- **Solución:** En `webApp/src/data/supabaseApi.ts`, en `insertPantryItem`, añadir `id: crypto.randomUUID()` al objeto insertado.

### Método espresso: títulos y "Café (g)"

- En **BrewViews.tsx** fila `brew-tech-coffee-espresso-titles` con "Café (g)" y "RATIO 1:2.0 - CONCENTRADO" en la misma línea; estilos en `features.css`.

### Deploy-web: TypeScript

- **Síntoma:** `Cannot find name 'diarySelectedPantryItemIdDraft'` / `setDiarySelectedPantryItemIdDraft`.
- **Solución:** Añadir ambas props a la destructuración del componente en **DiarySheets.tsx** (estaban en el tipo pero no en el parámetro).

---

## Android — Despensa y diario

### Guardar en despensa desde elaboración

- En **AppNavigation.kt**, callback `onCoffeeCreatedForBrewLab` guarda el id del ítem en `getBackStackEntry("brewlab")?.savedStateHandle` (no en `previousBackStackEntry`).

### Añadir a despensa

- **DiaryViewModel.kt:** `addToPantry` con `onFailure`; **AddStockScreen.kt:** poner `isSaving = false` en fallo; **AppNavigation.kt:** devolver ítem creado a elaboración vía `savedStateHandle`.

### Modal "Café terminado"

- **TimelineComponents.kt:** `DeleteConfirmationDialog` con parámetro opcional `confirmButtonText` (por defecto `"ELIMINAR"`). En **DiaryScreen.kt** y **TimelineScreen.kt** para la modal de café terminado se pasa `confirmButtonText = "CONFIRMAR"`.

---

## CI

### Git exit 128 en jobs `changes` y `deploy-web`

- En el job **changes**, paso "Checkout for file filter (push only)" con `ref: ${{ github.ref }}` explícito para alinearlo con deploy-web y release-android y evitar ref no encontrada (p. ej. Beta vs beta).
- En **REGISTRO_DESARROLLO_E_INCIDENCIAS.md** §2.3 se amplió qué comprobar si el error persiste (rama exacta, permisos workflow).

---

## Referencias

- Registro: `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md` §17.
- Workflow: `docs/RELEASE_DEPLOY_WORKFLOW.md` (tabla de despliegues).
