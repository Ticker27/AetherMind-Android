package com.aether.renderer

import android.content.Context
import android.os.Build

data class DeviceProfile(
    val manufacturer: String = Build.MANUFACTURER.orEmpty(),
    val model: String = Build.MODEL.orEmpty(),
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val screenWidthPx: Int = 0,
    val screenHeightPx: Int = 0,
    val density: Float = 1.0f,
    val densityDpi: Int = 160,
    val fontScale: Float = 1.0f,
    val orientation: Int = 0
) {
    val deviceKey: String
        get() = "${manufacturer.lowercase()}_${model.lowercase()}_${densityDpi}"

    companion object {
        fun from(context: Context): DeviceProfile {
            val metrics = context.resources.displayMetrics
            val config = context.resources.configuration
            return DeviceProfile(
                screenWidthPx = metrics.widthPixels,
                screenHeightPx = metrics.heightPixels,
                density = metrics.density,
                densityDpi = metrics.densityDpi,
                fontScale = config.fontScale,
                orientation = config.orientation
            )
        }
    }
}
