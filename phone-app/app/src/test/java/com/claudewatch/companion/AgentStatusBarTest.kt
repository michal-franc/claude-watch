package com.claudewatch.companion

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric tests for the agent status bar UI elements defined in activity_main.xml.
 * Verifies layout inflation, initial visibility, string resources, and color resources.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgentStatusBarTest {

    private lateinit var rootView: View
    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        // Wrap with app theme so ?attr/selectableItemBackgroundBorderless resolves
        context = ContextThemeWrapper(app, R.style.Theme_ClaudeCompanion)
        rootView = LayoutInflater.from(context).inflate(R.layout.activity_main, null)
    }

    @Test
    fun `layout inflates and contains agentStatusBar container`() {
        val agentStatusBar = rootView.findViewById<LinearLayout>(R.id.agentStatusBar)
        assertNotNull("agentStatusBar should exist in layout", agentStatusBar)
    }

    @Test
    fun `layout inflates and contains agentStatusText`() {
        val agentStatusText = rootView.findViewById<TextView>(R.id.agentStatusText)
        assertNotNull("agentStatusText should exist in layout", agentStatusText)
    }

    @Test
    fun `layout inflates and contains agentStatusSpinner`() {
        val agentStatusSpinner = rootView.findViewById<ProgressBar>(R.id.agentStatusSpinner)
        assertNotNull("agentStatusSpinner should exist in layout", agentStatusSpinner)
    }

    @Test
    fun `agentStatusBar is hidden by default`() {
        val agentStatusBar = rootView.findViewById<LinearLayout>(R.id.agentStatusBar)
        assertEquals(
            "agentStatusBar should be GONE initially",
            View.GONE,
            agentStatusBar.visibility
        )
    }

    @Test
    fun `agentStatusText is inside agentStatusBar`() {
        val agentStatusBar = rootView.findViewById<LinearLayout>(R.id.agentStatusBar)
        val agentStatusText = rootView.findViewById<TextView>(R.id.agentStatusText)
        assertSame(
            "agentStatusText parent should be agentStatusBar",
            agentStatusBar,
            agentStatusText.parent
        )
    }

    @Test
    fun `agentStatusSpinner is inside agentStatusBar`() {
        val agentStatusBar = rootView.findViewById<LinearLayout>(R.id.agentStatusBar)
        val agentStatusSpinner = rootView.findViewById<ProgressBar>(R.id.agentStatusSpinner)
        assertSame(
            "agentStatusSpinner parent should be agentStatusBar",
            agentStatusBar,
            agentStatusSpinner.parent
        )
    }

    @Test
    fun `agent_thinking string resolves to Thinking with ellipsis`() {
        val thinkingText = context.getString(R.string.agent_thinking)
        assertEquals("Thinking\u2026", thinkingText)
    }

    @Test
    fun `agent_using_tool string contains format placeholder`() {
        val template = context.getString(R.string.agent_using_tool, "Bash")
        assertTrue(
            "Formatted agent_using_tool should contain the tool name",
            template.contains("Bash")
        )
    }

    @Test
    fun `agent_using_tool string formats correctly for various tools`() {
        val tools = listOf("Bash", "Read", "Write", "Edit", "Glob", "Grep")
        for (tool in tools) {
            val formatted = context.getString(R.string.agent_using_tool, tool)
            assertTrue(
                "Formatted string for $tool should contain tool name",
                formatted.contains(tool)
            )
            assertTrue(
                "Formatted string for $tool should contain ellipsis",
                formatted.contains("\u2026")
            )
        }
    }

    @Test
    fun `agent_status_thinking color resource resolves to yellow`() {
        val color = ContextCompat.getColor(context, R.color.agent_status_thinking)
        // #FACC15 = 0xFFFACC15 with full alpha
        assertEquals(0xFFFACC15.toInt(), color)
    }

    @Test
    fun `agent_status_tool color resource resolves to blue`() {
        val color = ContextCompat.getColor(context, R.color.agent_status_tool)
        // #60A5FA = 0xFF60A5FA with full alpha
        assertEquals(0xFF60A5FA.toInt(), color)
    }

    @Test
    fun `agentStatusBar has horizontal orientation`() {
        val agentStatusBar = rootView.findViewById<LinearLayout>(R.id.agentStatusBar)
        assertEquals(
            "agentStatusBar should be horizontal",
            LinearLayout.HORIZONTAL,
            agentStatusBar.orientation
        )
    }

    @Test
    fun `agentStatusSpinner has layout params`() {
        val spinner = rootView.findViewById<ProgressBar>(R.id.agentStatusSpinner)
        val params = spinner.layoutParams
        assertNotNull("Spinner should have layout params", params)
    }

    @Test
    fun `agentStatusBar can be toggled visible and back to gone`() {
        val agentStatusBar = rootView.findViewById<LinearLayout>(R.id.agentStatusBar)

        // Initially GONE
        assertEquals(View.GONE, agentStatusBar.visibility)

        // Show it
        agentStatusBar.visibility = View.VISIBLE
        assertEquals(View.VISIBLE, agentStatusBar.visibility)

        // Hide it again
        agentStatusBar.visibility = View.GONE
        assertEquals(View.GONE, agentStatusBar.visibility)
    }

    @Test
    fun `agentStatusText can display thinking message`() {
        val agentStatusText = rootView.findViewById<TextView>(R.id.agentStatusText)
        agentStatusText.text = context.getString(R.string.agent_thinking)
        assertEquals("Thinking\u2026", agentStatusText.text.toString())
    }

    @Test
    fun `agentStatusText can display tool message`() {
        val agentStatusText = rootView.findViewById<TextView>(R.id.agentStatusText)
        agentStatusText.text = context.getString(R.string.agent_using_tool, "Bash")
        assertTrue(agentStatusText.text.toString().contains("Bash"))
    }
}
