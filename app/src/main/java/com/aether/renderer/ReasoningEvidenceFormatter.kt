package com.aether.renderer

object ReasoningEvidenceFormatter {
    fun formatSnapshot(snapshot: StrategicEvidenceSnapshot): String {
        return buildString {
            append(snapshot.label).append(' ')
            append(snapshot.phase).append(" | ")
            append(snapshot.compactLine).append(" | ")
            append("goal=").append(snapshot.goal).append(" | ")
            append("ranked=").append(snapshot.rankedPlanIds.joinToString(">"))
        }
    }

    fun formatScenarioResult(result: PlannerScenarioResult): String {
        return buildString {
            append(result.scenario.id).append(':')
            append(result.outcome.name).append(" | ")
            append(result.snapshot.compactLine).append(" | ")
            append("intentBand=").append(result.intentBand.name).append(" | ")
            append("findings=").append(result.findings.joinToString(";") { it.message })
        }
    }

    fun formatSuiteReport(report: StrategicReasoningAudit): String {
        return buildString {
            append(report.label).append(" pass=").append(report.passed).append(" | ")
            append("scenarios=").append(report.scenarioCount).append(" | ")
            append("intentTransitions=").append(report.intentTransitionTrace.transitionCount).append(" | ")
            append("bestPlans=").append(report.distinctBestPlanIds.joinToString(","))
        }
    }
}
