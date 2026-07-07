package com.aether.renderer

import android.content.Context

data class ProfileEditorState(
    val packageName: String = "",
    val label: String = "Unknown",
    val hudMode: HudMode = HudMode.COMPACT,
    val frozen: Boolean = false,
    val editorBadge: String = "PROFILE_EDIT_READY",
    val reason: String = "manual_profile_controls_ready"
) {
    val compactLine: String
        get() = "$editorBadge ${packageName.substringAfterLast('.')} mode=${hudMode.name} frozen=$frozen"
}

object ProfileEditor {
    private const val PREFS = "aether_profile_editor"

    fun current(context: Context?, profile: AppProfile): ProfileEditorState {
        val frozen = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.getBoolean("frozen_${profile.packageName}", false) ?: false
        return current(profile, frozen)
    }

    fun current(profile: AppProfile, frozen: Boolean = false): ProfileEditorState {
        return ProfileEditorState(
            packageName = profile.packageName,
            label = profile.label,
            hudMode = profile.hudMode,
            frozen = frozen,
            editorBadge = if (frozen) "PROFILE_FROZEN" else "PROFILE_EDIT_READY",
            reason = if (frozen) "manual_profile_freeze_enabled" else "manual_profile_controls_ready"
        )
    }

    fun freeze(context: Context, profile: AppProfile): ProfileEditorState {
        if (profile.packageName.isNotBlank()) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean("frozen_${profile.packageName}", true)
                .putString("frozen_mode_${profile.packageName}", profile.hudMode.name)
                .putString("frozen_label_${profile.packageName}", profile.label)
                .apply()
        }
        return current(profile, true)
    }

    fun reset(context: Context, profile: AppProfile): ProfileEditorState {
        if (profile.packageName.isNotBlank()) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove("frozen_${profile.packageName}")
                .remove("frozen_mode_${profile.packageName}")
                .remove("frozen_label_${profile.packageName}")
                .apply()
        }
        return current(profile, false)
    }
}
