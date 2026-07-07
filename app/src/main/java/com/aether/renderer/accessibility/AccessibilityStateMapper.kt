package com.aether.renderer.accessibility

import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aether.renderer.AetherRuntimeBus
import com.aether.renderer.AppProfile
import com.aether.renderer.HudMode
import com.aether.renderer.ObserverSnapshot
import com.aether.renderer.PhysicsStateBuffer
import com.aether.renderer.DeviceProfile
import com.aether.renderer.ScreenWorld
import com.aether.renderer.NodeAnchor
import com.aether.renderer.SafetyLevel
import com.aether.renderer.PolicyMode
import com.aether.renderer.PolicyV2State
import com.aether.renderer.PrivacyGuard
import com.aether.renderer.PrivacyMode
import com.aether.renderer.RuntimeBudgetMode
import com.aether.renderer.SecurityLockMode
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AccessibilityStateMapper(private val context: Context) {
    private val deviceProfile by lazy { DeviceProfile.from(context) }
    private var eventCount = 0
    private var lastSnapshot = ObserverSnapshot()
    private var lastCueX = 0.50f
    private var lastCueY = 0.78f
    private var lastTargetX = 0.50f
    private var lastTargetY = 0.34f
    private var lastConfidence = 0.35f
    private var lastRisk = 0.10f
    private var lastProfile = AppProfile()

    fun onEvent(event: AccessibilityEvent?, root: AccessibilityNodeInfo?) {
        if (event == null && root == null) return
        eventCount++

        val currentPackage = event?.packageName?.toString().orEmpty()
        val screenWorld = ScreenWorld.from(context)
        val timestamp = SystemClock.elapsedRealtimeNanos()
        if (currentPackage.isNotBlank()) {
            lastProfile = loadSavedProfile(currentPackage)
            AetherRuntimeBus.publishProfile(lastProfile)
        }

        val stats = NodeStats()
        root?.let { scanNode(it, 0, stats, screenWorld, currentPackage, timestamp) }

        if (stats.bestBounds.width() > 0 && stats.bestBounds.height() > 0) {
            updateTargetFromBounds(stats.bestBounds, stats.screenBounds)
        } else {
            val source = runCatching { event?.source }.getOrNull()
            if (source != null) {
                val bounds = Rect()
                source.getBoundsInScreen(bounds)
                updateTargetFromBounds(bounds, bounds)
                source.recycle()
            } else {
                val phase = (eventCount % 100) / 100.0f
                lastTargetX = 0.25f + phase * 0.50f
            }
        }

        lastConfidence = min(0.95f, 0.30f + min(20, stats.nodeCount) * 0.025f)
        lastRisk = max(0.02f, 0.24f - min(15, stats.clickableCount) * 0.01f)

        lastSnapshot = ObserverSnapshot(
            active = true,
            nodeCount = stats.nodeCount,
            clickableCount = stats.clickableCount,
            textNodeCount = stats.textNodeCount,
            visibleCount = stats.visibleCount,
            maxDepth = stats.maxDepth,
            lastPackage = currentPackage,
            lastClassName = event?.className?.toString().orEmpty(),
            lastEventType = event?.eventType ?: 0,
            timestampNanos = timestamp
        )
        AetherRuntimeBus.publishObserver(lastSnapshot)
        AetherRuntimeBus.publishWorld(deviceProfile, screenWorld, stats.anchors)
    }

    fun writeFrame(buffer: ByteBuffer, hudVisible: Boolean, aiActive: Boolean) {
        val decision = AetherRuntimeBus.decisionFlow.value
        val dx = lastTargetX - lastCueX
        val dy = lastTargetY - lastCueY
        val angle = dx.coerceIn(-1.0f, 1.0f)
        val rawPower = when (lastProfile.hudMode) {
            HudMode.MINIMAL -> (0.45f + abs(dy) * 0.45f).coerceIn(0.20f, 1.0f)
            HudMode.COMPACT -> (0.55f + abs(dy) * 0.60f).coerceIn(0.25f, 1.25f)
            HudMode.FULL -> (0.65f + abs(dy) * 0.75f).coerceIn(0.30f, 1.50f)
        }
        val privacy = AetherRuntimeBus.privacyGuardFlow.value
        val securityLock = AetherRuntimeBus.securityLockFlow.value
        val budget = AetherRuntimeBus.runtimeBudgetFlow.value
        val power = when {
            securityLock.mode == SecurityLockMode.HARD_LOCK -> rawPower.coerceAtMost(0.12f)
            budget.budgetMode == RuntimeBudgetMode.CRITICAL -> rawPower.coerceAtMost(0.16f)
            privacy.privacyMode == PrivacyMode.LOCKED -> rawPower.coerceAtMost(0.18f)
            privacy.privacyMode == PrivacyMode.SENSITIVE -> rawPower.coerceAtMost(0.30f)
            securityLock.mode == SecurityLockMode.SOFT_LOCK -> rawPower.coerceAtMost(0.34f)
            budget.budgetMode == RuntimeBudgetMode.THROTTLE -> rawPower.coerceAtMost(0.38f)
            decision.feedbackPolicyState == com.aether.renderer.FeedbackPolicyState.LOCKED_HOLD -> rawPower.coerceAtMost(0.28f)
            decision.feedbackPolicyState == com.aether.renderer.FeedbackPolicyState.CAUTION_HOLD -> rawPower.coerceAtMost(0.42f)
            decision.feedbackPolicyState == com.aether.renderer.FeedbackPolicyState.LEARNING_HOLD -> rawPower.coerceAtMost(0.36f)
            decision.policyV2State == PolicyV2State.LOCKED -> rawPower.coerceAtMost(0.32f)
            decision.policyV2State == PolicyV2State.CAUTION -> rawPower.coerceAtMost(0.50f)
            decision.policyMode == PolicyMode.SOFT_LOCK -> rawPower.coerceAtMost(0.40f)
            decision.safetyLevel == SafetyLevel.LOCKED -> rawPower.coerceAtMost(0.45f)
            decision.safetyLevel == SafetyLevel.CAUTION -> rawPower.coerceAtMost(0.85f)
            else -> rawPower
        }
        val observerBoost = if (lastSnapshot.active) 0.10f else 0.0f
        val flags = PhysicsStateBuffer.FLAG_TELEMETRY_ONLY or
            (if (hudVisible) PhysicsStateBuffer.FLAG_HUD_VISIBLE else 0) or
            (if (aiActive) PhysicsStateBuffer.FLAG_AI_ACTIVE else 0)

        PhysicsStateBuffer.write(
            buffer = buffer,
            flags = flags,
            cueX = lastCueX,
            cueY = lastCueY,
            targetX = lastTargetX,
            targetY = lastTargetY,
            angleOffset = angle,
            powerScale = power,
            velocityScale = 1.0f,
            errorMargin = 0.02f,
            confidenceBias = (max(lastConfidence, decision.confidence) + observerBoost + decision.learningConfidenceDelta).coerceIn(0.05f, 0.98f),
            riskBias = max(lastRisk, decision.risk + max(0.0f, decision.learningRiskDelta) + max(0.0f, decision.volatility * 0.04f) + privacy.sensitiveScore * 0.10f + budget.riskPressure * 0.04f).coerceIn(0.02f, 0.60f),
            cushionBounceCount = min(3, lastSnapshot.maxDepth / 5),
            timestampNanos = SystemClock.elapsedRealtimeNanos()
        )
    }

    fun loadSavedProfile(packageName: String): AppProfile {
        return com.aether.renderer.ProfileRules.load(context, packageName)
    }

    private fun scanNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        stats: NodeStats,
        screenWorld: ScreenWorld,
        packageName: String,
        timestampNanos: Long
    ) {
        stats.nodeCount++
        stats.maxDepth = max(stats.maxDepth, depth)
        if (node.isClickable) stats.clickableCount++
        if (node.isVisibleToUser) stats.visibleCount++
        if (!node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()) stats.textNodeCount++

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            if (stats.screenBounds.isEmpty) stats.screenBounds.set(bounds) else stats.screenBounds.union(bounds)
            val normalized = screenWorld.normalize(bounds)
            if (stats.anchors.size < 80 && (node.isClickable || node.isVisibleToUser || !node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank())) {
                stats.anchors.add(
                    NodeAnchor.from(
                        node = node,
                        packageName = packageName,
                        bounds = normalized,
                        depth = depth,
                        timestampNanos = timestampNanos,
                        allowTextHash = PrivacyGuard.allowTextHash(packageName)
                    )
                )
            }
            val score = bounds.width() * bounds.height() + if (node.isClickable) 100_000 else 0
            if (score > stats.bestScore) {
                stats.bestScore = score
                stats.bestBounds.set(bounds)
            }
        }

        val childCount = min(node.childCount, 64)
        for (i in 0 until childCount) {
            val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
            try {
                scanNode(child, depth + 1, stats, screenWorld, packageName, timestampNanos)
            } finally {
                child.recycle()
            }
        }
    }

    private fun updateTargetFromBounds(bounds: Rect, screenBounds: Rect) {
        val base = if (!screenBounds.isEmpty) screenBounds else bounds
        val width = max(1, base.width())
        val height = max(1, base.height())
        lastTargetX = ((bounds.centerX() - base.left).toFloat() / width.toFloat()).coerceIn(0.05f, 0.95f)
        lastTargetY = ((bounds.centerY() - base.top).toFloat() / height.toFloat()).coerceIn(0.05f, 0.95f)
        lastCueX = 0.50f
        lastCueY = 0.78f
    }

    private class NodeStats {
        var nodeCount = 0
        var clickableCount = 0
        var textNodeCount = 0
        var visibleCount = 0
        var maxDepth = 0
        var bestScore = -1
        val bestBounds = Rect()
        val screenBounds = Rect()
        val anchors = mutableListOf<NodeAnchor>()
    }
}
