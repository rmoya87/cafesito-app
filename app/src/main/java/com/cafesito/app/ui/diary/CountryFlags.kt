package com.cafesito.app.ui.diary

import java.text.Normalizer

/**
 * Emoji de bandera por país (ISO 3166-1 alpha-2) para marcadores en el mapa.
 * Mismas claves normalizadas que CountryCoords.
 */
object CountryFlags {
    private fun normalizeKey(name: String): String =
        Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")

    private val iso2 = mapOf(
        "colombia" to "CO",
        "brasil" to "BR",
        "etiopia" to "ET",
        "etiopía" to "ET",
        "mexico" to "MX",
        "méxico" to "MX",
        "guatemala" to "GT",
        "honduras" to "HN",
        "peru" to "PE",
        "perú" to "PE",
        "nicaragua" to "NI",
        "costa_rica" to "CR",
        "costa rica" to "CR",
        "kenya" to "KE",
        "kenia" to "KE",
        "indonesia" to "ID",
        "vietnam" to "VN",
        "viet_nam" to "VN",
        "india" to "IN",
        "rwanda" to "RW",
        "tanzania" to "TZ",
        "uganda" to "UG",
        "papua_nueva_guinea" to "PG",
        "papua nueva guinea" to "PG",
        "ecuador" to "EC",
        "bolivia" to "BO",
        "el_salvador" to "SV",
        "el salvador" to "SV",
        "yemen" to "YE",
        "jamaica" to "JM",
        "haiti" to "HT",
        "haití" to "HT",
        "republica_dominicana" to "DO",
        "república dominicana" to "DO",
        "camerun" to "CM",
        "camerún" to "CM",
        "burundi" to "BI",
        "china" to "CN",
        "tailandia" to "TH",
        "thailand" to "TH",
        "filipinas" to "PH",
        "laos" to "LA",
        "myanmar" to "MM",
        "timor_leste" to "TL",
        "timor oriental" to "TL",
        "madagascar" to "MG",
        "zambia" to "ZM",
        "malawi" to "MW",
        "zimbabwe" to "ZW",
        "venezuela" to "VE",
        "panama" to "PA",
        "panamá" to "PA",
        "cuba" to "CU",
        "puerto_rico" to "PR",
        "puerto rico" to "PR",
        "espana" to "ES",
        "españa" to "ES",
        "italia" to "IT",
        "portugal" to "PT",
        "francia" to "FR",
        "alemania" to "DE",
        "reino_unido" to "GB",
        "reino unido" to "GB",
        "usa" to "US",
        "eeuu" to "US",
        "estados unidos" to "US",
        "australia" to "AU",
        "japon" to "JP",
        "japón" to "JP",
        "corea_del_sur" to "KR",
        "corea del sur" to "KR",
        "taiwan" to "TW",
        "taiwán" to "TW",
        "nueva_zelanda" to "NZ",
        "nueva zelanda" to "NZ",
        "hawai" to "US",
        "hawái" to "US"
    )

    private fun isoToFlagEmoji(iso: String): String {
        if (iso.length != 2) return "\uD83C\uDF0D" // 🌍
        val a = 0x1F1E6 + (iso[0].code - 65)
        val b = 0x1F1E6 + (iso[1].code - 65)
        return String(Character.toChars(a)) + String(Character.toChars(b))
    }

    /** Devuelve el emoji de bandera para un nombre de país, o 🌍 si no hay código. */
    fun getFlagEmoji(countryName: String?): String {
        if (countryName.isNullOrBlank()) return "\uD83C\uDF0D"
        val code = iso2[normalizeKey(countryName)] ?: return "\uD83C\uDF0D"
        return isoToFlagEmoji(code)
    }
}
