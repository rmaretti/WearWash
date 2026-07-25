package com.wearwash.app.ui.screens.items

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemFormValidationTest {
    private val today = LocalDate.of(2026, 7, 25)

    @Test
    fun `valid form accepts optional dates price and non-negative counts`() {
        assertTrue(
            ItemFormState(
                name = "Linen shirt",
                purchaseDate = "2026-07-20",
                purchasePrice = "129.90",
                initialUsageCount = "0",
                initialWashingCount = "2",
            ).isValid(today),
        )
    }

    @Test
    fun `invalid dates future dates prices and counts are rejected`() {
        assertFalse(ItemFormState(name = "Shirt", purchaseDate = "not-a-date").isValid(today))
        assertFalse(ItemFormState(name = "Shirt", purchaseDate = "2026-07-26").isValid(today))
        assertFalse(ItemFormState(name = "Shirt", lastWashingDate = "2026-07-26").isValid(today))
        assertFalse(ItemFormState(name = "Shirt", purchasePrice = "-1").isValid(today))
        assertFalse(ItemFormState(name = "Shirt", initialUsageCount = "-1").isValid(today))
        assertFalse(ItemFormState(name = "Shirt", initialWashingCount = "abc").isValid(today))
    }

    @Test
    fun `name and active washing threshold are required`() {
        assertFalse(ItemFormState(name = "").isValid(today))
        assertFalse(
            ItemFormState(
                name = "Shirt",
                washingUsageThreshold = "0",
            ).isValid(today),
        )
    }
}
