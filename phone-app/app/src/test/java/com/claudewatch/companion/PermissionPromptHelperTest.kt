package com.claudewatch.companion

import android.view.View
import com.claudewatch.companion.network.ClaudePrompt
import com.claudewatch.companion.network.PromptOption
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure unit tests for [PermissionPromptHelper] — the extracted logic from
 * [MainActivity.updatePromptUI] that decides visibility states when a
 * permission prompt is shown or dismissed.
 *
 * These tests do NOT require Robolectric, so JaCoCo can instrument them.
 */
class PermissionPromptHelperTest {

    // --- computeViewState(null) — prompt dismissed ---

    @Test
    fun `null prompt hides promptContainer`() {
        val state = PermissionPromptHelper.computeViewState(null)
        assertEquals(View.GONE, state.promptContainerVisibility)
    }

    @Test
    fun `null prompt shows creatureView`() {
        val state = PermissionPromptHelper.computeViewState(null)
        assertEquals(View.VISIBLE, state.creatureViewVisibility)
    }

    @Test
    fun `null prompt shows inputBar`() {
        val state = PermissionPromptHelper.computeViewState(null)
        assertEquals(View.VISIBLE, state.inputBarVisibility)
    }

    @Test
    fun `null prompt sets permissionPromptActive to false`() {
        val state = PermissionPromptHelper.computeViewState(null)
        assertFalse(state.permissionPromptActive)
    }

    @Test
    fun `null prompt sets showTitle to false`() {
        val state = PermissionPromptHelper.computeViewState(null)
        assertFalse(state.showTitle)
    }

    @Test
    fun `null prompt sets showContext to false`() {
        val state = PermissionPromptHelper.computeViewState(null)
        assertFalse(state.showContext)
    }

    // --- computeViewState(prompt) — prompt active ---

    @Test
    fun `active prompt shows promptContainer`() {
        val state = PermissionPromptHelper.computeViewState(makePrompt("Allow?"))
        assertEquals(View.VISIBLE, state.promptContainerVisibility)
    }

    @Test
    fun `active prompt hides creatureView`() {
        val state = PermissionPromptHelper.computeViewState(makePrompt("Allow?"))
        assertEquals(View.GONE, state.creatureViewVisibility)
    }

    @Test
    fun `active prompt hides inputBar`() {
        val state = PermissionPromptHelper.computeViewState(makePrompt("Allow?"))
        assertEquals(View.GONE, state.inputBarVisibility)
    }

    @Test
    fun `active prompt sets permissionPromptActive to true`() {
        val state = PermissionPromptHelper.computeViewState(makePrompt("Allow?"))
        assertTrue(state.permissionPromptActive)
    }

    // --- Title visibility ---

    @Test
    fun `prompt with title shows title`() {
        val state = PermissionPromptHelper.computeViewState(
            makePrompt("Allow?", title = "PERMISSION REQUEST")
        )
        assertTrue(state.showTitle)
    }

    @Test
    fun `prompt with null title hides title`() {
        val state = PermissionPromptHelper.computeViewState(
            makePrompt("Allow?", title = null)
        )
        assertFalse(state.showTitle)
    }

    @Test
    fun `prompt with empty title hides title`() {
        val state = PermissionPromptHelper.computeViewState(
            makePrompt("Allow?", title = "")
        )
        assertFalse(state.showTitle)
    }

    // --- Context visibility ---

    @Test
    fun `prompt with context shows context`() {
        val state = PermissionPromptHelper.computeViewState(
            makePrompt("Allow?", context = "rm -rf /")
        )
        assertTrue(state.showContext)
    }

    @Test
    fun `prompt with null context hides context`() {
        val state = PermissionPromptHelper.computeViewState(
            makePrompt("Allow?", context = null)
        )
        assertFalse(state.showContext)
    }

    @Test
    fun `prompt with empty context hides context`() {
        val state = PermissionPromptHelper.computeViewState(
            makePrompt("Allow?", context = "")
        )
        assertFalse(state.showContext)
    }

    // --- computePostResponseState ---

    @Test
    fun `post-response state hides promptContainer`() {
        val state = PermissionPromptHelper.computePostResponseState()
        assertEquals(View.GONE, state.promptContainerVisibility)
    }

    @Test
    fun `post-response state shows creatureView`() {
        val state = PermissionPromptHelper.computePostResponseState()
        assertEquals(View.VISIBLE, state.creatureViewVisibility)
    }

    @Test
    fun `post-response state shows inputBar`() {
        val state = PermissionPromptHelper.computePostResponseState()
        assertEquals(View.VISIBLE, state.inputBarVisibility)
    }

    @Test
    fun `post-response state sets permissionPromptActive to false`() {
        val state = PermissionPromptHelper.computePostResponseState()
        assertFalse(state.permissionPromptActive)
    }

    @Test
    fun `post-response state matches null prompt state`() {
        val dismissed = PermissionPromptHelper.computeViewState(null)
        val postResponse = PermissionPromptHelper.computePostResponseState()
        assertEquals(dismissed, postResponse)
    }

    // --- Full cycle: show then dismiss ---

    @Test
    fun `show-dismiss cycle returns to initial state`() {
        val initial = PermissionPromptHelper.computeViewState(null)
        val shown = PermissionPromptHelper.computeViewState(makePrompt("Allow?"))
        val dismissed = PermissionPromptHelper.computeViewState(null)

        // Shown state differs from initial
        assertNotEquals(initial, shown)
        // Dismissed state matches initial
        assertEquals(initial, dismissed)
    }

    @Test
    fun `prompt with all fields populated shows title and context`() {
        val state = PermissionPromptHelper.computeViewState(
            makePrompt("Allow?", title = "BASH", context = "rm -rf /")
        )
        assertTrue(state.showTitle)
        assertTrue(state.showContext)
        assertTrue(state.permissionPromptActive)
    }

    @Test
    fun `prompt with no optional fields hides title and context`() {
        val state = PermissionPromptHelper.computeViewState(
            makePrompt("Allow?", title = null, context = null)
        )
        assertFalse(state.showTitle)
        assertFalse(state.showContext)
        assertTrue(state.permissionPromptActive)
    }

    // --- Permission prompt flag ---

    @Test
    fun `permission prompt sets permissionPromptActive`() {
        val prompt = ClaudePrompt(
            question = "Allow bash?",
            options = listOf(PromptOption(1, "Allow", "", false)),
            timestamp = "2025-01-01T00:00:00",
            isPermission = true,
            requestId = "req-123",
            toolName = "bash"
        )
        val state = PermissionPromptHelper.computeViewState(prompt)
        assertTrue(state.permissionPromptActive)
    }

    @Test
    fun `regular prompt also sets permissionPromptActive`() {
        val prompt = ClaudePrompt(
            question = "Choose an option",
            options = listOf(PromptOption(1, "A", "", false)),
            timestamp = "2025-01-01T00:00:00",
            isPermission = false
        )
        val state = PermissionPromptHelper.computeViewState(prompt)
        assertTrue(state.permissionPromptActive)
    }

    // --- Helper ---

    private fun makePrompt(
        question: String,
        title: String? = null,
        context: String? = null
    ) = ClaudePrompt(
        question = question,
        options = listOf(
            PromptOption(1, "Allow", "", false),
            PromptOption(2, "Deny", "", false)
        ),
        timestamp = "2025-01-01T00:00:00",
        title = title,
        context = context
    )
}
