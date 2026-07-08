package com.aether.renderer

enum class SkillCategory {
    OBSERVE,
    EXPLAIN,
    PLAN,
    PROTECT,
    PROFILE,
    MAINTENANCE
}

enum class SkillRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    BLOCKED
}

data class SkillDescriptor(
    val id: String,
    val title: String,
    val category: SkillCategory,
    val permission: SkillPermission = SkillPermission.PROPOSE_ONLY,
    val riskLevel: SkillRiskLevel = SkillRiskLevel.LOW,
    val telemetryOnly: Boolean = true,
    val executionEnabled: Boolean = false,
    val requiresUserApproval: Boolean = true,
    val reason: String = "skill_descriptor"
) {
    val canExecute: Boolean
        get() = false

    val compactLine: String
        get() = "$id/${permission.name}/exec=$canExecute"
}
