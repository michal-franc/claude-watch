package com.claudewatch.companion.wakeword

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.claudewatch.companion.R
import kotlin.math.sin

class AudioWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = context.getColor(R.color.primary)
        strokeCap = Paint.Cap.ROUND
    }

    private val path = Path()
    private var amplitude = 0f
    private var phase = 0f
    private var targetAmplitude = 0f

    // Animation
    private val animationRunnable = object : Runnable {
        override fun run() {
            // Smoothly interpolate amplitude
            amplitude += (targetAmplitude - amplitude) * 0.15f

            // Advance phase for wave movement
            phase += 0.15f
            if (phase > Math.PI * 2) {
                phase -= (Math.PI * 2).toFloat()
            }

            invalidate()
            postDelayed(this, 16) // ~60fps
        }
    }

    fun setAmplitude(amp: Float) {
        targetAmplitude = amp.coerceIn(0f, 1f)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(animationRunnable)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animationRunnable)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2

        // Base wave height scales with amplitude
        val waveHeight = (h / 2 - 10) * amplitude

        // Minimum visible wave when there's any amplitude
        val minWave = if (targetAmplitude > 0.01f) 8f else 0f
        val effectiveHeight = maxOf(waveHeight, minWave)

        path.reset()

        // Draw sine wave
        val frequency = 2.5f // Number of wave cycles
        val step = 4f // Pixel step for smoothness

        var x = 0f
        path.moveTo(x, centerY)

        while (x <= w) {
            val normalizedX = x / w * frequency * Math.PI * 2
            val y = centerY + sin(normalizedX + phase).toFloat() * effectiveHeight
            path.lineTo(x, y)
            x += step
        }

        canvas.drawPath(path, paint)
    }
}
