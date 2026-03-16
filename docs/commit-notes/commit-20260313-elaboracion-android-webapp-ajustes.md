# Nota de commit — Elaboración Android y WebApp: ajustes UI y Selecciona café como página

**Fecha:** 2026-03-13  
**Ámbito:** Android (BrewLab), WebApp (Brew, Selecciona café)

---

## Resumen

Ajustes de UI/UX en la pantalla de elaboración (BrewLab) en Android para alineación con WebApp, y en WebApp conversión del flujo "Selecciona café" de modal a página completa con TopBar y paridad de "Tu despensa".

---

## Android — Elaboración (BrewLab)

### Chips (método, tipo, tamaño)

- **Fondo chips no seleccionados:** En modo día, fondo gris claro (`AdviceCardBgLight`); en modo noche, fondo igual al de la página (`MaterialTheme.colorScheme.background`). Aplica a método, tipo y tamaño.
- **Borde:** Chips sin borde (transparente) para mantener consistencia.
- **Forma método:** Chips de método usan `Shapes.card` (16 dp) para igualar esquinas a los de tipo (antes `Shapes.shapeCardMedium`).

### Carruseles (método, tipo, tamaño)

- **Derecha:** El contenido del carrusel llega al borde derecho de la card (`BoxWithConstraints` + `width(maxWidth + 16.dp)`, `contentPadding.end = 0.dp`).
- **Izquierda:** Padding inicial 16 dp (`contentPadding.start = 16.dp`); al hacer scroll el contenido puede llegar hasta el borde izquierdo del área de scroll.

### Márgenes y secciones

- **Forma de elaboración:** Configuración (café, barista, temporizador) usa el mismo margen horizontal que "Selecciona café" (`Spacing.space4`); antes `Spacing.space6` en `ConfigStep` cuando `wrapInCard = false`.
- **Debajo de Tipo:** Aumentado margen: `Spacer(space2)` entre divisor y carrusel de tipo; divisor con `padding(vertical = Spacing.space3)` entre "Selecciona café" y Tipo para que la línea no se pise.
- **Debajo de Temporizador:** Reducido margen (divisor y fila del switch con menos padding vertical).

### Consejo del barista

- Card con fondo gris: modo día `AdviceCardBgLight`, modo noche `AdviceCardBgDark`; esquinas redondeadas (`Shapes.shapeCardMedium`); sin borde.

### Selecciona café (fila en la card)

- Flecha a la derecha: `Spacer(Modifier.weight(1f))` antes del icono para alinear correctamente.
- Imagen del café: cuadrada (40 dp), `ContentScale.Crop`, `Shapes.shapeCardMedium` (bordes redondeados, paridad con buscador).

### Selecciona café: modal → página

- **Nueva ruta:** `brewlab_select_coffee`. Al pulsar "Selecciona café" se navega a una pantalla completa con TopBar "Selecciona café" y botón atrás.
- **Pantalla:** `BrewLabSelectCoffeeScreen` muestra el mismo contenido que antes el modal (`ChooseCoffeeStep`): Tu despensa (ordenada por último uso), buscador, sugerencias, "Crear mi café".
- **ViewModel compartido:** Se usa el mismo `BrewLabViewModel` (scoped al back stack de brewlab) para despensa y cafés; la selección se devuelve vía `savedStateHandle` del entry "brewlab" y se aplica al volver.
- **Despensa:** Orden por último uso (actividad del usuario) ya aplicado en `DiaryRepository.getPantryItems()`; el café más recientemente usado aparece a la izquierda.

**Archivos Android principales:** `BrewLabScreen.kt`, `BrewLabComponents.kt`, `AppNavigation.kt`.

---

## WebApp — Elaboración y Selecciona café

### Selecciona café como página

- **Estado:** `brewSelectCoffeePageOpen` en `AppContainer`; al pulsar "Selecciona café" se muestra una vista de página completa en lugar del modal (`DiarySheets`).
- **TopBar:** Cuando la página está abierta, el TopBar muestra título "Selecciona café" y flecha atrás (no la X).
- **Contenido:** Página `BrewSelectCoffeePage`: bloque "Tu despensa" (misma lógica y UI que en Home), buscador, sugerencias (sin cafés que ya estén en despensa), botón "Crear mi café".
- **Despensa:** Orden por último uso (`brewPantryItems` desde `useAppDerivedData`).
- **Selección:** Al elegir un café se asigna a la elaboración y se cierra la página.

**Archivos WebApp principales:** `AppContainer.tsx`, `TopBar.tsx`, `BrewSelectCoffeePage.tsx` (nuevo), `features.css`, `useAppDerivedData.ts`.

---

## Referencias

- Registro: `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md` §16.
- Elaboración y colores (base): `docs/commit-notes/commit-20260304-05-elaboracion-brew-ui-colores-italiana.md`.
