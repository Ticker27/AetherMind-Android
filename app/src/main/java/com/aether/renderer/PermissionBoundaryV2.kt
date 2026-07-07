package com.aether.renderer

enum class BoundaryDecision {
    ALLOW_OBSERVE,
    DOWNGRADE_SCOPE,
    SOFT_LOCK,
    HARD_LOCK
}

data class PermissionBoundaryV2State(
    val decision: BoundaryDecision = BoundaryDecision.ALLOW_OBSERVE,
    val badge: String = "BOUNDARY_V2_OBSERVE",
    val effectiveTelemetryOnly: Boolean = true,
    val unsafeScopeDowngraded: Boolean = false,
    val sensitiveAppLock: Boolean = false,
    val reason: String = "observe_only_boundary"
) {
    val compactLine: String
        get() = "$badge down=$unsafeScopeDowngraded lock=$sensitiveAppLock"
}

object PermissionBoundaryV2 {
    fun evaluate(
        privacy: PrivacyGuardState,
        boundary: PermissionBoundaryState,
        audit: AuditHardeningState
    ): PermissionBoundaryV2State {
        val decision = when {
            privacy.privacyMode == PrivacyMode.LOCKED -> BoundaryDecision.HARD_LOCK
            boundary.mode == PermissionBoundaryMode.LOCKED -> BoundaryDecision.HARD_LOCK
            audit.severity == AuditSeverity.CRITICAL -> BoundaryDecision.HARD_LOCK
            privacy.privacyMode == PrivacyMode.SENSITIVE -> BoundaryDecision.SOFT_LOCK
            boundary.mode == PermissionBoundaryMode.LIMITED -> BoundaryDecision.DOWNGRADE_SCOPE
            boundary.mode == PermissionBoundaryMode.WAITING -> BoundaryDecision.DOWNGRADE_SCOPE
            else -> BoundaryDecision.ALLOW_OBSERVE
        }
        val badge = when (decision) {
            BoundaryDecision.ALLOW_OBSERVE -> "BOUNDARY_V2_OBSERVE"
            BoundaryDecision.DOWNGRADE_SCOPE -> "BOUNDARY_V2_DOWNGRADE"
            BoundaryDecision.SOFT_LOCK -> "BOUNDARY_V2_SOFT_LOCK"
            BoundaryDecision.HARD_LOCK -> "BOUNDARY_V2_HARD_LOCK"
        }
        return PermissionBoundaryV2State(
            decision = decision,
            badge = badge,
            effectiveTelemetryOnly = true,
            unsafeScopeDowngraded = decision == BoundaryDecision.DOWNGRADE_SCOPE || decision == BoundaryDecision.SOFT_LOCK || decision == BoundaryDecision.HARD_LOCK,
            sensitiveAppLock = decision == BoundaryDecision.SOFT_LOCK || decision == BoundaryDecision.HARD_LOCK,
            reason = when (decision) {
                BoundaryDecision.ALLOW_OBSERVE -> "permissions_and_privacy_ready"
                BoundaryDecision.DOWNGRADE_SCOPE -> "partial_permission_or_waiting_boundary"
                BoundaryDecision.SOFT_LOCK -> "sensitive_scope_soft_lock"
                BoundaryDecision.HARD_LOCK -> "privacy_or_audit_hard_lock"
            }
        )
    }
}
