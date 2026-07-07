package com.aether.renderer

import java.util.Locale

data class ExplainableUxState(
    val headline: String = "System observing safely.",
    val primaryReason: String = "Waiting for stable context.",
    val safetyReason: String = "Telemetry-only mode is active.",
    val privacyReason: String = "Raw UI text is not stored.",
    val performanceReason: String = "Runtime budget is normal.",
    val userActionHint: String = "Open target app and observe HUD status.",
    val readableLine: String = "OBSERVE: waiting for context",
    val sanitizedStatus: String = "AETHER_STATUS=OBSERVE",
    val accessibilitySummary: String = "AetherMind is observing only."
)

object ExplainableUx {
    fun explain(
        scene: SceneSnapshot,
        profile: AppProfile,
        decision: RuntimeDecision,
        security: SecurityPrivacyState,
        lock: SecurityLockState,
        budget: RuntimeBudgetState,
        privacy: PrivacyGuardState
    ): ExplainableUxState {
        val target = profile.label.ifBlank { scene.targetLabel }
        val safetyReason = when {
            lock.mode == SecurityLockMode.HARD_LOCK -> "Security hard lock is active."
            lock.mode == SecurityLockMode.SOFT_LOCK -> "Permission or privacy boundary requires soft lock."
            decision.safetyLevel == SafetyLevel.LOCKED -> "Risk gate locked this context."
            decision.safetyLevel == SafetyLevel.CAUTION -> "Context is not stable enough for readiness."
            else -> "Telemetry-only safety gate is clear."
        }
        val privacyReason = when (privacy.privacyMode) {
            PrivacyMode.LOCKED -> "Privacy guard locked sensitive scope."
            PrivacyMode.SENSITIVE -> "Sensitive app/screen detected; raw text and content hashes are suppressed."
            else -> "Privacy scope is minimized; raw UI text is not stored."
        }
        val performanceReason = when (budget.budgetMode) {
            RuntimeBudgetMode.CRITICAL -> "Runtime budget is critical; HUD should stay conservative."
            RuntimeBudgetMode.THROTTLE -> "Runtime pressure is high; throttle is recommended."
            RuntimeBudgetMode.WATCH -> "Runtime pressure is elevated; watch mode is active."
            RuntimeBudgetMode.NORMAL -> "Runtime budget is within normal limits."
        }
        val headline = when {
            lock.mode == SecurityLockMode.HARD_LOCK -> "Locked for privacy and safety."
            budget.budgetMode == RuntimeBudgetMode.CRITICAL -> "Runtime protection is active."
            decision.policyV2State == PolicyV2State.LOCKED -> "Policy V2 is locked."
            decision.policyV2State == PolicyV2State.CAUTION -> "Policy V2 is cautious."
            decision.trustBand == TrustBand.TRUSTED && decision.stabilityLabel == "STABLE" -> "Context is trusted and stable."
            else -> "System is learning this context."
        }
        val primaryReason = "ctx=${decision.contextLabel} screen=${decision.screenType.name} behavior=${decision.behaviorLabel} trust=${decision.trustLabel}"
        val hint = when {
            !security.boundary.overlayGranted -> "Grant overlay permission to show HUD."
            !security.boundary.accessibilityEnabled -> "Enable AetherMind Accessibility Service."
            lock.mode == SecurityLockMode.HARD_LOCK -> "Review privacy/security status before continuing."
            decision.sceneChanged -> "Wait for the screen to stabilize."
            budget.throttleRecommended -> "Reduce HUD detail or wait for runtime pressure to fall."
            else -> "Continue observing; no user action is required."
        }
        val readableLine = "${lock.badge} / ${decision.policyV2State.name} / ${decision.feedbackPolicyLabel} / ${budget.badge}"
        val sanitizedStatus = buildString {
            append("pkg=").append(scene.packageName.ifBlank { profile.packageName }.substringAfterLast('.'))
            append(";ctx=").append(decision.contextLabel)
            append(";screen=").append(decision.screenType.name)
            append(";safety=").append(decision.safetyBadge)
            append(";policy=").append(decision.policyV2State.name)
            append(";security=").append(lock.badge)
            append(";privacy=").append(privacy.dataScope.name)
            append(";budget=").append(budget.badge)
            append(";rawText=false;exportRaw=false")
        }
        return ExplainableUxState(
            headline = headline,
            primaryReason = primaryReason,
            safetyReason = safetyReason,
            privacyReason = privacyReason,
            performanceReason = performanceReason,
            userActionHint = hint,
            readableLine = readableLine,
            sanitizedStatus = sanitizedStatus,
            accessibilitySummary = "$target. $headline $hint"
        )
    }

    fun formatConfidence(value: Float): String = String.format(Locale.US, "%.2f", value)
}
