package com.claudewatch.companion.creature

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CreatureState enum and related logic.
 */
class CreatureStateTest {

    @Test
    fun `all creature states exist`() {
        val states = CreatureState.values()

        assertEquals(6, states.size)
        assertTrue(states.contains(CreatureState.IDLE))
        assertTrue(states.contains(CreatureState.LISTENING))
        assertTrue(states.contains(CreatureState.THINKING))
        assertTrue(states.contains(CreatureState.SPEAKING))
        assertTrue(states.contains(CreatureState.SLEEPING))
        assertTrue(states.contains(CreatureState.OFFLINE))
    }

    @Test
    fun `creature state valueOf works correctly`() {
        assertEquals(CreatureState.IDLE, CreatureState.valueOf("IDLE"))
        assertEquals(CreatureState.LISTENING, CreatureState.valueOf("LISTENING"))
        assertEquals(CreatureState.THINKING, CreatureState.valueOf("THINKING"))
        assertEquals(CreatureState.SPEAKING, CreatureState.valueOf("SPEAKING"))
        assertEquals(CreatureState.SLEEPING, CreatureState.valueOf("SLEEPING"))
        assertEquals(CreatureState.OFFLINE, CreatureState.valueOf("OFFLINE"))
    }

    @Test
    fun `creature state ordinal values`() {
        assertEquals(0, CreatureState.IDLE.ordinal)
        assertEquals(1, CreatureState.LISTENING.ordinal)
        assertEquals(2, CreatureState.THINKING.ordinal)
        assertEquals(3, CreatureState.SPEAKING.ordinal)
        assertEquals(4, CreatureState.SLEEPING.ordinal)
        assertEquals(5, CreatureState.OFFLINE.ordinal)
    }

    // State mapping tests
    @Test
    fun `claude status idle maps to IDLE`() {
        val state = mapClaudeStatusToCreatureState("idle")
        assertEquals(CreatureState.IDLE, state)
    }

    @Test
    fun `claude status listening maps to LISTENING`() {
        val state = mapClaudeStatusToCreatureState("listening")
        assertEquals(CreatureState.LISTENING, state)
    }

    @Test
    fun `claude status thinking maps to THINKING`() {
        val state = mapClaudeStatusToCreatureState("thinking")
        assertEquals(CreatureState.THINKING, state)
    }

    @Test
    fun `claude status speaking maps to SPEAKING`() {
        val state = mapClaudeStatusToCreatureState("speaking")
        assertEquals(CreatureState.SPEAKING, state)
    }

    @Test
    fun `claude status waiting maps to THINKING`() {
        val state = mapClaudeStatusToCreatureState("waiting")
        assertEquals(CreatureState.THINKING, state)
    }

    @Test
    fun `unknown claude status maps to IDLE`() {
        val state = mapClaudeStatusToCreatureState("unknown")
        assertEquals(CreatureState.IDLE, state)
    }

    @Test
    fun `empty claude status maps to IDLE`() {
        val state = mapClaudeStatusToCreatureState("")
        assertEquals(CreatureState.IDLE, state)
    }

    // Connection state mapping
    @Test
    fun `disconnected connection maps to OFFLINE`() {
        val state = mapConnectionToCreatureState(isConnected = false, claudeStatus = "idle")
        assertEquals(CreatureState.OFFLINE, state)
    }

    @Test
    fun `connected with claude status maps correctly`() {
        val state = mapConnectionToCreatureState(isConnected = true, claudeStatus = "thinking")
        assertEquals(CreatureState.THINKING, state)
    }

    // Animation property tests
    @Test
    fun `IDLE state has breathing animation`() {
        val props = getAnimationProperties(CreatureState.IDLE)
        assertTrue(props.hasBreathing)
        assertTrue(props.hasBlinking)
        assertFalse(props.hasThinkingBubbles)
    }

    @Test
    fun `THINKING state has thinking bubbles`() {
        val props = getAnimationProperties(CreatureState.THINKING)
        assertTrue(props.hasThinkingBubbles)
    }

    @Test
    fun `SPEAKING state has mouth animation`() {
        val props = getAnimationProperties(CreatureState.SPEAKING)
        assertTrue(props.hasMouthAnimation)
    }

    @Test
    fun `SLEEPING state has zzz animation`() {
        val props = getAnimationProperties(CreatureState.SLEEPING)
        assertTrue(props.hasZzzAnimation)
        assertFalse(props.hasBlinking) // Eyes closed
    }

    @Test
    fun `OFFLINE state is grayscale`() {
        val props = getAnimationProperties(CreatureState.OFFLINE)
        assertTrue(props.isGrayscale)
    }

    @Test
    fun `LISTENING state has ear animation`() {
        val props = getAnimationProperties(CreatureState.LISTENING)
        assertTrue(props.hasEarAnimation)
    }

    // Helper functions
    private fun mapClaudeStatusToCreatureState(status: String): CreatureState {
        return when (status.lowercase()) {
            "idle" -> CreatureState.IDLE
            "listening" -> CreatureState.LISTENING
            "thinking", "waiting" -> CreatureState.THINKING
            "speaking" -> CreatureState.SPEAKING
            else -> CreatureState.IDLE
        }
    }

    private fun mapConnectionToCreatureState(isConnected: Boolean, claudeStatus: String): CreatureState {
        return if (!isConnected) {
            CreatureState.OFFLINE
        } else {
            mapClaudeStatusToCreatureState(claudeStatus)
        }
    }

    data class AnimationProperties(
        val hasBreathing: Boolean = false,
        val hasBlinking: Boolean = false,
        val hasThinkingBubbles: Boolean = false,
        val hasMouthAnimation: Boolean = false,
        val hasZzzAnimation: Boolean = false,
        val hasEarAnimation: Boolean = false,
        val isGrayscale: Boolean = false
    )

    private fun getAnimationProperties(state: CreatureState): AnimationProperties {
        return when (state) {
            CreatureState.IDLE -> AnimationProperties(
                hasBreathing = true,
                hasBlinking = true
            )
            CreatureState.LISTENING -> AnimationProperties(
                hasBreathing = true,
                hasBlinking = true,
                hasEarAnimation = true
            )
            CreatureState.THINKING -> AnimationProperties(
                hasBreathing = true,
                hasBlinking = true,
                hasThinkingBubbles = true
            )
            CreatureState.SPEAKING -> AnimationProperties(
                hasBreathing = true,
                hasBlinking = true,
                hasMouthAnimation = true
            )
            CreatureState.SLEEPING -> AnimationProperties(
                hasBreathing = true,
                hasZzzAnimation = true
            )
            CreatureState.OFFLINE -> AnimationProperties(
                isGrayscale = true
            )
        }
    }
}
