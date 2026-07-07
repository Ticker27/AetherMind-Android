package com.aether.renderer

object IntentClassifier {
    fun classify(scene: SceneSnapshot, profile: AppProfile): RuntimeDecision {
        val interactionLoad = scene.clickableCount * 2 + scene.textNodeCount + scene.maxDepth
        val rawRisk = when {
            scene.nodeCount <= 0 -> 0.05f
            scene.clickableCount >= 24 -> 0.46f
            scene.clickableCount >= 20 -> 0.34f
            scene.textNodeCount >= 50 -> 0.28f
            else -> 0.10f + (scene.maxDepth.coerceAtMost(12) * 0.01f)
        }.coerceIn(0.02f, 0.60f)

        val rawConfidence = when {
            scene.nodeCount <= 0 -> 0.15f
            scene.visibleCount >= 20 && scene.clickableCount >= 4 -> 0.84f
            scene.visibleCount >= 8 -> 0.66f
            scene.nodeCount >= 4 -> 0.46f
            else -> 0.28f
        }.coerceIn(0.05f, 0.95f)

        val sceneLabel = when {
            scene.nodeCount <= 0 -> "IDLE"
            rawRisk >= 0.45f -> "RISK_DENSE_UI"
            interactionLoad >= 90 -> "COMPLEX_UI"
            scene.clickableCount >= 10 -> "ACTION_GRID"
            scene.textNodeCount >= 20 -> "TEXT_HEAVY"
            scene.maxDepth >= 8 -> "DEEP_TREE"
            else -> "GENERAL_UI"
        }

        val intentLabel = when (sceneLabel) {
            "RISK_DENSE_UI" -> "SAFETY_REVIEW"
            "COMPLEX_UI" -> "SAFETY_REVIEW"
            "ACTION_GRID" -> "CONTEXT_POSITIONING"
            "TEXT_HEAVY" -> "PROTECTIVE_REVIEW"
            "DEEP_TREE" -> "PROTECTIVE_REVIEW"
            "IDLE" -> "CONTEXT_POSITIONING"
            else -> "CONTEXT_POSITIONING"
        }

        val (confidence, smoothedRisk) = ConfidenceSmoother.smooth(scene.packageName, rawConfidence, rawRisk)
        val reason = "${scene.density}:${profile.source.name}:${profile.ruleTag}:${profile.label.ifBlank { scene.targetLabel }}"
        val base = RuntimeDecision(
            sceneLabel = sceneLabel,
            intentLabel = intentLabel,
            confidence = confidence,
            risk = smoothedRisk,
            hudMode = profile.hudMode,
            telemetryOnly = true,
            reason = reason
        )
        return SafetyGate.apply(scene, profile, base)
    }
}
