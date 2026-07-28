package dev.bikram.filepipe.ui.screens.ruledetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleAgeFilterValidationTest {
    @Test
    fun blankAndValidAgeFiltersPass() {
        assertNull(validateAgeFilterValues("", ""))
        assertNull(validateAgeFilterValues("1", MAX_FILE_AGE_DAYS.toString()))
        assertNull(validateAgeFilterValues("30", "90"))
    }

    @Test
    fun nonPositiveOversizedAndUnparseableValuesFail() {
        assertEquals(
            AgeFilterValidationError.OUT_OF_RANGE,
            validateAgeFilterValues("0", ""),
        )
        assertEquals(
            AgeFilterValidationError.OUT_OF_RANGE,
            validateAgeFilterValues("", (MAX_FILE_AGE_DAYS + 1).toString()),
        )
        assertEquals(
            AgeFilterValidationError.OUT_OF_RANGE,
            validateAgeFilterValues("999999999999999999999999", ""),
        )
    }

    @Test
    fun minimumCannotExceedMaximum() {
        assertEquals(
            AgeFilterValidationError.MINIMUM_EXCEEDS_MAXIMUM,
            validateAgeFilterValues("91", "90"),
        )
    }
}
