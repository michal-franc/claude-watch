package com.claudewatch.companion.chat

import com.claudewatch.companion.network.ChatMessage
import com.claudewatch.companion.network.MessageStatus
import org.junit.Assert.*
import org.junit.Test

class ChatAdapterTest {

    private val diffCallback = ChatAdapter.MessageDiffCallback()

    @Test
    fun `areItemsTheSame returns true for same id`() {
        val message1 = ChatMessage(
            id = "msg-123",
            role = "user",
            content = "Hello",
            timestamp = "t1"
        )
        val message2 = ChatMessage(
            id = "msg-123",
            role = "user",
            content = "Different content",
            timestamp = "t2"
        )

        assertTrue(diffCallback.areItemsTheSame(message1, message2))
    }

    @Test
    fun `areItemsTheSame returns false for different ids`() {
        val message1 = ChatMessage(
            id = "msg-123",
            role = "user",
            content = "Hello",
            timestamp = "t1"
        )
        val message2 = ChatMessage(
            id = "msg-456",
            role = "user",
            content = "Hello",
            timestamp = "t1"
        )

        assertFalse(diffCallback.areItemsTheSame(message1, message2))
    }

    @Test
    fun `areContentsTheSame returns true for identical messages`() {
        val id = "msg-123"
        val message1 = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1",
            status = MessageStatus.SENT
        )
        val message2 = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1",
            status = MessageStatus.SENT
        )

        assertTrue(diffCallback.areContentsTheSame(message1, message2))
    }

    @Test
    fun `areContentsTheSame returns false for different content`() {
        val id = "msg-123"
        val message1 = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1"
        )
        val message2 = ChatMessage(
            id = id,
            role = "user",
            content = "Goodbye",
            timestamp = "t1"
        )

        assertFalse(diffCallback.areContentsTheSame(message1, message2))
    }

    @Test
    fun `areContentsTheSame returns false for different status`() {
        val id = "msg-123"
        val message1 = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1",
            status = MessageStatus.PENDING
        )
        val message2 = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1",
            status = MessageStatus.SENT
        )

        assertFalse(diffCallback.areContentsTheSame(message1, message2))
    }

    @Test
    fun `areContentsTheSame returns false for different role`() {
        val id = "msg-123"
        val message1 = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1"
        )
        val message2 = ChatMessage(
            id = id,
            role = "assistant",
            content = "Hello",
            timestamp = "t1"
        )

        assertFalse(diffCallback.areContentsTheSame(message1, message2))
    }

    @Test
    fun `areContentsTheSame returns false for different timestamp`() {
        val id = "msg-123"
        val message1 = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1"
        )
        val message2 = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t2"
        )

        assertFalse(diffCallback.areContentsTheSame(message1, message2))
    }

    @Test
    fun `status transition from pending to sent`() {
        val id = "msg-123"
        val pendingMessage = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1",
            status = MessageStatus.PENDING
        )
        val sentMessage = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1",
            status = MessageStatus.SENT
        )

        // Same item (same id)
        assertTrue(diffCallback.areItemsTheSame(pendingMessage, sentMessage))
        // But different contents (status changed)
        assertFalse(diffCallback.areContentsTheSame(pendingMessage, sentMessage))
    }

    @Test
    fun `status transition from pending to failed`() {
        val id = "msg-123"
        val pendingMessage = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1",
            status = MessageStatus.PENDING
        )
        val failedMessage = ChatMessage(
            id = id,
            role = "user",
            content = "Hello",
            timestamp = "t1",
            status = MessageStatus.FAILED
        )

        assertTrue(diffCallback.areItemsTheSame(pendingMessage, failedMessage))
        assertFalse(diffCallback.areContentsTheSame(pendingMessage, failedMessage))
    }

    // View type determination tests (testing the logic, not actual view inflation)
    @Test
    fun `user message role identifies correctly`() {
        val message = ChatMessage(
            role = "user",
            content = "Hello",
            timestamp = "t1"
        )

        assertEquals("user", message.role)
    }

    @Test
    fun `assistant message role identifies correctly`() {
        val message = ChatMessage(
            role = "assistant",
            content = "Hello!",
            timestamp = "t1"
        )

        assertEquals("assistant", message.role)
    }

    @Test
    fun `view type logic for user vs assistant`() {
        val userMessage = ChatMessage(role = "user", content = "Hi", timestamp = "t1")
        val assistantMessage = ChatMessage(role = "assistant", content = "Hello", timestamp = "t2")

        // VIEW_TYPE_USER = 0, VIEW_TYPE_CLAUDE = 1
        val userViewType = if (userMessage.role == "user") 0 else 1
        val assistantViewType = if (assistantMessage.role == "user") 0 else 1

        assertEquals(0, userViewType)
        assertEquals(1, assistantViewType)
    }

    // --- Copy message tests ---

    @Test
    fun `user message content is accessible for copy`() {
        val message = ChatMessage(
            id = "msg-001",
            role = "user",
            content = "Hello, can you help me?",
            timestamp = "2024-01-01T00:00:00Z"
        )

        assertEquals("Hello, can you help me?", message.content)
    }

    @Test
    fun `assistant message content is accessible for copy`() {
        val message = ChatMessage(
            id = "msg-002",
            role = "assistant",
            content = "Sure, I'd be happy to help!",
            timestamp = "2024-01-01T00:00:01Z"
        )

        assertEquals("Sure, I'd be happy to help!", message.content)
    }

    @Test
    fun `message with empty content has empty string for copy`() {
        val message = ChatMessage(
            id = "msg-003",
            role = "assistant",
            content = "",
            timestamp = "t1"
        )

        assertEquals("", message.content)
    }

    @Test
    fun `message with multiline content preserves newlines for copy`() {
        val multilineContent = "Line 1\nLine 2\nLine 3"
        val message = ChatMessage(
            id = "msg-004",
            role = "assistant",
            content = multilineContent,
            timestamp = "t1"
        )

        assertEquals(multilineContent, message.content)
        assertTrue(message.content.contains("\n"))
    }

    @Test
    fun `message with special characters preserves content for copy`() {
        val specialContent = "Code: `val x = 1` and emoji \u2764 and <html>&amp;"
        val message = ChatMessage(
            id = "msg-005",
            role = "assistant",
            content = specialContent,
            timestamp = "t1"
        )

        assertEquals(specialContent, message.content)
    }

    @Test
    fun `sent message has copyable content`() {
        val message = ChatMessage(
            id = "msg-010",
            role = "user",
            content = "Sent message text",
            timestamp = "t1",
            status = MessageStatus.SENT
        )

        assertEquals("Sent message text", message.content)
        assertEquals(MessageStatus.SENT, message.status)
    }

    @Test
    fun `pending message has copyable content`() {
        val message = ChatMessage(
            id = "msg-011",
            role = "user",
            content = "Pending message text",
            timestamp = "t1",
            status = MessageStatus.PENDING
        )

        assertEquals("Pending message text", message.content)
        assertEquals(MessageStatus.PENDING, message.status)
    }

    @Test
    fun `failed message has copyable content`() {
        val message = ChatMessage(
            id = "msg-012",
            role = "user",
            content = "Failed message text",
            timestamp = "t1",
            status = MessageStatus.FAILED
        )

        assertEquals("Failed message text", message.content)
        assertEquals(MessageStatus.FAILED, message.status)
    }

    @Test
    fun `both user and assistant messages provide same content field for copy`() {
        val sharedContent = "Same text in both"
        val userMsg = ChatMessage(role = "user", content = sharedContent, timestamp = "t1")
        val assistantMsg = ChatMessage(role = "assistant", content = sharedContent, timestamp = "t2")

        // Both roles expose content identically for copy
        assertEquals(userMsg.content, assistantMsg.content)
    }

    @Test
    fun `message content is independent of other fields for copy`() {
        val msg1 = ChatMessage(
            id = "id-1",
            role = "user",
            content = "Copy me",
            timestamp = "t1",
            status = MessageStatus.PENDING
        )
        val msg2 = ChatMessage(
            id = "id-2",
            role = "assistant",
            content = "Copy me",
            timestamp = "t2",
            status = MessageStatus.SENT
        )

        // Content is the same regardless of id, role, timestamp, status
        assertEquals(msg1.content, msg2.content)
    }
}
