package com.aether.renderer

enum class LearningSignalKind {
    STABLE_POSITIVE,
    WATCH,
    RISK_NEGATIVE,
    COLD_START
}

data class LearningSignal(
    val packageName: String = "",
    val contextLabel: String = "CTX_UNKNOWN",
    val screenType: ScreenType = ScreenType.SCREEN_UNKNOWN,
    val confidence: Float = 0.0f,
    val risk: Float = 0.0f,
    val stability: Float = 0.0f,
    val behaviorStability: Float = 0.0f,
    val sessionSamples: Int = 0,
    val kind: LearningSignalKind = LearningSignalKind.COLD_START,
    val reason: String = "cold_start",
    val timestampNanos: Long = 0L
)
