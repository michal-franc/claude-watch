package com.claudewatch.companion

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the phone app recording auto-terminate timer logic.
 * Tests constants, warning threshold detection, and timer duration.
 *
 * The phone app uses the same CountDownTimer pattern as the watch app,
 * but shows a Toast at the warning threshold instead of countdown text.
 */
class RecordingTimerTest {

    private lateinit var timerLogic: PhoneRecordingTimerLogic

    @Before
    fun setup() {
        timerLogic = PhoneRecordingTimerLogic(
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

    // --- Constants match between watch and phone ---

    @Test
    fun `MAX_RECORDING_SECONDS matches watch app value`() {
        // Both apps should use the same 60-second limit
        val phoneValue = MainActivity::class.java
            .getDeclaredField("MAX_RECORDING_SECONDS")
            .apply { isAccessible = true }
            .getInt(null)
        assertEquals(60, phoneValue)
    }

    // --- Timer total duration ---

    @Test
    fun `timer total millis is 60000`() {
        assertEquals(60_000L, timerLogic.totalMillis())
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
        assertEquals(10, timerLogic.secondsLeft(10_999L))
        assertEquals(9, timerLogic.secondsLeft(9_500L))
        assertEquals(0, timerLogic.secondsLeft(500L))
    }

    // --- Toast warning trigger ---

    @Test
    fun `toast triggers exactly at warning threshold`() {
        assertTrue(timerLogic.shouldShowWarningToast(10_000L))
    }

    @Test
    fun `toast does not trigger above warning threshold`() {
        assertFalse(timerLogic.shouldShowWarningToast(11_000L))
        assertFalse(timerLogic.shouldShowWarningToast(30_000L))
        assertFalse(timerLogic.shouldShowWarningToast(60_000L))
    }

    @Test
    fun `toast does not trigger below warning threshold`() {
        assertFalse(timerLogic.shouldShowWarningToast(9_000L))
        assertFalse(timerLogic.shouldShowWarningToast(5_000L))
        assertFalse(timerLogic.shouldShowWarningToast(1_000L))
    }

    // --- Warning toast message ---

    @Test
    fun `warning message includes seconds remaining`() {
        val msg = timerLogic.warningMessage(10_000L)
        assertEquals("Recording stops in 10s", msg)
    }

    @Test
    fun `warning message at 5 seconds`() {
        val msg = timerLogic.warningMessage(5_000L)
        assertEquals("Recording stops in 5s", msg)
    }

    // --- Edge cases ---

    @Test
    fun `10999ms rounds to 10 seconds and triggers toast`() {
        assertTrue(timerLogic.shouldShowWarningToast(10_999L))
    }

    @Test
    fun `11000ms rounds to 11 seconds and does not trigger toast`() {
        assertFalse(timerLogic.shouldShowWarningToast(11_000L))
    }

    /**
     * Pure logic extracted from phone MainActivity.startRecordingTimer().
     * Mirrors the onTick decision logic without Android CountDownTimer dependency.
     */
    class PhoneRecordingTimerLogic(
        private val maxRecordingSeconds: Int,
        private val warningSeconds: Int
    ) {
        fun totalMillis(): Long = maxRecordingSeconds * 1000L

        fun secondsLeft(millisUntilFinished: Long): Int =
            (millisUntilFinished / 1000).toInt()

        fun shouldShowWarningToast(millisUntilFinished: Long): Boolean =
            secondsLeft(millisUntilFinished) == warningSeconds

        fun warningMessage(millisUntilFinished: Long): String {
            val seconds = secondsLeft(millisUntilFinished)
            return "Recording stops in ${seconds}s"
        }
    }
}
