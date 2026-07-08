package com.aether.renderer

object PlannerScenarioSuite {
    fun runDefault(): StrategicReasoningAudit = run(PlannerScenario.defaultMatrix())

    fun run(scenarios: List<PlannerScenario>): StrategicReasoningAudit {
        val results = scenarios.map { runScenario(it) }
        val trace = IntentTransitionTracer.trace(results)
        val suiteFindings = ArrayList<StrategicAuditFinding>()
        suiteFindings += IntentTransitionStability.audit(trace)
        suiteFindings += ReasoningStabilityLock.audit(results, trace)
        return StrategicReasoningAudit(
            label = ReasoningStabilityLock.LABEL,
            phase = ReasoningStabilityLock.PHASE,
            scenarioCount = results.size,
            results = results,
            intentTransitionTrace = trace,
            distinctBestPlanIds = results.map { it.evaluation.best.candidate.id }.toSet(),
            findings = suiteFindings + results.flatMap { it.findings },
            passed = suiteFindings.all { it.passed } && results.all { it.outcome == PlannerScenarioOutcome.PASS },
            reason = "planner_scenario_suite_completed"
        )
    }

    fun runScenario(scenario: PlannerScenario): PlannerScenarioResult {
        val state = scenario.toPhysicsState()
        val intent = StrategicIntentEngine.decide(state)
        val candidates = PlannerCandidateGenerator.generate(state, intent)
        val evaluation = StrategicCostModel.evaluate(state, intent, candidates)
        val explanation = PlanExplanation.build(state, intent, evaluation)
        val reasoning = StrategicReasoningState(
            physicsState = state,
            intent = intent,
            evaluation = evaluation,
            explanation = explanation,
            skill = evaluation.best.candidate.skill,
            telemetryOnly = true,
            autoControlEnabled = false,
            apiStatus = ReasoningStabilityLock.API_STATUS
        )
        val snapshot = StrategicReasoningEvidence.capture(scenario.id, reasoning)
        val rule = IntentTransitionRules.classify(state)
        val findings = ArrayList<StrategicAuditFinding>()
        findings += StrategicReasoningEvidence.validate(snapshot)
        findings += ReasoningStabilityLock.auditScenario(scenario, reasoning, rule)
        val passed = findings.all { it.passed }
        return PlannerScenarioResult(
            scenario = scenario,
            state = state,
            intent = intent,
            candidates = candidates,
            evaluation = evaluation,
            explanation = explanation,
            reasoning = reasoning,
            snapshot = snapshot,
            intentBand = rule.band,
            outcome = if (passed) PlannerScenarioOutcome.PASS else PlannerScenarioOutcome.FAIL,
            findings = findings
        )
    }
}
