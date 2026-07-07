package com.aether.renderer

data class UserControlPanelState(
    val resetLearningAvailable: Boolean = true,
    val resetPrivacyAvailable: Boolean = true,
    val exportSanitizedAvailable: Boolean = true,
    val freezeProfileAvailable: Boolean = true,
    val lastAction: String = "none",
    val actionCount: Int = 0,
    val panelBadge: String = "CONTROL_READY"
) {
    val compactLine: String
        get() = "$panelBadge action=$lastAction n=$actionCount"
}

object UserControlPanel {
    private var lastAction = "none"
    private var actionCount = 0

    fun current(security: SecurityLockState, retention: RetentionResetState): UserControlPanelState {
        return UserControlPanelState(
            resetLearningAvailable = true,
            resetPrivacyAvailable = retention.resetAvailable,
            exportSanitizedAvailable = security.autoControlEnabled == false,
            freezeProfileAvailable = true,
            lastAction = lastAction,
            actionCount = actionCount,
            panelBadge = when {
                security.mode == SecurityLockMode.HARD_LOCK -> "CONTROL_REVIEW"
                !retention.sessionOnly -> "CONTROL_RETENTION_CHECK"
                else -> "CONTROL_READY"
            }
        )
    }

    fun record(action: String): UserControlPanelState {
        lastAction = action
        actionCount += 1
        return current(AetherRuntimeBus.securityLockFlow.value, AetherRuntimeBus.retentionResetFlow.value)
    }
}
