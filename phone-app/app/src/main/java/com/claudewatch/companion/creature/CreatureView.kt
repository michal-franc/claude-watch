package com.claudewatch.companion.creature

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import com.claudewatch.companion.R
import kotlin.math.cos
import kotlin.math.sin

class CreatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentState = CreatureState.IDLE
    private var animationProgress = 0f
    private var breathingProgress = 0f
    private var blinkProgress = 0f
    private var thinkingBubbleProgress = 0f

    // Smooth transition properties (interpolated between states)
    private var earAngle = 0f
    private var targetEarAngle = 0f
    private var pupilOffsetX = 0f
    private var pupilOffsetY = 0f
    private var targetPupilOffsetX = 0f
    private var targetPupilOffsetY = 0f
    private var eyeClosure = 0f
    private var targetEyeClosure = 0f
    private var bodyGrayAmount = 0f
    private var targetBodyGrayAmount = 0f

    private var stateTransitionAnimator: ValueAnimator? = null

    // Paints
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bodyGradient: Shader
        get() {
            val centerX = width / 2f
            val centerY = height * 0.5f
            val radius = maxOf(minOf(width, height) * 0.35f, 1f)
            val gradientRadius = maxOf(radius * 1.5f, 1f)
            return RadialGradient(
                centerX, centerY - radius * 0.3f,
                gradientRadius,
                intArrayOf(
                    context.getColor(R.color.creature_orange_light),
                    context.getColor(R.color.creature_orange_dark)
                ),
                floatArrayOf(0.3f, 1f),
                Shader.TileMode.CLAMP
            )
        }

    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    private val mouthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 180
        style = Paint.Style.FILL
    }

    private val zzzPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.DEFAULT_BOLD
    }

    // Animators
    private val breathingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { animator ->
            breathingProgress = animator.animatedValue as Float
            invalidate()
        }
    }

    private val blinkAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
        duration = 200
        interpolator = LinearInterpolator()
        addUpdateListener { animator ->
            blinkProgress = animator.animatedValue as Float
            invalidate()
        }
    }

    private val thinkingAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animator ->
            thinkingBubbleProgress = animator.animatedValue as Float
            invalidate()
        }
    }

    private val mainAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { animator ->
            animationProgress = animator.animatedValue as Float
            invalidate()
        }
    }

    init {
        breathingAnimator.start()
        mainAnimator.start()
        startRandomBlinks()
    }

    private fun startRandomBlinks() {
        postDelayed({
            if (currentState != CreatureState.SLEEPING && currentState != CreatureState.THINKING) {
                blinkAnimator.start()
            }
            startRandomBlinks()
        }, (2000..5000).random().toLong())
    }

    fun setState(state: CreatureState) {
        if (currentState == state) return
        currentState = state

        when (state) {
            CreatureState.THINKING -> thinkingAnimator.start()
            else -> thinkingAnimator.cancel()
        }

        // Set targets based on new state
        targetEarAngle = when (state) {
            CreatureState.LISTENING -> -20f
            CreatureState.OFFLINE -> 30f
            CreatureState.SLEEPING -> 20f
            else -> 0f
        }
        targetPupilOffsetX = 0f
        targetPupilOffsetY = when (state) {
            CreatureState.LISTENING -> -2f
            CreatureState.OFFLINE -> 4f
            else -> 0f
        }
        targetEyeClosure = when (state) {
            CreatureState.SLEEPING, CreatureState.THINKING -> 1f
            else -> 0f
        }
        targetBodyGrayAmount = when (state) {
            CreatureState.OFFLINE -> 1f
            else -> 0f
        }

        // Animate from current values to targets
        val startEar = earAngle
        val startPupilX = pupilOffsetX
        val startPupilY = pupilOffsetY
        val startEyeClosure = eyeClosure
        val startGray = bodyGrayAmount

        stateTransitionAnimator?.cancel()
        stateTransitionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 350
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                earAngle = startEar + (targetEarAngle - startEar) * t
                pupilOffsetX = startPupilX + (targetPupilOffsetX - startPupilX) * t
                pupilOffsetY = startPupilY + (targetPupilOffsetY - startPupilY) * t
                eyeClosure = startEyeClosure + (targetEyeClosure - startEyeClosure) * t
                bodyGrayAmount = startGray + (targetBodyGrayAmount - startGray) * t
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height * 0.5f
        val baseRadius = minOf(width, height) * 0.3f

        // Apply breathing/bouncing effect
        val breathScale = 1f + breathingProgress * 0.03f
        val bounceOffset = when (currentState) {
            CreatureState.SPEAKING -> sin(animationProgress * Math.PI.toFloat() * 2) * 10f
            else -> 0f
        }

        canvas.save()
        canvas.translate(0f, -bounceOffset)
        canvas.scale(breathScale, breathScale, centerX, centerY)

        // Draw body (blob shape)
        drawBody(canvas, centerX, centerY, baseRadius)

        // Draw ears
        drawEars(canvas, centerX, centerY, baseRadius)

        // Draw eyes
        drawEyes(canvas, centerX, centerY, baseRadius)

        // Draw mouth
        drawMouth(canvas, centerX, centerY, baseRadius)

        canvas.restore()

        // Draw effects (not affected by body transform)
        when (currentState) {
            CreatureState.THINKING -> drawThinkingBubbles(canvas, centerX, centerY, baseRadius)
            CreatureState.SLEEPING -> drawZzz(canvas, centerX, centerY, baseRadius)
            else -> {}
        }
    }

    private fun drawBody(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        bodyPaint.shader = bodyGradient

        // Create a blob-like path
        val path = Path()
        val wobble = if (currentState == CreatureState.SPEAKING) {
            sin(animationProgress * Math.PI.toFloat() * 4) * 5f
        } else 0f

        path.addOval(
            cx - radius - wobble,
            cy - radius * 0.9f,
            cx + radius + wobble,
            cy + radius * 1.1f,
            Path.Direction.CW
        )

        canvas.drawPath(path, bodyPaint)
    }

    private fun drawEars(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val earRadius = radius * 0.2f
        val earOffset = radius * 0.7f

        canvas.save()
        // Left ear — uses smoothly interpolated earAngle
        canvas.save()
        canvas.rotate(earAngle, cx - earOffset, cy - radius * 0.6f)
        canvas.drawCircle(cx - earOffset, cy - radius * 0.8f, earRadius, bodyPaint)
        canvas.restore()

        // Right ear
        canvas.save()
        canvas.rotate(-earAngle, cx + earOffset, cy - radius * 0.6f)
        canvas.drawCircle(cx + earOffset, cy - radius * 0.8f, earRadius, bodyPaint)
        canvas.restore()
        canvas.restore()
    }

    private fun drawEyes(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val eyeRadius = radius * 0.18f
        val eyeSpacing = radius * 0.35f
        val eyeY = cy - radius * 0.15f

        // Combine interpolated state closure with blink
        val effectiveClosure = maxOf(eyeClosure, blinkProgress)

        // Draw eye whites
        val eyeScaleY = 1f - effectiveClosure * 0.9f

        canvas.save()
        canvas.scale(1f, eyeScaleY, cx - eyeSpacing, eyeY)
        canvas.drawCircle(cx - eyeSpacing, eyeY, eyeRadius, eyePaint)
        canvas.restore()

        canvas.save()
        canvas.scale(1f, eyeScaleY, cx + eyeSpacing, eyeY)
        canvas.drawCircle(cx + eyeSpacing, eyeY, eyeRadius, eyePaint)
        canvas.restore()

        // Draw pupils if eyes are open enough — uses smoothly interpolated offsets
        if (effectiveClosure < 0.5f) {
            val pupilRadius = eyeRadius * 0.5f

            canvas.drawCircle(
                cx - eyeSpacing + pupilOffsetX,
                eyeY + pupilOffsetY,
                pupilRadius,
                pupilPaint
            )
            canvas.drawCircle(
                cx + eyeSpacing + pupilOffsetX,
                eyeY + pupilOffsetY,
                pupilRadius,
                pupilPaint
            )
        }
    }

    private fun drawMouth(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val mouthY = cy + radius * 0.35f
        val mouthWidth = radius * 0.4f

        val path = Path()
        when (currentState) {
            CreatureState.SPEAKING -> {
                // Open mouth animation
                val openAmount = sin(animationProgress * Math.PI.toFloat() * 3) * 0.5f + 0.5f
                path.addOval(
                    cx - mouthWidth * 0.5f,
                    mouthY - radius * 0.1f * openAmount,
                    cx + mouthWidth * 0.5f,
                    mouthY + radius * 0.2f * openAmount,
                    Path.Direction.CW
                )
                canvas.drawPath(path, Paint(pupilPaint).apply { style = Paint.Style.FILL })
            }
            CreatureState.OFFLINE -> {
                // Sad frown
                path.moveTo(cx - mouthWidth, mouthY + 10f)
                path.quadTo(cx, mouthY - 10f, cx + mouthWidth, mouthY + 10f)
                canvas.drawPath(path, mouthPaint)
            }
            else -> {
                // Happy smile
                path.moveTo(cx - mouthWidth, mouthY)
                path.quadTo(cx, mouthY + 20f, cx + mouthWidth, mouthY)
                canvas.drawPath(path, mouthPaint)
            }
        }
    }

    private fun drawThinkingBubbles(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val bubbleX = cx + radius * 1.2f
        val bubbleY = cy - radius * 0.5f

        // Three bubbles of increasing size
        val sizes = floatArrayOf(6f, 10f, 16f)
        val offsets = floatArrayOf(0f, 20f, 45f)

        for (i in sizes.indices) {
            val alpha = ((thinkingBubbleProgress + i * 0.3f) % 1f)
            bubblePaint.alpha = (180 * (1f - alpha * 0.5f)).toInt()
            canvas.drawCircle(
                bubbleX + offsets[i] * cos(alpha * Math.PI.toFloat() * 0.2f),
                bubbleY - offsets[i] - alpha * 10f,
                sizes[i],
                bubblePaint
            )
        }
    }

    private fun drawZzz(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val startX = cx + radius * 0.8f
        val startY = cy - radius * 0.3f

        zzzPaint.alpha = 255
        canvas.drawText("z", startX, startY - animationProgress * 20f, zzzPaint)

        zzzPaint.alpha = 180
        zzzPaint.textSize = 40f
        canvas.drawText("z", startX + 15f, startY - 25f - animationProgress * 20f, zzzPaint)

        zzzPaint.alpha = 120
        zzzPaint.textSize = 48f
        canvas.drawText("Z", startX + 35f, startY - 55f - animationProgress * 20f, zzzPaint)

        zzzPaint.textSize = 32f  // Reset
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        breathingAnimator.cancel()
        blinkAnimator.cancel()
        thinkingAnimator.cancel()
        mainAnimator.cancel()
        stateTransitionAnimator?.cancel()
    }
}
