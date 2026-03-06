# Registro de desarrollo e incidencias

**Propósito:** Documentar cambios, correcciones y decisiones recientes para tenerlos en cuenta en próximos desarrollos o incidencias.  
**Última actualización:** 2026-03-05

---

## Índice rápido

1. [Elaboración (Brew), UI, colores, Italiana](#1-elaboración-brew-ui-colores-italiana)
2. [Incidencias CI/CD y TypeScript (deploy-web)](#2-incidencias-cicd-y-typescript-deploy-web)
3. [Ramas y entornos](#3-ramas-y-entornos)
4. [Referencias a otros documentos](#4-referencias-a-otros-documentos)

---

## 1. Elaboración (Brew), UI, colores, Italiana

Detalle completo en: **`docs/commit-notes/commit-20260304-05-elaboracion-brew-ui-colores-italiana.md`**.

Resumen para futuras modificaciones:

- **Crear mi café:** Botón “Crear mi café” en “Elige tu café” va al formulario de crear café (`addPantryItem?onlyActivity=true&origin=brewlab`). Formulario incluye “Cantidad del café (g)” (slider 0–2000 g) y título “CREAR MI CAFÉ”.
- **Tiempo de extracción (espresso):** Debe aplicarse al temporizador en “Proceso en curso”. En Android, `phasesTimeline` debe incluir `_brewTimeSeconds` en el `combine`.
- **Sin pantalla Resultado:** Todo queda en “Proceso en curso”: al terminar el timer se muestra la tarjeta “¿QUÉ SABOR HAS OBTENIDO?” y el “Guardar” en la topbar (solo texto). No hay botón Reiniciar al terminar; para repetir se vuelve atrás a Configuración.
- **Colores:**
  - **Tiempo (número y slider):** Modo día negro, modo noche blanco (no marrón). Incluye temporizador grande, tiempo transcurrido y barra de fases (`BrewTimeline`).
  - **Café (número en Ajustes técnicos):** Siempre marrón (`LocalCaramelAccent` / `--caramel-soft` / `--caramel-accent`).
  - **Sliders:** Agua = azul; ratio/café = marrón; tiempo = negro/blanco según tema; barra inactiva gris.
- **Modo Italiana (y Turco):** Mostrar slider “CANTIDAD DE CAFÉ (g)” cuando `isWaterEditable && !isRatioEditable`. Rango derivado de `waterMinMl/ratioMax` a `waterMaxMl/ratioMin`.

---

## 2. Incidencias CI/CD y TypeScript (deploy-web)

### 2.1 Error: `BrewBaristaTip[]` vs `{ label, value, icon: string }[]`

**Síntoma:** El job `deploy-web` fallaba con:

```text
Argument of type '{ label: string; value: string; icon: string; }[]' is not assignable to parameter of type 'BrewBaristaTip[]'.
Type '{ label: string; value: string; icon: string; }' is not assignable to type 'BrewBaristaTip'.
  Types of property 'icon' are incompatible.
  Type 'string' is not assignable to type '"coffee" | "grind" | "thermostat" | "water" | "clock"'.
```

**Causa:** En `webApp/src/core/brew.ts`, la variable `baseTips` se infiere por la cadena de ternarios y TypeScript acaba infiriendo `icon` como `string`.

**Solución:** Declarar explícitamente el tipo de `baseTips`:

```ts
const baseTips: BrewBaristaTip[] = !key ? defaults : key.includes("espresso") ? [ ... ] : ...
```

**Archivo:** `webApp/src/core/brew.ts` (función `getBrewBaristaTipsForMethod`).

---

### 2.2 Error: comparación `"coffee"` con tipo sin overlap

**Síntoma:** El job `deploy-web` fallaba con:

```text
This comparison appears to be unintentional because the types '"search" | "timeline" | "brewlab" | "diary" | "profile"' and '"coffee"' have no overlap.
```

**Causa:** En `AppContainer.tsx`, el `TopBar` se renderiza dentro de `{guardedActiveTab !== "coffee" ? ( <TopBar ... /> ) : ...}`. En ese bloque TypeScript restringe `guardedActiveTab` a los otros tabs, por lo que comparar de nuevo con `"coffee"` genera el error.

**Soluciones aplicadas:**

1. **Props del TopBar en la rama no-coffee:** En esa rama nunca estamos en `"coffee"`, así que se pasan valores fijos en lugar de depender de `guardedActiveTab === "coffee"`:
   - `coffeeTopbarFavoriteActive={false}`
   - `coffeeTopbarStockActive={false}`

2. **Clase `is-coffee` en el main shell:** Donde se usaba `guardedActiveTab === "coffee"` para la clase `main-shell-scroll is-coffee`, se cambió a `activeTab === "coffee"` (sin restricción de tipo).

**Archivos:** `webApp/src/app/AppContainer.tsx` (aprox. líneas 1760–1768).

---

## 3. Ramas y entornos

### 3.1 Ramas que se mantienen

Solo se usan estas ramas para entornos y despliegue:

| Rama     | Uso / entorno |
|----------|----------------|
| **main** | Desarrollo principal; no dispara el workflow de deploy. |
| **beta** | Pruebas abiertas (Play Console, web). |
| **alpha** | Pruebas cerradas. |
| **Interna** | Pruebas internas (solo Android en el workflow). |

Cualquier otra rama (codex/*, fix/*, webapp, webap, Alpha/Beta con mayúscula, etc.) se considera prescindible y puede eliminarse para no acumular ramas activas.

### 3.2 Acciones realizadas (limpieza de ramas)

- **Remoto (origin):** Se eliminaron las ramas `Alpha`, `Beta`, `webap`, `webapp`, `fix/alpha-consolidacion-webapp-webap-20260225`, `fix/widgets-diary-and-deprecations` y un gran número de ramas `codex/*`. Se mantienen solo `main`, `beta`, `alpha`, `Interna`. (Nota: la eliminación masiva de `codex/*` se lanzó en segundo plano; si alguna sigue existiendo en origin, se puede borrar con `git push origin --delete <nombre>`.)
- **Local:** Se eliminaron las ramas locales `Alpha`, `webap`, `beta` (luego se recreó `beta` desde `main`), `alpha-merge`, `beta-play-fix`, `webapp`. Se mantienen `main` y `beta` (y opcionalmente `alpha` si se trabaja con esa pestaña).

### 3.3 Flujo habitual: subir main y actualizar beta

1. Subir cambios a `main`:  
   `git push origin main`
2. Dejar `beta` igual que `main`:  
   `git push origin main:beta`

Si en remoto ya no existe `beta`, `main:beta` la crea. Si existe, la actualiza (puede requerir `--force` si la historia diverge y se quiere que beta sea exactamente main).

---

## 4. Referencias a otros documentos

| Tema | Documento |
|------|-----------|
| Workflow release y deploy (triggers, ramas, jobs) | `docs/RELEASE_DEPLOY_WORKFLOW.md` |
| Changelog detallado Brew/UI/colores/Italiana (04–05 mar) | `docs/commit-notes/commit-20260304-05-elaboracion-brew-ui-colores-italiana.md` |
| Compliance Play Console, orientación, edge-to-edge | `docs/ANDROID_PLAY_CONSOLE_COMPLIANCE.md` |
| ASO y App Links (assetlinks.json) | `docs/ASO_PLAY_STORE.md` |
| Revisión manual de crashes (sin automatización en CI) | `docs/CRASH_FIX_WEEKLY.md` |

---

*Este registro debe actualizarse cuando se resuelvan incidencias relevantes o se tomen decisiones que afecten a ramas, CI o comportamiento de la app (Android/WebApp).*
