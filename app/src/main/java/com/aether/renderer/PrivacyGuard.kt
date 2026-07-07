package com.aether.renderer

import java.util.Locale
import kotlin.math.max

enum class PrivacyMode {
    STANDARD,
    SENSITIVE,
    LOCKED
}

enum class DataScope {
    HASHED_ANCHORS_ONLY,
    BOUNDS_ONLY,
    METADATA_ONLY
}

data class PrivacyGuardState(
    val privacyMode: PrivacyMode = PrivacyMode.STANDARD,
    val dataScope: DataScope = DataScope.HASHED_ANCHORS_ONLY,
    val privacyBadge: String = "PRIVACY_OK",
    val rawTextStored: Boolean = false,
    val rawContentStored: Boolean = false,
    val externalExportAllowed: Boolean = false,
    val retentionPolicy: String = "SESSION_ONLY",
    val packageHash: Int = 0,
    val sensitiveScore: Float = 0.0f,
    val reason: String = "standard_scope"
) {
    val compactLine: String
        get() = "$privacyBadge/${dataScope.name} rawText=$rawTextStored retain=$retentionPolicy"
}

object PrivacyGuard {
    private val sensitiveTokens = listOf(
        "bank",
        "wallet",
        "pay",
        "auth",
        "password",
        "passcode",
        "otp",
        "secure",
        "permission",
        "settings",
        "account",
        "login"
    )

    fun evaluate(scene: SceneSnapshot, profile: AppProfile, decision: RuntimeDecision): PrivacyGuardState {
        val packageName = scene.packageName.ifBlank { profile.packageName }
        val score = sensitiveScore(packageName, scene, decision)
        val mode = when {
            score >= 0.72f || decision.feedbackPolicyState.name.contains("LOCKED") -> PrivacyMode.LOCKED
            score >= 0.36f -> PrivacyMode.SENSITIVE
            else -> PrivacyMode.STANDARD
        }
        val scope = when (mode) {
            PrivacyMode.STANDARD -> DataScope.HASHED_ANCHORS_ONLY
            PrivacyMode.SENSITIVE -> DataScope.BOUNDS_ONLY
            PrivacyMode.LOCKED -> DataScope.METADATA_ONLY
        }
        val badge = when (mode) {
            PrivacyMode.STANDARD -> "PRIVACY_OK"
            PrivacyMode.SENSITIVE -> "PRIVACY_GUARD"
            PrivacyMode.LOCKED -> "PRIVACY_LOCK"
        }
        val reason = when (mode) {
            PrivacyMode.STANDARD -> "hashed_anchor_scope"
            PrivacyMode.SENSITIVE -> "sensitive_package_or_screen"
            PrivacyMode.LOCKED -> "sensitive_lock_or_policy_lock"
        }
        return PrivacyGuardState(
            privacyMode = mode,
            dataScope = scope,
            privacyBadge = badge,
            rawTextStored = false,
            rawContentStored = false,
            externalExportAllowed = false,
            retentionPolicy = "SESSION_ONLY",
            packageHash = packageName.hashCode(),
            sensitiveScore = score,
            reason = reason
        )
    }

    fun allowTextHash(packageName: String): Boolean {
        return sensitiveScore(packageName) < 0.36f
    }

    private fun sensitiveScore(packageName: String, scene: SceneSnapshot? = null, decision: RuntimeDecision? = null): Float {
        val lower = packageName.lowercase(Locale.US)
        var score = 0.0f
        if (sensitiveTokens.any { lower.contains(it) }) score += 0.45f
        if (scene?.className?.lowercase(Locale.US)?.contains("password") == true) score += 0.30f
        if (decision?.screenType?.name?.contains("CONFIRM") == true) score += 0.18f
        if (decision?.screenType?.name?.contains("FORM") == true && lower.contains("account")) score += 0.20f
        if ((decision?.risk ?: 0.0f) >= 0.45f) score += 0.10f
        if ((decision?.uncertainty ?: 0.0f) >= 0.80f) score += 0.06f
        return max(0.0f, score).coerceIn(0.0f, 1.0f)
    }
}
