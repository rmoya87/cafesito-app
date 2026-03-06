# Brew Engine Parity Contract

Version: `2026.03.v1`

Objetivo: garantizar paridad estricta entre `shared` (Kotlin) y `webApp` (TypeScript).

## Casos canonicos (cafeina)

Cada implementacion debe devolver exactamente estos valores:

1. `method=Espresso, grams=18, hasCaffeine=true` -> `113 mg`
2. `method=Hario V60, grams=15, hasCaffeine=true` -> `91 mg`
3. `method=Hario V60, grams=15, hasCaffeine=false` -> `6 mg`
4. `method=Moca, grams=18, hasCaffeine=true` -> `125 mg`

## Casos canonicos (timeline)

1. `method=Hario V60, water=300`:
- 3 fases
- primera fase = `Bloom`

2. `method=Espresso, water=36`:
- 1 fase
- primera fase = `Extraccion`

## Casos canonicos (tamano de taza)

1. `amountMl=36` -> `Espresso`
2. `amountMl=260` -> `Mediano`
3. `amountMl=410` -> `Grande`

## Casos canonicos (perfil tecnico por metodo)

1. `method=Espresso`:
- `waterMinMl=25`
- `waterMaxMl=60`
- `defaultRatio=2.0`

2. `method=Hario V60`:
- `waterMinMl=180`
- `waterMaxMl=500`
- `defaultRatio=16.0`

## Casos canonicos (consejo en proceso en curso)

1. `method=Hario V60, ratio=16, water=300, phase=Bloom, remaining=4`:
- contiene `Asegura saturacion completa`
- contiene `4 s`

## Regla de cambios

Si se cambia cualquier constante del motor, se deben actualizar en el mismo PR:

- tests en `shared`
- tests en `webApp`
- esta tabla de contrato
