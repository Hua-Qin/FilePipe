package dev.bikram.filepipe.domain.usecase

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ExecuteRulesSequentiallyTest {
    @Test
    fun executionNeverOverlapsInputs() =
        runBlocking {
            var activeExecutions = 0
            var maximumActiveExecutions = 0

            val outputs =
                executeSequentially(listOf(1, 2, 3)) { input ->
                    activeExecutions += 1
                    maximumActiveExecutions = maxOf(maximumActiveExecutions, activeExecutions)
                    delay(1)
                    activeExecutions -= 1
                    input * 2
                }

            assertEquals(listOf(2, 4, 6), outputs)
            assertEquals(1, maximumActiveExecutions)
        }
}
