package com.aether.renderer

object QaManifest {
    const val label: String = "AT90_95_QA_LOCK"
    const val phase: String = "9.0-9.5"
    const val apiStatus: String = "FROZEN_PHASE9"
    const val telemetryOnly: Boolean = true
    const val autoControlEnabled: Boolean = false
    const val rawUiTextExported: Boolean = false
    const val qaScope: String = "STATIC_REGRESSION_SCENARIO_BUILD_MATRIX_CRASH_DIAGNOSTICS"

    fun summary(): String = "$label $phase $apiStatus qa=$qaScope"
}
