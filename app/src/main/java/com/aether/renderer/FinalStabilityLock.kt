package com.aether.renderer

object FinalStabilityLock {
    const val LABEL = "AT131_140_FINAL_STABILITY_LOCK"
    const val PHASE = "12.1-14.0"
    const val APP_VERSION = "14.0.0"
    const val VERSION_CODE = 1400
    const val CORE_VERSION = "7.1"
    const val PACKAGE_NAME = "com.aether.renderer"
    const val BASELINE_FROM = "AT126_129_REASONING_EVIDENCE_LOCK_FULL"
    const val API_STATUS = "FINAL_STABILITY_LOCK"

    const val TELEMETRY_ONLY = true
    const val AUTO_CONTROL_ENABLED = false
    const val EXECUTION_ENABLED = false
    const val SKILL_MODE = "PROPOSE_ONLY"
    const val RAW_UI_TEXT_STORED = false
    const val RAW_UI_TEXT_EXPORTED = false
    const val EXTERNAL_EXPORT_ALLOWED = false

    fun snapshot(
        contextLabel: String = "UNKNOWN",
        confidence: Float = 0.60f,
        risk: Float = 0.10f,
        trustScore: Float = 0.80f
    ): SkillSystemSnapshot {
        val proposal = SkillProposalEngine.propose(contextLabel, confidence, risk, trustScore)
        return SkillSystemSnapshot(proposal = proposal)
    }

    fun lockLine(): String {
        return "$LABEL v$APP_VERSION mode=$SKILL_MODE exec=$EXECUTION_ENABLED auto=$AUTO_CONTROL_ENABLED"
    }
}
