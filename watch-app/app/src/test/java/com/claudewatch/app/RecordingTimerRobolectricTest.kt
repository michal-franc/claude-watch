package com.claudewatch.app

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowCountDownTimer
import org.robolectric.Shadows.shadowOf

/**
 * Robolectric tests for the watch app recording timer.
 * Tests real Android classes (CountDownTimer, View inflation)
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
        context = RuntimeEnvironment.getApplication()
    }

    // --- Layout inflation ---

    @Test
    fun `watch activity_main layout inflates without errors`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        assertNotNull(view)
    }

    @Test
    fun `recordButton exists and is a TextView`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)
        assertNotNull("recordButton should exist in layout", recordButton)
    }

    @Test
    fun `recordButton default text is Rec`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)
        assertEquals("Rec", recordButton.text.toString())
    }

    @Test
    fun `stateIndicator exists and starts as GONE`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val stateIndicator = view.findViewById<TextView>(R.id.stateIndicator)
        assertNotNull("stateIndicator should exist in layout", stateIndicator)
        assertEquals(View.GONE, stateIndicator.visibility)
    }

    @Test
    fun `abortButton exists and starts as GONE`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val abortButton = view.findViewById<TextView>(R.id.abortButton)
        assertNotNull("abortButton should exist in layout", abortButton)
        assertEquals(View.GONE, abortButton.visibility)
    }

    @Test
    fun `audioControls exist and start as GONE`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val audioControls = view.findViewById<View>(R.id.audioControls)
        assertNotNull("audioControls should exist in layout", audioControls)
        assertEquals(View.GONE, audioControls.visibility)
    }

    @Test
    fun `promptOverlay exists and starts as GONE`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val promptOverlay = view.findViewById<View>(R.id.promptOverlay)
        assertNotNull("promptOverlay should exist in layout", promptOverlay)
        assertEquals(View.GONE, promptOverlay.visibility)
    }

    // --- CountDownTimer via ShadowCountDownTimer ---

    /**
     * Creates a CountDownTimer that mirrors startRecordingTimer() logic
     * and returns both the timer and its shadow for test control.
     */
    private fun createRecordingTimer(
        recordButton: TextView,
        maxSeconds: Int = 60,
        warningSeconds: Int = 10,
        onAutoStop: () -> Unit = {}
    ): Pair<CountDownTimer, ShadowCountDownTimer> {
        val timer = object : CountDownTimer(maxSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                if (secondsLeft <= warningSeconds) {
                    recordButton.text = "Stop ($secondsLeft)"
                } else {
                    recordButton.text = "Stop & Send"
                }
            }

            override fun onFinish() {
                onAutoStop()
            }
        }
        timer.start()
        val shadow = shadowOf(timer)
        return Pair(timer, shadow)
    }

    @Test
    fun `timer start marks it as started`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)

        val (_, shadow) = createRecordingTimer(recordButton)

        assertTrue("Timer should be marked as started", shadow.hasStarted())
    }

    @Test
    fun `onTick with 30 seconds remaining shows Stop and Send`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)

        val (_, shadow) = createRecordingTimer(recordButton)

        // Simulate tick at 30 seconds remaining (outside warning zone)
        shadow.invokeTick(30_000L)

        assertEquals("Stop & Send", recordButton.text.toString())
    }

    @Test
    fun `onTick with 10 seconds remaining shows countdown`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)

        val (_, shadow) = createRecordingTimer(recordButton)

        // Simulate tick at warning threshold
        shadow.invokeTick(10_000L)

        assertEquals("Stop (10)", recordButton.text.toString())
    }

    @Test
    fun `onTick with 5 seconds remaining shows countdown`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)

        val (_, shadow) = createRecordingTimer(recordButton)

        shadow.invokeTick(5_000L)

        assertEquals("Stop (5)", recordButton.text.toString())
    }

    @Test
    fun `onTick with 1 second remaining shows countdown`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)

        val (_, shadow) = createRecordingTimer(recordButton)

        shadow.invokeTick(1_000L)

        assertEquals("Stop (1)", recordButton.text.toString())
    }

    @Test
    fun `onTick transitions from normal to warning text`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)

        val (_, shadow) = createRecordingTimer(recordButton)

        // First tick: outside warning zone
        shadow.invokeTick(15_000L)
        assertEquals("Stop & Send", recordButton.text.toString())

        // Cross into warning zone
        shadow.invokeTick(10_000L)
        assertEquals("Stop (10)", recordButton.text.toString())

        // Continue counting down
        shadow.invokeTick(5_000L)
        assertEquals("Stop (5)", recordButton.text.toString())
    }

    @Test
    fun `onFinish triggers auto-stop callback`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)
        var autoStopped = false

        val (_, shadow) = createRecordingTimer(recordButton) {
            autoStopped = true
        }

        shadow.invokeFinish()

        assertTrue("Auto-stop callback should have fired", autoStopped)
    }

    @Test
    fun `11 seconds remaining is NOT in warning zone`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)

        val (_, shadow) = createRecordingTimer(recordButton)

        // 11 seconds is just above the warning threshold
        shadow.invokeTick(11_000L)

        assertEquals("Stop & Send", recordButton.text.toString())
    }

    @Test
    fun `10999ms rounds to 10 seconds and shows warning countdown`() {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        val recordButton = view.findViewById<TextView>(R.id.recordButton)

        val (_, shadow) = createRecordingTimer(recordButton)

        // 10999ms -> 10 seconds via integer division
        shadow.invokeTick(10_999L)

        assertEquals("Stop (10)", recordButton.text.toString())
    }
}
