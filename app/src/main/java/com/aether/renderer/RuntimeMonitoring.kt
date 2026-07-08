package com.aether.renderer

data class RuntimeMonitoringState(
    val active: Boolean = true,
    val frameCount: Long = 0L,
    val budgetBadge: String = "BUDGET_OK",
    val productionBadge: String = "PROD_LOCK_${FinalManifest.LABEL}",
    val health: String = "HEALTH_OK"
) {
    val compactLine: String
        get() = "$health frame=$frameCount $budgetBadge"
}

object RuntimeMonitoring {
    fun record(
        telemetry: RuntimeTelemetry?,
        frameCount: Long,
        budget: RuntimeBudgetState,
        production: ProductionLockState
    ): RuntimeMonitoringState {
        val health = when {
            !production.productionLocked -> "UNLOCKED"
            budget.throttleRecommended -> "THROTTLED"
            telemetry == null -> "WAITING"
            else -> "HEALTH_OK"
        }
        return RuntimeMonitoringState(
            active = true,
            frameCount = frameCount,
            budgetBadge = budget.badge,
            productionBadge = production.badge,
            health = health
        )
    }
}
