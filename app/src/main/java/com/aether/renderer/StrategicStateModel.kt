package com.aether.renderer

import java.util.Locale

data class PhysicsExperienceState(
    val packageName: String = "",
    val contextLabel: String = "CTX_UNKNOWN",
    val screenType: ScreenType = ScreenType.SCREEN_UNKNOWN,
    val observerActive: Boolean = false,
    val nodeCount: Int = 0,
    val clickableCount: Int = 0,
    val textNodeCount: Int = 0,
    val depth: Int = 0,
    val confidence: Float = 0.0f,
    val risk: Float = 0.0f,
    val trustScore: Float = 0.0f,
    val uncertainty: Float = 1.0f,
    val volatility: Float = 0.0f,
    val privacyMode: PrivacyMode = PrivacyMode.STANDARD,
    val securityMode: SecurityLockMode = SecurityLockMode.OPEN_OBSERVE,
    val budgetMode: RuntimeBudgetMode = RuntimeBudgetMode.NORMAL,
    val interactionDensity: Float = 0.0f,
    val stateStability: Float = 0.0f,
    val telemetryOnly: Boolean = true,
    val autoControlEnabled: Boolean = false,
    val reason: String = "state_waiting"
) {
    val badge: String
        get() = when {
            !observerActive -> "STATE_IDLE"
            securityMode == SecurityLockMode.HARD_LOCK -> "STATE_LOCKED"
            risk >= 0.45f -> "STATE_RISKY"
            stateStability >= 0.70f && confidence >= 0.70f -> "STATE_STABLE"
            else -> "STATE_LEARNING"
        }

    val compactLine: String
        get() = "$badge ${UiReadability.shortContext(contextLabel)} c=${confidence.fmt2()} r=${risk.fmt2()} d=${interactionDensity.fmt2()}"
}

object StrategicStateModel {
    fun evaluate(
        scene: SceneSnapshot,
        observer: ObserverSnapshot,
        decision: RuntimeDecision,
        trust: TrustModelState,
        privacy: PrivacyGuardState,
        security: SecurityLockState,
        budget: RuntimeBudgetState
    ): PhysicsExperienceState {
        val density = if (observer.nodeCount <= 0) {
            0.0f
        } else {
            ((observer.clickableCount.toFloat() * 0.55f + observer.textNodeCount.toFloat() * 0.20f) / observer.nodeCount.toFloat()).coerceIn(0.0f, 1.0f)
        }
        val stability = (
            decision.stabilityScore * 0.24f +
                decision.anchorStability * 0.18f +
                decision.behaviorStability * 0.18f +
                decision.contextConfidence * 0.20f +
                (1.0f - decision.volatility).coerceIn(0.0f, 1.0f) * 0.20f
            ).coerceIn(0.0f, 1.0f)
        return PhysicsExperienceState(
            packageName = scene.packageName.ifBlank { observer.lastPackage },
            contextLabel = decision.contextLabel,
            screenType = decision.screenType,
            observerActive = observer.active,
            nodeCount = observer.nodeCount,
            clickableCount = observer.clickableCount,
            textNodeCount = observer.textNodeCount,
            depth = observer.maxDepth,
            confidence = decision.confidence.coerceIn(0.0f, 1.0f),
            risk = decision.risk.coerceIn(0.0f, 1.0f),
            trustScore = if (trust.trustScore > 0f) trust.trustScore else decision.trustScore,
            uncertainty = decision.uncertainty.coerceIn(0.0f, 1.0f),
            volatility = decision.volatility.coerceIn(0.0f, 1.0f),
            privacyMode = privacy.privacyMode,
            securityMode = security.mode,
            budgetMode = budget.budgetMode,
            interactionDensity = density,
            stateStability = stability,
            telemetryOnly = security.telemetryOnly,
            autoControlEnabled = security.autoControlEnabled,
            reason = "from_observer_context_trust_budget"
        )
    }
}

internal fun Float.fmt2(): String = String.format(Locale.US, "%.2f", this)
