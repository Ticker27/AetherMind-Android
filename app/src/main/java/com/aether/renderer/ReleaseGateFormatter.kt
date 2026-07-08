package com.aether.renderer

object ReleaseGateFormatter {
    fun format(snapshot: ReleaseGateSnapshot = RuntimeIdentityVerifier.snapshot()): String {
        return buildString {
            append(snapshot.label)
            append(" v")
            append(snapshot.appVersion)
            append(" code=")
            append(snapshot.versionCode)
            append(" api=")
            append(snapshot.apiStatus)
            append(" skill=")
            append(snapshot.skillMode)
            append(" exec=")
            append(snapshot.executionEnabled)
            append(" auto=")
            append(snapshot.autoControlEnabled)
            append(" pass=")
            append(snapshot.passed)
        }
    }
}
