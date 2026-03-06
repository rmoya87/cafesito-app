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
