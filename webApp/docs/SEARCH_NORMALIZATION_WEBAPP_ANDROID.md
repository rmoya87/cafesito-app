# Búsqueda unificada (WebApp + Android) — Normalización y puntos de impacto

**Estado:** vigente  
**Última actualización:** 2026-03-26  
**Ámbito:** WebApp (`webApp/src/**`) y Android (`app/src/main/java/**`) en flujos de búsqueda de café/marca/usuario.

---

## 1. Objetivo

Asegurar que todas las búsquedas de texto relevantes usen una lógica homogénea:

- Ignorar diferencias de mayúsculas/minúsculas.
- Ignorar tildes/diacríticos.
- Ignorar apóstrofes y variantes (`'`, `’`, `` ` ``, `´`, `ʼ`).
- Normalizar espacios múltiples.

Ejemplo esperado: `Lor`, `L'Or`, `L´or`, `lór` deben comportarse igual.

---

## 2. Fuente de verdad por plataforma

### 2.1 WebApp

- Función canónica: `normalizeLookupText()`
- Archivo: `webApp/src/core/text.ts`

Reglas aplicadas por `normalizeLookupText()`:

1. `normalize("NFD")`
2. elimina diacríticos (`[\u0300-\u036f]`)
3. elimina apóstrofes (`['’\`´ʼ]`)
4. colapsa espacios
5. `trim()`
6. `toLowerCase()`

### 2.2 Android

- Funciones canónicas: `normalizeForSearch()` y `containsSearchQuery()`
- Archivo: `app/src/main/java/com/cafesito/app/ui/utils/SearchTextNormalizer.kt`

Reglas equivalentes:

1. `Normalizer.normalize(..., NFD)`
2. elimina marcas diacríticas (`\p{M}+`)
3. elimina apóstrofes (`['’\`´ʼ]`)
4. colapsa espacios
5. `lowercase(Locale.ROOT)`

---

## 3. Dónde impacta (mapa operativo)

## 3.1 WebApp — búsquedas de café/marca

- Búsqueda principal de Explorar: `webApp/src/hooks/domains/useAppDerivedData.ts`
- Selecciona café (Elaboración): `webApp/src/features/brew/BrewSelectCoffeePage.tsx`
- Añadir a despensa / selección en sheets de diario: `webApp/src/features/diary/DiarySheets.tsx`

Notas:

- En `Selecciona café`, el campo "Busca un café o marca" debe usar la misma normalización que Explorar.
- En el flujo Home > Tu despensa > `+` > Selecciona café, se reutiliza el componente de `BrewSelectCoffeePage`.

## 3.2 Android — búsquedas de café/marca

- Explorar (Search): `app/src/main/java/com/cafesito/app/ui/search/SearchViewModel.kt`
- Elaboración (Selecciona café): `app/src/main/java/com/cafesito/app/ui/components/BrewLabComponents.kt`
- Añadir café en diario: `app/src/main/java/com/cafesito/app/ui/diary/AddDiaryEntryScreen.kt`
- Añadir stock en despensa: `app/src/main/java/com/cafesito/app/ui/diary/AddStockScreen.kt`

---

## 4. Antipatrones prohibidos

- Filtrar con `toLowerCase()` directo sin pasar por la normalización canónica.
- Usar `contains(..., ignoreCase = true)` para búsquedas funcionales de café/marca donde se exija paridad.
- Duplicar regex de normalización en cada pantalla.

---

## 5. Checklist de regresión (obligatorio)

Al tocar cualquier flujo de búsqueda, validar mínimo:

1. `Lor` encuentra el mismo resultado que `L'Or`.
2. `L´or` encuentra el mismo resultado que `lor`.
3. `cafe` y `café` devuelven el mismo conjunto.
4. Se mantiene paridad WebApp/Android para el mismo término.

Pantallas mínimas a probar:

- WebApp: Explorar, Selecciona café (Elaboración), Añadir a despensa.
- Android: Search, Selecciona café (Brew), Añadir café (diario), Añadir stock.

---

## 6. Qué tocar si se cambia la regla

Si evoluciona la normalización:

1. Actualizar `webApp/src/core/text.ts` y `SearchTextNormalizer.kt`.
2. Revisar todos los consumidores listados en §3.
3. Ejecutar checklist de §5.
4. Actualizar este documento y `docs/REGISTRO_DESARROLLO_E_INCIDENCIAS.md`.
