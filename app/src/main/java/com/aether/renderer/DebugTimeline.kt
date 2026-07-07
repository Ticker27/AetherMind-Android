package com.aether.renderer

data class DebugTimelineState(
    val eventCount: Int = 0,
    val lastEvent: String = "none",
    val lastPackage: String = "",
    val lastContext: String = "CTX_UNKNOWN",
    val lastSafety: String = "CAUTION",
    val timelineBadge: String = "TIMELINE_EMPTY",
    val sessionOnly: Boolean = true
) {
    val compactLine: String
        get() = "$timelineBadge n=$eventCount event=$lastEvent ctx=$lastContext safety=$lastSafety"
}

object DebugTimeline {
    private const val LIMIT = 24
    private val events = ArrayDeque<DebugTimelineState>()

    fun record(source: String, scene: SceneSnapshot, decision: RuntimeDecision, ux: ExplainableUxState): DebugTimelineState {
        val next = DebugTimelineState(
            eventCount = events.size + 1,
            lastEvent = source,
            lastPackage = scene.packageName.substringAfterLast('.'),
            lastContext = decision.contextLabel,
            lastSafety = decision.safetyBadge,
            timelineBadge = when {
                decision.sceneChanged -> "TIMELINE_SCENE_CHANGED"
                decision.policyV2State == PolicyV2State.LOCKED -> "TIMELINE_LOCKED"
                ux.headline.contains("learning", ignoreCase = true) -> "TIMELINE_LEARNING"
                else -> "TIMELINE_ACTIVE"
            },
            sessionOnly = true
        )
        events.addLast(next)
        while (events.size > LIMIT) events.removeFirst()
        return next.copy(eventCount = events.size)
    }

    fun clear(): DebugTimelineState {
        events.clear()
        return DebugTimelineState()
    }

    fun current(): DebugTimelineState = events.lastOrNull() ?: DebugTimelineState()
}
