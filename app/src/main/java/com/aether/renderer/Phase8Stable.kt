package com.aether.renderer

data class Phase8StableState(
    val apiStatus: String = "FROZEN_PHASE8",
    val explainableHud: Boolean = true,
    val userControls: Boolean = true,
    val profileEditor: Boolean = true,
    val debugTimeline: Boolean = true,
    val feedbackSystem: Boolean = true,
    val telemetryOnly: Boolean = true,
    val autoControlEnabled: Boolean = false,
    val rawUiTextExported: Boolean = false,
    val badge: String = "PHASE8_STABLE"
) {
    val compactLine: String
        get() = "$badge telemetry=$telemetryOnly auto=$autoControlEnabled rawExport=$rawUiTextExported"
}

object Phase8Stable {
    const val LABEL = "AT80_85_UX_LOCK"
    const val API_STATUS = "FROZEN_PHASE8"
    const val TELEMETRY_ONLY = true
    const val AUTO_CONTROL_ENABLED = false

    fun current(): Phase8StableState = Phase8StableState()
}
