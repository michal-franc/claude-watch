package com.claudewatch.companion

import android.view.View
import android.widget.LinearLayout
import com.claudewatch.companion.creature.CreatureView
import com.claudewatch.companion.network.ClaudePrompt
import com.claudewatch.companion.network.PromptOption
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Method

/**
 * Robolectric tests for MainActivity.updatePromptUI(), covering the
 * permission-tap-blocking fix: hiding creatureView/inputBar when a prompt
 * is shown, restoring them when dismissed, and toggling permissionPromptActive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityPromptTest {

    private lateinit var activity: MainActivity
    private lateinit var updatePromptUI: Method

    @Before
    fun setUp() {
        // Build and create the activity (WebSocket will fail to connect - that's fine)
        activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .get()

        // Access the private updatePromptUI method via reflection
        updatePromptUI = MainActivity::class.java.getDeclaredMethod(
            "updatePromptUI",
            ClaudePrompt::class.java
        )
        updatePromptUI.isAccessible = true
    }

    @After
    fun tearDown() {
        MainActivity.permissionPromptActive.value = false
    }

    // --- Prompt shown: visibility changes ---

    @Test
    fun `updatePromptUI with prompt hides creatureView`() {
        val prompt = makePrompt("Allow tool?")
        updatePromptUI.invoke(activity, prompt)

        val creatureView = activity.findViewById<CreatureView>(R.id.creatureView)
        assertEquals(View.GONE, creatureView.visibility)
    }

    @Test
    fun `updatePromptUI with prompt hides inputBar`() {
        val prompt = makePrompt("Allow tool?")
        updatePromptUI.invoke(activity, prompt)

        val inputBar = activity.findViewById<LinearLayout>(R.id.inputBar)
        assertEquals(View.GONE, inputBar.visibility)
    }

    @Test
    fun `updatePromptUI with prompt shows promptContainer`() {
        val prompt = makePrompt("Allow tool?")
        updatePromptUI.invoke(activity, prompt)

        val promptContainer = activity.findViewById<LinearLayout>(R.id.promptContainer)
        assertEquals(View.VISIBLE, promptContainer.visibility)
    }

    @Test
    fun `updatePromptUI with prompt sets permissionPromptActive to true`() {
        val prompt = makePrompt("Allow tool?")
        updatePromptUI.invoke(activity, prompt)

        assertTrue(MainActivity.permissionPromptActive.value)
    }

    // --- Prompt dismissed (null): visibility restored ---

    @Test
    fun `updatePromptUI with null restores creatureView`() {
        // Show prompt first
        updatePromptUI.invoke(activity, makePrompt("Allow?"))
        // Then dismiss
        updatePromptUI.invoke(activity, null as ClaudePrompt?)

        val creatureView = activity.findViewById<CreatureView>(R.id.creatureView)
        assertEquals(View.VISIBLE, creatureView.visibility)
    }

    @Test
    fun `updatePromptUI with null restores inputBar`() {
        updatePromptUI.invoke(activity, makePrompt("Allow?"))
        updatePromptUI.invoke(activity, null as ClaudePrompt?)

        val inputBar = activity.findViewById<LinearLayout>(R.id.inputBar)
        assertEquals(View.VISIBLE, inputBar.visibility)
    }

    @Test
    fun `updatePromptUI with null hides promptContainer`() {
        updatePromptUI.invoke(activity, makePrompt("Allow?"))
        updatePromptUI.invoke(activity, null as ClaudePrompt?)

        val promptContainer = activity.findViewById<LinearLayout>(R.id.promptContainer)
        assertEquals(View.GONE, promptContainer.visibility)
    }

    @Test
    fun `updatePromptUI with null sets permissionPromptActive to false`() {
        // Activate first
        updatePromptUI.invoke(activity, makePrompt("Allow?"))
        assertTrue(MainActivity.permissionPromptActive.value)

        // Dismiss
        updatePromptUI.invoke(activity, null as ClaudePrompt?)
        assertFalse(MainActivity.permissionPromptActive.value)
    }

    // --- Prompt content rendering ---

    @Test
    fun `updatePromptUI shows title when present`() {
        val prompt = makePrompt("Allow?", title = "PERMISSION REQUEST")
        updatePromptUI.invoke(activity, prompt)

        val titleView = activity.findViewById<android.widget.TextView>(R.id.promptTitle)
        assertEquals(View.VISIBLE, titleView.visibility)
        assertEquals("PERMISSION REQUEST", titleView.text.toString())
    }

    @Test
    fun `updatePromptUI hides title when null`() {
        val prompt = makePrompt("Allow?", title = null)
        updatePromptUI.invoke(activity, prompt)

        val titleView = activity.findViewById<android.widget.TextView>(R.id.promptTitle)
        assertEquals(View.GONE, titleView.visibility)
    }

    @Test
    fun `updatePromptUI hides title when empty`() {
        val prompt = makePrompt("Allow?", title = "")
        updatePromptUI.invoke(activity, prompt)

        val titleView = activity.findViewById<android.widget.TextView>(R.id.promptTitle)
        assertEquals(View.GONE, titleView.visibility)
    }

    @Test
    fun `updatePromptUI shows context when present`() {
        val prompt = makePrompt("Allow?", context = "rm -rf /")
        updatePromptUI.invoke(activity, prompt)

        val contextView = activity.findViewById<android.widget.TextView>(R.id.promptContext)
        assertEquals(View.VISIBLE, contextView.visibility)
        assertEquals("rm -rf /", contextView.text.toString())
    }

    @Test
    fun `updatePromptUI hides context when null`() {
        val prompt = makePrompt("Allow?", context = null)
        updatePromptUI.invoke(activity, prompt)

        val contextView = activity.findViewById<android.widget.TextView>(R.id.promptContext)
        assertEquals(View.GONE, contextView.visibility)
    }

    @Test
    fun `updatePromptUI hides context when empty`() {
        val prompt = makePrompt("Allow?", context = "")
        updatePromptUI.invoke(activity, prompt)

        val contextView = activity.findViewById<android.widget.TextView>(R.id.promptContext)
        assertEquals(View.GONE, contextView.visibility)
    }

    @Test
    fun `updatePromptUI sets question text`() {
        val prompt = makePrompt("Do you want to allow this tool?")
        updatePromptUI.invoke(activity, prompt)

        val questionView = activity.findViewById<android.widget.TextView>(R.id.promptQuestion)
        assertEquals("Do you want to allow this tool?", questionView.text.toString())
    }

    @Test
    fun `updatePromptUI renders option buttons`() {
        val options = listOf(
            PromptOption(1, "Allow", "Let the tool run", false),
            PromptOption(2, "Deny", "Block the tool", false)
        )
        val prompt = makePrompt("Allow tool?", options = options)
        updatePromptUI.invoke(activity, prompt)

        val optionsContainer = activity.findViewById<LinearLayout>(R.id.promptOptions)
        assertEquals(2, optionsContainer.childCount)
    }

    @Test
    fun `updatePromptUI clears old options before adding new ones`() {
        // Show first prompt with 2 options
        val options1 = listOf(
            PromptOption(1, "A", "", false),
            PromptOption(2, "B", "", false)
        )
        updatePromptUI.invoke(activity, makePrompt("Q1?", options = options1))

        val optionsContainer = activity.findViewById<LinearLayout>(R.id.promptOptions)
        assertEquals(2, optionsContainer.childCount)

        // Show second prompt with 3 options
        val options2 = listOf(
            PromptOption(1, "X", "", false),
            PromptOption(2, "Y", "", false),
            PromptOption(3, "Z", "", false)
        )
        updatePromptUI.invoke(activity, makePrompt("Q2?", options = options2))

        assertEquals(3, optionsContainer.childCount)
    }

    @Test
    fun `updatePromptUI shows option description when non-empty`() {
        val options = listOf(
            PromptOption(1, "Allow", "Grant permission", false)
        )
        val prompt = makePrompt("Allow?", options = options)
        updatePromptUI.invoke(activity, prompt)

        val optionsContainer = activity.findViewById<LinearLayout>(R.id.promptOptions)
        val optionView = optionsContainer.getChildAt(0)
        val descText = optionView.findViewById<android.widget.TextView>(R.id.optionDescription)
        assertEquals(View.VISIBLE, descText.visibility)
        assertEquals("Grant permission", descText.text.toString())
    }

    @Test
    fun `updatePromptUI hides option description when empty`() {
        val options = listOf(
            PromptOption(1, "Allow", "", false)
        )
        val prompt = makePrompt("Allow?", options = options)
        updatePromptUI.invoke(activity, prompt)

        val optionsContainer = activity.findViewById<LinearLayout>(R.id.promptOptions)
        val optionView = optionsContainer.getChildAt(0)
        val descText = optionView.findViewById<android.widget.TextView>(R.id.optionDescription)
        assertEquals(View.GONE, descText.visibility)
    }

    @Test
    fun `updatePromptUI marks selected option`() {
        val options = listOf(
            PromptOption(1, "Allow", "", true),
            PromptOption(2, "Deny", "", false)
        )
        val prompt = makePrompt("Allow?", options = options)
        updatePromptUI.invoke(activity, prompt)

        val optionsContainer = activity.findViewById<LinearLayout>(R.id.promptOptions)
        val first = optionsContainer.getChildAt(0)
        val second = optionsContainer.getChildAt(1)
        assertTrue(first.isSelected)
        assertFalse(second.isSelected)
    }

    // --- Full show-dismiss cycle ---

    @Test
    fun `show then dismiss prompt restores full UI state`() {
        // Initial state
        assertEquals(View.VISIBLE, activity.findViewById<CreatureView>(R.id.creatureView).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<LinearLayout>(R.id.inputBar).visibility)
        assertEquals(View.GONE, activity.findViewById<LinearLayout>(R.id.promptContainer).visibility)
        assertFalse(MainActivity.permissionPromptActive.value)

        // Show prompt
        updatePromptUI.invoke(activity, makePrompt("Allow?"))
        assertEquals(View.GONE, activity.findViewById<CreatureView>(R.id.creatureView).visibility)
        assertEquals(View.GONE, activity.findViewById<LinearLayout>(R.id.inputBar).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<LinearLayout>(R.id.promptContainer).visibility)
        assertTrue(MainActivity.permissionPromptActive.value)

        // Dismiss prompt
        updatePromptUI.invoke(activity, null as ClaudePrompt?)
        assertEquals(View.VISIBLE, activity.findViewById<CreatureView>(R.id.creatureView).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<LinearLayout>(R.id.inputBar).visibility)
        assertEquals(View.GONE, activity.findViewById<LinearLayout>(R.id.promptContainer).visibility)
        assertFalse(MainActivity.permissionPromptActive.value)
    }

    // --- Helper ---

    private fun makePrompt(
        question: String,
        title: String? = null,
        context: String? = null,
        options: List<PromptOption> = listOf(
            PromptOption(1, "Allow", "", false),
            PromptOption(2, "Deny", "", false)
        )
    ) = ClaudePrompt(
        question = question,
        options = options,
        timestamp = "2025-01-01T00:00:00",
        title = title,
        context = context
    )
}
