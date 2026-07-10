package com.aether.renderer

object Phase3Lock {
    const val LABEL = "AT39_PHASE3_LOCK"
    const val PHASE = "3.9"
    const val APP_VERSION = "3.9.0"
    const val VERSION_CODE = 390
    const val CORE_VERSION = "7.1"
    const val TELEMETRY_ONLY = true
    const val API_STATUS = "FROZEN_PHASE3"
    const val BASELINE_FROM = "AT38_BATCH_UPGRADE"

    val requiredRuntimeFiles = listOf(
        "AetherRuntimeBus.kt",
        "SceneSnapshot.kt",
        "RuntimeDecision.kt",
        "IntentClassifier.kt",
        "ProfileRules.kt",
        "SafetyGate.kt",
        "SceneMemory.kt",
        "AdaptivePolicy.kt",
        "ActionIntentModel.kt",
        "SessionMemory.kt"
    )
}
