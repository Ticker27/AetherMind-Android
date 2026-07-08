package com.aether.renderer

data class SkillProposal(
    val descriptor: SkillDescriptor,
    val confidence: SkillConfidenceScore = SkillConfidenceScore.neutral(),
    val safety: SkillSafetyDecision = SkillSafetyDecision.safeProposal(),
    val sourceContext: String = "runtime",
    val message: String = descriptor.title
) {
    val isProposeOnly: Boolean
        get() = !descriptor.executionEnabled && !descriptor.canExecute && safety.executionAllowed == false

    val compactLine: String
        get() = "${descriptor.id}/${descriptor.permission.name}/exec=false score=${confidence.valueText}"
}
