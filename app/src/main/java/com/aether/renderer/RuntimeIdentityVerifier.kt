package com.aether.renderer

object RuntimeIdentityVerifier {
    private val legacyCurrentLabels = listOf(
        "PROD_LOCK v" + "11" + "." + "5",
        "AT120" + "_125_STRATEGIC_REASONING_CORE",
        "AT126" + "_129_REASONING_EVIDENCE_LOCK_FULL",
        "AT" + "130_SKILL_SYSTEM_PROPOSE_ONLY",
        "11" + ".5.0",
        "12" + ".0.0"
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
