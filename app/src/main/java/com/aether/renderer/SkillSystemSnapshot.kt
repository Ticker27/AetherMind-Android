package com.aether.renderer

data class SkillSystemSnapshot(
    val label: String = FinalStabilityLock.LABEL,
    val mode: String = FinalStabilityLock.SKILL_MODE,
    val registrySize: Int = SkillRegistry.all().size,
    val proposal: SkillProposal = SkillProposalEngine.propose(
        contextLabel = "UNKNOWN",
        confidence = 0.60f,
        risk = 0.10f,
        trustScore = 0.80f
    ),
    val blocker: SkillExecutionBlockState = SkillExecutionBlocker.locked(),
    val telemetryOnly: Boolean = true,
    val rawUiTextStored: Boolean = false,
    val rawUiTextExported: Boolean = false,
    val externalExportAllowed: Boolean = false
) {
    val badge: String
        get() = if (blocker.isLocked && proposal.isProposeOnly) "SKILL_SYSTEM_READY" else "SKILL_SYSTEM_BLOCKED"

    val compactLine: String
        get() = "$badge ${proposal.compactLine}"
}
