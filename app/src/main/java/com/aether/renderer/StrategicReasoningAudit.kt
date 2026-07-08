package com.aether.renderer

data class StrategicReasoningAudit(
    val label: String = ReasoningStabilityLock.LABEL,
    val phase: String = ReasoningStabilityLock.PHASE,
    val scenarioCount: Int = 0,
    val results: List<PlannerScenarioResult> = emptyList(),
    val intentTransitionTrace: IntentTransitionTrace = IntentTransitionTrace(),
    val distinctBestPlanIds: Set<String> = emptySet(),
    val findings: List<StrategicAuditFinding> = emptyList(),
    val passed: Boolean = false,
    val reason: String = "audit_waiting"
) {
    val failedFindings: List<StrategicAuditFinding>
        get() = findings.filter { !it.passed }

    val compactLine: String
        get() = "$label scenarios=$scenarioCount bestPlans=${distinctBestPlanIds.size} transitions=${intentTransitionTrace.transitionCount} pass=$passed"
}
