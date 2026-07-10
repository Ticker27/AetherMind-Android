package com.aether.renderer

data class DataRetentionState(
    val retentionPolicy: String = "SESSION_ONLY",
    val rawTextStored: Boolean = false,
    val rawContentStored: Boolean = false,
    val externalExportAllowed: Boolean = false,
    val resetAvailable: Boolean = true,
    val reason: String = "no_raw_ui_payloads"
)

object DataRetention {
    fun current(privacy: PrivacyGuardState = PrivacyGuardState()): DataRetentionState {
        return DataRetentionState(
            retentionPolicy = privacy.retentionPolicy,
            rawTextStored = false,
            rawContentStored = false,
            externalExportAllowed = false,
            resetAvailable = true,
            reason = "session_only_hash_or_bounds_memory"
        )
    }

    fun resetRuntimeSecurityState(): SecurityPrivacyState {
        val audit = AuditTrail.clear()
        val privacy = PrivacyGuardState(reason = "manual_runtime_reset")
        val retention = current(privacy)
        return SecurityPrivacyState(
            privacy = privacy,
            audit = audit,
            boundary = PermissionBoundaryState(),
            retention = retention
        )
    }
}
