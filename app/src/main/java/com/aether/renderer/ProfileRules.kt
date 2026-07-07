package com.aether.renderer

import android.content.Context

enum class ProfileSource {
    MANUAL,
    SAVED,
    RULE,
    FALLBACK
}

object ProfileRules {
    private const val PREFS = "aether_profiles"

    fun load(context: Context, packageName: String): AppProfile {
        if (packageName.isBlank()) return fallback(packageName)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedMode = prefs.getString("hud_mode_$packageName", null)
        val savedLabel = prefs.getString("label_$packageName", null)
        if (!savedMode.isNullOrBlank()) {
            val mode = runCatching { HudMode.valueOf(savedMode) }.getOrDefault(HudMode.COMPACT)
            return AppProfile(
                packageName = packageName,
                label = savedLabel ?: labelFromPackage(packageName),
                hudMode = mode,
                telemetryOnly = true,
                source = ProfileSource.SAVED,
                ruleTag = "saved"
            )
        }
        return ruleFor(packageName)
    }

    fun saveManual(context: Context, packageName: String, mode: HudMode): AppProfile {
        val label = labelFromPackage(packageName)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("hud_mode_$packageName", mode.name)
            .putString("label_$packageName", label)
            .putString("source_$packageName", ProfileSource.MANUAL.name)
            .apply()
        return AppProfile(
            packageName = packageName,
            label = label,
            hudMode = mode,
            telemetryOnly = true,
            source = ProfileSource.MANUAL,
            ruleTag = "manual"
        )
    }

    private fun ruleFor(packageName: String): AppProfile {
        val lower = packageName.lowercase()
        val mode = when {
            lower.contains("settings") -> HudMode.FULL
            lower.contains("chrome") || lower.contains("browser") -> HudMode.COMPACT
            lower.contains("launcher") || lower.contains("systemui") -> HudMode.MINIMAL
            lower.contains("game") -> HudMode.FULL
            else -> HudMode.COMPACT
        }
        val source = if (lower.isBlank()) ProfileSource.FALLBACK else ProfileSource.RULE
        return AppProfile(
            packageName = packageName,
            label = labelFromPackage(packageName),
            hudMode = mode,
            telemetryOnly = true,
            source = source,
            ruleTag = if (source == ProfileSource.RULE) "package_rule" else "fallback"
        )
    }

    fun fallback(packageName: String = ""): AppProfile = AppProfile(
        packageName = packageName,
        label = labelFromPackage(packageName).ifBlank { "SafeDefault" },
        hudMode = HudMode.COMPACT,
        telemetryOnly = true,
        source = ProfileSource.FALLBACK,
        ruleTag = "fallback"
    )

    fun labelFromPackage(packageName: String): String = packageName.substringAfterLast('.')
        .ifBlank { "Unknown" }
        .replace('-', '_')
        .replaceFirstChar { it.uppercase() }
}
