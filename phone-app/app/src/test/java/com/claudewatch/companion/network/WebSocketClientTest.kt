package com.claudewatch.companion.network

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Method

class WebSocketClientTest {

    private lateinit var client: WebSocketClient
    private lateinit var handleMessage: Method

    @Before
    fun setUp() {
        client = WebSocketClient("localhost:5567")
        handleMessage = WebSocketClient::class.java.getDeclaredMethod("handleMessage", String::class.java)
        handleMessage.isAccessible = true
    }

    private fun invokeHandleMessage(json: String) {
        handleMessage.invoke(client, json)
    }

    // --- handleMessage integration tests ---

    @Test
    fun `handleMessage state sets claudeState status to thinking`() {
        val json = JSONObject().apply {
            put("type", "state")
            put("status", "thinking")
            put("request_id", "req-100")
        }.toString()

        invokeHandleMessage(json)

        assertEquals("thinking", client.claudeState.value.status)
        assertEquals("req-100", client.claudeState.value.requestId)
    }

    @Test
    fun `handleMessage state idle clears currentTool`() {
        // First set a tool via thinking + tool
        invokeHandleMessage(JSONObject().apply {
            put("type", "state")
            put("status", "thinking")
        }.toString())
        invokeHandleMessage(JSONObject().apply {
            put("type", "tool")
            put("tool", "Bash")
        }.toString())
        assertEquals("Bash", client.claudeState.value.currentTool)

        // Now transition to idle -- should clear tool
        invokeHandleMessage(JSONObject().apply {
            put("type", "state")
            put("status", "idle")
        }.toString())

        assertEquals("idle", client.claudeState.value.status)
        assertNull(client.claudeState.value.currentTool)
    }

    @Test
    fun `handleMessage state thinking preserves currentTool`() {
        // Set tool
        invokeHandleMessage(JSONObject().apply {
            put("type", "state")
            put("status", "thinking")
        }.toString())
        invokeHandleMessage(JSONObject().apply {
            put("type", "tool")
            put("tool", "Read")
        }.toString())
        assertEquals("Read", client.claudeState.value.currentTool)

        // Another thinking state -- should keep tool
        invokeHandleMessage(JSONObject().apply {
            put("type", "state")
            put("status", "thinking")
            put("request_id", "req-200")
        }.toString())

        assertEquals("thinking", client.claudeState.value.status)
        assertEquals("Read", client.claudeState.value.currentTool)
    }

    @Test
    fun `handleMessage state speaking clears currentTool`() {
        invokeHandleMessage(JSONObject().apply {
            put("type", "state")
            put("status", "thinking")
        }.toString())
        invokeHandleMessage(JSONObject().apply {
            put("type", "tool")
            put("tool", "Write")
        }.toString())

        invokeHandleMessage(JSONObject().apply {
            put("type", "state")
            put("status", "speaking")
        }.toString())

        assertEquals("speaking", client.claudeState.value.status)
        assertNull(client.claudeState.value.currentTool)
    }

    @Test
    fun `handleMessage tool sets currentTool on claudeState`() {
        val json = JSONObject().apply {
            put("type", "tool")
            put("tool", "Bash")
        }.toString()

        invokeHandleMessage(json)

        assertEquals("Bash", client.claudeState.value.currentTool)
    }

    @Test
    fun `handleMessage tool with empty name does not update currentTool`() {
        // Set initial tool
        invokeHandleMessage(JSONObject().apply {
            put("type", "tool")
            put("tool", "Bash")
        }.toString())
        assertEquals("Bash", client.claudeState.value.currentTool)

        // Empty tool name should not change it
        invokeHandleMessage(JSONObject().apply {
            put("type", "tool")
            put("tool", "")
        }.toString())

        assertEquals("Bash", client.claudeState.value.currentTool)
    }

    @Test
    fun `handleMessage tool updates currentTool to new tool`() {
        invokeHandleMessage(JSONObject().apply {
            put("type", "tool")
            put("tool", "Bash")
        }.toString())
        assertEquals("Bash", client.claudeState.value.currentTool)

        invokeHandleMessage(JSONObject().apply {
            put("type", "tool")
            put("tool", "Read")
        }.toString())
        assertEquals("Read", client.claudeState.value.currentTool)
    }

    @Test
    fun `handleMessage chat adds message to chatMessages`() {
        val json = JSONObject().apply {
            put("type", "chat")
            put("role", "assistant")
            put("content", "Hello!")
            put("timestamp", "2024-01-01T12:00:00Z")
        }.toString()

        invokeHandleMessage(json)

        val messages = client.chatMessages.value
        assertEquals(1, messages.size)
        assertEquals("assistant", messages[0].role)
        assertEquals("Hello!", messages[0].content)
    }

    @Test
    fun `handleMessage chat accumulates messages`() {
        invokeHandleMessage(JSONObject().apply {
            put("type", "chat")
            put("role", "user")
            put("content", "Hi")
            put("timestamp", "t1")
        }.toString())
        invokeHandleMessage(JSONObject().apply {
            put("type", "chat")
            put("role", "assistant")
            put("content", "Hello!")
            put("timestamp", "t2")
        }.toString())

        assertEquals(2, client.chatMessages.value.size)
    }

    @Test
    fun `handleMessage history replaces chatMessages`() {
        // Add a chat message first
        invokeHandleMessage(JSONObject().apply {
            put("type", "chat")
            put("role", "user")
            put("content", "Old message")
            put("timestamp", "t0")
        }.toString())
        assertEquals(1, client.chatMessages.value.size)

        // History replaces
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", "First")
                put("timestamp", "t1")
            })
            put(JSONObject().apply {
                put("role", "assistant")
                put("content", "Second")
                put("timestamp", "t2")
            })
        }
        invokeHandleMessage(JSONObject().apply {
            put("type", "history")
            put("messages", messagesArray)
        }.toString())

        val messages = client.chatMessages.value
        assertEquals(2, messages.size)
        assertEquals("First", messages[0].content)
        assertEquals("Second", messages[1].content)
    }

    @Test
    fun `handleMessage history with empty array clears messages`() {
        invokeHandleMessage(JSONObject().apply {
            put("type", "chat")
            put("role", "user")
            put("content", "Hi")
            put("timestamp", "t1")
        }.toString())

        invokeHandleMessage(JSONObject().apply {
            put("type", "history")
            put("messages", JSONArray())
        }.toString())

        assertEquals(0, client.chatMessages.value.size)
    }

    @Test
    fun `handleMessage prompt sets currentPrompt`() {
        val optionsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("num", 1)
                put("label", "Yes")
                put("description", "Proceed")
                put("selected", false)
            })
        }
        val promptJson = JSONObject().apply {
            put("question", "Continue?")
            put("options", optionsArray)
            put("timestamp", "t1")
        }
        invokeHandleMessage(JSONObject().apply {
            put("type", "prompt")
            put("prompt", promptJson)
        }.toString())

        val prompt = client.currentPrompt.value
        assertNotNull(prompt)
        assertEquals("Continue?", prompt!!.question)
        assertEquals(1, prompt.options.size)
        assertEquals("Yes", prompt.options[0].label)
        assertFalse(prompt.isPermission)
    }

    @Test
    fun `handleMessage prompt with null prompt object clears currentPrompt`() {
        // Set a prompt first
        val promptJson = JSONObject().apply {
            put("question", "Q?")
            put("options", JSONArray())
            put("timestamp", "t1")
        }
        invokeHandleMessage(JSONObject().apply {
            put("type", "prompt")
            put("prompt", promptJson)
        }.toString())
        assertNotNull(client.currentPrompt.value)

        // Send prompt without "prompt" key
        invokeHandleMessage(JSONObject().apply {
            put("type", "prompt")
        }.toString())

        assertNull(client.currentPrompt.value)
    }

    @Test
    fun `handleMessage permission sets currentPrompt with isPermission true`() {
        val optionsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("num", 1)
                put("label", "Allow")
                put("description", "Allow it")
            })
            put(JSONObject().apply {
                put("num", 2)
                put("label", "Deny")
                put("description", "Deny it")
            })
        }
        invokeHandleMessage(JSONObject().apply {
            put("type", "permission")
            put("question", "Allow Bash?")
            put("options", optionsArray)
            put("tool_name", "Bash")
            put("context", "rm -rf /tmp")
            put("request_id", "perm-1")
        }.toString())

        val prompt = client.currentPrompt.value
        assertNotNull(prompt)
        assertTrue(prompt!!.isPermission)
        assertEquals("Allow Bash?", prompt.question)
        assertEquals("Bash", prompt.toolName)
        assertEquals("rm -rf /tmp", prompt.context)
        assertEquals("perm-1", prompt.requestId)
        assertEquals(2, prompt.options.size)
    }

    @Test
    fun `handleMessage permission_resolved clears matching prompt`() {
        // Set permission prompt
        invokeHandleMessage(JSONObject().apply {
            put("type", "permission")
            put("question", "Allow?")
            put("options", JSONArray())
            put("tool_name", "Bash")
            put("context", "ls")
            put("request_id", "perm-42")
        }.toString())
        assertNotNull(client.currentPrompt.value)

        // Resolve it
        invokeHandleMessage(JSONObject().apply {
            put("type", "permission_resolved")
            put("request_id", "perm-42")
        }.toString())

        assertNull(client.currentPrompt.value)
    }

    @Test
    fun `handleMessage permission_resolved does not clear non-matching prompt`() {
        invokeHandleMessage(JSONObject().apply {
            put("type", "permission")
            put("question", "Allow?")
            put("options", JSONArray())
            put("tool_name", "Bash")
            put("context", "ls")
            put("request_id", "perm-42")
        }.toString())

        // Resolve different request_id
        invokeHandleMessage(JSONObject().apply {
            put("type", "permission_resolved")
            put("request_id", "perm-99")
        }.toString())

        assertNotNull(client.currentPrompt.value)
    }

    @Test
    fun `handleMessage usage sets contextUsage`() {
        invokeHandleMessage(JSONObject().apply {
            put("type", "usage")
            put("total_context", 50000)
            put("context_window", 200000)
            put("context_percent", 25.0)
            put("cost_usd", 0.10)
        }.toString())

        val usage = client.contextUsage.value
        assertEquals(50000, usage.totalContext)
        assertEquals(200000, usage.contextWindow)
        assertEquals(25.0f, usage.contextPercent)
        assertEquals(0.10f, usage.costUsd, 0.001f)
    }

    @Test
    fun `handleMessage unknown type does not crash`() {
        invokeHandleMessage(JSONObject().apply {
            put("type", "future_type")
            put("data", "whatever")
        }.toString())

        // Should not throw; state should remain default
        assertEquals("idle", client.claudeState.value.status)
    }

    @Test
    fun `handleMessage malformed JSON does not crash`() {
        invokeHandleMessage("this is not json {{{")

        // Should not throw
        assertEquals("idle", client.claudeState.value.status)
    }

    @Test
    fun `handleMessage state with empty request_id sets null`() {
        invokeHandleMessage(JSONObject().apply {
            put("type", "state")
            put("status", "thinking")
            put("request_id", "")
        }.toString())

        assertEquals("thinking", client.claudeState.value.status)
        assertNull(client.claudeState.value.requestId)
    }

    // --- Full agent status lifecycle test ---

    @Test
    fun `full lifecycle - thinking then tool then idle`() {
        // Start thinking
        invokeHandleMessage(JSONObject().apply {
            put("type", "state")
            put("status", "thinking")
            put("request_id", "req-1")
        }.toString())
        assertEquals("thinking", client.claudeState.value.status)
        assertNull(client.claudeState.value.currentTool)

        // Tool used
        invokeHandleMessage(JSONObject().apply {
            put("type", "tool")
            put("tool", "Bash")
        }.toString())
        assertEquals("thinking", client.claudeState.value.status)
        assertEquals("Bash", client.claudeState.value.currentTool)

        // Switch tool
        invokeHandleMessage(JSONObject().apply {
            put("type", "tool")
            put("tool", "Read")
        }.toString())
        assertEquals("Read", client.claudeState.value.currentTool)

        // Back to idle
        invokeHandleMessage(JSONObject().apply {
            put("type", "state")
            put("status", "idle")
        }.toString())
        assertEquals("idle", client.claudeState.value.status)
        assertNull(client.claudeState.value.currentTool)
    }

    // --- WebSocket lifecycle tests ---

    @Test
    fun `disconnect sets status to disconnected`() {
        client.disconnect()
        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
    }

    @Test
    fun `destroy sets status to disconnected`() {
        client.destroy()
        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
    }

    @Test
    fun `connectionStatus starts as disconnected`() {
        assertEquals(ConnectionStatus.DISCONNECTED, client.connectionStatus.value)
    }

    @Test
    fun `prompt with title and context sets fields`() {
        val optionsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("num", 1)
                put("label", "OK")
                put("description", "Accept")
                put("selected", true)
            })
        }
        val promptJson = JSONObject().apply {
            put("question", "Allow?")
            put("options", optionsArray)
            put("timestamp", "t1")
            put("title", "Permission")
            put("context", "Some context")
        }
        invokeHandleMessage(JSONObject().apply {
            put("type", "prompt")
            put("prompt", promptJson)
        }.toString())

        val prompt = client.currentPrompt.value
        assertNotNull(prompt)
        assertEquals("Permission", prompt!!.title)
        assertEquals("Some context", prompt.context)
    }

    // --- Data class tests (original) ---

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
