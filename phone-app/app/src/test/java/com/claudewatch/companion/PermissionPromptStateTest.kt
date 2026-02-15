package com.claudewatch.companion

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [MainActivity.permissionPromptActive] StateFlow.
 *
 * This flow signals overlays (e.g. WakeWordActivity) that a permission prompt
 * is visible and they should dismiss to avoid blocking the prompt buttons.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PermissionPromptStateTest {

    @After
    fun teardown() {
        // Reset to default after each test so tests are independent
        MainActivity.permissionPromptActive.value = false
    }

    // --- Default value ---

    @Test
    fun `permissionPromptActive defaults to false`() {
        assertFalse(MainActivity.permissionPromptActive.value)
    }

    // --- Direct value manipulation ---

    @Test
    fun `setting permissionPromptActive to true is readable`() {
        MainActivity.permissionPromptActive.value = true
        assertTrue(MainActivity.permissionPromptActive.value)
    }

    @Test
    fun `setting permissionPromptActive back to false after true`() {
        MainActivity.permissionPromptActive.value = true
        assertTrue(MainActivity.permissionPromptActive.value)

        MainActivity.permissionPromptActive.value = false
        assertFalse(MainActivity.permissionPromptActive.value)
    }

    @Test
    fun `rapid toggling true-false-true works correctly`() {
        MainActivity.permissionPromptActive.value = true
        assertTrue(MainActivity.permissionPromptActive.value)

        MainActivity.permissionPromptActive.value = false
        assertFalse(MainActivity.permissionPromptActive.value)

        MainActivity.permissionPromptActive.value = true
        assertTrue(MainActivity.permissionPromptActive.value)
    }

    @Test
    fun `setting same value twice does not break state`() {
        MainActivity.permissionPromptActive.value = true
        MainActivity.permissionPromptActive.value = true
        assertTrue(MainActivity.permissionPromptActive.value)

        MainActivity.permissionPromptActive.value = false
        MainActivity.permissionPromptActive.value = false
        assertFalse(MainActivity.permissionPromptActive.value)
    }

    // --- Flow collection ---

    @Test
    fun `flow collector receives initial value`() = runTest {
        val value = MainActivity.permissionPromptActive.first()
        assertFalse(value)
    }

    @Test
    fun `flow collector receives updates when value changes`() = runTest {
        val collected = mutableListOf<Boolean>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            MainActivity.permissionPromptActive.collect { collected.add(it) }
        }

        // Initial value is already collected (false)
        MainActivity.permissionPromptActive.value = true
        MainActivity.permissionPromptActive.value = false

        job.cancel()

        assertEquals(listOf(false, true, false), collected)
    }

    @Test
    fun `flow collector sees true when prompt becomes active`() = runTest {
        val collected = mutableListOf<Boolean>()

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            MainActivity.permissionPromptActive.collect { collected.add(it) }
        }

        MainActivity.permissionPromptActive.value = true

        job.cancel()

        // Should have collected: initial false, then true
        assertTrue(collected.contains(true))
        assertEquals(true, collected.last())
    }

    @Test
    fun `multiple collectors all receive the same updates`() = runTest {
        val collector1 = mutableListOf<Boolean>()
        val collector2 = mutableListOf<Boolean>()

        val job1 = launch(UnconfinedTestDispatcher(testScheduler)) {
            MainActivity.permissionPromptActive.collect { collector1.add(it) }
        }
        val job2 = launch(UnconfinedTestDispatcher(testScheduler)) {
            MainActivity.permissionPromptActive.collect { collector2.add(it) }
        }

        MainActivity.permissionPromptActive.value = true
        MainActivity.permissionPromptActive.value = false

        job1.cancel()
        job2.cancel()

        assertEquals(collector1, collector2)
        assertEquals(listOf(false, true, false), collector1)
    }
}
