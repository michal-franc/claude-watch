package com.claudewatch.app.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WatchWebSocketClient(
    private val serverAddress: String
) {
    companion object {
        private const val TAG = "WatchWebSocket"
        private const val RECONNECT_DELAY_MS = 5000L
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
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

    private val _currentPrompt = MutableStateFlow<ClaudePrompt?>(null)
    val currentPrompt: StateFlow<ClaudePrompt?> = _currentPrompt

    private val _contextUsage = MutableStateFlow(ContextUsage())
    val contextUsage: StateFlow<ContextUsage> = _contextUsage

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
        if (_connectionStatus.value == ConnectionStatus.CONNECTING) return

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
                "prompt" -> {
                    val promptJson = json.optJSONObject("prompt")
                    if (promptJson != null) {
                        _currentPrompt.value = parsePrompt(promptJson, isPermission = false)
                    } else {
                        _currentPrompt.value = null
                    }
                }
                "permission" -> {
                    val optionsArray = json.optJSONArray("options") ?: JSONArray()
                    val options = parseOptions(optionsArray)
                    _currentPrompt.value = ClaudePrompt(
                        question = json.optString("question"),
                        options = options,
                        timestamp = System.currentTimeMillis().toString(),
                        title = json.optString("tool_name"),
                        context = if (json.has("context")) json.optString("context") else null,
                        requestId = json.optString("request_id"),
                        toolName = json.optString("tool_name"),
                        isPermission = true
                    )
                }
                "permission_resolved" -> {
                    val resolvedId = json.optString("request_id")
                    if (_currentPrompt.value?.requestId == resolvedId) {
                        _currentPrompt.value = null
                    }
                }
                "usage" -> {
                    _contextUsage.value = ContextUsage(
                        totalContext = json.optInt("total_context", 0),
                        contextWindow = json.optInt("context_window", 200000),
                        contextPercent = json.optDouble("context_percent", 0.0).toFloat(),
                        costUsd = json.optDouble("cost_usd", 0.0).toFloat()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }

    private fun parsePrompt(json: JSONObject, isPermission: Boolean): ClaudePrompt {
        val optionsArray = json.optJSONArray("options") ?: JSONArray()
        val options = parseOptions(optionsArray)
        return ClaudePrompt(
            question = json.optString("question"),
            options = options,
            timestamp = json.optString("timestamp"),
            title = if (json.has("title")) json.optString("title") else null,
            context = if (json.has("context")) json.optString("context") else null,
            requestId = if (json.has("request_id")) json.optString("request_id") else null,
            toolName = if (json.has("tool_name")) json.optString("tool_name") else null,
            isPermission = isPermission
        )
    }

    private fun parseOptions(array: JSONArray): List<PromptOption> {
        val options = mutableListOf<PromptOption>()
        for (i in 0 until array.length()) {
            val optJson = array.getJSONObject(i)
            options.add(PromptOption(
                num = optJson.optInt("num"),
                label = optJson.optString("label"),
                description = optJson.optString("description", ""),
                selected = optJson.optBoolean("selected", false)
            ))
        }
        return options
    }
}
