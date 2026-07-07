package com.aether.renderer

object StrategicReasoningEvidence {
    fun capture(scenarioId: String, reasoning: StrategicReasoningState): StrategicEvidenceSnapshot {
        val ranked = reasoning.evaluation.ranked.map { it.candidate.id }
        return StrategicEvidenceSnapshot(
            scenarioId = scenarioId,
            stateBadge = reasoning.physicsState.badge,
            intentBadge = reasoning.intent.badge,
            intentType = reasoning.intent.type,
            goal = reasoning.intent.goal,
            candidateCount = reasoning.evaluation.candidateCount,
            rankedPlanIds = ranked,
            bestPlanId = reasoning.evaluation.best.candidate.id,
            bestPlanLabel = reasoning.evaluation.best.candidate.label,
            bestPlanScore = reasoning.evaluation.best.score,
            explanationSummary = reasoning.explanation.summary,
            explanationWhy = reasoning.explanation.why,
            explanationSafety = reasoning.explanation.safety,
            confidence = reasoning.physicsState.confidence,
            risk = reasoning.physicsState.risk,
            uncertainty = reasoning.physicsState.uncertainty,
            volatility = reasoning.physicsState.volatility,
            telemetryOnly = reasoning.telemetryOnly && reasoning.physicsState.telemetryOnly,
            autoControlEnabled = reasoning.autoControlEnabled || reasoning.physicsState.autoControlEnabled,
            actionExecutionEnabled = ReasoningStabilityLock.ACTION_EXECUTION_ENABLED,
            skillLayerEnabled = ReasoningStabilityLock.SKILL_LAYER_ENABLED,
            reason = "captured_from_strategic_reasoning_state"
        )
    }

    fun validate(snapshot: StrategicEvidenceSnapshot): StrategicAuditFinding {
        val failures = ArrayList<String>(8)
        if (!snapshot.hasSafetyLock) failures += "safety_flags_not_locked"
        if (!snapshot.hasReasoningChain) failures += "reasoning_chain_incomplete"
        if (snapshot.candidateCount <= 0) failures += "no_candidates"
        if (snapshot.bestPlanId.isBlank()) failures += "missing_best_plan"
        if (snapshot.explanationSummary.isBlank()) failures += "missing_summary"
        if (!mentionsRequiredEvidence(snapshot)) failures += "explanation_missing_goal_risk_confidence"
        return StrategicAuditFinding(
            id = "AT126_EVIDENCE_${snapshot.scenarioId}",
            passed = failures.isEmpty(),
            severity = if (failures.isEmpty()) AuditSeverity.INFO else AuditSeverity.ERROR,
            message = if (failures.isEmpty()) "reasoning_evidence_complete" else failures.joinToString(","),
            evidence = snapshot.compactLine
        )
    }

    fun captureFromInputs(
        scenarioId: String,
        scene: SceneSnapshot,
        observer: ObserverSnapshot,
        decision: RuntimeDecision,
        trust: TrustModelState,
        privacy: PrivacyGuardState,
        security: SecurityLockState,
        budget: RuntimeBudgetState
    ): StrategicEvidenceSnapshot {
        return capture(
            scenarioId = scenarioId,
            reasoning = StrategicReasoningCore.evaluate(scene, observer, decision, trust, privacy, security, budget)
        )
    }

    private fun mentionsRequiredEvidence(snapshot: StrategicEvidenceSnapshot): Boolean {
        val text = listOf(
            snapshot.explanationSummary,
            snapshot.explanationWhy,
            snapshot.explanationSafety
        ).joinToString(" ").lowercase(java.util.Locale.US)
        return text.contains("intent=") && text.contains("confidence=") && text.contains("risk=")
    }
}
