package com.aether.renderer

data class SkillAuditEvent(
    val id: String,
    val skillId: String,
    val decision: String,
    val executionAllowed: Boolean = false,
    val rawUiTextStored: Boolean = false,
    val rawUiTextExported: Boolean = false,
    val externalExportAllowed: Boolean = false,
    val reason: String
)

object SkillAuditTrail {
    fun fromProposal(proposal: SkillProposal): SkillAuditEvent = SkillAuditEvent(
        id = "AT140_SKILL_${proposal.descriptor.id}",
        skillId = proposal.descriptor.id,
        decision = proposal.safety.badge,
        executionAllowed = false,
        rawUiTextStored = false,
        rawUiTextExported = false,
        externalExportAllowed = false,
        reason = proposal.safety.reason
    )

    fun compact(event: SkillAuditEvent): String {
        return "${event.skillId}/${event.decision}/exec=${event.executionAllowed}"
    }
}
