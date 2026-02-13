package com.cafesito.app.ui.components

import java.util.Locale

fun String.toCoffeeNameFormat(locale: Locale = Locale.getDefault()): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.lowercase(locale).replaceFirstChar { first ->
        if (first.isLowerCase()) first.titlecase(locale) else first.toString()
    }
}

fun String.toCoffeeBrandFormat(locale: Locale = Locale.getDefault()): String = trim().uppercase(locale)
