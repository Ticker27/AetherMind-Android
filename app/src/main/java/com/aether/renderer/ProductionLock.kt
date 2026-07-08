package com.aether.renderer

data class ProductionLockState(
    val productionLocked: Boolean = true,
    val apiStatus: String = "FINAL_STABILITY_LOCK",
    val telemetryOnly: Boolean = true,
    val autoControlEnabled: Boolean = false,
    val rawUiTextExported: Boolean = false,
    val releaseLabel: String = "AT131_140_FINAL_STABILITY_LOCK"
) {
    val badge: String
        get() = if (productionLocked) "PROD_LOCK_${FinalManifest.LABEL}" else "PROD_OPEN"

    val compactLine: String
        get() = "$badge telemetry=$telemetryOnly auto=$autoControlEnabled raw=$rawUiTextExported"
}

object ProductionLock {
    const val LABEL: String = "AT131_140_FINAL_STABILITY_LOCK"
    const val API_STATUS: String = "FINAL_STABILITY_LOCK"
    const val AUTO_CONTROL_ENABLED: Boolean = false
    const val TELEMETRY_ONLY: Boolean = true
    const val RAW_UI_TEXT_EXPORTED: Boolean = false

    fun current(): ProductionLockState = ProductionLockState()
}
