package com.wearwash.app.domain.logic

import com.wearwash.app.domain.model.WashingCriteriaType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WashingReadinessTest {
    private val today = LocalDate.of(2026, 7, 23)

    @Test
    fun `usage rule becomes ready at threshold`() {
        val readiness = evaluateWashingReadiness(
            WashingRule(WashingCriteriaType.ByUsage, usageThreshold = 3),
            usesSinceWash = 3,
            lastWashingDate = null,
            today = today,
        )

        assertTrue(readiness.needsWashing)
        assertEquals(WashingReadinessReason.Usage, readiness.reason)
    }

    @Test
    fun `usage rule remains clean below threshold`() {
        val readiness = evaluateWashingReadiness(
            WashingRule(WashingCriteriaType.ByUsage, usageThreshold = 3),
            usesSinceWash = 2,
            lastWashingDate = null,
            today = today,
        )

        assertFalse(readiness.needsWashing)
        assertNull(readiness.reason)
    }

    @Test
    fun `date rule becomes ready at threshold`() {
        val readiness = evaluateWashingReadiness(
            WashingRule(WashingCriteriaType.ByDate, dayThreshold = 7),
            usesSinceWash = 0,
            lastWashingDate = today.minusDays(7),
            today = today,
        )

        assertTrue(readiness.needsWashing)
        assertEquals(WashingReadinessReason.Date, readiness.reason)
    }

    @Test
    fun `date rule without a previous wash is not ready`() {
        val readiness = evaluateWashingReadiness(
            WashingRule(WashingCriteriaType.ByDate, dayThreshold = 7),
            usesSinceWash = 0,
            lastWashingDate = null,
            today = today,
        )

        assertFalse(readiness.needsWashing)
    }

    @Test
    fun `either rule reports both thresholds when both are reached`() {
        val readiness = evaluateWashingReadiness(
            WashingRule(
                WashingCriteriaType.ByUsageOrDate,
                usageThreshold = 2,
                dayThreshold = 5,
            ),
            usesSinceWash = 2,
            lastWashingDate = today.minusDays(5),
            today = today,
        )

        assertTrue(readiness.needsWashing)
        assertEquals(WashingReadinessReason.UsageAndDate, readiness.reason)
    }

    @Test
    fun `manual rule never becomes automatically ready`() {
        val readiness = evaluateWashingReadiness(
            WashingRule(
                WashingCriteriaType.Manual,
                usageThreshold = 1,
                dayThreshold = 1,
            ),
            usesSinceWash = 100,
            lastWashingDate = today.minusYears(1),
            today = today,
        )

        assertFalse(readiness.needsWashing)
        assertNull(readiness.reason)
    }
}
