package com.aether.renderer

data class Phase10ExternalTestLockState(
    val label: String = FinalManifest.LABEL,
    val phase: String = FinalManifest.PHASE,
    val status: String = "READY_FOR_TERMUX_AND_EXTERNAL_RUNTIME_TEST",
    val baselineVerified: Boolean = BaselineVerifier.pass(),
    val termuxHarnessReady: Boolean = true,
    val cloudBuildStillAuthoritative: Boolean = true,
    val note: String = "Termux validates source integrity; final APK build requires Android SDK/NDK environment or cloud CI."
)

object Phase10ExternalTestLock {
    fun current(): Phase10ExternalTestLockState = Phase10ExternalTestLockState()
}
