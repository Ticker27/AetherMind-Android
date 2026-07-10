package com.aether.renderer

data class StrategicEvidenceSnapshot(
    val label: String = ReasoningStabilityLock.LABEL,
    val phase: String = ReasoningStabilityLock.PHASE,
    val scenarioId: String = "runtime",
    val stateBadge: String = "STATE_UNKNOWN",
    val intentBadge: String = "INTENT_UNKNOWN",
    val intentType: StrategicIntentType = StrategicIntentType.OBSERVE,
    val goal: String = "observe_safely",
    val candidateCount: Int = 0,
    val rankedPlanIds: List<String> = emptyList(),
    val bestPlanId: String = "observe",
    val bestPlanLabel: String = "observe_more",
    val bestPlanScore: Float = 0.0f,
    val explanationSummary: String = "",
    val explanationWhy: String = "",
    val explanationSafety: String = "",
    val confidence: Float = 0.0f,
    val risk: Float = 0.0f,
    val uncertainty: Float = 1.0f,
    val volatility: Float = 0.0f,
    val telemetryOnly: Boolean = true,
    val autoControlEnabled: Boolean = false,
    val actionExecutionEnabled: Boolean = false,
    val skillLayerEnabled: Boolean = false,
    val reason: String = "evidence_snapshot"
) {
    val hasRankedPlans: Boolean
        get() = rankedPlanIds.isNotEmpty() && candidateCount >= rankedPlanIds.size

    val hasReasoningChain: Boolean
        get() = stateBadge.isNotBlank() && intentBadge.isNotBlank() && goal.isNotBlank() && explanationWhy.isNotBlank()

    val hasSafetyLock: Boolean
        get() = telemetryOnly && !autoControlEnabled && !actionExecutionEnabled && !skillLayerEnabled

    val compactLine: String
        get() = "${scenarioId}:${stateBadge}/${intentBadge} best=$bestPlanId score=${bestPlanScore.fmt2()} n=$candidateCount safe=$hasSafetyLock"
}
