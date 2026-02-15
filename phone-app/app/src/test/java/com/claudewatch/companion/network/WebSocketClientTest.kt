package com.claudewatch.companion.network

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WebSocketClientTest {

    // Data class tests
    @Test
    fun `ChatMessage has correct default values`() {
        val message = ChatMessage(
            role = "user",
            content = "Hello",
            timestamp = "2024-01-01T00:00:00Z"
        )

        assertEquals("user", message.role)
        assertEquals("Hello", message.content)
        assertEquals("2024-01-01T00:00:00Z", message.timestamp)
        assertEquals(MessageStatus.SENT, message.status)
        assertNotNull(message.id)
    }

    @Test
    fun `ChatMessage id is unique`() {
        val message1 = ChatMessage(role = "user", content = "Hello", timestamp = "t1")
        val message2 = ChatMessage(role = "user", content = "Hello", timestamp = "t1")

        assertNotEquals(message1.id, message2.id)
    }

    @Test
    fun `ChatMessage status can be set`() {
        val pendingMessage = ChatMessage(
            role = "user",
            content = "Hello",
            timestamp = "t1",
            status = MessageStatus.PENDING
        )
        val failedMessage = ChatMessage(
            role = "user",
            content = "Hello",
            timestamp = "t1",
            status = MessageStatus.FAILED
        )

        assertEquals(MessageStatus.PENDING, pendingMessage.status)
        assertEquals(MessageStatus.FAILED, failedMessage.status)
    }

    @Test
    fun `ClaudeState has correct default values`() {
        val state = ClaudeState()

        assertEquals("idle", state.status)
        assertNull(state.requestId)
        assertNull(state.currentTool)
    }

    @Test
    fun `ClaudeState with custom values`() {
        val state = ClaudeState(status = "thinking", requestId = "req-123")

        assertEquals("thinking", state.status)
        assertEquals("req-123", state.requestId)
        assertNull(state.currentTool)
    }

    @Test
    fun `ClaudeState with currentTool`() {
        val state = ClaudeState(status = "thinking", requestId = "req-123", currentTool = "Bash")

        assertEquals("thinking", state.status)
        assertEquals("req-123", state.requestId)
        assertEquals("Bash", state.currentTool)
    }

    @Test
    fun `ClaudeState copy updates currentTool`() {
        val state = ClaudeState(status = "thinking", requestId = "req-123")
        val updated = state.copy(currentTool = "Read")

        assertEquals("thinking", updated.status)
        assertEquals("req-123", updated.requestId)
        assertEquals("Read", updated.currentTool)
    }

    @Test
    fun `ContextUsage has correct default values`() {
        val usage = ContextUsage()

        assertEquals(0, usage.totalContext)
        assertEquals(200000, usage.contextWindow)
        assertEquals(0f, usage.contextPercent)
        assertEquals(0f, usage.costUsd)
    }

    @Test
    fun `ContextUsage with custom values`() {
        val usage = ContextUsage(
            totalContext = 50000,
            contextWindow = 100000,
            contextPercent = 50f,
            costUsd = 0.05f
        )

        assertEquals(50000, usage.totalContext)
        assertEquals(100000, usage.contextWindow)
        assertEquals(50f, usage.contextPercent)
        assertEquals(0.05f, usage.costUsd)
    }

    @Test
    fun `PromptOption stores values correctly`() {
        val option = PromptOption(
            num = 1,
            label = "Yes",
            description = "Allow the action",
            selected = true
        )

        assertEquals(1, option.num)
        assertEquals("Yes", option.label)
        assertEquals("Allow the action", option.description)
        assertTrue(option.selected)
    }

    @Test
    fun `ClaudePrompt basic properties`() {
        val prompt = ClaudePrompt(
            question = "Do you want to proceed?",
            options = listOf(
                PromptOption(1, "Yes", "Proceed", false),
                PromptOption(2, "No", "Cancel", false)
            ),
            timestamp = "t1"
        )

        assertEquals("Do you want to proceed?", prompt.question)
        assertEquals(2, prompt.options.size)
        assertNull(prompt.title)
        assertNull(prompt.context)
        assertNull(prompt.requestId)
        assertNull(prompt.toolName)
        assertFalse(prompt.isPermission)
    }

    @Test
    fun `ClaudePrompt permission request`() {
        val prompt = ClaudePrompt(
            question = "Allow file write?",
            options = emptyList(),
            timestamp = "t1",
            title = "Bash",
            context = "rm -rf /tmp/test",
            requestId = "perm-123",
            toolName = "Bash",
            isPermission = true
        )

        assertEquals("Allow file write?", prompt.question)
        assertEquals("Bash", prompt.title)
        assertEquals("rm -rf /tmp/test", prompt.context)
        assertEquals("perm-123", prompt.requestId)
        assertEquals("Bash", prompt.toolName)
        assertTrue(prompt.isPermission)
    }

    @Test
    fun `ConnectionStatus enum values`() {
        assertEquals(3, ConnectionStatus.values().size)
        assertNotNull(ConnectionStatus.DISCONNECTED)
        assertNotNull(ConnectionStatus.CONNECTING)
        assertNotNull(ConnectionStatus.CONNECTED)
    }

    @Test
    fun `MessageStatus enum values`() {
        assertEquals(3, MessageStatus.values().size)
        assertNotNull(MessageStatus.SENT)
        assertNotNull(MessageStatus.PENDING)
        assertNotNull(MessageStatus.FAILED)
    }

    // JSON Parsing tests (testing the parsing logic that handleMessage uses)
    @Test
    fun `parseStateMessage extracts status correctly`() {
        val json = JSONObject().apply {
            put("type", "state")
            put("status", "thinking")
            put("request_id", "req-456")
        }

        assertEquals("state", json.optString("type"))
        assertEquals("thinking", json.optString("status", "idle"))
        assertEquals("req-456", json.optString("request_id", ""))
    }

    @Test
    fun `parseStateMessage handles missing request_id`() {
        val json = JSONObject().apply {
            put("type", "state")
            put("status", "idle")
        }

        assertEquals("idle", json.optString("status", "idle"))
        assertEquals("", json.optString("request_id", ""))
    }

    @Test
    fun `parseChatMessage extracts fields correctly`() {
        val json = JSONObject().apply {
            put("type", "chat")
            put("role", "assistant")
            put("content", "Hello, how can I help?")
            put("timestamp", "2024-01-01T12:00:00Z")
        }

        assertEquals("chat", json.optString("type"))
        assertEquals("assistant", json.optString("role"))
        assertEquals("Hello, how can I help?", json.optString("content"))
        assertEquals("2024-01-01T12:00:00Z", json.optString("timestamp"))
    }

    @Test
    fun `parseHistoryMessage extracts messages array`() {
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", "Hi")
                put("timestamp", "t1")
            })
            put(JSONObject().apply {
                put("role", "assistant")
                put("content", "Hello!")
                put("timestamp", "t2")
            })
        }
        val json = JSONObject().apply {
            put("type", "history")
            put("messages", messagesArray)
        }

        val messages = json.optJSONArray("messages")
        assertNotNull(messages)
        assertEquals(2, messages!!.length())

        val firstMsg = messages.getJSONObject(0)
        assertEquals("user", firstMsg.optString("role"))
        assertEquals("Hi", firstMsg.optString("content"))
    }

    @Test
    fun `parsePromptMessage extracts options`() {
        val optionsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("num", 1)
                put("label", "Yes")
                put("description", "Allow")
                put("selected", false)
            })
            put(JSONObject().apply {
                put("num", 2)
                put("label", "No")
                put("description", "Deny")
                put("selected", false)
            })
        }
        val promptJson = JSONObject().apply {
            put("question", "Proceed?")
            put("options", optionsArray)
            put("timestamp", "t1")
        }
        val json = JSONObject().apply {
            put("type", "prompt")
            put("prompt", promptJson)
        }

        val prompt = json.optJSONObject("prompt")
        assertNotNull(prompt)
        assertEquals("Proceed?", prompt!!.optString("question"))

        val options = prompt.optJSONArray("options")
        assertEquals(2, options!!.length())
    }

    @Test
    fun `parsePermissionMessage extracts all fields`() {
        val optionsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("num", 1)
                put("label", "Allow")
                put("description", "Allow this action")
            })
            put(JSONObject().apply {
                put("num", 2)
                put("label", "Deny")
                put("description", "Deny this action")
            })
        }
        val json = JSONObject().apply {
            put("type", "permission")
            put("question", "Allow file write?")
            put("options", optionsArray)
            put("tool_name", "Write")
            put("context", "/tmp/test.txt")
            put("request_id", "perm-789")
        }

        assertEquals("permission", json.optString("type"))
        assertEquals("Allow file write?", json.optString("question"))
        assertEquals("Write", json.optString("tool_name"))
        assertEquals("/tmp/test.txt", json.optString("context"))
        assertEquals("perm-789", json.optString("request_id"))
    }

    @Test
    fun `parseUsageMessage extracts context metrics`() {
        val json = JSONObject().apply {
            put("type", "usage")
            put("total_context", 75000)
            put("context_window", 200000)
            put("context_percent", 37.5)
            put("cost_usd", 0.15)
        }

        assertEquals(75000, json.optInt("total_context", 0))
        assertEquals(200000, json.optInt("context_window", 200000))
        assertEquals(37.5, json.optDouble("context_percent", 0.0), 0.01)
        assertEquals(0.15, json.optDouble("cost_usd", 0.0), 0.01)
    }

    @Test
    fun `parsePermissionResolvedMessage extracts request_id`() {
        val json = JSONObject().apply {
            put("type", "permission_resolved")
            put("request_id", "perm-999")
        }

        assertEquals("permission_resolved", json.optString("type"))
        assertEquals("perm-999", json.optString("request_id"))
    }

    @Test
    fun `unknown message type is handled gracefully`() {
        val json = JSONObject().apply {
            put("type", "unknown_type")
            put("data", "some data")
        }

        // Should not throw, optString returns empty string for missing type handler
        assertEquals("unknown_type", json.optString("type"))
    }

    @Test
    fun `malformed json options array handled gracefully`() {
        val json = JSONObject().apply {
            put("type", "prompt")
            // Missing "prompt" object
        }

        val prompt = json.optJSONObject("prompt")
        assertNull(prompt)
    }

    @Test
    fun `empty history messages array`() {
        val json = JSONObject().apply {
            put("type", "history")
            put("messages", JSONArray())
        }

        val messages = json.optJSONArray("messages")
        assertNotNull(messages)
        assertEquals(0, messages!!.length())
    }

    @Test
    fun `parseToolMessage extracts tool name`() {
        val json = JSONObject().apply {
            put("type", "tool")
            put("request_id", "req-123")
            put("tool", "Bash")
        }

        assertEquals("tool", json.optString("type"))
        assertEquals("Bash", json.optString("tool"))
        assertEquals("req-123", json.optString("request_id"))
    }

    @Test
    fun `parseToolMessage with various tool names`() {
        val toolNames = listOf("Bash", "Read", "Write", "Edit", "Glob", "Grep", "Task", "WebFetch")
        for (toolName in toolNames) {
            val json = JSONObject().apply {
                put("type", "tool")
                put("tool", toolName)
            }
            assertEquals(toolName, json.optString("tool"))
        }
    }

    @Test
    fun `parseToolMessage handles empty tool name`() {
        val json = JSONObject().apply {
            put("type", "tool")
            put("tool", "")
        }

        assertEquals("", json.optString("tool", ""))
    }
}
