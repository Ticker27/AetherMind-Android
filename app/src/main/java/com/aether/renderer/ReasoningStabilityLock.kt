package com.aether.renderer

object ReasoningStabilityLock {
    const val LABEL = "AT126_129_REASONING_EVIDENCE_LOCK"
    const val PHASE = "11.6-11.9"
    const val BASELINE_FROM = StrategicReasoningLock.LABEL
    const val API_STATUS = StrategicReasoningLock.API_STATUS
    const val TELEMETRY_ONLY = true
    const val AUTO_CONTROL_ENABLED = false
    const val ACTION_EXECUTION_ENABLED = false
    const val SKILL_LAYER_ENABLED = false
    const val PLANNER_EXECUTES_ACTIONS = false

    fun auditScenario(
        scenario: PlannerScenario,
        reasoning: StrategicReasoningState,
        rule: IntentTransitionRule = IntentTransitionRules.classify(reasoning.physicsState)
    ): StrategicAuditFinding {
        val failures = ArrayList<String>(8)
        val invariants = ReasoningSafetyInvariants.evaluate(reasoning)
        if (rule.band != scenario.expectedBand) failures += "band_mismatch"
        if (reasoning.intent.type != scenario.expectedIntent) failures += "scenario_expected_intent_mismatch"
        if (reasoning.intent.type != rule.expectedIntent) failures += "rule_expected_intent_mismatch"
        if (reasoning.evaluation.candidateCount <= 1 && scenario.kind != PlannerScenarioKind.LOCKED) failures += "candidate_count_not_diverse"
        if (reasoning.explanation.summary.isBlank() || reasoning.explanation.why.isBlank()) failures += "explanation_empty"
        if (invariants.any { !it.passed }) failures += invariants.filter { !it.passed }.joinToString(",") { it.id }
        return StrategicAuditFinding(
            id = "AT129_SCENARIO_${scenario.id}",
            passed = failures.isEmpty(),
            severity = if (failures.isEmpty()) AuditSeverity.INFO else AuditSeverity.ERROR,
            message = if (failures.isEmpty()) "scenario_reasoning_stable" else failures.joinToString(","),
            evidence = reasoning.compactLine
        )
    }

    fun audit(results: List<PlannerScenarioResult>, trace: IntentTransitionTrace): StrategicAuditFinding {
        val failures = ArrayList<String>(8)
        val bestPlans = results.map { it.evaluation.best.candidate.id }.toSet()
        val intents = results.map { it.intent.type }.toSet()
        if (results.size < 5) failures += "scenario_matrix_too_small"
        if (bestPlans.size < 2) failures += "best_plan_not_scenario_sensitive"
        if (intents.size < 3) failures += "intent_not_scenario_sensitive"
        if (!trace.passed) failures += "intent_trace_failed"
        if (results.any { it.reasoning.evaluation.candidateCount <= 0 }) failures += "empty_candidate_set"
        if (results.any { it.reasoning.skill.canExecuteNow }) failures += "execution_path_opened"
        if (results.any { !it.reasoning.telemetryOnly || it.reasoning.autoControlEnabled }) failures += "telemetry_lock_broken"
        if (results.any { it.explanation.summary.isBlank() || it.explanation.why.isBlank() || it.explanation.safety.isBlank() }) failures += "generic_or_empty_explanation"
        return StrategicAuditFinding(
            id = "AT129_REASONING_STABILITY_LOCK",
            passed = failures.isEmpty(),
            severity = if (failures.isEmpty()) AuditSeverity.INFO else AuditSeverity.ERROR,
            message = if (failures.isEmpty()) "reasoning_stability_locked" else failures.joinToString(","),
            evidence = "bestPlans=${bestPlans.joinToString(",")}; intents=${intents.joinToString(",")}; ${trace.compactLine}"
        )
    }
}
