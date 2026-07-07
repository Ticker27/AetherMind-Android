package com.aether.renderer

import kotlin.math.max
import kotlin.math.min

object ConfidenceSmoother {
    private var lastPackage: String = ""
    private var lastConfidence: Float = 0.0f
    private var lastRisk: Float = 0.0f

    fun smooth(packageName: String, confidence: Float, risk: Float): Pair<Float, Float> {
        val alpha = if (packageName == lastPackage) 0.35f else 0.70f
        val nextConfidence = blend(lastConfidence, confidence, alpha).coerceIn(0.05f, 0.95f)
        val nextRisk = blend(lastRisk, risk, alpha).coerceIn(0.02f, 0.60f)
        lastPackage = packageName
        lastConfidence = nextConfidence
        lastRisk = nextRisk
        return nextConfidence to nextRisk
    }

    private fun blend(old: Float, next: Float, alpha: Float): Float {
        val safeOld = if (old <= 0f) next else old
        return safeOld * (1f - alpha) + next * alpha
    }
}
