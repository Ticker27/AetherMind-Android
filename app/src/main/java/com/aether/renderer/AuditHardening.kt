package com.aether.renderer

enum class AuditSeverity {
    INFO,
    NOTICE,
    WARNING,
    ERROR,
    CRITICAL
}

enum class PrivacyEventClass {
    NORMAL,
    SENSITIVE,
    PERMISSION,
    RETENTION,
    LOCK
}

data class AuditHardeningState(
    val severity: AuditSeverity = AuditSeverity.INFO,
    val eventClass: PrivacyEventClass = PrivacyEventClass.NORMAL,
    val totalEvents: Int = 0,
    val warningEvents: Int = 0,
    val criticalEvents: Int = 0,
    val sessionOnly: Boolean = true,
    val rawPayloadStored: Boolean = false,
    val reason: String = "audit_hardening_ready"
) {
    val badge: String
        get() = when (severity) {
            AuditSeverity.INFO -> "AUDIT_INFO"
            AuditSeverity.NOTICE -> "AUDIT_NOTICE"
            AuditSeverity.WARNING -> "AUDIT_WARN"
            AuditSeverity.ERROR -> "AUDIT_ERROR"
            AuditSeverity.CRITICAL -> "AUDIT_CRITICAL"
        }

    val compactLine: String
        get() = "$badge ${eventClass.name} n=$totalEvents w=$warningEvents c=$criticalEvents"
}

object AuditHardening {
    private var total = 0
    private var warnings = 0
    private var criticals = 0

    fun evaluate(eventName: String, privacy: PrivacyGuardState, boundary: PermissionBoundaryState): AuditHardeningState {
        total += 1
        val eventClass = when {
            privacy.privacyMode == PrivacyMode.LOCKED -> PrivacyEventClass.LOCK
            boundary.mode == PermissionBoundaryMode.LOCKED -> PrivacyEventClass.LOCK
            boundary.mode == PermissionBoundaryMode.WAITING -> PrivacyEventClass.PERMISSION
            privacy.privacyMode == PrivacyMode.SENSITIVE -> PrivacyEventClass.SENSITIVE
            eventName.contains("RESET", ignoreCase = true) -> PrivacyEventClass.RETENTION
            else -> PrivacyEventClass.NORMAL
        }
        val severity = when (eventClass) {
            PrivacyEventClass.LOCK -> AuditSeverity.CRITICAL
            PrivacyEventClass.SENSITIVE -> AuditSeverity.WARNING
            PrivacyEventClass.PERMISSION -> AuditSeverity.NOTICE
            PrivacyEventClass.RETENTION -> AuditSeverity.NOTICE
            PrivacyEventClass.NORMAL -> AuditSeverity.INFO
        }
        if (severity == AuditSeverity.WARNING) warnings += 1
        if (severity == AuditSeverity.CRITICAL) criticals += 1
        return AuditHardeningState(
            severity = severity,
            eventClass = eventClass,
            totalEvents = total,
            warningEvents = warnings,
            criticalEvents = criticals,
            sessionOnly = true,
            rawPayloadStored = false,
            reason = "classified_${eventClass.name.lowercase()}"
        )
    }

    fun clear(): AuditHardeningState {
        total = 0
        warnings = 0
        criticals = 0
        return AuditHardeningState(reason = "audit_hardening_reset")
    }
}
