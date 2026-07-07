package com.aether.renderer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class AetherOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var hudView: View
    private lateinit var hudRoot: LinearLayout
    private lateinit var hudIntentText: TextView
    private lateinit var hudDetailText: TextView
    private lateinit var hudObserverText: TextView
    private lateinit var hudParams: WindowManager.LayoutParams
    private var viewAttached = false
    private var lastPlacementKey: String = ""

    companion object {
        var instance: AetherOverlayService? = null
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        attachHud()
    }

    override fun onDestroy() {
        detachHud()
        instance = null
        super.onDestroy()
    }

    fun updateHud(telemetry: RuntimeTelemetry?) {
        if (!::hudRoot.isInitialized) return
        if (telemetry == null || !telemetry.hudVisible) {
            hudRoot.visibility = View.GONE
            return
        }

        val gate = AetherRuntimeBus.dashboardOverlayGateFlow.value
        if (gate.hideFloatingHud) {
            hudRoot.visibility = View.GONE
            return
        }

        val profile = AetherRuntimeBus.profileFlow.value
        val observer = AetherRuntimeBus.observer
        val decision = AetherRuntimeBus.decisionFlow.value
        val privacy = AetherRuntimeBus.privacyGuardFlow.value
        val securityLock = AetherRuntimeBus.securityLockFlow.value
        val runtimeBudget = AetherRuntimeBus.runtimeBudgetFlow.value
        val explain = AetherRuntimeBus.explainableUxFlow.value
        val userFeedback = AetherRuntimeBus.userFeedbackFlow.value
        val production = AetherRuntimeBus.productionLockFlow.value
        val monitor = AetherRuntimeBus.runtimeMonitoringFlow.value
        val mode = if (gate.forceExternalCompact && profile.hudMode == HudMode.FULL) HudMode.COMPACT else profile.hudMode
        applyHudPlacement(mode, false)

        hudRoot.visibility = View.VISIBLE
        val color = when {
            privacy.privacyMode == PrivacyMode.LOCKED -> "#E91E63"
            privacy.privacyMode == PrivacyMode.SENSITIVE -> "#FF7043"
            decision.safetyLevel == SafetyLevel.LOCKED -> "#F44336"
            decision.safetyLevel == SafetyLevel.CAUTION -> "#FFC107"
            else -> "#40D290"
        }

        val title = UiReadability.short(profile.label.ifBlank { "AETHER" }, 12)
        val intent = UiReadability.actionIntentLabel(decision.actionIntent)
        val context = UiReadability.short(UiReadability.shortContext(decision.contextLabel), 18)
        hudIntentText.text = when (mode) {
            HudMode.MINIMAL -> HudDensityPatch.minimalIntent(decision)
            else -> "$title: $intent ${decision.safetyBadge}"
        }
        hudIntentText.setTextColor(Color.parseColor(color))

        when (mode) {
            HudMode.MINIMAL -> {
                hudDetailText.text = HudDensityPatch.minimalDetail(decision)
                hudObserverText.visibility = View.GONE
            }
            HudMode.COMPACT -> {
                hudDetailText.text = HudDensityPatch.compactDetail(telemetry, decision, runtimeBudget)
                hudObserverText.visibility = View.VISIBLE
                hudObserverText.text = HudDensityPatch.compactObserver(observer, privacy, explain)
            }
            HudMode.FULL -> {
                hudDetailText.text = "$context / ${decision.policyV2State.name} / ${decision.trustLabel}"
                hudObserverText.visibility = View.VISIBLE
                hudObserverText.text = buildString {
                    append("pkg=${UiReadability.compactPackage(profile.packageName)} obs=${observer.nodeCount}/${observer.clickableCount}/${observer.textNodeCount}\n")
                    append("safe=${decision.safetyBadge} privacy=${privacy.privacyBadge} budget=${runtimeBudget.badge}\n")
                    append("monitor=${UiReadability.short(monitor.compactLine, 42)}\n")
                    append("why=${UiReadability.short(explain.primaryReason, 46)}\n")
                    append("gate=${gate.reason}")
                }
            }
        }
    }

    private fun attachHud() {
        if (viewAttached) return
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        hudView = LayoutInflater.from(this).inflate(R.layout.layout_aether_hud, null)
        hudRoot = hudView.findViewById(R.id.hudRoot)
        hudIntentText = hudView.findViewById(R.id.hudIntentText)
        hudDetailText = hudView.findViewById(R.id.hudDetailText)
        hudObserverText = hudView.findViewById(R.id.hudObserverText)
        hudRoot.visibility = View.GONE
        clamp(hudIntentText, 1)
        clamp(hudDetailText, 1)
        clamp(hudObserverText, 4)

        hudParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 12
            y = 96
        }

        runCatching {
            windowManager.addView(hudView, hudParams)
            viewAttached = true
        }.onFailure {
            stopSelf()
        }
    }


    private fun applyHudPlacement(mode: HudMode, dashboardSafe: Boolean) {
        if (!viewAttached || !::hudParams.isInitialized || !::windowManager.isInitialized) return
        val key = "${mode.name}:$dashboardSafe"
        if (key == lastPlacementKey) return

        hudParams.gravity = if (dashboardSafe) {
            Gravity.BOTTOM or Gravity.END
        } else {
            Gravity.TOP or Gravity.END
        }
        hudParams.x = 12
        hudParams.y = when {
            dashboardSafe -> 112
            mode == HudMode.MINIMAL -> 88
            else -> 104
        }
        lastPlacementKey = key
        runCatching { windowManager.updateViewLayout(hudView, hudParams) }
    }

    private fun clamp(view: TextView, lines: Int) {
        view.maxLines = lines
        view.ellipsize = TextUtils.TruncateAt.END
    }

    private fun detachHud() {
        if (viewAttached && ::windowManager.isInitialized && ::hudView.isInitialized) {
            runCatching { windowManager.removeView(hudView) }
        }
        viewAttached = false
    }

    private fun fmt(value: Float): String = String.format(Locale.US, "%.2f", value)
}
