package com.claudewatch.app

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the recording auto-terminate timer logic.
 * Tests countdown formatting, warning threshold detection, and timer constants.
 *
 * Since CountDownTimer is an Android class that can't run in plain JUnit,
 * we extract the pure decision logic into a testable helper and verify
 * the algorithm directly.
 */
class RecordingTimerTest {

    private lateinit var timerLogic: RecordingTimerLogic

    @Before
    fun setup() {
        timerLogic = RecordingTimerLogic(
            maxRecordingSeconds = 60,
            warningSeconds = 10
        )
    }

    // --- Constants ---

    @Test
    fun `MAX_RECORDING_SECONDS is 60`() {
        // private const val in companion is compiled as a private static field on the outer class
        assertEquals(60, MainActivity::class.java
            .getDeclaredField("MAX_RECORDING_SECONDS")
            .apply { isAccessible = true }
            .getInt(null))
    }

    @Test
    fun `WARNING_SECONDS is 10`() {
        assertEquals(10, MainActivity::class.java
            .getDeclaredField("WARNING_SECONDS")
            .apply { isAccessible = true }
            .getInt(null))
    }

    // --- Seconds remaining calculation ---

    @Test
    fun `millisUntilFinished converts to correct seconds remaining`() {
        assertEquals(60, timerLogic.secondsLeft(60_000L))
        assertEquals(30, timerLogic.secondsLeft(30_000L))
        assertEquals(10, timerLogic.secondsLeft(10_000L))
        assertEquals(1, timerLogic.secondsLeft(1_000L))
        assertEquals(0, timerLogic.secondsLeft(0L))
    }

    @Test
    fun `millisUntilFinished rounds down partial seconds`() {
        // 10999ms -> 10 seconds (integer division)
        assertEquals(10, timerLogic.secondsLeft(10_999L))
        // 9500ms -> 9 seconds
        assertEquals(9, timerLogic.secondsLeft(9_500L))
        // 500ms -> 0 seconds
        assertEquals(0, timerLogic.secondsLeft(500L))
    }

    // --- Warning zone detection ---

    @Test
    fun `secondsLeft at warning threshold is in warning zone`() {
        assertTrue(timerLogic.isInWarningZone(10_000L))
    }

    @Test
    fun `secondsLeft below warning threshold is in warning zone`() {
        assertTrue(timerLogic.isInWarningZone(9_000L))
        assertTrue(timerLogic.isInWarningZone(5_000L))
        assertTrue(timerLogic.isInWarningZone(1_000L))
    }

    @Test
    fun `secondsLeft above warning threshold is not in warning zone`() {
        assertFalse(timerLogic.isInWarningZone(11_000L))
        assertFalse(timerLogic.isInWarningZone(30_000L))
        assertFalse(timerLogic.isInWarningZone(60_000L))
    }

    @Test
    fun `zero seconds remaining is in warning zone`() {
        assertTrue(timerLogic.isInWarningZone(0L))
    }

    // --- Warning haptic trigger ---

    @Test
    fun `haptic warning triggers exactly at warning threshold`() {
        assertTrue(timerLogic.shouldTriggerWarningHaptic(10_000L))
    }

    @Test
    fun `haptic warning does not trigger above warning threshold`() {
        assertFalse(timerLogic.shouldTriggerWarningHaptic(11_000L))
        assertFalse(timerLogic.shouldTriggerWarningHaptic(30_000L))
    }

    @Test
    fun `haptic warning does not trigger below warning threshold`() {
        assertFalse(timerLogic.shouldTriggerWarningHaptic(9_000L))
        assertFalse(timerLogic.shouldTriggerWarningHaptic(5_000L))
        assertFalse(timerLogic.shouldTriggerWarningHaptic(1_000L))
    }

    // --- Button text ---

    @Test
    fun `button text shows countdown in warning zone`() {
        assertEquals("Stop (10)", timerLogic.buttonText(10_000L))
        assertEquals("Stop (5)", timerLogic.buttonText(5_000L))
        assertEquals("Stop (1)", timerLogic.buttonText(1_000L))
    }

    @Test
    fun `button text shows Stop and Send outside warning zone`() {
        assertEquals("Stop & Send", timerLogic.buttonText(11_000L))
        assertEquals("Stop & Send", timerLogic.buttonText(30_000L))
        assertEquals("Stop & Send", timerLogic.buttonText(60_000L))
    }

    // --- Timer total duration ---

    @Test
    fun `timer total millis is maxRecordingSeconds times 1000`() {
        assertEquals(60_000L, timerLogic.totalMillis())
    }

    // --- Edge cases ---

    @Test
    fun `warning zone boundary at 10999ms rounds to 10 seconds and is in warning zone`() {
        // 10999ms -> 10 seconds, which equals WARNING_SECONDS
        assertTrue(timerLogic.isInWarningZone(10_999L))
        assertTrue(timerLogic.shouldTriggerWarningHaptic(10_999L))
        assertEquals("Stop (10)", timerLogic.buttonText(10_999L))
    }

    @Test
    fun `11000ms rounds to 11 seconds and is NOT in warning zone`() {
        assertFalse(timerLogic.isInWarningZone(11_000L))
        assertEquals("Stop & Send", timerLogic.buttonText(11_000L))
    }

    @Test
    fun `custom timer values work correctly`() {
        val custom = RecordingTimerLogic(maxRecordingSeconds = 30, warningSeconds = 5)
        assertEquals(30_000L, custom.totalMillis())
        assertTrue(custom.isInWarningZone(5_000L))
        assertFalse(custom.isInWarningZone(6_000L))
        assertTrue(custom.shouldTriggerWarningHaptic(5_000L))
        assertEquals("Stop (3)", custom.buttonText(3_000L))
    }

    /**
     * Pure logic extracted from MainActivity.startRecordingTimer().
     * Mirrors the onTick decision logic without Android CountDownTimer dependency.
     */
    class RecordingTimerLogic(
        private val maxRecordingSeconds: Int,
        private val warningSeconds: Int
    ) {
        fun totalMillis(): Long = maxRecordingSeconds * 1000L

        fun secondsLeft(millisUntilFinished: Long): Int =
            (millisUntilFinished / 1000).toInt()

        fun isInWarningZone(millisUntilFinished: Long): Boolean =
            secondsLeft(millisUntilFinished) <= warningSeconds

        fun shouldTriggerWarningHaptic(millisUntilFinished: Long): Boolean =
            secondsLeft(millisUntilFinished) == warningSeconds

        fun buttonText(millisUntilFinished: Long): String {
            val seconds = secondsLeft(millisUntilFinished)
            return if (seconds <= warningSeconds) {
                "Stop ($seconds)"
            } else {
                "Stop & Send"
            }
        }
    }
}
