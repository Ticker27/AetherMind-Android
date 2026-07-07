package com.aether.renderer

enum class RuntimeBudgetMode {
    NORMAL,
    WATCH,
    THROTTLE,
    CRITICAL
}

data class RuntimeBudgetState(
    val frameCount: Long = 0L,
    val budgetMode: RuntimeBudgetMode = RuntimeBudgetMode.NORMAL,
    val avgConfidence: Float = 0.0f,
    val avgRisk: Float = 0.0f,
    val riskPressure: Float = 0.0f,
    val throttleRecommended: Boolean = false,
    val reason: String = "runtime_budget_ready"
) {
    val badge: String
        get() = when (budgetMode) {
            RuntimeBudgetMode.NORMAL -> "BUDGET_NORMAL"
            RuntimeBudgetMode.WATCH -> "BUDGET_WATCH"
            RuntimeBudgetMode.THROTTLE -> "BUDGET_THROTTLE"
            RuntimeBudgetMode.CRITICAL -> "BUDGET_CRITICAL"
        }

    val compactLine: String
        get() = "$badge frames=$frameCount c=${avgConfidence.format2()} r=${avgRisk.format2()}"
}

object RuntimeBudget {
    private var frames = 0L
    private var avgConfidence = 0.0f
    private var avgRisk = 0.0f

    fun record(telemetry: RuntimeTelemetry?, decision: RuntimeDecision, security: SecurityLockState): RuntimeBudgetState {
        if (telemetry != null) frames += 1L
        val nextConfidence = telemetry?.confidenceBias ?: decision.confidence
        val nextRisk = decision.risk
        avgConfidence = smooth(avgConfidence, nextConfidence)
        avgRisk = smooth(avgRisk, nextRisk)
        val pressure = (avgRisk + decision.volatility * 0.35f + decision.uncertainty * 0.20f).coerceIn(0.0f, 1.0f)
        val mode = when {
            security.mode == SecurityLockMode.HARD_LOCK -> RuntimeBudgetMode.CRITICAL
            pressure >= 0.62f -> RuntimeBudgetMode.THROTTLE
            pressure >= 0.38f -> RuntimeBudgetMode.WATCH
            else -> RuntimeBudgetMode.NORMAL
        }
        return RuntimeBudgetState(
            frameCount = frames,
            budgetMode = mode,
            avgConfidence = avgConfidence,
            avgRisk = avgRisk,
            riskPressure = pressure,
            throttleRecommended = mode == RuntimeBudgetMode.THROTTLE || mode == RuntimeBudgetMode.CRITICAL,
            reason = when (mode) {
                RuntimeBudgetMode.NORMAL -> "within_runtime_budget"
                RuntimeBudgetMode.WATCH -> "risk_pressure_watch"
                RuntimeBudgetMode.THROTTLE -> "risk_pressure_throttle"
                RuntimeBudgetMode.CRITICAL -> "security_or_budget_critical"
            }
        )
    }

    private fun smooth(old: Float, next: Float): Float {
        val base = if (old <= 0f) next else old
        return (base * 0.85f + next * 0.15f).coerceIn(0.0f, 1.0f)
    }
}

private fun Float.format2(): String = String.format(java.util.Locale.US, "%.2f", this)
