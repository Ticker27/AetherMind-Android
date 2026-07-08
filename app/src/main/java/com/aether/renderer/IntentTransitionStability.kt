package com.aether.renderer

object IntentTransitionStability {
    fun audit(trace: IntentTransitionTrace): StrategicAuditFinding {
        val failures = ArrayList<String>(5)
        if (trace.steps.isEmpty()) failures += "empty_trace"
        if (trace.distinctBands.size < 3) failures += "missing_ready_caution_safety_band"
        if (trace.distinctIntents.size < 3) failures += "intent_collapsed"
        if (trace.transitionCount < 2) failures += "not_enough_transitions"
        if (trace.steps.any { !it.matched }) failures += "intent_rule_mismatch"
        return StrategicAuditFinding(
            id = "AT127_INTENT_TRANSITION_TRACE",
            passed = failures.isEmpty(),
            severity = if (failures.isEmpty()) AuditSeverity.INFO else AuditSeverity.ERROR,
            message = if (failures.isEmpty()) trace.reason else failures.joinToString(","),
            evidence = trace.compactLine
        )
    }
}
