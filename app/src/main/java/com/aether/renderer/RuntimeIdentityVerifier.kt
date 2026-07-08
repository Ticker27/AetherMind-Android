package com.aether.renderer

object RuntimeIdentityVerifier {
    private val legacyCurrentLabels = listOf(
        FinalManifest.LABEL,
        FinalManifest.APP_VERSION,
        FinalManifest.VERSION_CODE.toString()
    )

    fun snapshot(): ReleaseGateSnapshot = ReleaseGateSnapshot()

    fun verify(snapshot: ReleaseGateSnapshot = snapshot()): Boolean = snapshot.passed

    fun assertRuntimeLine(line: String): Boolean {
        val noLegacy = legacyCurrentLabels.none { line.contains(it) }
        val hasCurrent = line.contains(FinalManifest.LABEL) || line.contains(FinalManifest.APP_VERSION)
        return noLegacy && hasCurrent
    }

    fun compactLine(): String {
        val snap = snapshot()
        return "${snap.label} v${snap.appVersion} code=${snap.versionCode} mode=${snap.skillMode} exec=${snap.executionEnabled} auto=${snap.autoControlEnabled} pass=${snap.passed}"
    }
}
