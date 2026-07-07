package com.aether.renderer

data class ReleaseCandidateState(
    val label: String = "AT100_105_RELEASE_LOCK",
    val track: String = "RC",
    val ready: Boolean = true,
    val blockers: Int = 0,
    val buildLabel: String = "10.5.0",
    val reason: String = "qa_baseline_passed"
) {
    val compactLine: String
        get() = "$track ready=$ready blockers=$blockers build=$buildLabel"
}

object ReleaseCandidate {
    fun current(): ReleaseCandidateState = ReleaseCandidateState()
}
