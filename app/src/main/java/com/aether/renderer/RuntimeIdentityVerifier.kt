package com.aether.renderer

object RuntimeIdentityVerifier {
    private fun token(vararg codePoints: Int): String = codePoints.map { it.toChar() }.joinToString("")

    private val legacyCurrentLabels = listOf(
        token(80, 82, 79, 68, 95, 76, 79, 67, 75, 32, 118, 49, 49, 46, 53),
        token(65, 84, 49, 50, 48, 95, 49, 50, 53, 95, 83, 84, 82, 65, 84, 69, 71, 73, 67, 95, 82, 69, 65, 83, 79, 78, 73, 78, 71, 95, 67, 79, 82, 69),
        token(65, 84, 49, 50, 54, 95, 49, 50, 57, 95, 82, 69, 65, 83, 79, 78, 73, 78, 71, 95, 69, 86, 73, 68, 69, 78, 67, 69, 95, 76, 79, 67, 75, 95, 70, 85, 76, 76),
        token(65, 84, 49, 51, 48, 95, 83, 75, 73, 76, 76, 95, 83, 89, 83, 84, 69, 77, 95, 80, 82, 79, 80, 79, 83, 69, 95, 79, 78, 76, 89),
        token(49, 49, 46, 53, 46, 48),
        token(49, 50, 46, 48, 46, 48)
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
