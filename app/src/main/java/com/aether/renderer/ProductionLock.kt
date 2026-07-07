package com.aether.renderer

data class ProductionLockState(
    val productionLocked: Boolean = true,
    val apiStatus: String = "PRODUCTION_LOCKED",
    val telemetryOnly: Boolean = true,
    val autoControlEnabled: Boolean = false,
    val rawUiTextExported: Boolean = false,
    val releaseLabel: String = "AT100_105_RELEASE_LOCK"
) {
    val badge: String
        get() = if (productionLocked) "PROD_LOCK" else "PROD_OPEN"

    val compactLine: String
        get() = "$badge telemetry=$telemetryOnly auto=$autoControlEnabled raw=$rawUiTextExported"
}

object ProductionLock {
    const val LABEL: String = "AT100_105_RELEASE_LOCK"
    const val API_STATUS: String = "PRODUCTION_LOCKED"
    const val AUTO_CONTROL_ENABLED: Boolean = false
    const val TELEMETRY_ONLY: Boolean = true
    const val RAW_UI_TEXT_EXPORTED: Boolean = false

    fun current(): ProductionLockState = ProductionLockState()
}
