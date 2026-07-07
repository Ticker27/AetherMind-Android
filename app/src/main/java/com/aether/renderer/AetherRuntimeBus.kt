package com.aether.renderer

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class RuntimeValue<T>(initial: T) {
    @Volatile
    var value: T = initial
        private set

    private val listeners = CopyOnWriteArrayList<(T) -> Unit>()

    fun set(next: T) {
        value = next
        listeners.forEach { it(next) }
    }

    fun subscribe(listener: (T) -> Unit) {
        listeners.add(listener)
        listener(value)
    }
}

enum class HudMode {
    MINIMAL,
    COMPACT,
    FULL
}

data class AppProfile(
    val packageName: String = "",
    val label: String = "Unknown",
    val hudMode: HudMode = HudMode.COMPACT,
    val telemetryOnly: Boolean = true,
    val source: ProfileSource = ProfileSource.FALLBACK,
    val ruleTag: String = "fallback"
)

data class ObserverSnapshot(
    val active: Boolean = false,
    val nodeCount: Int = 0,
    val clickableCount: Int = 0,
    val textNodeCount: Int = 0,
    val visibleCount: Int = 0,
    val maxDepth: Int = 0,
    val lastPackage: String = "",
    val lastClassName: String = "",
    val lastEventType: Int = 0,
    val timestampNanos: Long = 0L
) {
    val label: String
        get() = if (active) {
            "Observer: ACTIVE nodes=$nodeCount click=$clickableCount text=$textNodeCount depth=$maxDepth"
        } else {
            "Observer: IDLE"
        }
}

object AetherRuntimeBus {
    @Volatile var observer: ObserverSnapshot = ObserverSnapshot()
        private set

    @Volatile var telemetry: RuntimeTelemetry? = null
        private set

    val packageFlow = RuntimeValue("")
    val profileFlow = RuntimeValue(AppProfile())
    val sceneFlow = RuntimeValue(SceneSnapshot())
    val decisionFlow = RuntimeValue(RuntimeDecision())
    val sceneMemoryFlow = RuntimeValue(SceneMemoryState())
    val sessionMemoryFlow = RuntimeValue(SessionMemoryState())
    val deviceProfileFlow = RuntimeValue(DeviceProfile())
    val screenWorldFlow = RuntimeValue(ScreenWorld())
    val screenTypeFlow = RuntimeValue(ScreenTypeState())
    val anchorMemoryFlow = RuntimeValue(AnchorMemoryState())
    val contextFlow = RuntimeValue(ContextState())
    val behaviorFlow = RuntimeValue(BehaviorProfileState())
    val learningFlow = RuntimeValue(LearningState())
    val learningMemoryFlow = RuntimeValue(LearningMemoryState())
    val trustModelFlow = RuntimeValue(TrustModelState())
    val feedbackPolicyFlow = RuntimeValue(FeedbackPolicyStatus())
    val privacyGuardFlow = RuntimeValue(PrivacyGuardState())
    val auditTrailFlow = RuntimeValue(AuditTrailState())
    val permissionBoundaryFlow = RuntimeValue(PermissionBoundaryState())
    val dataRetentionFlow = RuntimeValue(DataRetentionState())
    val securityPrivacyFlow = RuntimeValue(SecurityPrivacyState())
    val auditHardeningFlow = RuntimeValue(AuditHardeningState())
    val permissionBoundaryV2Flow = RuntimeValue(PermissionBoundaryV2State())
    val retentionResetFlow = RuntimeValue(RetentionResetState())
    val securityLockFlow = RuntimeValue(SecurityLockState())
    val runtimeBudgetFlow = RuntimeValue(RuntimeBudgetState())
    val explainableUxFlow = RuntimeValue(ExplainableUxState())
    val userControlPanelFlow = RuntimeValue(UserControlPanelState())
    val profileEditorFlow = RuntimeValue(ProfileEditorState())
    val debugTimelineFlow = RuntimeValue(DebugTimelineState())
    val userFeedbackFlow = RuntimeValue(UserFeedbackState())
    val phase8StableFlow = RuntimeValue(Phase8StableState())
    val releaseCandidateFlow = RuntimeValue(ReleaseCandidate.current())
    val runtimeMonitoringFlow = RuntimeValue(RuntimeMonitoringState())
    val updateChannelFlow = RuntimeValue(UpdateChannel.current())
    val rollbackSystemFlow = RuntimeValue(RollbackSystem.current())
    val maintenanceModeFlow = RuntimeValue(MaintenanceMode.current())
    val productionLockFlow = RuntimeValue(ProductionLock.current())
    val dashboardOverlayGateFlow = RuntimeValue(DashboardOverlayGate.externalVisible())
    val strategicReasoningFlow = RuntimeValue(StrategicReasoningState())

    private val frameCounter = AtomicLong(0L)

    fun publishObserver(snapshot: ObserverSnapshot) {
        observer = snapshot
        if (snapshot.lastPackage.isNotBlank() && snapshot.lastPackage != packageFlow.value) {
            packageFlow.set(snapshot.lastPackage)
        }
        val scene = SceneSnapshot(
            packageName = snapshot.lastPackage,
            className = snapshot.lastClassName,
            eventType = snapshot.lastEventType,
            nodeCount = snapshot.nodeCount,
            clickableCount = snapshot.clickableCount,
            textNodeCount = snapshot.textNodeCount,
            visibleCount = snapshot.visibleCount,
            maxDepth = snapshot.maxDepth,
            timestampNanos = snapshot.timestampNanos
        )
        sceneFlow.set(scene)
        val decision = buildDecision(scene, profileFlow.value)
        decisionFlow.set(decision)
        publishSecurityState(scene, profileFlow.value, decision, "OBSERVER")
        publishDerivedState(scene.packageName)
    }

    fun publishWorld(device: DeviceProfile, world: ScreenWorld, anchors: List<NodeAnchor>) {
        deviceProfileFlow.set(device)
        screenWorldFlow.set(world)
        val scene = sceneFlow.value
        val screenType = ScreenTypeModel.classify(scene, anchors, world)
        screenTypeFlow.set(screenType)
        val anchorMemory = AnchorMemory.update(scene.packageName, anchors, screenType.type)
        anchorMemoryFlow.set(anchorMemory)
        if (scene.packageName.isNotBlank()) {
            val decision = buildDecision(scene, profileFlow.value)
            decisionFlow.set(decision)
            publishSecurityState(scene, profileFlow.value, decision, "WORLD")
            publishDerivedState(scene.packageName)
        }
    }

    fun publishTelemetry(value: RuntimeTelemetry?) {
        telemetry = value
        if (value != null) frameCounter.incrementAndGet()
        runtimeBudgetFlow.set(RuntimeBudget.record(value, decisionFlow.value, securityLockFlow.value))
        releaseCandidateFlow.set(ReleaseCandidate.current())
        updateChannelFlow.set(UpdateChannel.current())
        rollbackSystemFlow.set(RollbackSystem.current())
        maintenanceModeFlow.set(MaintenanceMode.current())
        productionLockFlow.set(ProductionLock.current())
        runtimeMonitoringFlow.set(RuntimeMonitoring.record(value, frameCounter.get(), runtimeBudgetFlow.value, productionLockFlow.value))
        publishUxState(if (value != null) "TELEMETRY" else "TELEMETRY_NULL")
    }

    fun publishProfile(profile: AppProfile) {
        if (profile.packageName.isNotBlank()) packageFlow.set(profile.packageName)
        profileFlow.set(profile)
        val scene = sceneFlow.value
        val decision = buildDecision(scene, profile)
        decisionFlow.set(decision)
        publishSecurityState(scene, profile, decision, "PROFILE")
        publishDerivedState(scene.packageName)
    }

    fun publishPermissionBoundary(boundary: PermissionBoundaryState) {
        val privacy = privacyGuardFlow.value
        val adjusted = PermissionBoundary.evaluate(
            overlayGranted = boundary.overlayGranted,
            accessibilityEnabled = boundary.accessibilityEnabled,
            hudVisible = boundary.hudVisible,
            aiActive = boundary.aiActive,
            privacy = privacy
        )
        permissionBoundaryFlow.set(adjusted)
        val state = securityPrivacyFlow.value.copy(boundary = adjusted)
        securityPrivacyFlow.set(state)
        val auditHardening = AuditHardening.evaluate("BOUNDARY", privacy, adjusted)
        val boundaryV2 = PermissionBoundaryV2.evaluate(privacy, adjusted, auditHardening)
        val retentionReset = RetentionResetControls.current(dataRetentionFlow.value)
        val securityLock = SecurityLock.evaluate(state, boundaryV2, retentionReset)
        auditHardeningFlow.set(auditHardening)
        permissionBoundaryV2Flow.set(boundaryV2)
        retentionResetFlow.set(retentionReset)
        securityLockFlow.set(securityLock)
        publishUxState("BOUNDARY")
    }

    fun resetSecurityRuntimeState() {
        val security = DataRetention.resetRuntimeSecurityState()
        privacyGuardFlow.set(security.privacy)
        auditTrailFlow.set(security.audit)
        permissionBoundaryFlow.set(security.boundary)
        dataRetentionFlow.set(security.retention)
        securityPrivacyFlow.set(security)
        val auditHardening = AuditHardening.clear()
        val boundaryV2 = PermissionBoundaryV2.evaluate(security.privacy, security.boundary, auditHardening)
        val retentionReset = RetentionResetControls.markReset()
        val securityLock = SecurityLock.evaluate(security, boundaryV2, retentionReset)
        auditHardeningFlow.set(auditHardening)
        permissionBoundaryV2Flow.set(boundaryV2)
        retentionResetFlow.set(retentionReset)
        securityLockFlow.set(securityLock)
        debugTimelineFlow.set(DebugTimeline.clear())
        userFeedbackFlow.set(UserFeedbackSystem.reset())
        publishUxState("RESET_SECURITY")
    }

    private fun publishSecurityState(
        scene: SceneSnapshot,
        profile: AppProfile,
        decision: RuntimeDecision,
        eventName: String
    ) {
        val privacy = PrivacyGuard.evaluate(scene, profile, decision)
        val currentBoundary = permissionBoundaryFlow.value
        val boundary = PermissionBoundary.evaluate(
            overlayGranted = currentBoundary.overlayGranted,
            accessibilityEnabled = currentBoundary.accessibilityEnabled,
            hudVisible = currentBoundary.hudVisible,
            aiActive = currentBoundary.aiActive,
            privacy = privacy
        )
        val audit = AuditTrail.record(eventName, scene.packageName.ifBlank { profile.packageName }, privacy, decision)
        val retention = DataRetention.current(privacy)
        val security = SecurityPrivacyState(
            privacy = privacy,
            audit = audit,
            boundary = boundary,
            retention = retention
        )
        val auditHardening = AuditHardening.evaluate(eventName, privacy, boundary)
        val boundaryV2 = PermissionBoundaryV2.evaluate(privacy, boundary, auditHardening)
        val retentionReset = RetentionResetControls.current(retention)
        val securityLock = SecurityLock.evaluate(security, boundaryV2, retentionReset)
        privacyGuardFlow.set(privacy)
        auditTrailFlow.set(audit)
        permissionBoundaryFlow.set(boundary)
        dataRetentionFlow.set(retention)
        securityPrivacyFlow.set(security)
        auditHardeningFlow.set(auditHardening)
        permissionBoundaryV2Flow.set(boundaryV2)
        retentionResetFlow.set(retentionReset)
        securityLockFlow.set(securityLock)
        runtimeBudgetFlow.set(RuntimeBudget.record(null, decision, securityLock))
        publishUxState(eventName)
    }

    private fun buildDecision(scene: SceneSnapshot, profile: AppProfile): RuntimeDecision {
        val base = IntentClassifier.classify(scene, profile)
        val action = ActionIntentModel.refine(scene, profile, base)
        val withScreen = ScreenTypeModel.enrich(action, screenTypeFlow.value, anchorMemoryFlow.value)
        val withSceneMemory = SceneMemory.enrich(scene, withScreen)
        val sessionState = SessionMemory.current(scene.packageName) ?: SessionMemoryState()
        val sceneState = SceneMemory.current(scene.packageName) ?: SceneMemoryState()
        val context = ContextEngine.update(
            scene = scene,
            decision = withSceneMemory,
            screen = screenTypeFlow.value,
            anchorMemory = anchorMemoryFlow.value,
            sceneMemory = sceneState,
            session = sessionState
        )
        val withContext = ContextEngine.enrich(withSceneMemory, context)
        val behavior = BehaviorProfile.update(scene, withContext, context)
        val withBehavior = BehaviorProfile.enrich(withContext, behavior)
        val adaptive = AdaptivePolicy.apply(scene, profile, withBehavior)
        val policyV2 = PolicyV2.apply(scene, profile, adaptive)
        val withSession = SessionMemory.enrich(scene, policyV2)
        val withLearning = LearningLoop.enrich(scene, withSession)
        val withLearningMemory = LearningMemory.enrich(scene, withLearning)
        val withTrust = TrustModel.enrich(scene, withLearningMemory)
        return FeedbackPolicy.apply(scene, profile, withTrust)
    }

    private fun publishDerivedState(packageName: String) {
        val key = packageName.ifBlank { "unknown" }
        learningFlow.set(LearningLoop.current(key) ?: LearningState())
        learningMemoryFlow.set(LearningMemory.current(key) ?: LearningMemoryState())
        trustModelFlow.set(TrustModel.current(key) ?: TrustModelState())
        feedbackPolicyFlow.set(FeedbackPolicy.current(key) ?: FeedbackPolicyStatus())
        sceneMemoryFlow.set(SceneMemory.current(key) ?: SceneMemoryState())
        sessionMemoryFlow.set(SessionMemory.current(key) ?: SessionMemoryState())
        contextFlow.set(ContextEngine.current(key) ?: ContextState())
        behaviorFlow.set(BehaviorProfile.current(key) ?: BehaviorProfileState())
        userFeedbackFlow.set(UserFeedbackSystem.current())
        publishUxState("DERIVED")
    }

    fun publishUserFeedback(kind: UserFeedbackKind) {
        userFeedbackFlow.set(UserFeedbackSystem.submit(kind))
        publishUxState("USER_FEEDBACK_${kind.name}")
    }

    fun publishDashboardOverlayGate(state: DashboardOverlayGateState) {
        dashboardOverlayGateFlow.set(state)
        publishUxState("DASHBOARD_OVERLAY_GATE_${state.badge}")
    }

    fun clearDebugTimeline() {
        debugTimelineFlow.set(DebugTimeline.clear())
        publishUxState("DEBUG_CLEAR")
    }

    private fun publishUxState(source: String) {
        val scene = sceneFlow.value
        val profile = profileFlow.value
        val decision = decisionFlow.value
        val explanation = ExplainableUx.explain(
            scene = scene,
            profile = profile,
            decision = decision,
            security = securityPrivacyFlow.value,
            lock = securityLockFlow.value,
            budget = runtimeBudgetFlow.value,
            privacy = privacyGuardFlow.value
        )
        explainableUxFlow.set(explanation)
        userControlPanelFlow.set(UserControlPanel.current(securityLockFlow.value, retentionResetFlow.value))
        profileEditorFlow.set(ProfileEditor.current(profile))
        debugTimelineFlow.set(DebugTimeline.record(source, scene, decision, explanation))
        userFeedbackFlow.set(UserFeedbackSystem.current())
        phase8StableFlow.set(Phase8Stable.current())
        strategicReasoningFlow.set(
            StrategicReasoningCore.evaluate(
                scene = scene,
                observer = observer,
                decision = decision,
                trust = trustModelFlow.value,
                privacy = privacyGuardFlow.value,
                security = securityLockFlow.value,
                budget = runtimeBudgetFlow.value
            )
        )
    }

    fun frameCount(): Long = frameCounter.get()
}
