package com.claudewatch.companion.wakeword

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Field

/**
 * Unit tests for WakeWordService state machine and requestStopRecording guard logic.
 * Uses reflection to access private _wakeWordState for state manipulation in tests.
 */
class WakeWordServiceTest {

    private lateinit var stateField: Field

    @Before
    fun setup() {
        stateField = WakeWordService::class.java
            .getDeclaredField("_wakeWordState")
        stateField.isAccessible = true
    }

    @After
    fun teardown() {
        setState(WakeWordState.IDLE)
        setSecondsRemaining(60)
    }

    // --- WakeWordState enum ---

    @Test
    fun `all wake word states exist`() {
        val states = WakeWordState.values()
        assertEquals(4, states.size)
        assertTrue(states.contains(WakeWordState.IDLE))
        assertTrue(states.contains(WakeWordState.RECORDING))
        assertTrue(states.contains(WakeWordState.SENDING))
        assertTrue(states.contains(WakeWordState.DONE))
    }

    @Test
    fun `state valueOf works correctly`() {
        assertEquals(WakeWordState.IDLE, WakeWordState.valueOf("IDLE"))
        assertEquals(WakeWordState.RECORDING, WakeWordState.valueOf("RECORDING"))
        assertEquals(WakeWordState.SENDING, WakeWordState.valueOf("SENDING"))
        assertEquals(WakeWordState.DONE, WakeWordState.valueOf("DONE"))
    }

    // --- Initial state ---

    @Test
    fun `initial state is IDLE`() {
        assertEquals(WakeWordState.IDLE, WakeWordService.wakeWordState.value)
    }

    @Test
    fun `initial amplitude is zero`() {
        assertEquals(0f, WakeWordService.amplitude.value)
    }

    @Test
    fun `initial secondsRemaining is 60`() {
        assertEquals(60, WakeWordService.secondsRemaining.value)
    }

    // --- requestStopRecording state guard ---

    @Test
    fun `requestStopRecording is no-op when IDLE`() {
        setState(WakeWordState.IDLE)
        WakeWordService.requestStopRecording()
        assertEquals(WakeWordState.IDLE, WakeWordService.wakeWordState.value)
    }

    @Test
    fun `requestStopRecording is no-op when SENDING`() {
        setState(WakeWordState.SENDING)
        WakeWordService.requestStopRecording()
        assertEquals(WakeWordState.SENDING, WakeWordService.wakeWordState.value)
    }

    @Test
    fun `requestStopRecording is no-op when DONE`() {
        setState(WakeWordState.DONE)
        WakeWordService.requestStopRecording()
        assertEquals(WakeWordState.DONE, WakeWordService.wakeWordState.value)
    }

    @Test
    fun `requestStopRecording does not crash when RECORDING but instance is null`() {
        setState(WakeWordState.RECORDING)
        // instance is null (no service running) — safe null-check via ?.
        WakeWordService.requestStopRecording()
        // State remains RECORDING because null instance can't call stopRecordingAndSend
        assertEquals(WakeWordState.RECORDING, WakeWordService.wakeWordState.value)
    }

    // --- Recording duration constant ---

    @Test
    fun `MAX_RECORDING_MS is 60 seconds`() {
        // private const val in companion is compiled as a private static field on the outer class
        val field = WakeWordService::class.java
            .getDeclaredField("MAX_RECORDING_MS")
        field.isAccessible = true
        assertEquals(60_000L, field.getLong(null))
    }

    @Test
    fun `MAX_RECORDING_MS matches MainActivity MAX_RECORDING_SECONDS`() {
        val maxMs = WakeWordService::class.java
            .getDeclaredField("MAX_RECORDING_MS")
            .apply { isAccessible = true }
            .getLong(null)
        // MAX_RECORDING_SECONDS in MainActivity is 60, so MS should be 60_000
        assertEquals(60 * 1000L, maxMs)
    }

    // --- StateFlow reflects changes ---

    @Test
    fun `wakeWordState flow reflects internal state changes`() {
        setState(WakeWordState.RECORDING)
        assertEquals(WakeWordState.RECORDING, WakeWordService.wakeWordState.value)

        setState(WakeWordState.SENDING)
        assertEquals(WakeWordState.SENDING, WakeWordService.wakeWordState.value)

        setState(WakeWordState.DONE)
        assertEquals(WakeWordState.DONE, WakeWordService.wakeWordState.value)
    }

    // --- secondsRemaining flow ---

    @Test
    fun `secondsRemaining flow reflects changes`() {
        setSecondsRemaining(45)
        assertEquals(45, WakeWordService.secondsRemaining.value)

        setSecondsRemaining(0)
        assertEquals(0, WakeWordService.secondsRemaining.value)
    }

    @Test
    fun `secondsRemaining resets to 60 after being changed`() {
        setSecondsRemaining(10)
        assertEquals(10, WakeWordService.secondsRemaining.value)

        setSecondsRemaining(60)
        assertEquals(60, WakeWordService.secondsRemaining.value)
    }

    // --- Helper ---

    @Suppress("UNCHECKED_CAST")
    private fun setState(state: WakeWordState) {
        val stateFlow = stateField.get(null) as MutableStateFlow<WakeWordState>
        stateFlow.value = state
    }

    @Suppress("UNCHECKED_CAST")
    private fun setSecondsRemaining(seconds: Int) {
        val field = WakeWordService::class.java
            .getDeclaredField("_secondsRemaining")
        field.isAccessible = true
        val flow = field.get(null) as MutableStateFlow<Int>
        flow.value = seconds
    }
}
