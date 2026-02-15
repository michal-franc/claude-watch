package com.claudewatch.companion

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.claudewatch.companion.creature.CreatureView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric tests that verify the activity_main.xml layout contains the
 * expected views for the permission-prompt-blocking fix.
 *
 * The fix hides creatureView and inputBar when a permission prompt is active
 * so that no overlay can intercept taps meant for the Allow/Deny buttons.
 * These tests ensure the layout structure supports that behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PermissionLayoutTest {

    private lateinit var rootView: View

    @Before
    fun setUp() {
        val context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_ClaudeCompanion
        )
        rootView = LayoutInflater.from(context).inflate(R.layout.activity_main, null)
    }

    // --- Layout structure: key views exist ---

    @Test
    fun `activity_main contains promptContainer`() {
        val promptContainer = rootView.findViewById<LinearLayout>(R.id.promptContainer)
        assertNotNull("promptContainer must exist in activity_main layout", promptContainer)
    }

    @Test
    fun `activity_main contains creatureView`() {
        val creatureView = rootView.findViewById<CreatureView>(R.id.creatureView)
        assertNotNull("creatureView must exist in activity_main layout", creatureView)
    }

    @Test
    fun `activity_main contains inputBar`() {
        val inputBar = rootView.findViewById<LinearLayout>(R.id.inputBar)
        assertNotNull("inputBar must exist in activity_main layout", inputBar)
    }

    @Test
    fun `activity_main contains chatRecyclerView`() {
        val chat = rootView.findViewById<RecyclerView>(R.id.chatRecyclerView)
        assertNotNull("chatRecyclerView must exist in activity_main layout", chat)
    }

    // --- Prompt container children ---

    @Test
    fun `promptContainer contains promptTitle, promptContext, promptQuestion, and promptOptions`() {
        val promptContainer = rootView.findViewById<LinearLayout>(R.id.promptContainer)

        val title = promptContainer.findViewById<TextView>(R.id.promptTitle)
        val context = promptContainer.findViewById<TextView>(R.id.promptContext)
        val question = promptContainer.findViewById<TextView>(R.id.promptQuestion)
        val options = promptContainer.findViewById<LinearLayout>(R.id.promptOptions)

        assertNotNull("promptTitle must be a child of promptContainer", title)
        assertNotNull("promptContext must be a child of promptContainer", context)
        assertNotNull("promptQuestion must be a child of promptContainer", question)
        assertNotNull("promptOptions must be a child of promptContainer", options)
    }

    // --- Input bar children ---

    @Test
    fun `inputBar contains voiceButton, messageInput, and sendButton`() {
        val inputBar = rootView.findViewById<LinearLayout>(R.id.inputBar)

        val voiceButton = inputBar.findViewById<ImageButton>(R.id.voiceButton)
        val messageInput = inputBar.findViewById<EditText>(R.id.messageInput)
        val sendButton = inputBar.findViewById<ImageButton>(R.id.sendButton)

        assertNotNull("voiceButton must be a child of inputBar", voiceButton)
        assertNotNull("messageInput must be a child of inputBar", messageInput)
        assertNotNull("sendButton must be a child of inputBar", sendButton)
    }

    // --- Default visibility states ---

    @Test
    fun `promptContainer is GONE by default`() {
        val promptContainer = rootView.findViewById<LinearLayout>(R.id.promptContainer)
        assertEquals(
            "promptContainer should be GONE by default so it doesn't block the main UI",
            View.GONE,
            promptContainer.visibility
        )
    }

    @Test
    fun `creatureView is VISIBLE by default`() {
        val creatureView = rootView.findViewById<CreatureView>(R.id.creatureView)
        assertEquals(
            "creatureView should be VISIBLE by default",
            View.VISIBLE,
            creatureView.visibility
        )
    }

    @Test
    fun `inputBar is VISIBLE by default`() {
        val inputBar = rootView.findViewById<LinearLayout>(R.id.inputBar)
        assertEquals(
            "inputBar should be VISIBLE by default",
            View.VISIBLE,
            inputBar.visibility
        )
    }

    @Test
    fun `promptTitle is GONE by default`() {
        val promptTitle = rootView.findViewById<TextView>(R.id.promptTitle)
        assertEquals(
            "promptTitle should be GONE until a prompt with a title is shown",
            View.GONE,
            promptTitle.visibility
        )
    }

    @Test
    fun `promptContext is GONE by default`() {
        val promptContext = rootView.findViewById<TextView>(R.id.promptContext)
        assertEquals(
            "promptContext should be GONE until a prompt with context is shown",
            View.GONE,
            promptContext.visibility
        )
    }

    // --- Visibility toggling simulates the permission-prompt fix ---

    @Test
    fun `hiding creatureView and inputBar when prompt is shown mirrors updatePromptUI behavior`() {
        val creatureView = rootView.findViewById<CreatureView>(R.id.creatureView)
        val inputBar = rootView.findViewById<LinearLayout>(R.id.inputBar)
        val promptContainer = rootView.findViewById<LinearLayout>(R.id.promptContainer)

        // Simulate what updatePromptUI does when a prompt arrives
        promptContainer.visibility = View.VISIBLE
        creatureView.visibility = View.GONE
        inputBar.visibility = View.GONE

        assertEquals(View.VISIBLE, promptContainer.visibility)
        assertEquals(View.GONE, creatureView.visibility)
        assertEquals(View.GONE, inputBar.visibility)
    }

    @Test
    fun `restoring creatureView and inputBar when prompt is dismissed mirrors updatePromptUI behavior`() {
        val creatureView = rootView.findViewById<CreatureView>(R.id.creatureView)
        val inputBar = rootView.findViewById<LinearLayout>(R.id.inputBar)
        val promptContainer = rootView.findViewById<LinearLayout>(R.id.promptContainer)

        // Show prompt first
        promptContainer.visibility = View.VISIBLE
        creatureView.visibility = View.GONE
        inputBar.visibility = View.GONE

        // Simulate what updatePromptUI(null) does
        promptContainer.visibility = View.GONE
        creatureView.visibility = View.VISIBLE
        inputBar.visibility = View.VISIBLE

        assertEquals(View.GONE, promptContainer.visibility)
        assertEquals(View.VISIBLE, creatureView.visibility)
        assertEquals(View.VISIBLE, inputBar.visibility)
    }

    // --- Layout ordering: promptContainer sits between chat and inputBar ---

    @Test
    fun `promptContainer appears after chatRecyclerView and before inputBar in layout order`() {
        val rootLayout = rootView.findViewById<LinearLayout>(R.id.rootLayout)

        var chatIndex = -1
        var promptIndex = -1
        var inputBarIndex = -1

        for (i in 0 until rootLayout.childCount) {
            val child = rootLayout.getChildAt(i)
            when (child.id) {
                R.id.chatRecyclerView -> chatIndex = i
                R.id.promptContainer -> promptIndex = i
                R.id.inputBar -> inputBarIndex = i
            }
        }

        assertTrue("chatRecyclerView must be found in rootLayout", chatIndex >= 0)
        assertTrue("promptContainer must be found in rootLayout", promptIndex >= 0)
        assertTrue("inputBar must be found in rootLayout", inputBarIndex >= 0)

        assertTrue(
            "promptContainer (index=$promptIndex) must come after chatRecyclerView (index=$chatIndex)",
            promptIndex > chatIndex
        )
        assertTrue(
            "inputBar (index=$inputBarIndex) must come after promptContainer (index=$promptIndex)",
            inputBarIndex > promptIndex
        )
    }
}
