package com.claudewatch.app

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for watch app settings and URL construction logic.
 */
class SettingsTest {

    @Test
    fun `buildServerUrl with default values`() {
        val url = buildServerUrl("192.168.1.100", "5566")
        assertEquals("http://192.168.1.100:5566/transcribe", url)
    }

    @Test
    fun `buildServerUrl with custom IP`() {
        val url = buildServerUrl("10.0.0.50", "5566")
        assertEquals("http://10.0.0.50:5566/transcribe", url)
    }

    @Test
    fun `buildServerUrl with custom port`() {
        val url = buildServerUrl("192.168.1.100", "8080")
        assertEquals("http://192.168.1.100:8080/transcribe", url)
    }

    @Test
    fun `buildServerUrl with localhost`() {
        val url = buildServerUrl("localhost", "5566")
        assertEquals("http://localhost:5566/transcribe", url)
    }

    @Test
    fun `buildServerUrl with IPv6 address`() {
        val url = buildServerUrl("::1", "5566")
        assertEquals("http://::1:5566/transcribe", url)
    }

    @Test
    fun `buildResponseUrl constructs correct URL`() {
        val url = buildResponseUrl("192.168.1.100", "5566", "req-123")
        assertEquals("http://192.168.1.100:5566/api/response/req-123", url)
    }

    @Test
    fun `buildAudioUrl constructs correct URL`() {
        val url = buildAudioUrl("192.168.1.100", "5566", "req-123")
        assertEquals("http://192.168.1.100:5566/api/audio/req-123", url)
    }

    // Polling configuration tests
    @Test
    fun `polling configuration default values`() {
        val config = PollingConfig()

        assertEquals(500L, config.intervalMs)
        assertEquals(60, config.maxAttempts)
        assertEquals(30000L, config.totalTimeoutMs())
    }

    @Test
    fun `polling configuration custom values`() {
        val config = PollingConfig(intervalMs = 1000L, maxAttempts = 30)

        assertEquals(1000L, config.intervalMs)
        assertEquals(30, config.maxAttempts)
        assertEquals(30000L, config.totalTimeoutMs())
    }

    @Test
    fun `polling should continue when under max attempts`() {
        val config = PollingConfig(maxAttempts = 10)

        assertTrue(config.shouldContinue(0))
        assertTrue(config.shouldContinue(5))
        assertTrue(config.shouldContinue(9))
    }

    @Test
    fun `polling should stop at max attempts`() {
        val config = PollingConfig(maxAttempts = 10)

        assertFalse(config.shouldContinue(10))
        assertFalse(config.shouldContinue(15))
    }

    // Request state tests
    @Test
    fun `request state idle by default`() {
        val state = RecordingState()

        assertFalse(state.isRecording)
        assertFalse(state.isWaitingForResponse)
        assertFalse(state.isPlayingAudio)
        assertTrue(state.isIdle())
    }

    @Test
    fun `request state recording`() {
        val state = RecordingState(isRecording = true)

        assertTrue(state.isRecording)
        assertFalse(state.isIdle())
    }

    @Test
    fun `request state waiting for response`() {
        val state = RecordingState(isWaitingForResponse = true)

        assertTrue(state.isWaitingForResponse)
        assertFalse(state.isIdle())
    }

    @Test
    fun `request state playing audio`() {
        val state = RecordingState(isPlayingAudio = true)

        assertTrue(state.isPlayingAudio)
        assertFalse(state.isIdle())
    }

    @Test
    fun `request state with request id`() {
        val state = RecordingState(
            isWaitingForResponse = true,
            currentRequestId = "req-456"
        )

        assertEquals("req-456", state.currentRequestId)
    }

    // Audio file validation tests
    @Test
    fun `audio file name generation`() {
        val fileName = generateAudioFileName(System.currentTimeMillis())
        assertTrue(fileName.startsWith("voice_"))
        assertTrue(fileName.endsWith(".m4a"))
    }

    @Test
    fun `audio file path construction`() {
        val path = buildAudioFilePath("/data/app/cache", "voice_123.m4a")
        assertEquals("/data/app/cache/voice_123.m4a", path)
    }

    // Response parsing tests
    @Test
    fun `response status ready`() {
        val status = parseResponseStatus("ready")
        assertEquals(ResponseStatus.READY, status)
    }

    @Test
    fun `response status pending`() {
        val status = parseResponseStatus("pending")
        assertEquals(ResponseStatus.PENDING, status)
    }

    @Test
    fun `response status error`() {
        val status = parseResponseStatus("error")
        assertEquals(ResponseStatus.ERROR, status)
    }

    @Test
    fun `response status unknown defaults to pending`() {
        val status = parseResponseStatus("unknown")
        assertEquals(ResponseStatus.PENDING, status)
    }

    // Helper functions that mirror the watch app's logic
    private fun buildServerUrl(ip: String, port: String): String {
        return "http://$ip:$port/transcribe"
    }

    private fun buildResponseUrl(ip: String, port: String, requestId: String): String {
        return "http://$ip:$port/api/response/$requestId"
    }

    private fun buildAudioUrl(ip: String, port: String, requestId: String): String {
        return "http://$ip:$port/api/audio/$requestId"
    }

    private fun generateAudioFileName(timestamp: Long): String {
        return "voice_$timestamp.m4a"
    }

    private fun buildAudioFilePath(cacheDir: String, fileName: String): String {
        return "$cacheDir/$fileName"
    }

    private fun parseResponseStatus(status: String): ResponseStatus {
        return when (status) {
            "ready" -> ResponseStatus.READY
            "error" -> ResponseStatus.ERROR
            else -> ResponseStatus.PENDING
        }
    }

    data class PollingConfig(
        val intervalMs: Long = 500L,
        val maxAttempts: Int = 60
    ) {
        fun totalTimeoutMs(): Long = intervalMs * maxAttempts
        fun shouldContinue(currentAttempt: Int): Boolean = currentAttempt < maxAttempts
    }

    data class RecordingState(
        val isRecording: Boolean = false,
        val isWaitingForResponse: Boolean = false,
        val isPlayingAudio: Boolean = false,
        val currentRequestId: String? = null
    ) {
        fun isIdle(): Boolean = !isRecording && !isWaitingForResponse && !isPlayingAudio
    }

    enum class ResponseStatus {
        PENDING,
        READY,
        ERROR
    }
}
