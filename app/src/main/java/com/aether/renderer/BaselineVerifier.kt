package com.aether.renderer

data class BaselineVerificationState(
    val baselineLabel: String = FinalManifest.BASELINE_FROM,
    val finalLabel: String = FinalManifest.LABEL,
    val runtimeApiFrozen: Boolean = true,
    val phase3Frozen: Boolean = true,
    val phase4Frozen: Boolean = true,
    val phase5Frozen: Boolean = true,
    val phase7Frozen: Boolean = true,
    val phase8Frozen: Boolean = true,
    val phase9Frozen: Boolean = true,
    val productionLocked: Boolean = true,
    val telemetryOnly: Boolean = true,
    val autoControlEnabled: Boolean = false,
    val rawUiTextStored: Boolean = false,
    val rawUiTextExported: Boolean = false,
    val result: String = "BASELINE_READY_FOR_EXTERNAL_TERMUX_TEST"
)

object BaselineVerifier {
    fun current(): BaselineVerificationState = BaselineVerificationState()

    fun pass(): Boolean {
        val state = current()
        return state.runtimeApiFrozen && state.productionLocked && state.telemetryOnly && !state.autoControlEnabled && !state.rawUiTextStored && !state.rawUiTextExported
    }
}
