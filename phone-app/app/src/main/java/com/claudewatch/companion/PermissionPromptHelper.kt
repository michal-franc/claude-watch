package com.claudewatch.companion

import android.view.View
import com.claudewatch.companion.network.ClaudePrompt

/**
 * Determines the view visibility state when a permission prompt is shown or dismissed.
 * Extracted from [MainActivity.updatePromptUI] for testability.
 *
 * When a prompt is active, the creature view and input bar must be hidden so they cannot
 * intercept touch events meant for the prompt buttons. When dismissed, they are restored.
 */
object PermissionPromptHelper {

    /**
     * Result describing what visibility each view should have and whether
     * [MainActivity.permissionPromptActive] should be true or false.
     */
    data class PromptViewState(
        val promptContainerVisibility: Int,
        val creatureViewVisibility: Int,
        val inputBarVisibility: Int,
        val permissionPromptActive: Boolean,
        val showTitle: Boolean,
        val showContext: Boolean
    )

    /**
     * Compute the view state for a given prompt (or null if dismissed).
     */
    fun computeViewState(prompt: ClaudePrompt?): PromptViewState {
        if (prompt == null) {
            return PromptViewState(
                promptContainerVisibility = View.GONE,
                creatureViewVisibility = View.VISIBLE,
                inputBarVisibility = View.VISIBLE,
                permissionPromptActive = false,
                showTitle = false,
                showContext = false
            )
        }

        return PromptViewState(
            promptContainerVisibility = View.VISIBLE,
            creatureViewVisibility = View.GONE,
            inputBarVisibility = View.GONE,
            permissionPromptActive = true,
            showTitle = !prompt.title.isNullOrEmpty(),
            showContext = !prompt.context.isNullOrEmpty()
        )
    }

    /**
     * Compute the view state after a prompt response is successfully sent.
     * Same as dismissing the prompt: restore all views.
     */
    fun computePostResponseState(): PromptViewState {
        return computeViewState(null)
    }
}
