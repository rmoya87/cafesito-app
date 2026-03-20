package com.cafesito.app.ui.utils

import java.text.Normalizer
import java.util.Locale

private val DIACRITICS_REGEX = Regex("\\p{M}+")
private val APOSTROPHES_REGEX = Regex("['’`´ʼ]")
private val MULTI_SPACE_REGEX = Regex("\\s+")

fun String?.normalizeForSearch(): String {
    return Normalizer.normalize((this ?: "").trim(), Normalizer.Form.NFD)
        .replace(DIACRITICS_REGEX, "")
        .replace(APOSTROPHES_REGEX, "")
        .replace(MULTI_SPACE_REGEX, " ")
        .lowercase(Locale.ROOT)
}

fun String?.containsSearchQuery(rawQuery: String): Boolean {
    val normalizedQuery = rawQuery.normalizeForSearch()
    if (normalizedQuery.isBlank()) return true
    return this.normalizeForSearch().contains(normalizedQuery)
}
