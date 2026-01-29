package com.claudewatch.app.network

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val timestamp: String
)

data class ClaudeState(
    val status: String = "idle",  // idle, listening, thinking, speaking, waiting
    val requestId: String? = null
)

data class ContextUsage(
    val totalContext: Int = 0,
    val contextWindow: Int = 200000,
    val contextPercent: Float = 0f,
    val costUsd: Float = 0f
)

data class PromptOption(
    val num: Int,
    val label: String,
    val description: String,
    val selected: Boolean
)

data class ClaudePrompt(
    val question: String,
    val options: List<PromptOption>,
    val timestamp: String,
    val title: String? = null,
    val context: String? = null,
    val requestId: String? = null,
    val toolName: String? = null,
    val isPermission: Boolean = false
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}
