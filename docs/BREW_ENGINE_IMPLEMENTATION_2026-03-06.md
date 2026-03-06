# Brew Engine - Cambios Implementados y Guia de Evolucion

Fecha: `2026-03-06`
Version de motor: `2026.03.v1`

## Objetivo

Unificar la logica de elaboracion y diario para Android, iOS (via shared) y WebApp, evitando divergencias en:

- Calculo de cafeina.
- Configuracion tecnica por metodo.
- Timeline de elaboracion.
- Consejos del barista.
- Etiqueta de tamano de taza al guardar en diario.

## Fuente unica de verdad

- Shared KMP: `shared/src/commonMain/kotlin/com/cafesito/shared/domain/brew/BrewEngine.kt`
- WebApp espejo funcional: `webApp/src/core/brewEngine.ts`

Regla: cualquier ajuste de negocio debe empezar en `BrewEngine` y reflejarse en tests de contrato de `shared` y `webApp`.

## Cambios funcionales implementados

### 1) Motor comun ampliado

Se anadieron al motor:

- `methodProfileFor(method)`
- `baristaTipsForMethod(method)`
- `brewAdvice(method, ratio, waterMl)`
- `brewingProcessAdvice(method, ratio, waterMl, phaseLabel, remainingInPhaseSeconds)`
- `cupSizeLabelForAmountMl(amountMl)`

Se mantiene:

- `estimateCaffeineMg(...)`
- `timelineForMethod(method, waterMl)`
- `hasCaffeineFromLabel(...)`

### 2) Android BrewLab conectado al motor

Archivos:

- `app/src/main/java/com/cafesito/app/ui/brewlab/BrewLabViewModel.kt`
- `app/src/main/java/com/cafesito/app/ui/brewlab/BrewLabScreen.kt`
- `app/src/main/java/com/cafesito/app/ui/components/BrewLabComponents.kt`

Cambios:

- `selectedMethodProfile` en ViewModel para exponer rango/default por metodo.
- `selectMethod` aplica `defaultWaterMl` y `defaultRatio` del motor.
- `setWaterAmount` y `setRatio` con clamp y step segun perfil.
- `setCoffeeGrams` recalcula ratio respetando perfil.
- `brewValuation` usa `BrewEngine.brewAdvice`.
- `ConfigStep` usa perfiles dinamicos para sliders e inputs (agua y ratio).
- `Proceso en curso` muestra consejo dinamico en vivo segun metodo + configuracion + fase + tiempo restante.
- Guardado a diario en BrewLab ahora incluye `sizeLabel` con `cupSizeLabelForAmountMl`.

### 3) WebApp diario alineado al motor

Archivos:

- `webApp/src/core/brewEngine.ts`
- `webApp/src/hooks/domains/useDiaryActions.ts`

Cambios:

- `saveBrewToDiary` calcula cafeina con `estimateCaffeineMg` (no heuristica local).
- `saveBrewToDiary` guarda `coffeeGrams` y `sizeLabel` con funcion comun `cupSizeLabelForAmountMl`.
- `handleUpdateDiaryEntry` soporta `coffeeGrams` y `sizeLabel` para mantener consistencia al editar.

## Contrato de perfiles por metodo

Se definieron rangos/defaults por metodo (agua/ratio) en motor. Ejemplos:

- Espresso: agua `25-60 ml`, ratio `1.8-2.8`, default `2.0`.
- Hario V60: agua `180-500 ml`, ratio `14-17`, default `16`.
- Italiana: agua `60-320 ml`, ratio `7.5-12`, default `10`.
- Prensa francesa: agua `250-1000 ml`, ratio `12-16`, default `14.5`.

Tabla completa: ver `BrewEngine.kt`.

## Regla de cafeina (clave funcional)

La cafeina depende de:

- Gramos de cafe.
- Metodo/preparacion (factor de extraccion).
- Si tiene cafeina o es descafeinado.

No depende directamente del tamano de taza si los gramos son iguales.

## Timeline de elaboracion

La duracion/fases se calculan por metodo en el motor comun. Android consume ese timeline desde ViewModel.

## Tests y blindaje de paridad

Shared:

- `shared/src/commonTest/kotlin/com/cafesito/shared/domain/brew/BrewEngineTest.kt`

Web:

- `webApp/src/core/brewEngine.test.ts`

Contrato documentado en:

- `docs/BREW_ENGINE_PARITY_CONTRACT.md`

## Validacion ejecutada

- Android/KMP: `./gradlew :app:compileDebugKotlin :shared:compileKotlinMetadata` -> OK.
- Web tests: `npm run test -- brewEngine.test.ts` -> OK.
- Web build completo: fallo preexistente en `webApp/src/app/AppContainer.tsx` (comparacion de ruta con `"coffee"`).

## Reglas para futuros desarrollos

1. Cambios de negocio solo en motor comun primero.
2. Si se cambia formula, perfiles, tips o timeline:
- actualizar `shared` y `webApp`.
- actualizar `docs/BREW_ENGINE_PARITY_CONTRACT.md`.
- actualizar tests de ambos lados.
3. Nunca introducir heuristicas locales de cafeina o tamano en UI.
4. Guardado de elaboracion en diario siempre con:
- `coffeeGrams`
- `sizeLabel`
- `preparationType`
- `caffeineMg` del motor

## Pendiente estructural (iOS)

El motor ya esta listo para iOS via `shared`. Cuando exista flujo BrewLab en iOS, debe consumir `BrewEngine` sin duplicar reglas.
