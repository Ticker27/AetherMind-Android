package com.aether.renderer

data class RetentionResetState(
    val resetAvailable: Boolean = true,
    val resetCounter: Int = 0,
    val sessionOnly: Boolean = true,
    val rawTextStored: Boolean = false,
    val rawContentStored: Boolean = false,
    val externalExportAllowed: Boolean = false,
    val lastResetNanos: Long = 0L,
    val reason: String = "reset_ready"
) {
    val badge: String
        get() = if (sessionOnly && !rawTextStored && !externalExportAllowed) "RETENTION_SESSION_ONLY" else "RETENTION_CHECK"

    val compactLine: String
        get() = "$badge resets=$resetCounter raw=$rawTextStored export=$externalExportAllowed"
}

object RetentionResetControls {
    private var resetCounter = 0
    private var lastResetNanos = 0L

    fun current(retention: DataRetentionState): RetentionResetState {
        return RetentionResetState(
            resetAvailable = retention.resetAvailable,
            resetCounter = resetCounter,
            sessionOnly = retention.retentionPolicy == "SESSION_ONLY",
            rawTextStored = retention.rawTextStored,
            rawContentStored = retention.rawContentStored,
            externalExportAllowed = retention.externalExportAllowed,
            lastResetNanos = lastResetNanos,
            reason = retention.reason
        )
    }

    fun markReset(): RetentionResetState {
        resetCounter += 1
        lastResetNanos = android.os.SystemClock.elapsedRealtimeNanos()
        return RetentionResetState(
            resetCounter = resetCounter,
            lastResetNanos = lastResetNanos,
            reason = "manual_runtime_privacy_reset"
        )
    }
}
