package com.aether.renderer

data class DashboardOverlayGateState(
    val dashboardVisible: Boolean = false,
    val hideFloatingHud: Boolean = false,
    val forceExternalCompact: Boolean = false,
    val reason: String = "external_runtime"
) {
    val badge: String
        get() = if (dashboardVisible) "DASHBOARD_HUD_HIDDEN" else "EXTERNAL_HUD_VISIBLE"
}

object DashboardOverlayGate {
    const val LABEL = "AT113_115_DASHBOARD_HUD_HARD_LOCK"

    fun dashboardVisible(): DashboardOverlayGateState = DashboardOverlayGateState(
        dashboardVisible = true,
        hideFloatingHud = true,
        forceExternalCompact = false,
        reason = "dashboard_owns_status"
    )

    fun externalVisible(): DashboardOverlayGateState = DashboardOverlayGateState(
        dashboardVisible = false,
        hideFloatingHud = false,
        forceExternalCompact = true,
        reason = "external_overlay_allowed"
    )
}
