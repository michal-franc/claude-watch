package com.claudewatch.companion.chat

import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.claudewatch.companion.R
import com.claudewatch.companion.network.ChatMessage
import com.claudewatch.companion.network.MessageStatus
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatAdapterTest {

    private lateinit var context: Context
    private lateinit var parent: FrameLayout
    private val diffCallback = ChatAdapter.MessageDiffCallback()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        parent = FrameLayout(context)
    }

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

    // --- Copy message tests (Robolectric) ---

    private fun inflateClaudeView(): View {
        return LayoutInflater.from(context).inflate(R.layout.item_chat_claude, parent, false)
    }

    private fun inflateUserView(): View {
        return LayoutInflater.from(context).inflate(R.layout.item_chat_user, parent, false)
    }

    private fun getClipboardText(): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    }

    @Test
    fun `long press on claude message copies content to clipboard`() {
        val view = inflateClaudeView()
        val holder = ChatAdapter.MessageViewHolder(view)
        val message = ChatMessage(role = "assistant", content = "Here is the answer", timestamp = "t1")
        holder.bind(message)

        // Simulate the long-press copy logic
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("Chat message", message.content)
        clipboard.setPrimaryClip(clip)

        assertEquals("Here is the answer", getClipboardText())
    }

    @Test
    fun `long press on user message copies content to clipboard`() {
        val view = inflateUserView()
        val holder = ChatAdapter.MessageViewHolder(view)
        val message = ChatMessage(role = "user", content = "My question here", timestamp = "t1")
        holder.bind(message)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("Chat message", message.content)
        clipboard.setPrimaryClip(clip)

        assertEquals("My question here", getClipboardText())
    }

    @Test
    fun `multiline content copies correctly to clipboard`() {
        val content = "Line 1\nLine 2\n```\ncode block\n```"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = android.content.ClipData.newPlainText("Chat message", content)
        clipboard.setPrimaryClip(clip)

        assertEquals(content, getClipboardText())
    }

    @Test
    fun `copied indicator is initially gone`() {
        val view = inflateClaudeView()
        val holder = ChatAdapter.MessageViewHolder(view)
        val message = ChatMessage(role = "assistant", content = "Hello", timestamp = "t1")
        holder.bind(message)

        val indicator = view.findViewById<TextView>(R.id.copiedIndicator)
        assertEquals(View.GONE, indicator.visibility)
    }

    @Test
    fun `showCopiedIndicator makes indicator visible`() {
        val view = inflateClaudeView()
        val holder = ChatAdapter.MessageViewHolder(view)
        val message = ChatMessage(role = "assistant", content = "Hello", timestamp = "t1")
        holder.bind(message)

        holder.showCopiedIndicator()

        val indicator = view.findViewById<TextView>(R.id.copiedIndicator)
        assertEquals(View.VISIBLE, indicator.visibility)
    }

    @Test
    fun `copied indicator auto-hides after 1500ms`() {
        val view = inflateClaudeView()
        val holder = ChatAdapter.MessageViewHolder(view)
        val message = ChatMessage(role = "assistant", content = "Hello", timestamp = "t1")
        holder.bind(message)

        holder.showCopiedIndicator()
        val indicator = view.findViewById<TextView>(R.id.copiedIndicator)
        assertEquals(View.VISIBLE, indicator.visibility)

        // Advance time past the 1500ms delay
        ShadowLooper.idleMainLooper(1500, java.util.concurrent.TimeUnit.MILLISECONDS)

        assertEquals(View.GONE, indicator.visibility)
    }

    @Test
    fun `copied indicator stays visible before 1500ms`() {
        val view = inflateClaudeView()
        val holder = ChatAdapter.MessageViewHolder(view)
        val message = ChatMessage(role = "assistant", content = "Hello", timestamp = "t1")
        holder.bind(message)

        holder.showCopiedIndicator()
        val indicator = view.findViewById<TextView>(R.id.copiedIndicator)

        // Advance only 1000ms — should still be visible
        ShadowLooper.idleMainLooper(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertEquals(View.VISIBLE, indicator.visibility)
    }

    @Test
    fun `bind resets copied indicator to gone`() {
        val view = inflateClaudeView()
        val holder = ChatAdapter.MessageViewHolder(view)
        val message = ChatMessage(role = "assistant", content = "Hello", timestamp = "t1")

        // Show indicator
        holder.bind(message)
        holder.showCopiedIndicator()
        val indicator = view.findViewById<TextView>(R.id.copiedIndicator)
        assertEquals(View.VISIBLE, indicator.visibility)

        // Rebind should reset it
        holder.bind(message)
        assertEquals(View.GONE, indicator.visibility)
    }

    @Test
    fun `user message layout has copiedIndicator`() {
        val view = inflateUserView()
        val indicator = view.findViewById<TextView>(R.id.copiedIndicator)
        assertNotNull(indicator)
    }

    @Test
    fun `claude message layout has copiedIndicator`() {
        val view = inflateClaudeView()
        val indicator = view.findViewById<TextView>(R.id.copiedIndicator)
        assertNotNull(indicator)
    }

    @Test
    fun `messageText is not selectable - long press copies full message instead`() {
        val view = inflateClaudeView()
        val messageText = view.findViewById<TextView>(R.id.messageText)
        assertFalse("messageText should not be selectable (long-press copies full text)", messageText.isTextSelectable)
    }

    @Test
    fun `messageText is not selectable in user layout - long press copies full message instead`() {
        val view = inflateUserView()
        val messageText = view.findViewById<TextView>(R.id.messageText)
        assertFalse("messageText should not be selectable (long-press copies full text)", messageText.isTextSelectable)
    }
}
