package com.aether.renderer

enum class PermissionBoundaryMode {
    READY,
    LIMITED,
    WAITING,
    LOCKED
}

data class PermissionBoundaryState(
    val overlayGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val hudVisible: Boolean = false,
    val aiActive: Boolean = false,
    val mode: PermissionBoundaryMode = PermissionBoundaryMode.WAITING,
    val boundaryBadge: String = "PERMISSION_WAIT",
    val reason: String = "permissions_missing"
) {
    val compactLine: String
        get() = "$boundaryBadge overlay=$overlayGranted access=$accessibilityEnabled hud=$hudVisible ai=$aiActive"
}

object PermissionBoundary {
    fun evaluate(
        overlayGranted: Boolean,
        accessibilityEnabled: Boolean,
        hudVisible: Boolean,
        aiActive: Boolean,
        privacy: PrivacyGuardState = PrivacyGuardState()
    ): PermissionBoundaryState {
        val mode = when {
            privacy.privacyMode == PrivacyMode.LOCKED -> PermissionBoundaryMode.LOCKED
            overlayGranted && accessibilityEnabled -> PermissionBoundaryMode.READY
            overlayGranted || accessibilityEnabled -> PermissionBoundaryMode.LIMITED
            else -> PermissionBoundaryMode.WAITING
        }
        val badge = when (mode) {
            PermissionBoundaryMode.READY -> "BOUNDARY_READY"
            PermissionBoundaryMode.LIMITED -> "BOUNDARY_LIMITED"
            PermissionBoundaryMode.WAITING -> "PERMISSION_WAIT"
            PermissionBoundaryMode.LOCKED -> "PRIVACY_LOCKED"
        }
        val reason = when (mode) {
            PermissionBoundaryMode.READY -> "runtime_permissions_ready"
            PermissionBoundaryMode.LIMITED -> "partial_permission_boundary"
            PermissionBoundaryMode.WAITING -> "permissions_missing"
            PermissionBoundaryMode.LOCKED -> "privacy_guard_locked"
        }
        return PermissionBoundaryState(
            overlayGranted = overlayGranted,
            accessibilityEnabled = accessibilityEnabled,
            hudVisible = hudVisible,
            aiActive = aiActive,
            mode = mode,
            boundaryBadge = badge,
            reason = reason
        )
    }
}
