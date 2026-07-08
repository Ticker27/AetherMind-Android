package com.aether.renderer

import android.content.Context
import android.graphics.Rect

data class NormalizedBounds(
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 0f,
    val h: Float = 0f
) {
    val centerX: Float get() = (x + w / 2f).coerceIn(0f, 1f)
    val centerY: Float get() = (y + h / 2f).coerceIn(0f, 1f)
    val area: Float get() = (w * h).coerceIn(0f, 1f)
    val zone: String get() = when {
        centerY < 0.18f -> "TOP"
        centerY > 0.82f -> "BOTTOM"
        centerX < 0.25f -> "LEFT"
        centerX > 0.75f -> "RIGHT"
        else -> "CENTER"
    }
}

data class ScreenWorld(
    val widthPx: Int = 1,
    val heightPx: Int = 1,
    val density: Float = 1.0f,
    val densityDpi: Int = 160,
    val orientation: Int = 0,
    val usableTopNorm: Float = 0f,
    val usableBottomNorm: Float = 1f
) {
    val aspectRatio: Float get() = widthPx.toFloat() / heightPx.coerceAtLeast(1).toFloat()
    val densityBucket: String get() = when {
        densityDpi >= 560 -> "xxxhdpi"
        densityDpi >= 420 -> "xxhdpi"
        densityDpi >= 280 -> "xhdpi"
        densityDpi >= 200 -> "hdpi"
        else -> "mdpi"
    }

    fun normalize(bounds: Rect): NormalizedBounds {
        val w = widthPx.coerceAtLeast(1).toFloat()
        val h = heightPx.coerceAtLeast(1).toFloat()
        return NormalizedBounds(
            x = (bounds.left / w).coerceIn(0f, 1f),
            y = (bounds.top / h).coerceIn(0f, 1f),
            w = (bounds.width() / w).coerceIn(0f, 1f),
            h = (bounds.height() / h).coerceIn(0f, 1f)
        )
    }

    companion object {
        fun from(context: Context): ScreenWorld {
            val metrics = context.resources.displayMetrics
            val config = context.resources.configuration
            return ScreenWorld(
                widthPx = metrics.widthPixels.coerceAtLeast(1),
                heightPx = metrics.heightPixels.coerceAtLeast(1),
                density = metrics.density,
                densityDpi = metrics.densityDpi,
                orientation = config.orientation
            )
        }
    }
}
