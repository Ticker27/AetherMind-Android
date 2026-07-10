package com.aether.renderer

data class ReasoningProbeTelemetry(
    val label: String = ReasoningStabilityLock.LABEL,
    val auditPassed: Boolean,
    val compactReport: String,
    val scenarioLines: List<String>,
    val failedFindings: List<String>,
    val telemetryOnly: Boolean = true,
    val autoControlEnabled: Boolean = false,
    val actionExecutionEnabled: Boolean = false,
    val skillLayerEnabled: Boolean = false
) {
    val compactLine: String
        get() = "$label pass=$auditPassed scenarios=${scenarioLines.size} telemetryOnly=$telemetryOnly auto=$autoControlEnabled"
}

object ReasoningScenarioProbe {
    fun runDefaultTelemetry(): ReasoningProbeTelemetry {
        return toTelemetry(PlannerScenarioSuite.runDefault())
    }

    fun runTelemetry(scenarios: List<PlannerScenario>): ReasoningProbeTelemetry {
        return toTelemetry(PlannerScenarioSuite.run(scenarios))
    }

    private fun toTelemetry(audit: StrategicReasoningAudit): ReasoningProbeTelemetry {
        return ReasoningProbeTelemetry(
            auditPassed = audit.passed,
            compactReport = ReasoningEvidenceFormatter.formatSuiteReport(audit),
            scenarioLines = audit.results.map { ReasoningEvidenceFormatter.formatScenarioResult(it) },
            failedFindings = audit.failedFindings.map { it.compactLine },
            telemetryOnly = ReasoningStabilityLock.TELEMETRY_ONLY,
            autoControlEnabled = ReasoningStabilityLock.AUTO_CONTROL_ENABLED,
            actionExecutionEnabled = ReasoningStabilityLock.ACTION_EXECUTION_ENABLED,
            skillLayerEnabled = ReasoningStabilityLock.SKILL_LAYER_ENABLED
        )
    }
}
