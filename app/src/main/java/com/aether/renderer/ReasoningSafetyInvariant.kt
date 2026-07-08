package com.aether.renderer

data class StrategicAuditFinding(
    val id: String,
    val passed: Boolean,
    val severity: AuditSeverity,
    val message: String,
    val evidence: String = ""
) {
    val compactLine: String
        get() = "$id pass=$passed severity=${severity.name} msg=$message"
}

data class ReasoningSafetyInvariant(
    val id: String,
    val passed: Boolean,
    val reason: String,
    val evidence: String = ""
)

object ReasoningSafetyInvariants {
    fun evaluate(reasoning: StrategicReasoningState): List<ReasoningSafetyInvariant> {
        val plans = reasoning.evaluation.ranked.map { it.candidate }
        return listOf(
            ReasoningSafetyInvariant(
                id = "telemetry_only_true",
                passed = reasoning.telemetryOnly && reasoning.physicsState.telemetryOnly,
                reason = "reasoning_must_export_telemetry_only",
                evidence = reasoning.compactLine
            ),
            ReasoningSafetyInvariant(
                id = "auto_control_false",
                passed = !reasoning.autoControlEnabled && !reasoning.physicsState.autoControlEnabled,
                reason = "auto_control_must_remain_disabled",
                evidence = reasoning.compactLine
            ),
            ReasoningSafetyInvariant(
                id = "no_candidate_executes_now",
                passed = plans.all { !it.skill.canExecuteNow },
                reason = "planner_candidates_must_not_open_execution_path",
                evidence = plans.joinToString(",") { it.skill.compactLine }
            ),
            ReasoningSafetyInvariant(
                id = "forbidden_skill_not_best_action",
                passed = reasoning.evaluation.best.candidate.skill.permission != SkillPermission.FORBIDDEN || reasoning.evaluation.best.candidate.id == "no_action",
                reason = "forbidden_permission_can_only_surface_as_hold_no_action",
                evidence = reasoning.evaluation.best.compactLine
            ),
            ReasoningSafetyInvariant(
                id = "api_status_locked",
                passed = reasoning.apiStatus == ReasoningStabilityLock.API_STATUS,
                reason = "api_status_must_stay_strategic_telemetry_only",
                evidence = reasoning.apiStatus
            )
        )
    }
}
