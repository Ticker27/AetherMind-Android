package com.aether.renderer

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var observerStateBuffer: java.nio.ByteBuffer
    private lateinit var outputStateBuffer: java.nio.ByteBuffer
    private val devKey: String? = null
    private val prefs by lazy { getSharedPreferences("aether_runtime", Context.MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var spinnerHudMode: Spinner
    private var lastBoundPackage: String = ""

    private val refreshLoop = object : Runnable {
        override fun run() {
            bindCurrentProfileToUi()
            updatePanelStatus(AetherRuntimeBus.telemetry)
            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!AetherIntegrationLoop.initializeAetherNative()) {
            findViewById<TextView>(R.id.tvCoreStatus).text = "Aether Core: ABI FAILED"
            return
        }

        observerStateBuffer = NativeTrajectoryBridge.newStateBuffer()
        outputStateBuffer = NativeTrajectoryBridge.newStateBuffer()
        spinnerHudMode = findViewById(R.id.spinnerHudMode)

        setupHudModeSpinner()
        bindButtons()
        updatePanelStatus(null)
        runFirstPermissionFlow()
    }

    override fun onResume() {
        super.onResume()
        setDashboardOverlayGate(true)
        handler.removeCallbacks(refreshLoop)
        handler.post(refreshLoop)
        syncPermissionButtons()
        if (hasAllRuntimePermissions()) startHudService()
    }

    override fun onPause() {
        handler.removeCallbacks(refreshLoop)
        setDashboardOverlayGate(false)
        super.onPause()
    }

    override fun onDestroy() {
        setDashboardOverlayGate(false)
        super.onDestroy()
    }

    private fun setDashboardOverlayGate(visible: Boolean) {
        val state = if (visible) DashboardOverlayGate.dashboardVisible() else DashboardOverlayGate.externalVisible()
        AetherRuntimeBus.publishDashboardOverlayGate(state)
        AetherOverlayService.instance?.updateHud(AetherRuntimeBus.telemetry)
    }

    private fun setupHudModeSpinner() {
        val modes = HudMode.entries.map { it.name }
        spinnerHudMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        spinnerHudMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val pkg = AetherRuntimeBus.packageFlow.value
                if (pkg.isBlank()) return
                val mode = HudMode.entries[position]
                saveProfile(pkg, mode)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindCurrentProfileToUi() {
        val pkg = AetherRuntimeBus.packageFlow.value
        if (pkg.isBlank()) return
        if (pkg != lastBoundPackage) {
            lastBoundPackage = pkg
            val profile = currentProfile(pkg)
            AetherRuntimeBus.publishProfile(profile)
            val pos = HudMode.entries.indexOf(profile.hudMode).coerceAtLeast(0)
            if (spinnerHudMode.selectedItemPosition != pos) spinnerHudMode.setSelection(pos, false)
        }
    }

    private fun currentProfile(packageName: String): AppProfile {
        return ProfileRules.load(this, packageName)
    }

    private fun saveProfile(packageName: String, mode: HudMode) {
        val profile = ProfileRules.saveManual(this, packageName, mode)
        AetherRuntimeBus.publishProfile(profile)
        AetherOverlayService.instance?.updateHud(AetherRuntimeBus.telemetry)
    }

    private fun bindButtons() {
        findViewById<Button>(R.id.btnOverlayPermission).setOnClickListener { requestOverlayPermission() }
        findViewById<Button>(R.id.btnAccessibilitySettings).setOnClickListener { openAccessibilitySettings() }
        findViewById<Button>(R.id.btnTestFrame).setOnClickListener { runTestNativeFrame() }

        findViewById<Button>(R.id.btnToggleHud).setOnClickListener {
            AetherIntegrationLoop.nativeOnKeyEvent(24, true)
            val visible = AetherIntegrationLoop.nativeHudVisible()
            if (visible) startHudService() else AetherOverlayService.instance?.updateHud(null)
            updatePanelStatus(AetherRuntimeBus.telemetry)
        }

        findViewById<Button>(R.id.btnToggleAi).setOnClickListener {
            val now = android.os.SystemClock.elapsedRealtimeNanos()
            AetherIntegrationLoop.nativeOnTouchEvent(0, 500f, 1000f, 1000f, 2000f, now)
            AetherIntegrationLoop.nativeOnTouchEvent(0, 500f, 1000f, 1000f, 2000f, now + 120_000_000L)
            runTestNativeFrame(showToast = false)
            updatePanelStatus(AetherRuntimeBus.telemetry)
        }

        findViewById<Button>(R.id.btnResetRuntimePrivacy).setOnClickListener { resetRuntimePrivacy() }
        findViewById<Button>(R.id.btnFreezeProfile).setOnClickListener { freezeCurrentProfile() }
        findViewById<Button>(R.id.btnExportSanitized).setOnClickListener { copySanitizedStatus() }
        findViewById<Button>(R.id.btnFeedbackHelpful).setOnClickListener { submitUxFeedback(UserFeedbackKind.HELPFUL) }
        findViewById<Button>(R.id.btnFeedbackCaution).setOnClickListener { submitUxFeedback(UserFeedbackKind.NEEDS_CAUTION) }
    }

    private fun runFirstPermissionFlow() {
        syncPermissionButtons()
        if (!Settings.canDrawOverlays(this) && !prefs.getBoolean("overlay_prompted", false)) {
            prefs.edit().putBoolean("overlay_prompted", true).apply()
            requestOverlayPermission()
            return
        }
        if (Settings.canDrawOverlays(this) && !isAccessibilityEnabled() && !prefs.getBoolean("accessibility_prompted", false)) {
            prefs.edit().putBoolean("accessibility_prompted", true).apply()
            openAccessibilitySettings()
            return
        }
        if (hasAllRuntimePermissions()) startHudService()
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            toast("Overlay already granted")
            startHudService()
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun startHudService() {
        if (Settings.canDrawOverlays(this)) {
            runCatching { startService(Intent(this, AetherOverlayService::class.java)) }
        }
    }

    private fun runTestNativeFrame(showToast: Boolean = true) {
        PhysicsStateBuffer.write(
            buffer = observerStateBuffer,
            flags = PhysicsStateBuffer.FLAG_TELEMETRY_ONLY or
                (if (AetherIntegrationLoop.nativeHudVisible()) PhysicsStateBuffer.FLAG_HUD_VISIBLE else 0) or
                (if (AetherIntegrationLoop.nativeAiActive()) PhysicsStateBuffer.FLAG_AI_ACTIVE else 0),
            cueX = 0.50f,
            cueY = 0.78f,
            targetX = 0.50f,
            targetY = 0.30f,
            angleOffset = 0.0f,
            powerScale = 0.85f,
            velocityScale = 1.0f,
            errorMargin = 0.02f,
            confidenceBias = 0.65f,
            riskBias = 0.10f,
            cushionBounceCount = 0,
            timestampNanos = android.os.SystemClock.elapsedRealtimeNanos()
        )
        val success = AetherIntegrationLoop.runFrameStrict(observerStateBuffer, outputStateBuffer, devKey)
        val telemetry = if (success) PhysicsStateBuffer.readTelemetry(outputStateBuffer).copy(telemetryOnly = true) else null
        AetherRuntimeBus.publishTelemetry(telemetry)
        updatePanelStatus(telemetry)
        startHudService()
        AetherOverlayService.instance?.updateHud(telemetry)
        if (showToast) toast(if (success) "Native frame OK" else "Native frame failed")
    }

    private fun updatePanelStatus(telemetry: RuntimeTelemetry?) {
        val snap = AetherRuntimeBus.observer
        val profile = AetherRuntimeBus.profileFlow.value
        val scene = AetherRuntimeBus.sceneFlow.value
        val decision = AetherRuntimeBus.decisionFlow.value
        val memory = AetherRuntimeBus.sceneMemoryFlow.value
        val session = AetherRuntimeBus.sessionMemoryFlow.value
        val screenType = AetherRuntimeBus.screenTypeFlow.value
        val anchorMemory = AetherRuntimeBus.anchorMemoryFlow.value
        val world = AetherRuntimeBus.screenWorldFlow.value
        val contextState = AetherRuntimeBus.contextFlow.value
        val behavior = AetherRuntimeBus.behaviorFlow.value
        val learning = AetherRuntimeBus.learningFlow.value
        val learningMemory = AetherRuntimeBus.learningMemoryFlow.value
        val trust = AetherRuntimeBus.trustModelFlow.value
        val feedback = AetherRuntimeBus.feedbackPolicyFlow.value
        val privacy = AetherRuntimeBus.privacyGuardFlow.value
        val boundary = PermissionBoundary.evaluate(
            overlayGranted = Settings.canDrawOverlays(this),
            accessibilityEnabled = isAccessibilityEnabled(),
            hudVisible = AetherIntegrationLoop.nativeHudVisible(),
            aiActive = AetherIntegrationLoop.nativeAiActive(),
            privacy = privacy
        )
        AetherRuntimeBus.publishPermissionBoundary(boundary)
        val security = AetherRuntimeBus.securityPrivacyFlow.value
        val audit = AetherRuntimeBus.auditTrailFlow.value
        val retention = AetherRuntimeBus.dataRetentionFlow.value
        val auditHardening = AetherRuntimeBus.auditHardeningFlow.value
        val boundaryV2 = AetherRuntimeBus.permissionBoundaryV2Flow.value
        val retentionReset = AetherRuntimeBus.retentionResetFlow.value
        val securityLock = AetherRuntimeBus.securityLockFlow.value
        val runtimeBudget = AetherRuntimeBus.runtimeBudgetFlow.value
        val explain = AetherRuntimeBus.explainableUxFlow.value
        val control = AetherRuntimeBus.userControlPanelFlow.value
        val editor = ProfileEditor.current(this, profile)
        AetherRuntimeBus.profileEditorFlow.set(editor)
        val timeline = AetherRuntimeBus.debugTimelineFlow.value
        val userFeedback = AetherRuntimeBus.userFeedbackFlow.value
        val phase8 = AetherRuntimeBus.phase8StableFlow.value
        val release = AetherRuntimeBus.releaseCandidateFlow.value
        val monitor = AetherRuntimeBus.runtimeMonitoringFlow.value
        val channel = AetherRuntimeBus.updateChannelFlow.value
        val rollback = AetherRuntimeBus.rollbackSystemFlow.value
        val maintenance = AetherRuntimeBus.maintenanceModeFlow.value
        val production = AetherRuntimeBus.productionLockFlow.value
        val hudGate = AetherRuntimeBus.dashboardOverlayGateFlow.value
        val strategic = AetherRuntimeBus.strategicReasoningFlow.value
        val pkgLabel = UiReadability.compactPackage(profile.packageName.ifBlank { snap.lastPackage })
        val ctxLabel = UiReadability.shortContext(contextState.contextLabel)
        findViewById<TextView>(R.id.tvCoreStatus).text = "Aether Core: ${production.badge} v11.5"
        findViewById<TextView>(R.id.tvAbiStatus).text = "ABI: ${NativeTrajectoryBridge.nativeStateSize()} bytes"
        findViewById<TextView>(R.id.tvOverlayStatus).text = "Overlay: ${if (boundary.overlayGranted) "GRANTED" else "MISSING"}"
        findViewById<TextView>(R.id.tvAccessibilityStatus).text = "Accessibility: ${if (boundary.accessibilityEnabled) "ENABLED" else "DISABLED"}"
        findViewById<TextView>(R.id.tvHudStatus).text = "HUD: ${if (boundary.hudVisible) "ON" else "OFF"} mode=${profile.hudMode.name} gate=${hudGate.badge}"
        findViewById<TextView>(R.id.tvAiStatus).text = "AI: ${if (boundary.aiActive) "ACTIVE" else "IDLE"} / ${decision.safetyBadge} / ${security.securityBadge}"
        findViewById<TextView>(R.id.tvObserverStatus).text = "Observer: ACTIVE nodes=${snap.nodeCount} click=${snap.clickableCount} text=${snap.textNodeCount} depth=${snap.maxDepth}"
        findViewById<TextView>(R.id.tvTargetAppStatus).text = "Target: $pkgLabel src=${profile.source.name} ${world.widthPx}x${world.heightPx}/${world.densityBucket}"
        findViewById<TextView>(R.id.tvSceneStatus).text = "Context: $ctxLabel c=${fmt(contextState.contextConfidence)} r=${fmt(contextState.contextRisk)}"
        findViewById<TextView>(R.id.tvLearningStatus).text = "Learning: ${learning.label} ${learningMemory.trend} trust=${fmt(learning.trustScore)} n=${learning.samples}"
        findViewById<TextView>(R.id.tvTrustStatus).text = "Trust: ${trust.label} score=${fmt(trust.trustScore)} vol=${fmt(trust.volatility)}"
        findViewById<TextView>(R.id.tvFeedbackStatus).text = "Feedback: ${feedback.label} n=${feedback.samples}"
        findViewById<TextView>(R.id.tvSecurityStatus).text = "Security: ${securityLock.badge} ${boundaryV2.badge} scope=${privacy.dataScope.name}"
        findViewById<TextView>(R.id.tvAuditStatus).text = "Audit: ${auditHardening.badge} retain=${retentionReset.badge} raw=${retention.rawTextStored}"
        findViewById<TextView>(R.id.tvPerformanceStatus).text = "Budget: ${runtimeBudget.badge} throttle=${runtimeBudget.throttleRecommended} monitor=${monitor.health}"
        findViewById<TextView>(R.id.tvExplainStatus).text = "Strategic: ${UiReadability.short(strategic.compactLine, 58)}"
        findViewById<TextView>(R.id.tvControlStatus).text = "Plan: ${UiReadability.short(strategic.explanation.compactLine, 58)}"
        findViewById<TextView>(R.id.tvProfileEditorStatus).text = "Profile: ${editor.compactLine}"
        findViewById<TextView>(R.id.tvDebugTimelineStatus).text = "Timeline: ${timeline.compactLine}"
        findViewById<TextView>(R.id.tvUserFeedbackStatus).text = "Skill: ${strategic.skill.compactLine} prod=${production.badge}"

        val intentLabel = UiReadability.dashboardIntent(telemetry, AetherIntegrationLoop.nativeLatestHudIntent())
        val detail = telemetry?.let {
            " ${decision.safetyBadge} ${runtimeBudget.badge} ctx=$ctxLabel conf=${fmt(it.confidenceBias)} risk=${fmt(decision.risk)} trend=${decision.sessionTrend}"
        } ?: " ${decision.safetyBadge}"
        findViewById<TextView>(R.id.tvIntentStatus).text = "Intent: $intentLabel$detail"

        findViewById<Button>(R.id.btnToggleHud).text = if (AetherIntegrationLoop.nativeHudVisible()) "STOP HUD" else "START HUD"
        findViewById<Button>(R.id.btnToggleAi).text = if (AetherIntegrationLoop.nativeAiActive()) "SET AI IDLE" else "SET AI ACTIVE"
        syncPermissionButtons()
    }

    private fun fmt(value: Float): String = String.format(Locale.US, "%.2f", value)

    private fun resetRuntimePrivacy() {
        AetherRuntimeBus.resetSecurityRuntimeState()
        AetherRuntimeBus.userControlPanelFlow.set(UserControlPanel.record("reset_runtime_privacy"))
        updatePanelStatus(AetherRuntimeBus.telemetry)
        toast("Runtime privacy/session state reset")
    }

    private fun freezeCurrentProfile() {
        val profile = AetherRuntimeBus.profileFlow.value
        if (profile.packageName.isBlank()) {
            toast("No target package to freeze")
            return
        }
        val editor = ProfileEditor.freeze(this, profile)
        AetherRuntimeBus.profileEditorFlow.set(editor)
        AetherRuntimeBus.userControlPanelFlow.set(UserControlPanel.record("freeze_profile"))
        updatePanelStatus(AetherRuntimeBus.telemetry)
        toast("Profile frozen: ${profile.packageName.substringAfterLast('.')}")
    }

    private fun copySanitizedStatus() {
        val sanitized = AetherRuntimeBus.explainableUxFlow.value.sanitizedStatus
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AetherMind sanitized status", sanitized))
        AetherRuntimeBus.userControlPanelFlow.set(UserControlPanel.record("copy_sanitized_status"))
        updatePanelStatus(AetherRuntimeBus.telemetry)
        toast("Sanitized status copied")
    }

    private fun submitUxFeedback(kind: UserFeedbackKind) {
        AetherRuntimeBus.publishUserFeedback(kind)
        AetherRuntimeBus.userControlPanelFlow.set(UserControlPanel.record("feedback_${kind.name.lowercase(Locale.US)}"))
        updatePanelStatus(AetherRuntimeBus.telemetry)
        toast("Feedback recorded: ${kind.name}")
    }

    private fun syncPermissionButtons() {
        findViewById<Button>(R.id.btnOverlayPermission).visibility =
            if (Settings.canDrawOverlays(this)) View.GONE else View.VISIBLE
        findViewById<Button>(R.id.btnAccessibilitySettings).visibility =
            if (isAccessibilityEnabled()) View.GONE else View.VISIBLE
    }

    private fun hasAllRuntimePermissions(): Boolean = Settings.canDrawOverlays(this) && isAccessibilityEnabled()

    private fun isAccessibilityEnabled(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expected = "$packageName/.accessibility.AetherAccessibilityService"
        return enabled.any { it.resolveInfo.serviceInfo.packageName == packageName || it.id == expected }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
