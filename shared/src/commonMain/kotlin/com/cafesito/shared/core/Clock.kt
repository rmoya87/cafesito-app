package com.cafesito.shared.core

import kotlinx.datetime.Clock

fun interface TimeProvider {
    fun nowMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
}
