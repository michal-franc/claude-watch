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

    // --- Helper ---

    @Suppress("UNCHECKED_CAST")
    private fun setState(state: WakeWordState) {
        val stateFlow = stateField.get(null) as MutableStateFlow<WakeWordState>
        stateFlow.value = state
    }
}
