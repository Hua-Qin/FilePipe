package dev.bikram.filepipe.ui.screens.ruledetail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleFileSizeConversionTest {
    @Test
    fun fractionalMegabytesRoundTripWithoutLosingBytes() {
        val bytes = 1_572_864L

        val displayValue = formatFileSizeMegabytes(bytes)

        assertEquals("1.5", displayValue)
        assertEquals(bytes, parseFileSizeMegabytes(displayValue))
    }

    @Test
    fun arbitraryByteCountRoundTripsExactly() {
        val bytes = 1_000_001L

        val displayValue = formatFileSizeMegabytes(bytes)

        assertEquals(bytes, parseFileSizeMegabytes(displayValue))
    }

    @Test
    fun decimalCommaAndFractionalBytesAreHandled() {
        assertEquals(1_572_864L, parseFileSizeMegabytes("1,5"))
        assertEquals(104_858L, parseFileSizeMegabytes("0.1"))
        assertNull(parseFileSizeMegabytes("0"))
        assertNull(parseFileSizeMegabytes("invalid"))
    }
}
