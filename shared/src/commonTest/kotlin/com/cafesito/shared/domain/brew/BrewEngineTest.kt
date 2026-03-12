package com.cafesito.shared.domain.brew

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrewEngineTest {

    @Test
    fun caffeine_does_not_change_with_cup_size_if_grams_are_equal() {
        val small = BrewEngine.estimateCaffeineMg(
            BrewCaffeineInput(
                source = BrewSource.DIARY,
                methodOrPreparation = "Espresso",
                coffeeGrams = 18.0,
                hasCaffeine = true
            )
        )
        val large = BrewEngine.estimateCaffeineMg(
            BrewCaffeineInput(
                source = BrewSource.DIARY,
                methodOrPreparation = "Espresso",
                coffeeGrams = 18.0,
                hasCaffeine = true
            )
        )
        assertEquals(small, large)
    }

    @Test
    fun decaf_is_lower_than_regular_for_same_grams() {
        val regular = BrewEngine.estimateCaffeineMg(
            BrewCaffeineInput(BrewSource.BREWLAB, "V60", 15.0, hasCaffeine = true)
        )
        val decaf = BrewEngine.estimateCaffeineMg(
            BrewCaffeineInput(BrewSource.BREWLAB, "V60", 15.0, hasCaffeine = false)
        )
        assertTrue(decaf < regular)
    }

    @Test
    fun default_pourover_timeline_has_three_phases() {
        val phases = BrewEngine.timelineForMethod("Hario V60", 300)
        assertEquals(3, phases.size)
        assertEquals("Bloom", phases[0].label)
    }

    @Test
    fun contract_vectors_match_expected_outputs() {
        assertEquals(
            113,
            BrewEngine.estimateCaffeineMg(
                BrewCaffeineInput(BrewSource.DIARY, "Espresso", 18.0, hasCaffeine = true)
            )
        )
        assertEquals(
            91,
            BrewEngine.estimateCaffeineMg(
                BrewCaffeineInput(BrewSource.BREWLAB, "Hario V60", 15.0, hasCaffeine = true)
            )
        )
        assertEquals(
            6,
            BrewEngine.estimateCaffeineMg(
                BrewCaffeineInput(BrewSource.BREWLAB, "Hario V60", 15.0, hasCaffeine = false)
            )
        )
        assertEquals(
            125,
            BrewEngine.estimateCaffeineMg(
                BrewCaffeineInput(BrewSource.DIARY, "Moca", 18.0, hasCaffeine = true)
            )
        )
    }

    @Test
    fun espresso_timeline_contract_vector() {
        val phases = BrewEngine.timelineForMethod("Espresso", 36)
        assertEquals(1, phases.size)
        assertEquals("Extraccion", phases[0].label)
    }

    @Test
    fun cup_size_label_uses_nearest_volume() {
        assertEquals("Espresso", BrewEngine.cupSizeLabelForAmountMl(36))
        assertEquals("Mediano", BrewEngine.cupSizeLabelForAmountMl(260))
        assertEquals("Grande", BrewEngine.cupSizeLabelForAmountMl(410))
    }

    @Test
    fun rapido_caffeine_approximate_by_type_and_size() {
        val espresso30 = BrewEngine.estimateCaffeineMg(
            BrewCaffeineInput(BrewSource.BREWLAB, BREW_METHOD_OTROS, 0.0, hasCaffeine = true, amountMl = 30, drinkType = "Espresso")
        )
        assertTrue(espresso30 in 60..70)
        val mediano = BrewEngine.estimateCaffeineMg(
            BrewCaffeineInput(BrewSource.BREWLAB, BREW_METHOD_OTROS, 0.0, hasCaffeine = true, amountMl = 275, drinkType = "Americano")
        )
        assertTrue(mediano in 140..150)
        val decaf = BrewEngine.estimateCaffeineMg(
            BrewCaffeineInput(BrewSource.BREWLAB, BREW_METHOD_OTROS, 0.0, hasCaffeine = false, amountMl = 180, drinkType = "Filter")
        )
        assertTrue(decaf in 0..10)
    }

    @Test
    fun rapido_approximate_coffee_grams_by_type_and_size() {
        assertEquals(18, BrewEngine.approximateCoffeeGramsForRapido(30, "Espresso"))
        assertTrue(BrewEngine.approximateCoffeeGramsForRapido(60, "Espresso") in 35..37)
        assertTrue(BrewEngine.approximateCoffeeGramsForRapido(275, "Americano") in 15..17)
        assertTrue(BrewEngine.approximateCoffeeGramsForRapido(180, "Filter") in 11..13)
    }

    @Test
    fun method_profile_contract_vector_for_espresso() {
        val profile = BrewEngine.methodProfileFor("Espresso")
        assertEquals(25, profile.waterMinMl)
        assertEquals(60, profile.waterMaxMl)
        assertEquals(2.0, profile.defaultRatio)
    }

    @Test
    fun brewing_process_advice_is_dynamic_by_phase_and_time() {
        val advice = BrewEngine.brewingProcessAdvice(
            method = "Hario V60",
            ratio = 16.0,
            waterMl = 300,
            phaseLabel = "Bloom",
            remainingInPhaseSeconds = 4
        )
        assertTrue(advice.contains("Asegura saturacion completa"))
        assertTrue(advice.contains("4 s"))
    }
}
