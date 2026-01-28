package com.claudewatch.companion.kiosk

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for KioskManager tap detection logic.
 * Tests the triple-tap detection algorithm without Android dependencies.
 */
class KioskManagerTest {

    // Test the tap detection algorithm in isolation
    private lateinit var tapDetector: TapDetector

    @Before
    fun setup() {
        tapDetector = TapDetector(
            tapThresholdMs = 500L,
            tapsRequired = 3,
            cornerSizePx = 50f
        )
    }

    @Test
    fun `tap outside corner resets tap count`() {
        // Simulate taps outside the corner (x > 50 or y > 50)
        tapDetector.simulateTap(100f, 100f, 0L)
        tapDetector.simulateTap(100f, 100f, 100L)
        tapDetector.simulateTap(100f, 100f, 200L)

        assertFalse(tapDetector.shouldExit())
    }

    @Test
    fun `three taps in corner within threshold triggers exit`() {
        // All taps in corner (x < 50, y < 50) within 1500ms (3 * 500ms)
        tapDetector.simulateTap(25f, 25f, 0L)
        tapDetector.simulateTap(25f, 25f, 400L)
        tapDetector.simulateTap(25f, 25f, 800L)

        assertTrue(tapDetector.shouldExit())
    }

    @Test
    fun `two taps in corner does not trigger exit`() {
        tapDetector.simulateTap(25f, 25f, 0L)
        tapDetector.simulateTap(25f, 25f, 400L)

        assertFalse(tapDetector.shouldExit())
    }

    @Test
    fun `taps spread too far apart do not trigger exit`() {
        // Taps too slow (> 1500ms total for 3 taps)
        tapDetector.simulateTap(25f, 25f, 0L)
        tapDetector.simulateTap(25f, 25f, 1000L)
        tapDetector.simulateTap(25f, 25f, 2500L) // More than 1500ms from first

        assertFalse(tapDetector.shouldExit())
    }

    @Test
    fun `tap outside corner clears tap history`() {
        tapDetector.simulateTap(25f, 25f, 0L)
        tapDetector.simulateTap(25f, 25f, 400L)
        // Tap outside corner
        tapDetector.simulateTap(100f, 100f, 800L)
        // Try to complete sequence
        tapDetector.simulateTap(25f, 25f, 1000L)

        assertFalse(tapDetector.shouldExit())
    }

    @Test
    fun `four taps with first one expired still triggers exit`() {
        // First tap will be removed due to time, but last 3 are valid
        tapDetector.simulateTap(25f, 25f, 0L)
        tapDetector.simulateTap(25f, 25f, 1600L) // First tap expires here
        tapDetector.simulateTap(25f, 25f, 1800L)
        tapDetector.simulateTap(25f, 25f, 2000L)

        assertTrue(tapDetector.shouldExit())
    }

    @Test
    fun `edge case tap at exactly corner boundary is inside`() {
        // Tap at exactly x=50, y=50 is inside corner (uses > not >=)
        // So (50 > 50) is false, meaning tap counts as inside
        tapDetector.simulateTap(50f, 50f, 0L)
        tapDetector.simulateTap(50f, 50f, 400L)
        tapDetector.simulateTap(50f, 50f, 800L)

        assertTrue(tapDetector.shouldExit())
    }

    @Test
    fun `tap at 51,51 is outside corner`() {
        // Tap at x=51, y=51 is outside corner (51 > 50 is true)
        tapDetector.simulateTap(51f, 51f, 0L)
        tapDetector.simulateTap(51f, 51f, 400L)
        tapDetector.simulateTap(51f, 51f, 800L)

        assertFalse(tapDetector.shouldExit())
    }

    @Test
    fun `tap at 49,49 is inside corner`() {
        tapDetector.simulateTap(49f, 49f, 0L)
        tapDetector.simulateTap(49f, 49f, 400L)
        tapDetector.simulateTap(49f, 49f, 800L)

        assertTrue(tapDetector.shouldExit())
    }

    @Test
    fun `tap with x in corner but y outside does not count`() {
        tapDetector.simulateTap(25f, 100f, 0L)
        tapDetector.simulateTap(25f, 100f, 400L)
        tapDetector.simulateTap(25f, 100f, 800L)

        assertFalse(tapDetector.shouldExit())
    }

    @Test
    fun `tap with y in corner but x outside does not count`() {
        tapDetector.simulateTap(100f, 25f, 0L)
        tapDetector.simulateTap(100f, 25f, 400L)
        tapDetector.simulateTap(100f, 25f, 800L)

        assertFalse(tapDetector.shouldExit())
    }

    @Test
    fun `reset clears state after successful exit`() {
        tapDetector.simulateTap(25f, 25f, 0L)
        tapDetector.simulateTap(25f, 25f, 400L)
        tapDetector.simulateTap(25f, 25f, 800L)

        assertTrue(tapDetector.shouldExit())

        // After reset
        tapDetector.reset()
        assertFalse(tapDetector.shouldExit())
    }

    @Test
    fun `old taps are removed based on threshold`() {
        // Simulate old taps being pruned
        tapDetector.simulateTap(25f, 25f, 0L)
        tapDetector.simulateTap(25f, 25f, 100L)
        // Large time jump - old taps should be removed
        tapDetector.simulateTap(25f, 25f, 2000L)
        tapDetector.simulateTap(25f, 25f, 2100L)
        tapDetector.simulateTap(25f, 25f, 2200L)

        assertTrue(tapDetector.shouldExit())
    }

    /**
     * Helper class that implements the tap detection algorithm
     * matching KioskManager's logic, but without Android dependencies.
     */
    class TapDetector(
        private val tapThresholdMs: Long,
        private val tapsRequired: Int,
        private val cornerSizePx: Float
    ) {
        private val tapTimes = mutableListOf<Long>()
        private var exitTriggered = false

        fun simulateTap(x: Float, y: Float, timeMs: Long): Boolean {
            // Check if tap is in corner
            if (x > cornerSizePx || y > cornerSizePx) {
                tapTimes.clear()
                return false
            }

            tapTimes.add(timeMs)

            // Remove old taps
            val maxAge = tapThresholdMs * tapsRequired
            tapTimes.removeAll { timeMs - it > maxAge }

            // Check if we have enough taps
            if (tapTimes.size >= tapsRequired) {
                val recentTaps = tapTimes.takeLast(tapsRequired)
                if (recentTaps.last() - recentTaps.first() <= maxAge) {
                    tapTimes.clear()
                    exitTriggered = true
                    return true
                }
            }

            return false
        }

        fun shouldExit(): Boolean = exitTriggered

        fun reset() {
            tapTimes.clear()
            exitTriggered = false
        }
    }
}
