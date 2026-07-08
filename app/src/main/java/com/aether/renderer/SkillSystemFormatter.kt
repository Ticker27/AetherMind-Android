package com.aether.renderer

object SkillSystemFormatter {
    fun format(snapshot: SkillSystemSnapshot): String {
        return "Skill: ${snapshot.proposal.descriptor.id}/${snapshot.mode}/exec=false prod=${snapshot.badge}"
    }

    fun auditLine(snapshot: SkillSystemSnapshot): String {
        val audit = SkillAuditTrail.fromProposal(snapshot.proposal)
        return SkillAuditTrail.compact(audit)
    }

    fun registryLine(): String {
        return "SkillRegistry: n=${SkillRegistry.all().size} mode=${SkillRegistry.MODE} exec=false"
    }
}
