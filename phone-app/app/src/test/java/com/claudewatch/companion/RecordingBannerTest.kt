package com.claudewatch.companion

import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for the recording indicator banner in activity_main.xml.
 * Verifies the banner view exists, starts hidden, and can be toggled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecordingBannerTest {

    private lateinit var rootView: View
    private lateinit var recordingStatusBar: View
    private lateinit var recordingDot: View
    private lateinit var recordingStatusText: TextView

    @Before
    fun setup() {
        val context = androidx.appcompat.view.ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(),
            R.style.Theme_ClaudeCompanion
        )
        rootView = LayoutInflater.from(context)
            .inflate(R.layout.activity_main, null)
        recordingStatusBar = rootView.findViewById(R.id.recordingStatusBar)
        recordingDot = rootView.findViewById(R.id.recordingDot)
        recordingStatusText = rootView.findViewById(R.id.recordingStatusText)
    }

    @Test
    fun `recording status bar exists in layout`() {
        assertNotNull(recordingStatusBar)
        assertNotNull(recordingDot)
        assertNotNull(recordingStatusText)
    }

    @Test
    fun `recording status bar starts GONE`() {
        assertEquals(View.GONE, recordingStatusBar.visibility)
    }

    @Test
    fun `recording status bar becomes VISIBLE when shown`() {
        recordingStatusBar.visibility = View.VISIBLE
        assertEquals(View.VISIBLE, recordingStatusBar.visibility)
    }

    @Test
    fun `recording status bar returns GONE after hide`() {
        recordingStatusBar.visibility = View.VISIBLE
        assertEquals(View.VISIBLE, recordingStatusBar.visibility)

        recordingStatusBar.visibility = View.GONE
        assertEquals(View.GONE, recordingStatusBar.visibility)
    }

    @Test
    fun `recording countdown text can be formatted`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val text = context.getString(R.string.recording_countdown, 45)
        assertTrue(text.contains("45"))
    }

    @Test
    fun `recording dot can load pulse animation`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val anim = AnimationUtils.loadAnimation(context, R.anim.pulse)
        assertNotNull(anim)
    }
}
