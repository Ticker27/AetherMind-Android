package com.aether.renderer

object HudDensityPatch {
    const val LABEL = "AT131_140_FINAL_STABILITY_LOCK"

    fun minimalIntent(decision: RuntimeDecision): String {
        return "${UiReadability.actionIntentLabel(decision.actionIntent)} · ${decision.safetyBadge}"
    }

    fun minimalDetail(decision: RuntimeDecision): String {
        val context = UiReadability.shortContext(decision.contextLabel)
        return "$context · c=${format(decision.confidence)} r=${format(decision.risk)}"
    }

    fun compactDetail(telemetry: RuntimeTelemetry, decision: RuntimeDecision, budget: RuntimeBudgetState): String {
        val ai = if (telemetry.aiActive) "AI=ON" else "AI=IDLE"
        val context = UiReadability.short(UiReadability.shortContext(decision.contextLabel), 14)
        return "$ai · $context · ${budget.badge}"
    }

    fun compactObserver(observer: ObserverSnapshot, privacy: PrivacyGuardState, explain: ExplainableUxState): String {
        val line1 = "obs ${observer.nodeCount}/${observer.clickableCount}/${observer.textNodeCount} · ${privacy.privacyBadge}"
        val line2 = UiReadability.short(explain.headline, 36)
        return "$line1\n$line2"
    }

    fun shouldUseDashboardSafePlacement(appPackageName: String, profile: AppProfile, observer: ObserverSnapshot): Boolean {
        return profile.packageName == appPackageName || observer.lastPackage == appPackageName
    }

    private fun format(value: Float): String = String.format(java.util.Locale.US, "%.2f", value)
}
