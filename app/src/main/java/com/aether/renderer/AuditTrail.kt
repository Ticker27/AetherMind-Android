package com.aether.renderer

data class AuditEntry(
    val event: String,
    val packageHash: Int,
    val privacyBadge: String,
    val policyState: String,
    val timestampNanos: Long
)

data class AuditTrailState(
    val auditCount: Int = 0,
    val lastEvent: String = "NONE",
    val lastPrivacyBadge: String = "PRIVACY_OK",
    val lastPolicyState: String = "OBSERVE",
    val rawPayloadStored: Boolean = false,
    val exportAllowed: Boolean = false,
    val reason: String = "session_audit_only"
) {
    val compactLine: String
        get() = "Audit: n=$auditCount last=$lastEvent $lastPrivacyBadge"
}

object AuditTrail {
    private const val MAX_EVENTS = 64
    private val events = ArrayDeque<AuditEntry>()

    fun record(event: String, packageName: String, privacy: PrivacyGuardState, decision: RuntimeDecision): AuditTrailState {
        val entry = AuditEntry(
            event = event,
            packageHash = packageName.hashCode(),
            privacyBadge = privacy.privacyBadge,
            policyState = decision.policyV2State.name,
            timestampNanos = android.os.SystemClock.elapsedRealtimeNanos()
        )
        events.addLast(entry)
        while (events.size > MAX_EVENTS) events.removeFirst()
        return current()
    }

    fun current(): AuditTrailState {
        val last = events.lastOrNull()
        return AuditTrailState(
            auditCount = events.size,
            lastEvent = last?.event ?: "NONE",
            lastPrivacyBadge = last?.privacyBadge ?: "PRIVACY_OK",
            lastPolicyState = last?.policyState ?: "OBSERVE",
            rawPayloadStored = false,
            exportAllowed = false,
            reason = "session_ring_buffer_no_raw_payload"
        )
    }

    fun clear(): AuditTrailState {
        events.clear()
        return current()
    }
}
