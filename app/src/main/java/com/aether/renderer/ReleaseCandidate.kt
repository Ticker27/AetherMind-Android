package com.aether.renderer

data class ReleaseCandidateState(
    val label: String = "AT131_140_FINAL_STABILITY_LOCK",
    val track: String = "RC",
    val ready: Boolean = true,
    val blockers: Int = 0,
    val buildLabel: String = "14.0.0",
    val reason: String = "final_stability_lock_passed"
) {
    val compactLine: String
        get() = "$track ready=$ready blockers=$blockers build=$buildLabel"
}

object ReleaseCandidate {
    fun current(): ReleaseCandidateState = ReleaseCandidateState()
}
