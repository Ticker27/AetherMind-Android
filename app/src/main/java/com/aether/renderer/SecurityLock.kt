package com.aether.renderer

enum class SecurityLockMode {
    OPEN_OBSERVE,
    GUARDED,
    SOFT_LOCK,
    HARD_LOCK
}

data class SecurityLockState(
    val mode: SecurityLockMode = SecurityLockMode.OPEN_OBSERVE,
    val badge: String = "SECURITY_OPEN_OBSERVE",
    val telemetryOnly: Boolean = true,
    val autoControlEnabled: Boolean = false,
    val learningOverrideSafety: Boolean = false,
    val reason: String = "security_observe_only"
) {
    val compactLine: String
        get() = "$badge telemetry=$telemetryOnly auto=$autoControlEnabled"
}

object SecurityLock {
    fun evaluate(
        security: SecurityPrivacyState,
        boundaryV2: PermissionBoundaryV2State,
        retention: RetentionResetState
    ): SecurityLockState {
        val mode = when {
            boundaryV2.decision == BoundaryDecision.HARD_LOCK -> SecurityLockMode.HARD_LOCK
            security.privacy.privacyMode == PrivacyMode.LOCKED -> SecurityLockMode.HARD_LOCK
            boundaryV2.decision == BoundaryDecision.SOFT_LOCK -> SecurityLockMode.SOFT_LOCK
            security.privacy.privacyMode == PrivacyMode.SENSITIVE -> SecurityLockMode.GUARDED
            !retention.sessionOnly || retention.rawTextStored || retention.externalExportAllowed -> SecurityLockMode.HARD_LOCK
            else -> SecurityLockMode.OPEN_OBSERVE
        }
        val badge = when (mode) {
            SecurityLockMode.OPEN_OBSERVE -> "SECURITY_OPEN_OBSERVE"
            SecurityLockMode.GUARDED -> "SECURITY_GUARDED"
            SecurityLockMode.SOFT_LOCK -> "SECURITY_SOFT_LOCK"
            SecurityLockMode.HARD_LOCK -> "SECURITY_HARD_LOCK"
        }
        return SecurityLockState(
            mode = mode,
            badge = badge,
            telemetryOnly = true,
            autoControlEnabled = false,
            learningOverrideSafety = false,
            reason = when (mode) {
                SecurityLockMode.OPEN_OBSERVE -> "privacy_boundary_open_observe"
                SecurityLockMode.GUARDED -> "sensitive_scope_guarded"
                SecurityLockMode.SOFT_LOCK -> "boundary_v2_soft_lock"
                SecurityLockMode.HARD_LOCK -> "hard_security_lock"
            }
        )
    }
}
