package com.cafesito.shared.domain.brew

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class BrewSource {
    BREWLAB,
    DIARY
}

data class BrewCaffeineInput(
    val source: BrewSource,
    val methodOrPreparation: String,
    val coffeeGrams: Double,
    val hasCaffeine: Boolean,
    /** Para método Rápido: volumen en ml (tamaño) para estimar cafeína de forma aproximada. */
    val amountMl: Int? = null,
    /** Para método Rápido: tipo de bebida (ej. Espresso, Americano) para estimar cafeína de forma aproximada. */
    val drinkType: String? = null
)

data class BrewTimelinePhase(
    val label: String,
    val instruction: String,
    val durationSeconds: Int
)

data class BrewMethodProfile(
    val waterMinMl: Int,
    val waterMaxMl: Int,
    val waterStepMl: Int,
    val defaultWaterMl: Int,
    val ratioMin: Double,
    val ratioMax: Double,
    val ratioStep: Double,
    val defaultRatio: Double
)

data class BrewBaristaTip(
    val label: String,
    val value: String,
    val iconKey: String
)

data class BrewTimeProfile(
    val minSeconds: Int,
    val maxSeconds: Int,
    val defaultSeconds: Int
)

object BrewEngine {
    const val CALCULATION_VERSION: String = "2026.03.v1"

    private const val BASE_MG_PER_GRAM_REGULAR = 6.3
    private const val BASE_MG_PER_GRAM_DECAF = 0.45

    private val DEFAULT_METHOD_PROFILE = BrewMethodProfile(
        waterMinMl = 150,
        waterMaxMl = 600,
        waterStepMl = 10,
        defaultWaterMl = 300,
        ratioMin = 14.0,
        ratioMax = 18.0,
        ratioStep = 0.5,
        defaultRatio = 16.0
    )

    private val DEFAULT_TIME_PROFILE = BrewTimeProfile(
        minSeconds = 90,
        maxSeconds = 360,
        defaultSeconds = 180
    )

    fun estimateCaffeineMg(input: BrewCaffeineInput): Int {
        val normMethod = normalize(input.methodOrPreparation)
        val useRapidoApprox = (normMethod == "rapido" || normMethod == "otros") &&
            input.amountMl != null && input.amountMl > 0 && !input.drinkType.isNullOrBlank()
        if (useRapidoApprox) {
            return approximateCaffeineForRapido(input.amountMl!!, input.drinkType!!, input.hasCaffeine)
        }
        val grams = max(0.0, input.coffeeGrams)
        if (grams <= 0.0) return 0
        val mgPerGram = if (input.hasCaffeine) BASE_MG_PER_GRAM_REGULAR else BASE_MG_PER_GRAM_DECAF
        val methodFactor = methodFactor(normMethod)
        return max(0, (grams * mgPerGram * methodFactor).roundToInt())
    }

    /**
     * Estimación aproximada de cafeína para método Rápido en función del tipo de bebida y tamaño (ml).
     * Valores de referencia: espresso ~65 mg/30 ml; filtro por tamaño típico (pequeño ~95, mediano ~145, grande ~195, XL ~250).
     */
    private fun approximateCaffeineForRapido(amountMl: Int, drinkType: String, hasCaffeine: Boolean): Int {
        val typeNorm = normalize(drinkType)
        val regularMg = when {
            typeNorm.contains("espresso") -> {
                val shots = amountMl.toDouble() / 30.0
                max(0, (shots * 65.0).roundToInt().coerceIn(0, 260))
            }
            else -> {
                val sizeTable = listOf(30 to 65, 180 to 95, 275 to 145, 375 to 195, 475 to 250)
                val (_, nearestMg) = sizeTable.minByOrNull { abs(it.first - amountMl) } ?: (275 to 145)
                val prev = sizeTable.filter { it.first <= amountMl }.maxByOrNull { it.first }
                val next = sizeTable.filter { it.first >= amountMl }.minByOrNull { it.first }
                when {
                    prev == null && next != null -> next.second
                    prev != null && next == null -> prev.second
                    prev != null && next != null && prev.first != next.first -> {
                        val t = (amountMl - prev.first).toDouble() / (next.first - prev.first)
                        max(0, (prev.second + t * (next.second - prev.second)).roundToInt())
                    }
                    else -> nearestMg
                }
            }
        }
        return if (hasCaffeine) regularMg else max(0, (regularMg * 0.05).roundToInt())
    }

    fun methodProfileFor(method: String): BrewMethodProfile {
        val key = normalize(method)
        return when {
            key == "agua" -> BrewMethodProfile(
                waterMinMl = 100,
                waterMaxMl = 1000,
                waterStepMl = 50,
                defaultWaterMl = 250,
                ratioMin = 0.0,
                ratioMax = 0.0,
                ratioStep = 0.0,
                defaultRatio = 0.0
            )
            key.contains("espresso") -> BrewMethodProfile(25, 60, 1, 36, 1.8, 2.8, 0.1, 2.0)
            key.contains("italiana") -> BrewMethodProfile(60, 320, 5, 150, 7.5, 12.0, 0.5, 10.0)
            key.contains("turco") -> BrewMethodProfile(60, 180, 5, 90, 8.0, 12.0, 0.5, 10.0)
            key.contains("aeropress") -> BrewMethodProfile(150, 300, 5, 220, 13.0, 17.0, 0.5, 15.0)
            key.contains("prensa") -> BrewMethodProfile(250, 1000, 10, 400, 12.0, 16.0, 0.5, 14.5)
            key.contains("chemex") -> BrewMethodProfile(300, 900, 10, 500, 14.0, 17.0, 0.5, 15.5)
            key.contains("goteo") -> BrewMethodProfile(200, 1200, 10, 400, 15.0, 18.0, 0.5, 16.5)
            key.contains("hario") || key.contains("v60") -> BrewMethodProfile(180, 500, 5, 300, 14.0, 17.0, 0.5, 16.0)
            key.contains("sifon") -> BrewMethodProfile(200, 700, 10, 350, 14.0, 17.0, 0.5, 15.0)
            key.contains("manual") -> BrewMethodProfile(150, 600, 10, 300, 14.0, 18.0, 0.5, 16.0)
            else -> DEFAULT_METHOD_PROFILE
        }
    }

    fun timeProfileFor(method: String): BrewTimeProfile {
        val key = normalize(method)
        return when {
            key == "agua" -> BrewTimeProfile(minSeconds = 0, maxSeconds = 0, defaultSeconds = 0)
            key.contains("espresso") -> BrewTimeProfile(20, 40, 27)
            key.contains("italiana") -> BrewTimeProfile(120, 360, 210)
            key.contains("turco") -> BrewTimeProfile(90, 240, 160)
            key.contains("aeropress") -> BrewTimeProfile(90, 240, 150)
            key.contains("prensa") -> BrewTimeProfile(180, 420, 240)
            key.contains("chemex") -> BrewTimeProfile(180, 360, 240)
            key.contains("goteo") -> BrewTimeProfile(180, 420, 300)
            key.contains("hario") || key.contains("v60") || key.contains("manual") -> BrewTimeProfile(120, 330, 195)
            key.contains("sifon") -> BrewTimeProfile(120, 300, 195)
            else -> DEFAULT_TIME_PROFILE
        }
    }

    fun timelineForMethod(
        method: String,
        waterMl: Int,
        espressoSeconds: Int? = null,
        targetTotalSeconds: Int? = null
    ): List<BrewTimelinePhase> {
        val key = normalize(method)
        if (key.contains("espresso")) {
            val extractionSeconds = max(20, min(40, espressoSeconds ?: 25))
            return listOf(
                BrewTimelinePhase(
                    label = "Extraccion",
                    instruction = "Aplica presion constante. Vigila el flujo: debe ser como un hilo de miel. Busca obtener unos 36-40g de liquido final.",
                    durationSeconds = extractionSeconds
                )
            )
        }
        if (key.contains("prensa")) {
            return scaleTimelineToTarget(listOf(
                BrewTimelinePhase(
                    label = "Inmersion",
                    instruction = "Vierte todo el agua caliente uniformemente sobre el cafe. Coloca la tapa sin presionar para mantener el calor.",
                    durationSeconds = 240
                )
            ), targetTotalSeconds)
        }
        if (key.contains("aeropress")) {
            return scaleTimelineToTarget(listOf(
                BrewTimelinePhase(
                    label = "Pre-infusion",
                    instruction = "Vierte unos 50ml de agua para humedecer todo el cafe. Remueve suavemente 3 veces para asegurar una extraccion uniforme.",
                    durationSeconds = 30
                ),
                BrewTimelinePhase(
                    label = "Infusion",
                    instruction = "Anade el resto del agua. Deja que el cafe repose e interactue con el agua para extraer todos sus sabores.",
                    durationSeconds = 90
                ),
                BrewTimelinePhase(
                    label = "Presion",
                    instruction = "Presiona el embolo hacia abajo con una fuerza firme y constante. Escucha el 'sssh' final y detente.",
                    durationSeconds = 30
                )
            ), targetTotalSeconds)
        }
        if (key.contains("italiana")) {
            val boilTime = (120 + (waterMl * 0.2)).roundToInt()
            return scaleTimelineToTarget(listOf(
                BrewTimelinePhase(
                    label = "Calentamiento",
                    instruction = "Manten el fuego medio-bajo. El agua en la base empezara a crear presion para subir por la chimenea.",
                    durationSeconds = boilTime
                ),
                BrewTimelinePhase(
                    label = "Extraccion",
                    instruction = "Cuando el cafe empiece a salir, baja el fuego o retiralo. Escucha el burbujeo suave y detente antes del chorro final.",
                    durationSeconds = 40
                )
            ), targetTotalSeconds)
        }
        if (key.contains("turco")) {
            return scaleTimelineToTarget(listOf(
                BrewTimelinePhase(
                    label = "Infusion",
                    instruction = "Calienta a fuego muy lento hasta que veas que se forma una espuma densa y oscura en la superficie.",
                    durationSeconds = 120
                ),
                BrewTimelinePhase(
                    label = "Levantamiento 1",
                    instruction = "Retira el cezve del fuego justo antes de que hierva. Deja que la espuma baje un poco y vuelve al fuego.",
                    durationSeconds = 20
                ),
                BrewTimelinePhase(
                    label = "Levantamiento 2",
                    instruction = "Repite el proceso: deja que suba la espuma por segunda vez para intensificar el cuerpo y sabor.",
                    durationSeconds = 20
                ),
                BrewTimelinePhase(
                    label = "Toque final",
                    instruction = "Ultimo ciclo de espuma. El cafe turco se caracteriza por su densidad y su sedimento unico.",
                    durationSeconds = 20
                )
            ), targetTotalSeconds)
        }
        if (key.contains("sifon")) {
            return scaleTimelineToTarget(listOf(
                BrewTimelinePhase(
                    label = "Ascenso",
                    instruction = "La presion enviara el agua a la camara superior. Espera a que se estabilice antes de anadir el cafe.",
                    durationSeconds = 90
                ),
                BrewTimelinePhase(
                    label = "Mezcla",
                    instruction = "Anade el cafe molido y remueve en circulos suavemente. Asegurate de que todo el cafe este sumergido.",
                    durationSeconds = 60
                ),
                BrewTimelinePhase(
                    label = "Efecto vacio",
                    instruction = "Retira la fuente de calor. El enfriamiento creara un vacio que filtrara el cafe hacia abajo a traves del filtro.",
                    durationSeconds = 45
                )
            ), targetTotalSeconds)
        }

        val boundedWater = min(1200, max(50, waterMl))
        val totalPourTime = max(90, min(300, (120 + (boundedWater - 250) * 0.18).roundToInt()))
        val bloomMl = boundedWater / 10
        val pourMl = boundedWater - bloomMl
        val phases = listOf(
            BrewTimelinePhase(
                label = "Bloom",
                instruction = "Humedece el cafe con ${bloomMl}ml de agua y espera a que libere CO2.",
                durationSeconds = 30
            ),
            BrewTimelinePhase(
                label = "Vertido principal",
                instruction = "Vierte ${pourMl}ml en circulos lentos y controlados.",
                durationSeconds = totalPourTime
            ),
            BrewTimelinePhase(
                label = "Drenado",
                instruction = "Deja que el lecho termine de drenar para completar la extraccion.",
                durationSeconds = 35
            )
        )
        return scaleTimelineToTarget(phases, targetTotalSeconds)
    }

    fun baristaTipsForMethod(
        method: String,
        ratio: Double? = null,
        waterMl: Int? = null,
        coffeeGrams: Double? = null,
        brewTimeSeconds: Int? = null
    ): List<BrewBaristaTip> {
        val key = normalize(method)
        val defaults = listOf(
            BrewBaristaTip("MOLIENDA", "Media", "grind"),
            BrewBaristaTip("TEMPERATURA", "92-96C", "thermostat"),
            BrewBaristaTip("RATIO", "1:15 a 1:17", "coffee"),
            BrewBaristaTip("BLOOM", "30-45s con 2x de agua", "water"),
            BrewBaristaTip("VERTIDO", "Constante y en espiral", "water"),
            BrewBaristaTip("TIEMPO", "2:30-3:30", "clock"),
            BrewBaristaTip("AJUSTE ACIDEZ", "Muele mas fino", "grind"),
            BrewBaristaTip("AJUSTE AMARGOR", "Muele mas grueso", "grind")
        )
        val baseTips = if (key.isBlank()) defaults else when {
            key.contains("espresso") -> listOf(
                BrewBaristaTip("MOLIENDA", "Fina", "grind"),
                BrewBaristaTip("TEMPERATURA", "90-94C", "thermostat"),
                BrewBaristaTip("RATIO", "1:2 (ej. 18g -> 36g)", "coffee"),
                BrewBaristaTip("TIEMPO", "25-32s", "clock"),
                BrewBaristaTip("DISTRIBUCION", "Nivela antes del tamp", "coffee"),
                BrewBaristaTip("PREINFUSION", "Suave para evitar canalizacion", "water"),
                BrewBaristaTip("AJUSTE RAPIDO", "Si corre rapido, mas fino", "grind"),
                BrewBaristaTip("AJUSTE LENTO", "Si se ahoga, mas grueso", "grind")
            )
            key.contains("italiana") -> listOf(
                BrewBaristaTip("MOLIENDA", "Media-fina", "grind"),
                BrewBaristaTip("AGUA", "Caliente en base", "water"),
                BrewBaristaTip("FUEGO", "Medio-bajo", "thermostat"),
                BrewBaristaTip("CORTE", "Retira al primer burbujeo", "clock"),
                BrewBaristaTip("FILTRO", "No compactar cafe", "coffee"),
                BrewBaristaTip("RATIO", "Mas cafe para mas cuerpo", "coffee"),
                BrewBaristaTip("AMARGOR", "Evita fuego alto", "thermostat")
            )
            key.contains("aeropress") -> listOf(
                BrewBaristaTip("MOLIENDA", "Fina-media", "grind"),
                BrewBaristaTip("TEMPERATURA", "85-92C", "thermostat"),
                BrewBaristaTip("INFUSION", "1:30-2:00", "clock"),
                BrewBaristaTip("PRESION", "Suave y constante", "coffee"),
                BrewBaristaTip("REMOVIDO", "1-2 agitaciones suaves", "water"),
                BrewBaristaTip("PAPEL", "Mas limpieza en taza", "coffee"),
                BrewBaristaTip("METAL", "Mas cuerpo y textura", "coffee")
            )
            key.contains("chemex") -> listOf(
                BrewBaristaTip("MOLIENDA", "Media-gruesa", "grind"),
                BrewBaristaTip("TEMPERATURA", "93-96C", "thermostat"),
                BrewBaristaTip("FILTRO", "Enjuague generoso", "water"),
                BrewBaristaTip("TIEMPO", "3:30-4:30", "clock"),
                BrewBaristaTip("VERTIDO", "Pausado, sin colapsar filtro", "water"),
                BrewBaristaTip("RATIO", "1:15 a 1:16", "coffee"),
                BrewBaristaTip("AJUSTE LENTO", "Si drena lento, mas grueso", "grind")
            )
            key.contains("prensa") -> listOf(
                BrewBaristaTip("MOLIENDA", "Gruesa y uniforme", "grind"),
                BrewBaristaTip("TEMPERATURA", "93-96C", "thermostat"),
                BrewBaristaTip("INFUSION", "4:00", "clock"),
                BrewBaristaTip("PRENSADO", "Lento, sin golpear", "coffee"),
                BrewBaristaTip("COSTRA", "Romper y retirar espuma", "water"),
                BrewBaristaTip("RATIO", "1:14 a 1:16", "coffee"),
                BrewBaristaTip("DECANTAR", "Servir al terminar", "clock")
            )
            key.contains("sifon") -> listOf(
                BrewBaristaTip("MOLIENDA", "Media", "grind"),
                BrewBaristaTip("TEMPERATURA", "91-94C", "thermostat"),
                BrewBaristaTip("AGITACION", "Suave y breve", "water"),
                BrewBaristaTip("BAJADA", "45-60s al vacio", "clock"),
                BrewBaristaTip("HERVOR", "Controlado, no violento", "thermostat"),
                BrewBaristaTip("CONTACTO", "1:30-2:30 total", "clock"),
                BrewBaristaTip("FILTRO", "Limpio para evitar rancidez", "coffee")
            )
            key.contains("turco") -> listOf(
                BrewBaristaTip("MOLIENDA", "Extra fina", "grind"),
                BrewBaristaTip("FUEGO", "Muy bajo", "thermostat"),
                BrewBaristaTip("ESPUMA", "3 levantamientos", "coffee"),
                BrewBaristaTip("AGUA", "Casi ebullicion, no hervir", "water"),
                BrewBaristaTip("REMOVIDO", "Solo al inicio", "water"),
                BrewBaristaTip("DESCANSO", "Breve antes de servir", "clock"),
                BrewBaristaTip("DENSIDAD", "Taza corta y concentrada", "coffee")
            )
            key.contains("goteo") -> listOf(
                BrewBaristaTip("MOLIENDA", "Media", "grind"),
                BrewBaristaTip("RATIO", "55-65g por litro", "coffee"),
                BrewBaristaTip("TEMPERATURA", "92-96C", "thermostat"),
                BrewBaristaTip("SERVICIO", "Consumir recien hecho", "clock"),
                BrewBaristaTip("FILTRO", "Enjuagar antes de usar", "water"),
                BrewBaristaTip("CARGA", "Nivelar cama de cafe", "coffee"),
                BrewBaristaTip("PLACA", "Evitar sobrecalentamiento", "thermostat")
            )
            key.contains("hario") || key.contains("v60") || key.contains("manual") -> listOf(
                BrewBaristaTip("MOLIENDA", "Media-fina", "grind"),
                BrewBaristaTip("BLOOM", "30-45s con 2x de agua", "water"),
                BrewBaristaTip("TEMPERATURA", "92-96C", "thermostat"),
                BrewBaristaTip("TIEMPO", "2:30-3:15", "clock"),
                BrewBaristaTip("RATIO", "1:15 a 1:17", "coffee"),
                BrewBaristaTip("VERTIDO", "Pulsos cortos y constantes", "water"),
                BrewBaristaTip("AJUSTE ACIDEZ", "Muele mas fino", "grind"),
                BrewBaristaTip("AJUSTE AMARGOR", "Muele mas grueso", "grind")
            )
            else -> defaults
        }

        val compactBaseTips = compactBaristaFixedTips(baseTips)
        if (ratio == null || waterMl == null) return compactBaseTips

        val profile = methodProfileFor(method)
        val safeRatio = ratio.coerceIn(profile.ratioMin, profile.ratioMax)
        val safeWater = waterMl.coerceIn(profile.waterMinMl, profile.waterMaxMl)
        val safeCoffeeGrams = max(1.0, coffeeGrams ?: (safeWater / max(0.1, safeRatio)))
        val ratioSpan = max(0.1, profile.ratioMax - profile.ratioMin)
        val ratioNormalized = (safeRatio - profile.ratioMin) / ratioSpan
        val waterSpan = max(1, profile.waterMaxMl - profile.waterMinMl)
        val waterNormalized = (safeWater - profile.waterMinMl).toDouble() / waterSpan.toDouble()
        val isEspresso = key.contains("espresso")

        val dynamicTips = mutableListOf(
            if (ratioNormalized <= 0.3) {
                BrewBaristaTip("PERFIL ACTUAL", "Mas concentrado; si amarga, abre punto de molienda.", "grind")
            } else if (ratioNormalized >= 0.7) {
                BrewBaristaTip("PERFIL ACTUAL", "Mas ligero; si queda acuoso, muele un poco mas fino.", "grind")
            } else {
                BrewBaristaTip("PERFIL ACTUAL", "Equilibrado; manten ritmo y distribucion constantes.", "coffee")
            },
            if (waterNormalized <= 0.33) {
                BrewBaristaTip("VOLUMEN", "Tramo corto del metodo: prioriza control y uniformidad.", "water")
            } else if (waterNormalized >= 0.66) {
                BrewBaristaTip("VOLUMEN", "Tramo alto del metodo: evita dilucion con vertido estable.", "water")
            } else {
                BrewBaristaTip("VOLUMEN", "Tramo medio: buen balance entre cuerpo y claridad.", "water")
            }
        )

        if (isEspresso) {
            val timeProfile = timeProfileFor(method)
            val safeBrewTime = (brewTimeSeconds ?: timeProfile.defaultSeconds).coerceIn(timeProfile.minSeconds, timeProfile.maxSeconds)
            dynamicTips += when {
                safeBrewTime < 25 -> BrewBaristaTip("TIEMPO ACTUAL", "Corto: sube 1-2 s o afina molienda para mas extraccion.", "clock")
                safeBrewTime > 32 -> BrewBaristaTip("TIEMPO ACTUAL", "Largo: baja 1-2 s o abre molienda para evitar amargor.", "clock")
                else -> BrewBaristaTip("TIEMPO ACTUAL", "En ventana ideal: busca flujo continuo y crema uniforme.", "clock")
            }
            dynamicTips += when {
                safeCoffeeGrams < 16.0 -> BrewBaristaTip("DOSIS", "Baja para espresso; puedes subirla si buscas mas cuerpo.", "coffee")
                safeCoffeeGrams > 20.0 -> BrewBaristaTip("DOSIS", "Alta para espresso; cuida no sobre-extraer.", "coffee")
                else -> BrewBaristaTip("DOSIS", "Dentro de rango clasico para espresso.", "coffee")
            }
        } else {
            dynamicTips += if (safeRatio <= profile.defaultRatio) {
                BrewBaristaTip("RATIO ACTUAL", "Mas intenso; vierte suave para mantener dulzor.", "coffee")
            } else {
                BrewBaristaTip("RATIO ACTUAL", "Mas limpio; si falta cuerpo, sube extraccion.", "coffee")
            }
        }

        return dynamicTips + compactBaseTips
    }

    @Suppress("UNUSED_PARAMETER")
    fun brewAdvice(method: String, ratio: Double, waterMl: Int): String {
        val key = normalize(method)
        return if (key.contains("espresso")) {
            "Configuracion aplicada. Manten flujo estable y ajusta con los consejos del barista."
        } else {
            "Configuracion aplicada. Usa los consejos del barista para afinar molienda, vertido y cuerpo."
        }
    }
    fun brewingProcessAdvice(
        method: String,
        ratio: Double,
        waterMl: Int,
        phaseLabel: String,
        remainingInPhaseSeconds: Int,
        brewTimeSeconds: Int? = null
    ): String {
        val key = normalize(method)
        val phase = normalize(phaseLabel)
        val profile = methodProfileFor(method)
        val span = max(0.1, profile.ratioMax - profile.ratioMin)
        val normalized = (ratio - profile.ratioMin) / span

        val extractionTip = if (key.contains("espresso")) {
            val time = max(20, min(40, brewTimeSeconds ?: 27))
            when {
                time < 25 -> "Extraccion corta: puede quedar acida; afina molienda o sube 1-2 s."
                time <= 32 -> "Extraccion en ventana ideal: manten flujo estable y crema uniforme."
                else -> "Extraccion larga: puede amargar; abre molienda o corta antes."
            }
        } else when {
            normalized <= 0.3 -> "Perfil concentrado: usa vertido suave y evita agitar de mas."
            normalized >= 0.7 -> "Perfil ligero: para mas cuerpo, aumenta un poco contacto o finura."
            else -> "Perfil equilibrado: manten ritmo y flujo constantes."
        }

        val phaseTip = when {
            phase.contains("bloom") || phase.contains("pre-infusion") -> "Asegura saturacion completa del lecho antes de continuar."
            phase.contains("vertido") || phase.contains("mezcla") -> "Mantiene altura corta de vertido para no canalizar."
            phase.contains("extraccion") || phase.contains("presion") -> "Controla el flujo: si acelera demasiado, corrige mas fino."
            phase.contains("inmersion") || phase.contains("infusion") -> "Mantiene temperatura estable, sin remover en exceso."
            else -> "Busca consistencia de flujo y lecho uniforme."
        }

        val timeTip = when {
            remainingInPhaseSeconds <= 5 -> "Cierra esta fase en $remainingInPhaseSeconds s y prepara la transicion."
            remainingInPhaseSeconds <= 15 -> "Queda poco de fase: prioriza precision sobre velocidad."
            else -> "Mantiene el patron actual para sostener la extraccion."
        }

        val methodTip = when {
            key.contains("espresso") -> "En espresso, corta al rubio claro para evitar amargor final."
            key.contains("italiana") -> "En italiana, retira al primer burbujeo fuerte para no quemar."
            key.contains("prensa") -> "En prensa, rompe costra suave y decanta al terminar."
            key.contains("aeropress") -> "En aeropress, presion constante sin empujar de golpe."
            else -> "Cuida temperatura y distribucion para una taza limpia."
        }

        val methodTimeProfile = timeProfileFor(method)
        val timeConfigTip = if (key.contains("espresso")) {
            brewTimeSeconds?.let { configured ->
                when {
                    configured < methodTimeProfile.defaultSeconds * 0.85 -> "Tiempo corto para espresso: potencia acidez."
                    configured > methodTimeProfile.defaultSeconds * 1.15 -> "Tiempo largo para espresso: aumenta cuerpo y riesgo de amargor."
                    else -> "Tiempo de espresso en ventana recomendada."
                }
            }.orEmpty()
        } else ""

        val volumeTip = when {
            waterMl > profile.defaultWaterMl * 1.25 -> "Receta larga: vigila no diluir en exceso."
            waterMl < profile.defaultWaterMl * 0.75 -> "Receta corta: evita sobre-extraer por exceso de contacto."
            else -> "Volumen dentro de rango recomendado para este metodo."
        }

        return "$phaseTip $extractionTip $timeTip $timeConfigTip $methodTip $volumeTip"
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun scaleTimelineToTarget(
        phases: List<BrewTimelinePhase>,
        targetTotalSeconds: Int?
    ): List<BrewTimelinePhase> {
        if (phases.isEmpty()) return phases
        val target = targetTotalSeconds ?: return phases
        if (target <= 0) return phases
        val baseTotal = phases.sumOf { it.durationSeconds }.coerceAtLeast(1)
        if (baseTotal == target) return phases
        val scaledRaw = phases.map { phase ->
            val ratio = phase.durationSeconds.toDouble() / baseTotal.toDouble()
            max(1, (target * ratio).toInt())
        }.toMutableList()
        val diff = target - scaledRaw.sum()
        if (diff != 0) {
            val lastIdx = scaledRaw.lastIndex
            scaledRaw[lastIdx] = max(1, scaledRaw[lastIdx] + diff)
        }
        return phases.mapIndexed { index, phase ->
            phase.copy(durationSeconds = scaledRaw[index])
        }
    }
    fun cupSizeLabelForAmountMl(amountMl: Int): String {
        val options = listOf(
            30 to "Espresso",
            180 to "Pequeño",
            275 to "Mediano",
            375 to "Grande",
            475 to "Tazón XL"
        )
        val safeAmount = max(0, amountMl)
        return options.minByOrNull { abs(it.first - safeAmount) }?.second ?: "Mediano"
    }

    /**
     * Dosis aproximada de café (gramos) para método Rápido según tipo de bebida y tamaño (ml).
     * Espresso: ~18 g por shot (30 ml); filtro/otros: por tamaño típico (~12–22 g).
     */
    fun approximateCoffeeGramsForRapido(amountMl: Int, drinkType: String): Int {
        val typeNorm = normalize(drinkType)
        return when {
            typeNorm.contains("espresso") -> {
                val shots = amountMl.toDouble() / 30.0
                max(1, (shots * 18.0).roundToInt().coerceIn(1, 100))
            }
            else -> {
                val sizeTable = listOf(30 to 18, 180 to 12, 275 to 16, 375 to 20, 475 to 24)
                val prev = sizeTable.filter { it.first <= amountMl }.maxByOrNull { it.first }
                val next = sizeTable.filter { it.first >= amountMl }.minByOrNull { it.first }
                when {
                    prev == null && next != null -> next.second
                    prev != null && next == null -> prev.second
                    prev != null && next != null && prev.first != next.first -> {
                        val t = (amountMl - prev.first).toDouble() / (next.first - prev.first)
                        max(1, (prev.second + t * (next.second - prev.second)).roundToInt())
                    }
                    else -> sizeTable.minByOrNull { abs(it.first - amountMl) }?.second ?: 16
                }
            }
        }
    }

    fun hasCaffeineFromLabel(caffeineLabel: String?): Boolean {
        val key = normalize(caffeineLabel.orEmpty())
        if (key.isBlank()) return true
        if (key.contains("descaf")) return false
        if (key == "no") return false
        if (key.contains("sin cafe")) return false
        if (key.contains("sincafe")) return false
        return true
    }

    private fun methodFactor(normalized: String): Double = when {
        normalized.contains("moca") -> 1.10
        normalized.contains("turco") -> 1.10
        normalized.contains("italiana") -> 1.08
        normalized.contains("romano") -> 0.92
        normalized.contains("frappuccino") -> 0.88
        normalized.contains("vienes") -> 0.90
        normalized.contains("chemex") -> 0.95
        normalized.contains("hario") || normalized.contains("v60") || normalized.contains("manual") -> 0.96
        normalized.contains("prensa") -> 0.92
        normalized.contains("sifon") -> 0.95
        else -> 1.0
    }

    private fun compactBaristaFixedTips(tips: List<BrewBaristaTip>): List<BrewBaristaTip> {
        if (tips.isEmpty()) return tips

        val baseBlocks = mutableListOf<String>()
        val processBlocks = mutableListOf<String>()
        val adjustBlocks = mutableListOf<String>()
        val extraBlocks = mutableListOf<String>()

        tips.forEach { tip ->
            val key = normalize(tip.label)
            val block = "${tip.label}: ${tip.value}"
            val isBase = key.contains("molienda") || key.contains("temperatura") || key.contains("ratio")
            val isAdjust = key.contains("ajuste") || key.contains("acidez") || key.contains("amargor")
            val isProcess = key.contains("tiempo") ||
                key.contains("bloom") ||
                key.contains("vertido") ||
                key.contains("infusion") ||
                key.contains("presion") ||
                key.contains("preinfusion") ||
                key.contains("filtro") ||
                key.contains("fuego") ||
                key.contains("agua") ||
                key.contains("removido") ||
                key.contains("contacto") ||
                key.contains("corte") ||
                key.contains("servicio")

            when {
                isBase -> baseBlocks += block
                isAdjust -> adjustBlocks += block
                isProcess -> processBlocks += block
                else -> extraBlocks += block
            }
        }

        val compact = mutableListOf<BrewBaristaTip>()
        if (baseBlocks.isNotEmpty()) compact += BrewBaristaTip("BASE", baseBlocks.joinToString(" · "), "coffee")
        if (processBlocks.isNotEmpty()) compact += BrewBaristaTip("PROCESO", processBlocks.joinToString(" · "), "water")
        if (adjustBlocks.isNotEmpty()) compact += BrewBaristaTip("AJUSTE", adjustBlocks.joinToString(" · "), "grind")
        if (extraBlocks.isNotEmpty()) compact += BrewBaristaTip("DETALLE", extraBlocks.joinToString(" · "), "clock")
        return if (compact.isNotEmpty()) compact else tips
    }

    private fun normalize(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace("�", "a")
            .replace("�", "e")
            .replace("�", "i")
            .replace("�", "o")
            .replace("�", "u")
            .replace("�", "n")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
    }
}


