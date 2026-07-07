package com.aether.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class AetherTrajectorySurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private val running = AtomicBoolean(false)
    private var renderThread: RenderThread? = null

    init {
        setZOrderOnTop(false)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        holder.addCallback(this)

        isClickable = false
        isFocusable = false
        isFocusableInTouchMode = false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (running.compareAndSet(false, true)) {
            renderThread = RenderThread(holder, running).also { thread ->
                thread.start()
            }
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running.set(false)
        renderThread?.joinSafely()
        renderThread = null
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        return false
    }

    private class RenderThread(
        private val holder: SurfaceHolder,
        private val running: AtomicBoolean
    ) : Thread("AetherTrajectoryRenderThread") {

        private companion object {
            private const val FRAME_NANOS = 16_666_667L
            private const val OFFSET_FLAGS = 4
            private const val OFFSET_CUE_X = 8
            private const val OFFSET_CUE_Y = 12
            private const val OFFSET_TARGET_X = 16
            private const val OFFSET_TARGET_Y = 20
            private const val OFFSET_ANGLE_OFFSET = 24
            private const val OFFSET_POWER_SCALE = 28
            private const val OFFSET_VELOCITY_SCALE = 32
            private const val OFFSET_ERROR_MARGIN = 36
            private const val OFFSET_CONFIDENCE_BIAS = 40
            private const val OFFSET_RISK_BIAS = 44
        }

        private val stateBuffer: ByteBuffer =
            NativeTrajectoryBridge.allocateStateBuffer()

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 80, 220, 255)
            strokeWidth = 5.0f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(105, 80, 220, 255)
            strokeWidth = 13.0f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val tremorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(95, 255, 255, 255)
            strokeWidth = 2.0f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 255, 255, 255)
            style = Paint.Style.FILL
        }

        private val riskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 255, 180, 80)
            style = Paint.Style.STROKE
            strokeWidth = 3.0f
        }

        override fun run() {
            var nextFrame = System.nanoTime()

            while (running.get()) {
                val now = System.nanoTime()

                if (now < nextFrame) {
                    val sleepMillis = (nextFrame - now) / 1_000_000L

                    if (sleepMillis > 0L) {
                        sleep(sleepMillis)
                    } else {
                        yield()
                    }

                    continue
                }

                drawFrame()
                nextFrame += FRAME_NANOS

                if (System.nanoTime() - nextFrame > FRAME_NANOS * 4L) {
                    nextFrame = System.nanoTime()
                }
            }
        }

        private fun drawFrame() {
            val canvas = try {
                holder.lockCanvas()
            } catch (_: Throwable) {
                null
            } ?: return

            try {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                if (!NativeTrajectoryBridge.copyLatestState(stateBuffer)) {
                    return
                }

                drawTrajectory(canvas, stateBuffer)
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun drawTrajectory(
            canvas: Canvas,
            state: ByteBuffer
        ) {
            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()

            val cueX = clamp01(state.getFloat(OFFSET_CUE_X)) * width
            val cueY = clamp01(state.getFloat(OFFSET_CUE_Y)) * height

            val targetX = clamp01(state.getFloat(OFFSET_TARGET_X)) * width
            val targetY = clamp01(state.getFloat(OFFSET_TARGET_Y)) * height

            val dx = targetX - cueX
            val dy = targetY - cueY
            val baseLength = hypot(dx, dy).coerceAtLeast(1.0f)

            val dirX = dx / baseLength
            val dirY = dy / baseLength

            val angle = state.getFloat(OFFSET_ANGLE_OFFSET)
            val rotatedX = dirX * cos(angle) - dirY * sin(angle)
            val rotatedY = dirX * sin(angle) + dirY * cos(angle)

            val power = state.getFloat(OFFSET_POWER_SCALE).coerceIn(0.2f, 1.6f)
            val velocity = state.getFloat(OFFSET_VELOCITY_SCALE).coerceIn(0.0f, 2.0f)
            val error = state.getFloat(OFFSET_ERROR_MARGIN).coerceIn(0.0f, 1.0f)
            val confidence = state.getFloat(OFFSET_CONFIDENCE_BIAS).coerceIn(-1.0f, 1.0f)
            val risk = state.getFloat(OFFSET_RISK_BIAS).coerceIn(-1.0f, 1.0f)
            val flags = state.getInt(OFFSET_FLAGS)

            val extension = baseLength * (0.65f + power * 0.65f + velocity * 0.20f)
            val endX = cueX + rotatedX * extension
            val endY = cueY + rotatedY * extension

            ghostPaint.strokeWidth = 10.0f + error * 18.0f
            ghostPaint.alpha = (80.0f + confidence.coerceAtLeast(0.0f) * 75.0f).toInt()
            linePaint.alpha = (150.0f + confidence.coerceAtLeast(0.0f) * 70.0f).toInt()

            canvas.drawLine(cueX, cueY, endX, endY, ghostPaint)
            canvas.drawLine(cueX, cueY, endX, endY, linePaint)

            if (error > 0.015f) {
                val normalX = -rotatedY
                val normalY = rotatedX
                val spread = (8.0f + extension * error * 0.18f).coerceAtMost(72.0f)

                canvas.drawLine(
                    cueX + normalX * spread,
                    cueY + normalY * spread,
                    endX + normalX * spread,
                    endY + normalY * spread,
                    tremorPaint
                )

                canvas.drawLine(
                    cueX - normalX * spread,
                    cueY - normalY * spread,
                    endX - normalX * spread,
                    endY - normalY * spread,
                    tremorPaint
                )
            }

            if (risk > 0.0f || (flags and 0x00000002) != 0) {
                val riskRadius = 12.0f + risk.coerceAtLeast(0.0f) * 30.0f
                canvas.drawCircle(targetX, targetY, riskRadius, riskPaint)
            }

            canvas.drawCircle(cueX, cueY, 7.0f, pointPaint)
            canvas.drawCircle(targetX, targetY, 7.0f, pointPaint)
        }

        private fun clamp01(value: Float): Float {
            return when {
                value < 0.0f -> 0.0f
                value > 1.0f -> 1.0f
                else -> value
            }
        }

        private fun joinSafely() {
            try {
                join(500L)
            } catch (_: InterruptedException) {
                interrupt()
            }
        }
    }
}
