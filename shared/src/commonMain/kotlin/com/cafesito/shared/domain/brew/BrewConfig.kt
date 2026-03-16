package com.cafesito.shared.domain.brew

/**
 * Entrada mínima de diario para ordenar métodos por uso reciente.
 */
data class BrewDiaryEntryForOrder(
    val preparationType: String,
    val type: String,
    val timestamp: Long
)

/**
 * Nombres de métodos de elaboración estándar (orden alfabético base).
 * Las plataformas (Android/Web) mapean estos nombres a iconos/drawables.
 */
val BREW_METHOD_NAMES: List<String> = listOf(
    "Aeropress",
    "Chemex",
    "Espresso",
    "Goteo",
    "Hario V60",
    "Italiana",
    "Manual",
    "Prensa francesa",
    "Sifón",
    "Turco"
)

/** Nombre del método «rápido» (sin parámetros); antes «Otros». */
const val BREW_METHOD_OTROS: String = "Rápido"
const val BREW_METHOD_AGUA: String = "Agua"

/** Límites libres en elaboración: el usuario puede poner la cantidad de agua y café que quiera. Ratio y forma se calculan en base a ello. */
const val BREW_WATER_ABS_MIN_ML = 1
const val BREW_WATER_ABS_MAX_ML = 5000
const val BREW_COFFEE_ABS_MIN_G = 0.5f
const val BREW_COFFEE_ABS_MAX_G = 2000f

/** Rango solo del slider café (0–50 g); el input numérico permite desde ABS_MIN hasta ABS_MAX. */
const val BREW_SLIDER_MIN_COFFEE_G = 0f
const val BREW_SLIDER_MAX_COFFEE_G = 50f
/** Rango solo del slider agua (0–500 ml); el input numérico permite hasta ABS_MAX. */
const val BREW_SLIDER_MIN_WATER_ML = 0
const val BREW_SLIDER_MAX_WATER_ML = 500
const val BREW_SLIDER_MAX_TIME_S = 60

/**
 * Extrae el nombre del método de elaboración de una entrada para ordenar.
 * Acepta "Otros" en datos antiguos y lo normaliza a BREW_METHOD_OTROS ("Rápido").
 */
private fun BrewDiaryEntryForOrder.extractMethodName(byName: Set<String>): String? = when {
    type.uppercase() == "WATER" -> BREW_METHOD_AGUA
    else -> {
        val prep = preparationType.trim()
        val match = Regex("^Lab:\\s*([^(]+)").find(prep)
        val raw = match?.groupValues?.getOrNull(1)?.trim() ?: ""
        when {
            raw.isNotBlank() && raw in byName -> raw
            raw.equals("Otros", ignoreCase = true) -> BREW_METHOD_OTROS
            else -> null
        }
    }
}

/**
 * Orden para mostrar métodos: el último utilizado primero (más a la izquierda), luego el anterior, etc.;
 * los no usados en orden alfabético; Otros y Agua al final si no se usaron.
 * @param diaryEntries entradas de diario (cualquier orden; se usa el timestamp para el último uso)
 * @return lista de nombres de método en el orden a mostrar
 */
fun getOrderedBrewMethods(diaryEntries: List<BrewDiaryEntryForOrder>): List<String> {
    val allNames = BREW_METHOD_NAMES + BREW_METHOD_OTROS + BREW_METHOD_AGUA
    val byName = allNames.toSet()
    // Por cada método usado, timestamp del último uso (máximo)
    val lastUsedTimestamp = mutableMapOf<String, Long>()
    for (entry in diaryEntries) {
        val methodName = entry.extractMethodName(byName) ?: continue
        val current = lastUsedTimestamp[methodName] ?: 0L
        if (entry.timestamp > current) lastUsedTimestamp[methodName] = entry.timestamp
    }
    // Ordenar usados por último uso descendente (más reciente primero = izquierda)
    val usedOrder = lastUsedTimestamp.entries
        .sortedByDescending { it.value }
        .map { it.key }
    val unused = BREW_METHOD_NAMES.filter { it !in usedOrder }.sorted()
    val result = (usedOrder + unused).toMutableList()
    if (BREW_METHOD_OTROS !in result) result.add(BREW_METHOD_OTROS)
    if (BREW_METHOD_AGUA !in result) result.add(BREW_METHOD_AGUA)
    return result
}
