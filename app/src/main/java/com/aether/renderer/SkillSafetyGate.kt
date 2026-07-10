package com.aether.renderer

data class SkillSafetyDecision(
    val allowed: Boolean,
    val executionAllowed: Boolean = false,
    val telemetryOnly: Boolean = true,
    val reason: String = "skill_safety_gate"
) {
    val badge: String
        get() = when {
            executionAllowed -> "SKILL_BLOCK_REQUIRED"
            allowed -> "SKILL_PROPOSE_ONLY"
            else -> "SKILL_FORBIDDEN"
        }

    companion object {
        fun safeProposal(): SkillSafetyDecision = SkillSafetyDecision(
            allowed = true,
            executionAllowed = false,
            telemetryOnly = true,
            reason = "proposal_only_safe"
        )
    }
}

object SkillSafetyGate {
    const val TELEMETRY_ONLY = true
    const val AUTO_CONTROL_ENABLED = false
    const val EXECUTION_ENABLED = false
    const val MODE = "PROPOSE_ONLY"

    fun evaluate(descriptor: SkillDescriptor): SkillSafetyDecision {
        if (descriptor.permission == SkillPermission.FORBIDDEN) {
            return SkillSafetyDecision(false, false, true, "permission_forbidden")
        }
        if (descriptor.riskLevel == SkillRiskLevel.BLOCKED || descriptor.riskLevel == SkillRiskLevel.HIGH) {
            return SkillSafetyDecision(false, false, true, "risk_blocked")
        }
        if (descriptor.executionEnabled || descriptor.canExecute) {
            return SkillSafetyDecision(false, false, true, "execution_blocked")
        }
        return SkillSafetyDecision.safeProposal()
    }
}
