package com.claudewatch.companion

import android.os.CountDownTimer
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowCountDownTimer
import org.robolectric.shadows.ShadowToast

/**
 * Robolectric tests for the phone app recording timer.
 * Tests real Android classes (CountDownTimer, View inflation, Toast)
 * on a Robolectric-simulated Android environment.
 *
 * Note: Robolectric's ShadowCountDownTimer makes start() a no-op and provides
 * invokeTick(ms) / invokeFinish() to manually drive the timer. This is the
 * officially supported way to test CountDownTimer behavior with Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordingTimerRobolectricTest {

    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        // Use the app's Material theme to support ?attr/ references in layouts
        context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(),
            R.style.Theme_ClaudeCompanion
        )
    }

    // --- Layout inflation ---

    @Test
    fun `phone activity_main layout inflates without errors`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        assertNotNull(view)
    }

    @Test
    fun `voiceButton exists and is an ImageButton`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val voiceButton = view.findViewById<ImageButton>(R.id.voiceButton)
        assertNotNull("voiceButton should exist in layout", voiceButton)
    }

    @Test
    fun `sendButton exists in layout`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val sendButton = view.findViewById<ImageButton>(R.id.sendButton)
        assertNotNull("sendButton should exist in layout", sendButton)
    }

    @Test
    fun `messageInput exists and is an EditText`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val messageInput = view.findViewById<EditText>(R.id.messageInput)
        assertNotNull("messageInput should exist in layout", messageInput)
    }

    @Test
    fun `promptContainer exists and starts as GONE`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val promptContainer = view.findViewById<View>(R.id.promptContainer)
        assertNotNull("promptContainer should exist in layout", promptContainer)
        assertEquals(View.GONE, promptContainer.visibility)
    }

    @Test
    fun `contextBar exists and starts as GONE`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val contextBar = view.findViewById<View>(R.id.contextBar)
        assertNotNull("contextBar should exist in layout", contextBar)
        assertEquals(View.GONE, contextBar.visibility)
    }

    @Test
    fun `kioskExitButton exists and starts as GONE`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val kioskExitButton = view.findViewById<ImageButton>(R.id.kioskExitButton)
        assertNotNull("kioskExitButton should exist in layout", kioskExitButton)
        assertEquals(View.GONE, kioskExitButton.visibility)
    }

    // --- CountDownTimer via ShadowCountDownTimer ---

    /**
     * Creates a CountDownTimer that mirrors the phone app's startRecordingTimer() logic.
     * Returns both the timer and its shadow for test control.
     */
    private fun createRecordingTimer(
        maxSeconds: Int = 60,
        warningSeconds: Int = 10,
        onAutoStop: () -> Unit = {}
    ): Pair<CountDownTimer, ShadowCountDownTimer> {
        val timer = object : CountDownTimer(maxSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                if (secondsLeft == warningSeconds) {
                    Toast.makeText(
                        context,
                        "Recording stops in ${secondsLeft}s",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFinish() {
                Toast.makeText(context, "Max recording time reached", Toast.LENGTH_SHORT).show()
                onAutoStop()
            }
        }
        timer.start()
        val shadow = shadowOf(timer)
        return Pair(timer, shadow)
    }

    @Test
    fun `timer start marks it as started`() {
        val (_, shadow) = createRecordingTimer()
        assertTrue("Timer should be marked as started", shadow.hasStarted())
    }

    @Test
    fun `onTick at warning threshold shows toast`() {
        val (_, shadow) = createRecordingTimer()

        shadow.invokeTick(10_000L)

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertNotNull("Toast should be shown at warning threshold", latestToast)
        assertEquals("Recording stops in 10s", latestToast)
    }

    @Test
    fun `onTick above warning threshold does not show toast`() {
        val (_, shadow) = createRecordingTimer()

        shadow.invokeTick(30_000L)

        assertNull(
            "No toast should be shown above warning threshold",
            ShadowToast.getLatestToast()
        )
    }

    @Test
    fun `onTick below warning threshold does not show toast`() {
        val (_, shadow) = createRecordingTimer()

        // 5 seconds remaining -- not the exact threshold so no toast
        shadow.invokeTick(5_000L)

        assertNull(
            "No toast should be shown below warning threshold (only at exact threshold)",
            ShadowToast.getLatestToast()
        )
    }

    @Test
    fun `onFinish triggers auto-stop with toast`() {
        var autoStopped = false

        val (_, shadow) = createRecordingTimer {
            autoStopped = true
        }

        shadow.invokeFinish()

        assertTrue("Auto-stop callback should have fired", autoStopped)
        assertEquals("Max recording time reached", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `10999ms rounds to 10 seconds and triggers warning toast`() {
        val (_, shadow) = createRecordingTimer()

        // 10999ms -> 10 seconds via integer division, matching warning threshold
        shadow.invokeTick(10_999L)

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertEquals("Recording stops in 10s", latestToast)
    }

    @Test
    fun `11000ms does not trigger warning toast`() {
        val (_, shadow) = createRecordingTimer()

        // 11000ms -> 11 seconds, above warning threshold
        shadow.invokeTick(11_000L)

        assertNull(
            "No toast at 11 seconds",
            ShadowToast.getLatestToast()
        )
    }

    @Test
    fun `sequential ticks show toast only at exact warning threshold`() {
        val (_, shadow) = createRecordingTimer()

        // Simulate countdown from 12 to 8
        shadow.invokeTick(12_000L)
        assertNull("No toast at 12s", ShadowToast.getLatestToast())

        shadow.invokeTick(11_000L)
        assertNull("No toast at 11s", ShadowToast.getLatestToast())

        shadow.invokeTick(10_000L)
        assertEquals("Toast at 10s", "Recording stops in 10s", ShadowToast.getTextOfLatestToast())

        // Reset toast state by recording the toast count
        val toastCountAfterWarning = ShadowToast.shownToastCount()

        shadow.invokeTick(9_000L)
        assertEquals(
            "No additional toast at 9s",
            toastCountAfterWarning,
            ShadowToast.shownToastCount()
        )

        shadow.invokeTick(8_000L)
        assertEquals(
            "No additional toast at 8s",
            toastCountAfterWarning,
            ShadowToast.shownToastCount()
        )
    }
}
