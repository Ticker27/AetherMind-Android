package com.aether.renderer

data class RuntimeDecision(
    val sceneLabel: String = "IDLE",
    val intentLabel: String = "POSITIONING",
    val confidence: Float = 0.0f,
    val risk: Float = 0.0f,
    val hudMode: HudMode = HudMode.COMPACT,
    val telemetryOnly: Boolean = true,
    val reason: String = "waiting",
    val safetyLevel: SafetyLevel = SafetyLevel.CAUTION,
    val safetyBadge: String = "CAUTION",
    val gateReason: String = "waiting_scene",
    val stabilityScore: Float = 0.0f,
    val stabilityLabel: String = "LEARNING",
    val sceneChanged: Boolean = false,
    val memorySamples: Int = 0,
    val policyMode: PolicyMode = PolicyMode.OBSERVE,
    val policyReason: String = "observe_policy",
    val actionIntent: ActionIntent = ActionIntent.POSITIONING,
    val actionDetail: String = "positioning",
    val sessionTrend: String = "NEW",
    val sessionSamples: Int = 0,
    val sessionAvgConfidence: Float = 0.0f,
    val sessionAvgRisk: Float = 0.0f,
    val screenType: ScreenType = ScreenType.SCREEN_UNKNOWN,
    val screenConfidence: Float = 0.0f,
    val screenReason: String = "unknown",
    val anchorCount: Int = 0,
    val stableAnchorCount: Int = 0,
    val anchorStability: Float = 0.0f,
    val anchorLabel: String = "NO_ANCHOR",
    val contextLabel: String = "CTX_UNKNOWN",
    val contextConfidence: Float = 0.0f,
    val contextRisk: Float = 0.0f,
    val contextReason: String = "unknown",
    val behaviorLabel: String = "BEHAVIOR_LEARNING",
    val behaviorStability: Float = 0.0f,
    val behaviorSamples: Int = 0,
    val behaviorRisk: Float = 0.0f,
    val policyV2State: PolicyV2State = PolicyV2State.OBSERVE,
    val policyV2Reason: String = "v2_observe_context",
    val learningLabel: String = "LEARNING_COLD",
    val learningReason: String = "cold_start",
    val learningTrust: Float = 0.0f,
    val learningSamples: Int = 0,
    val learningConfidenceDelta: Float = 0.0f,
    val learningRiskDelta: Float = 0.0f,
    val learningMemoryLabel: String = "MEMORY_COLD",
    val learningMemoryTrend: String = "MEMORY_NEW",
    val learningMemoryReason: String = "memory_cold_start",
    val learningMemorySamples: Int = 0,
    val learningMemoryTrustAvg: Float = 0.0f,
    val learningMemoryVolatility: Float = 0.0f,
    val trustBand: TrustBand = TrustBand.LOW,
    val trustScore: Float = 0.0f,
    val uncertainty: Float = 1.0f,
    val volatility: Float = 0.0f,
    val trustLabel: String = "TRUST_LOW",
    val trustReason: String = "trust_cold_start",
    val feedbackPolicyState: FeedbackPolicyState = FeedbackPolicyState.OBSERVE,
    val feedbackPolicyLabel: String = "FEEDBACK_OBSERVE",
    val feedbackPolicyReason: String = "feedback_observe",
    val feedbackSamples: Int = 0
) {
    val safeLabel: String
        get() = safetyBadge

    val compactLine: String
        get() = "${UiReadability.shortContext(contextLabel)}/${UiReadability.intentLabel(intentLabel)} conf=${confidence.format2()} risk=${risk.format2()} $safetyBadge $trustLabel"

    val fullLine: String
        get() = "${UiReadability.shortContext(contextLabel)} / ${policyV2State.name} / $safetyBadge / $sessionTrend / $trustLabel"
}

private fun Float.format2(): String = String.format(java.util.Locale.US, "%.2f", this)
