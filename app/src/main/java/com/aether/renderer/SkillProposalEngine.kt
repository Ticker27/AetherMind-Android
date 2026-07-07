package com.aether.renderer

object SkillProposalEngine {
    const val LABEL = "AT131_140_FINAL_STABILITY_LOCK"

    fun propose(
        contextLabel: String,
        confidence: Float,
        risk: Float,
        trustScore: Float,
        requestedSkillId: String? = null
    ): SkillProposal {
        val descriptor = requestedSkillId?.let { SkillRegistry.find(it) } ?: chooseDescriptor(contextLabel, risk)
        val score = SkillConfidenceScore.fromSignals(confidence, trustScore, risk)
        val safety = SkillSafetyGate.evaluate(descriptor)
        val blocker = SkillExecutionBlocker.enforce(descriptor)
        val safeDescriptor = if (blocker.isLocked && safety.allowed) descriptor else SkillRegistry.safeFallback()
        return SkillProposal(
            descriptor = safeDescriptor,
            confidence = score,
            safety = SkillSafetyGate.evaluate(safeDescriptor),
            sourceContext = contextLabel,
            message = "${safeDescriptor.title}: ${score.badge}"
        )
    }

    private fun chooseDescriptor(contextLabel: String, risk: Float): SkillDescriptor {
        if (risk >= 0.70f) return SkillRegistry.find("review_safety_boundary") ?: SkillRegistry.safeFallback()
        return when {
            contextLabel.contains("BROWSING", ignoreCase = true) -> SkillRegistry.find("suggest_next_step")
            contextLabel.contains("PROFILE", ignoreCase = true) -> SkillRegistry.find("profile_edit_suggestion")
            contextLabel.contains("SECURITY", ignoreCase = true) -> SkillRegistry.find("review_safety_boundary")
            else -> SkillRegistry.find("explain_current_state")
        } ?: SkillRegistry.safeFallback()
    }
}
