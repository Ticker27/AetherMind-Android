package com.aether.renderer

object UiReadability {
    const val LABEL = "AT131_140_FINAL_STABILITY_LOCK"

    fun nativeIntentLabel(intent: Int): String = when (intent) {
        0 -> "ACTIVE_ANALYSIS"
        1 -> "PROTECTIVE_REVIEW"
        2 -> "SAFETY_REVIEW"
        else -> "CONTEXT_POSITIONING"
    }

    fun actionIntentLabel(action: ActionIntent): String = when (action) {
        ActionIntent.POSITIONING -> "CONTEXT_POSITIONING"
        ActionIntent.DEFENSIVE -> "PROTECTIVE_REVIEW"
        ActionIntent.SAFETY_PLAY -> "SAFETY_REVIEW"
        ActionIntent.SEARCH_SCAN -> "SEARCH_SCAN"
        ActionIntent.TEXT_REVIEW -> "TEXT_REVIEW"
        ActionIntent.MENU_GRID -> "MENU_GRID"
        ActionIntent.WAITING -> "WAITING"
    }

    fun intentLabel(raw: String): String = when (raw) {
        "OFFENSIVE" -> "ACTIVE_ANALYSIS"
        "DEFENSIVE" -> "PROTECTIVE_REVIEW"
        "SAFETY_PLAY" -> "SAFETY_REVIEW"
        "POSITIONING" -> "CONTEXT_POSITIONING"
        else -> raw
    }

    fun shortContext(label: String): String = label
        .removePrefix("CTX_")
        .removePrefix("SCREEN_")
        .ifBlank { "UNKNOWN" }

    fun short(text: String, max: Int): String {
        if (text.length <= max) return text
        if (max <= 1) return text.take(max)
        return text.take(max - 1) + "…"
    }

    fun compactPackage(packageName: String): String = packageName.substringAfterLast('.').ifBlank { "aether" }

    fun dashboardIntent(telemetry: RuntimeTelemetry?, fallbackNativeIntent: Int): String {
        return nativeIntentLabel(telemetry?.intent ?: fallbackNativeIntent)
    }
}
