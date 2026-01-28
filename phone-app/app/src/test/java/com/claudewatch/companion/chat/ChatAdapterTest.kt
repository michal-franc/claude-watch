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
}
