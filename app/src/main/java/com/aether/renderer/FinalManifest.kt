package com.aether.renderer

data class FinalManifestState(
    val label: String = FinalManifest.LABEL,
    val appVersion: String = FinalManifest.APP_VERSION,
    val versionCode: Int = FinalManifest.VERSION_CODE,
    val coreVersion: String = FinalManifest.CORE_VERSION,
    val packageName: String = FinalManifest.PACKAGE_NAME,
    val telemetryOnly: Boolean = true,
    val autoControlEnabled: Boolean = false,
    val rawUiTextExported: Boolean = false,
    val externalExportAllowed: Boolean = false,
    val productionLocked: Boolean = true,
    val termuxReady: Boolean = true,
    val modulesLocked: Int = FinalManifest.MODULES.size
)

object FinalManifest {
    const val LABEL = "AT120_125_STRATEGIC_REASONING_CORE"
    const val PHASE = "11.0-11.5"
    const val APP_VERSION = "11.5.0"
    const val VERSION_CODE = 1250
    const val CORE_VERSION = "7.1"
    const val PACKAGE_NAME = "com.aether.renderer"
    const val BASELINE_FROM = "AT113_115_UI_HARD_LOCK"
    const val API_STATUS = "FINAL_EXTERNAL_TEST_LOCK"

    val MODULES = listOf(
        "Foundation",
        "RuntimeState",
        "DecisionSafetyMemory",
        "WorldContextBehavior",
        "LearningTrustFeedback",
        "SecurityPrivacy",
        "PerformanceResourceControl",
        "UxExplainability",
        "QaDiagnostics",
        "ReleaseMaintenance",
        "TermuxExternalTestHarness",
        "UiReadabilityPatch",
        "HudPositionDensityPatch",
        "DashboardOverlayGate",
        "UiHardLock",
        "StrategicReasoningCore"
    )

    fun current(): FinalManifestState = FinalManifestState()

    fun summary(): String = "$LABEL v$APP_VERSION code=$VERSION_CODE core=$CORE_VERSION modules=${MODULES.size}"
}
