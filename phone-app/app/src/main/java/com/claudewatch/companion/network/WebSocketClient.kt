package com.claudewatch.companion.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: String
)

data class ClaudeState(
    val status: String = "idle",  // idle, listening, thinking, speaking
    val requestId: String? = null
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

class WebSocketClient(
    private val serverAddress: String
) {
    companion object {
        private const val TAG = "WebSocketClient"
        private const val RECONNECT_DELAY_MS = 5000L
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)  // No timeout for WebSocket
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // State flows for UI observation
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _claudeState = MutableStateFlow(ClaudeState())
    val claudeState: StateFlow<ClaudeState> = _claudeState

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "WebSocket connected")
            _connectionStatus.value = ConnectionStatus.CONNECTED
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received: $text")
            handleMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket closing: $code $reason")
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket closed: $code $reason")
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error", t)
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
            scheduleReconnect()
        }
    }

    fun connect() {
        if (_connectionStatus.value == ConnectionStatus.CONNECTING) {
            return
        }

        _connectionStatus.value = ConnectionStatus.CONNECTING
        reconnectJob?.cancel()

        val wsUrl = "ws://$serverAddress/ws"
        Log.i(TAG, "Connecting to $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, listener)
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    fun destroy() {
        disconnect()
        scope.cancel()
        client.dispatcher.executorService.shutdown()
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            connect()
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "state" -> {
                    val requestId = json.optString("request_id", "")
                    _claudeState.value = ClaudeState(
                        status = json.optString("status", "idle"),
                        requestId = requestId.ifEmpty { null }
                    )
                }
                "chat" -> {
                    val message = ChatMessage(
                        role = json.optString("role"),
                        content = json.optString("content"),
                        timestamp = json.optString("timestamp")
                    )
                    _chatMessages.value = _chatMessages.value + message
                }
                "history" -> {
                    val messagesArray = json.optJSONArray("messages") ?: JSONArray()
                    val messages = mutableListOf<ChatMessage>()
                    for (i in 0 until messagesArray.length()) {
                        val msgJson = messagesArray.getJSONObject(i)
                        messages.add(ChatMessage(
                            role = msgJson.optString("role"),
                            content = msgJson.optString("content"),
                            timestamp = msgJson.optString("timestamp")
                        ))
                    }
                    _chatMessages.value = messages
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }
}
