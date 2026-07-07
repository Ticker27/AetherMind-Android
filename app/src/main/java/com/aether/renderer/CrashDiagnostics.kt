package com.aether.renderer

data class CrashDiagnosticState(
    val crashReportExported: Boolean = false,
    val rawUiTextIncluded: Boolean = false,
    val lastKnownPhase: String = QaManifest.phase,
    val mode: String = "SESSION_ONLY_SANITIZED"
)

object CrashDiagnostics {
    fun current(): CrashDiagnosticState = CrashDiagnosticState()
}
