package com.cafesito.shared.domain.diary

/**
 * Objetivos y cálculo de tendencia del diario.
 * Lógica de negocio compartida por Android, iOS y webApp.
 */
object DiaryAnalyticsTargets {

    /**
     * Objetivo de hidratación (ml) para el período.
     * Hoy: 2000 ml, Semana: 14000 ml, Mes: 60000 ml.
     */
    fun hydrationTargetMl(period: DiaryPeriod): Int = when (period) {
        DiaryPeriod.HOY -> 2000
        DiaryPeriod.SEMANA -> 14_000
        DiaryPeriod.MES -> 60_000
    }

    /**
     * Objetivo de cafeína (mg) para el período.
     * Hoy: 160 mg, Semana: 1120 mg, Mes: 4800 mg.
     */
    fun caffeineTargetMg(period: DiaryPeriod): Int = when (period) {
        DiaryPeriod.HOY -> 160
        DiaryPeriod.SEMANA -> 1_120
        DiaryPeriod.MES -> 4_800
    }

    /**
     * Porcentaje de tendencia vs objetivo: (actual - objetivo) / objetivo * 100.
     * Positivo = por encima del objetivo, negativo = por debajo.
     * Se muestra en UI con flecha ↑/↓ y valor absoluto.
     */
    fun trendPercent(actual: Int, target: Int): Int {
        if (target <= 0) return 0
        return ((actual - target).toDouble() / target * 100).toInt()
    }

    /**
     * Progreso de hidratación 0..100 (actual / objetivo, limitado a 100).
     */
    fun hydrationProgressPercent(actualMl: Int, period: DiaryPeriod): Int {
        val target = hydrationTargetMl(period)
        if (target <= 0) return 0
        return (actualMl.coerceAtLeast(0).toDouble() / target * 100).toInt().coerceIn(0, 100)
    }
}
