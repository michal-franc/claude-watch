package com.claudewatch.companion.kiosk

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController

class KioskManager(private val activity: Activity) {

    companion object {
        private const val TAP_THRESHOLD_MS = 500L
        private const val TAPS_REQUIRED = 3
        private const val CORNER_SIZE_DP = 50
    }

    private var isKioskMode = false
    private var tapTimes = mutableListOf<Long>()
    private var onExitKiosk: (() -> Unit)? = null

    fun enterKioskMode(onExit: () -> Unit) {
        onExitKiosk = onExit
        isKioskMode = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }

    fun exitKioskMode() {
        isKioskMode = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.show(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    fun handleTap(x: Float, y: Float): Boolean {
        if (!isKioskMode) return false

        // Check if tap is in top-left corner
        val cornerSizePx = CORNER_SIZE_DP * activity.resources.displayMetrics.density
        if (x > cornerSizePx || y > cornerSizePx) {
            tapTimes.clear()
            return false
        }

        val now = System.currentTimeMillis()
        tapTimes.add(now)

        // Remove old taps
        tapTimes.removeAll { now - it > TAP_THRESHOLD_MS * TAPS_REQUIRED }

        // Check if we have enough taps
        if (tapTimes.size >= TAPS_REQUIRED) {
            // Verify taps are within threshold
            val recentTaps = tapTimes.takeLast(TAPS_REQUIRED)
            if (recentTaps.last() - recentTaps.first() <= TAP_THRESHOLD_MS * TAPS_REQUIRED) {
                tapTimes.clear()
                exitKioskMode()
                onExitKiosk?.invoke()
                return true
            }
        }

        return false
    }

    fun isInKioskMode(): Boolean = isKioskMode
}
