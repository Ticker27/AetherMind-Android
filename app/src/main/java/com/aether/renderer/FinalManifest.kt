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
    const val LABEL = "AT131_140_FINAL_STABILITY_LOCK"
    const val PHASE = "12.1-14.0"
    const val APP_VERSION = "14.0.0"
    const val VERSION_CODE = 1400
    const val CORE_VERSION = "7.1"
    const val PACKAGE_NAME = "com.aether.renderer"
    const val BASELINE_FROM = "AT126_129_REASONING_EVIDENCE_LOCK_FULL"
    const val API_STATUS = "FINAL_STABILITY_LOCK"

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
        "StrategicReasoningCore",
        "StrategicEvidenceSnapshot",
        "StrategicReasoningEvidence",
        "ReasoningEvidenceFormatter",
        "ReasoningScenarioProbe",
        "IntentTransitionRule",
        "IntentTransitionTrace",
        "IntentTransitionStability",
        "PlannerScenario",
        "PlannerScenarioResult",
        "PlannerScenarioSuite",
        "ReasoningSafetyInvariant",
        "StrategicReasoningAudit",
        "ReasoningStabilityLock",
        "SkillDescriptor",
        "SkillRegistry",
        "SkillProposal",
        "SkillProposalEngine",
        "SkillSafetyGate",
        "SkillConfidenceScore",
        "SkillExecutionBlocker",
        "SkillAuditTrail",
        "SkillSystemSnapshot",
        "SkillSystemFormatter",
        "FinalStabilityLock",
        "ReleaseGateSnapshot",
        "RuntimeIdentityVerifier",
        "ReleaseGateFormatter"
    )

    fun current(): FinalManifestState = FinalManifestState()

    fun summary(): String = "$LABEL v$APP_VERSION code=$VERSION_CODE core=$CORE_VERSION modules=${MODULES.size}"
}
