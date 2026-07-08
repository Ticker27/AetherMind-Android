package com.aether.renderer

object SkillRegistry {
    const val LABEL = "AT131_140_FINAL_STABILITY_LOCK"
    const val MODE = "PROPOSE_ONLY"
    const val EXECUTION_ENABLED = false

    val builtIns: List<SkillDescriptor> = listOf(
        SkillDescriptor(
            id = "observe_screen_state",
            title = "Observe screen state",
            category = SkillCategory.OBSERVE,
            permission = SkillPermission.ALLOW_TELEMETRY,
            reason = "metadata_observation_only"
        ),
        SkillDescriptor(
            id = "explain_current_state",
            title = "Explain current state",
            category = SkillCategory.EXPLAIN,
            permission = SkillPermission.ALLOW_TELEMETRY,
            reason = "local_explanation_only"
        ),
        SkillDescriptor(
            id = "suggest_next_step",
            title = "Suggest next step",
            category = SkillCategory.PLAN,
            permission = SkillPermission.PROPOSE_ONLY,
            reason = "proposal_without_execution"
        ),
        SkillDescriptor(
            id = "review_safety_boundary",
            title = "Review safety boundary",
            category = SkillCategory.PROTECT,
            permission = SkillPermission.PROPOSE_ONLY,
            reason = "safety_telemetry_review"
        ),
        SkillDescriptor(
            id = "profile_edit_suggestion",
            title = "Suggest profile edit",
            category = SkillCategory.PROFILE,
            permission = SkillPermission.PROPOSE_ONLY,
            riskLevel = SkillRiskLevel.MEDIUM,
            reason = "proposal_requires_user_decision"
        ),
        SkillDescriptor(
            id = "external_action_request",
            title = "External action request",
            category = SkillCategory.MAINTENANCE,
            permission = SkillPermission.FORBIDDEN,
            riskLevel = SkillRiskLevel.BLOCKED,
            reason = "external_execution_disabled"
        )
    )

    fun all(): List<SkillDescriptor> = builtIns

    fun find(id: String): SkillDescriptor? = builtIns.firstOrNull { it.id == id }

    fun safeFallback(): SkillDescriptor = builtIns.first { it.id == "suggest_next_step" }
}
