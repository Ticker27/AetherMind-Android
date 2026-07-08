package com.aether.renderer

enum class PlannerScenarioOutcome {
    PASS,
    FAIL
}

data class PlannerScenarioResult(
    val scenario: PlannerScenario,
    val state: PhysicsExperienceState,
    val intent: StrategicIntentState,
    val candidates: List<PlanCandidate>,
    val evaluation: PlanEvaluationState,
    val explanation: PlanExplanationState,
    val reasoning: StrategicReasoningState,
    val snapshot: StrategicEvidenceSnapshot,
    val intentBand: IntentReadinessBand,
    val outcome: PlannerScenarioOutcome,
    val findings: List<StrategicAuditFinding>
) {
    val compactLine: String
        get() = "${scenario.id}:${outcome.name} ${snapshot.compactLine} band=${intentBand.name}"
}
