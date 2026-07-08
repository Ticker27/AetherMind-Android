package com.aether.renderer

data class SkillConfidenceScore(
    val value: Float,
    val reason: String = "skill_confidence"
) {
    val normalized: Float = value.coerceIn(0.0f, 1.0f)

    val valueText: String
        get() = String.format(java.util.Locale.US, "%.2f", normalized)

    val badge: String
        get() = when {
            normalized >= 0.85f -> "SKILL_CONF_HIGH"
            normalized >= 0.60f -> "SKILL_CONF_MED"
            else -> "SKILL_CONF_LOW"
        }

    companion object {
        fun neutral(): SkillConfidenceScore = SkillConfidenceScore(0.60f, "neutral_proposal")

        fun fromSignals(contextConfidence: Float, trustScore: Float, risk: Float): SkillConfidenceScore {
            val raw = (contextConfidence * 0.45f) + (trustScore * 0.40f) + ((1.0f - risk.coerceIn(0.0f, 1.0f)) * 0.15f)
            return SkillConfidenceScore(raw, "context_trust_risk")
        }
    }
}
