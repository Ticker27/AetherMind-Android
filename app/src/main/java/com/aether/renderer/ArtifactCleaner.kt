package com.aether.renderer

data class ArtifactCleanerState(
    val keepSourceZip: Boolean = true,
    val keepSha256: Boolean = true,
    val keepBuildReports: Boolean = true,
    val removeIntermediateWorkFolders: Boolean = true,
    val removeDuplicatePatchZips: Boolean = true,
    val removeLogs: Boolean = true,
    val policy: String = "KEEP_FINAL_ONLY"
)

object ArtifactCleaner {
    fun current(): ArtifactCleanerState = ArtifactCleanerState()
}
