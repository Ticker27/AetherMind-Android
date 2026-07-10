package com.aether.renderer

enum class SkillPermission {
    ALLOW_TELEMETRY,
    PROPOSE_ONLY,
    USER_APPROVAL_REQUIRED,
    FORBIDDEN
}

data class SkillCard(
    val id: String,
    val permission: SkillPermission,
    val telemetryOnly: Boolean = true,
    val canExecuteNow: Boolean = false,
    val reason: String = "skill_policy"
) {
    val compactLine: String
        get() = "$id/${permission.name}/exec=$canExecuteNow"
}

object SkillBoundary {
    val observeScreen = SkillCard("ObserveScreen", SkillPermission.ALLOW_TELEMETRY, reason = "read_metadata_only")
    val explainState = SkillCard("ExplainState", SkillPermission.ALLOW_TELEMETRY, reason = "local_explanation")
    val suggestPlan = SkillCard("SuggestPlan", SkillPermission.PROPOSE_ONLY, reason = "no_action_execution")
    val proposeSafeTap = SkillCard("SafeTapProposal", SkillPermission.USER_APPROVAL_REQUIRED, canExecuteNow = false, reason = "approval_required_future_layer")
    val externalAction = SkillCard("ExternalAction", SkillPermission.FORBIDDEN, canExecuteNow = false, reason = "auto_control_disabled")

    fun forPlan(planId: String): SkillCard {
        return when (planId) {
            "observe" -> observeScreen
            "explain" -> explainState
            "suggest" -> suggestPlan
            "safe_tap_proposal" -> proposeSafeTap
            else -> suggestPlan.copy(id = planId, reason = "default_proposal_only")
        }
    }
}
