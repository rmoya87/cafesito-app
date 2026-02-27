package com.cafesito.app.ui.components

import java.util.Locale

fun String.toCoffeeNameFormat(locale: Locale = Locale.getDefault()): String {
    if (isEmpty()) return ""
    // We only trim start to prevent leading spaces. We keep trailing spaces so users can type words separated by space.
    val trimmed = trimStart()
    if (trimmed.isEmpty()) return ""
    return trimmed.lowercase(locale).replaceFirstChar { first ->
        if (first.isLowerCase()) first.titlecase(locale) else first.toString()
    }
}

fun String.toCoffeeBrandFormat(locale: Locale = Locale.getDefault()): String = trimStart().uppercase(locale)
