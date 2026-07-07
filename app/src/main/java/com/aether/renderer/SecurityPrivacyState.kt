package com.aether.renderer

data class SecurityPrivacyState(
    val privacy: PrivacyGuardState = PrivacyGuardState(),
    val audit: AuditTrailState = AuditTrailState(),
    val boundary: PermissionBoundaryState = PermissionBoundaryState(),
    val retention: DataRetentionState = DataRetentionState()
) {
    val securityBadge: String
        get() = when {
            privacy.privacyMode == PrivacyMode.LOCKED -> "SECURITY_LOCK"
            boundary.mode == PermissionBoundaryMode.WAITING -> "SECURITY_WAIT"
            boundary.mode == PermissionBoundaryMode.LIMITED -> "SECURITY_LIMITED"
            privacy.privacyMode == PrivacyMode.SENSITIVE -> "SECURITY_GUARD"
            else -> "SECURITY_OK"
        }

    val compactLine: String
        get() = "$securityBadge ${privacy.privacyBadge} ${boundary.boundaryBadge} audit=${audit.auditCount}"
}

object SecurityPrivacy {
    const val LABEL = "AT61_70_SECURITY_PERFORMANCE_BATCH"
    const val API_STATUS = "PHASE6_COMPLETE_PHASE7_START"
    const val TELEMETRY_ONLY = true
    const val AUTO_CONTROL_ENABLED = false
    const val RAW_TEXT_STORED = false
}
