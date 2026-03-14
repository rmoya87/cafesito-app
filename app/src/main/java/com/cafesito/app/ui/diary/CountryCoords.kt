package com.cafesito.app.ui.diary

import java.text.Normalizer

/** Coordenadas lat/lng independientes del proveedor de mapas. */
data class MapCoords(val lat: Double, val lng: Double)

/**
 * Coordenadas aproximadas por país (origen de café) para marcadores en el mapa.
 * Claves normalizadas a minúsculas sin acentos.
 */
object CountryCoords {
    private fun normalizeKey(name: String): String =
        Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    private val coords = mapOf(
        "colombia" to MapCoords(4.57, -74.29),
        "brasil" to MapCoords(-14.24, -51.92),
        "etiopia" to MapCoords(9.03, 38.74),
        "etiopía" to MapCoords(9.03, 38.74),
        "mexico" to MapCoords(23.63, -102.55),
        "méxico" to MapCoords(23.63, -102.55),
        "guatemala" to MapCoords(15.78, -90.23),
        "honduras" to MapCoords(15.2, -86.24),
        "peru" to MapCoords(-9.19, -75.02),
        "perú" to MapCoords(-9.19, -75.02),
        "nicaragua" to MapCoords(12.87, -85.21),
        "costa_rica" to MapCoords(9.75, -83.75),
        "costa rica" to MapCoords(9.75, -83.75),
        "kenya" to MapCoords(-0.02, 37.91),
        "kenia" to MapCoords(-0.02, 37.91),
        "indonesia" to MapCoords(-0.79, 113.92),
        "vietnam" to MapCoords(14.06, 108.28),
        "viet_nam" to MapCoords(14.06, 108.28),
        "india" to MapCoords(20.59, 78.96),
        "rwanda" to MapCoords(-1.94, 29.87),
        "tanzania" to MapCoords(-6.37, 34.89),
        "uganda" to MapCoords(1.37, 32.29),
        "papua_nueva_guinea" to MapCoords(-6.31, 143.96),
        "papua nueva guinea" to MapCoords(-6.31, 143.96),
        "ecuador" to MapCoords(-1.83, -78.18),
        "bolivia" to MapCoords(-16.29, -63.59),
        "el_salvador" to MapCoords(13.79, -88.9),
        "el salvador" to MapCoords(13.79, -88.9),
        "yemen" to MapCoords(15.55, 48.52),
        "jamaica" to MapCoords(18.11, -77.3),
        "haiti" to MapCoords(18.97, -72.29),
        "haití" to MapCoords(18.97, -72.29),
        "republica_dominicana" to MapCoords(18.74, -70.16),
        "república dominicana" to MapCoords(18.74, -70.16),
        "camerun" to MapCoords(6.37, 12.35),
        "camerún" to MapCoords(6.37, 12.35),
        "burundi" to MapCoords(-3.37, 29.92),
        "china" to MapCoords(35.86, 104.2),
        "tailandia" to MapCoords(15.87, 100.99),
        "thailand" to MapCoords(15.87, 100.99),
        "filipinas" to MapCoords(12.88, 121.77),
        "laos" to MapCoords(19.86, 102.5),
        "myanmar" to MapCoords(21.91, 95.96),
        "timor_leste" to MapCoords(-8.87, 125.73),
        "timor oriental" to MapCoords(-8.87, 125.73),
        "madagascar" to MapCoords(-18.77, 46.87),
        "zambia" to MapCoords(-13.13, 27.85),
        "malawi" to MapCoords(-13.25, 34.3),
        "zimbabwe" to MapCoords(-19.02, 29.15),
        "venezuela" to MapCoords(6.42, -66.59),
        "panama" to MapCoords(8.54, -80.78),
        "panamá" to MapCoords(8.54, -80.78),
        "cuba" to MapCoords(21.52, -77.78),
        "puerto_rico" to MapCoords(18.22, -66.59),
        "puerto rico" to MapCoords(18.22, -66.59),
        "espana" to MapCoords(40.46, -3.75),
        "españa" to MapCoords(40.46, -3.75),
        "italia" to MapCoords(41.87, 12.57),
        "portugal" to MapCoords(39.4, -8.22),
        "francia" to MapCoords(46.23, 2.21),
        "alemania" to MapCoords(51.17, 10.45),
        "reino_unido" to MapCoords(55.38, -3.44),
        "reino unido" to MapCoords(55.38, -3.44),
        "usa" to MapCoords(37.09, -95.71),
        "eeuu" to MapCoords(37.09, -95.71),
        "estados unidos" to MapCoords(37.09, -95.71),
        "australia" to MapCoords(-25.27, 133.78),
        "japon" to MapCoords(36.2, 138.25),
        "japón" to MapCoords(36.2, 138.25),
        "corea_del_sur" to MapCoords(35.91, 127.77),
        "corea del sur" to MapCoords(35.91, 127.77),
        "taiwan" to MapCoords(23.7, 120.96),
        "taiwán" to MapCoords(23.7, 120.96),
        "nueva_zelanda" to MapCoords(-40.9, 174.89),
        "nueva zelanda" to MapCoords(-40.9, 174.89),
        "hawai" to MapCoords(19.9, -155.58),
        "hawái" to MapCoords(19.9, -155.58),
        "blend" to MapCoords(20.0, 0.0),
        "blends" to MapCoords(20.0, 0.0),
        "varios" to MapCoords(20.0, 0.0),
        "varios origenes" to MapCoords(20.0, 0.0),
        "varios orígenes" to MapCoords(20.0, 0.0)
    )

    fun getCoords(countryName: String?): MapCoords? {
        if (countryName.isNullOrBlank()) return null
        return coords[normalizeKey(countryName)]
    }
}
