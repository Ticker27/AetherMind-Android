package com.aether.renderer

data class MaintenanceModeState(
    val enabled: Boolean = false,
    val limitedTelemetry: Boolean = true,
    val status: String = "NORMAL",
    val reason: String = "production_ready"
) {
    val compactLine: String
        get() = "$status limitedTelemetry=$limitedTelemetry"
}

object MaintenanceMode {
    fun current(): MaintenanceModeState = MaintenanceModeState()
}
