package com.aether.renderer

data class ReleaseReviewResolutionState(
    val reviewLabel: String = "FINAL_REVIEW_RESOLUTION",
    val canonicalSource: String = "AT100_105_RELEASE_LOCK",
    val archiveInputsAllowedAsEvidence: Boolean = true,
    val archiveInputsAllowedAsBaseline: Boolean = false,
    val mixedPatchAllowed: Boolean = false,
    val resolvedStatus: String = "CANONICAL_RELEASE_LINE_SELECTED",
    val reason: String = "Use locked release line as baseline; archive files are evidence only."
)

object ReleaseReviewResolution {
    fun current(): ReleaseReviewResolutionState = ReleaseReviewResolutionState()
}
