package com.aethermind.execution

object AetherRuntime {
    private var dispatcher: AetherActionDispatcher? = null
    private var activeTargetPackage: String? = null

    @Synchronized
    fun startForTargetPackage(targetPackage: String) {
        val normalizedTarget = targetPackage.trim()
        if (normalizedTarget.isBlank()) return

        if (dispatcher != null && activeTargetPackage == normalizedTarget) {
            return
        }

        dispatcher?.stop("TARGET_PACKAGE_CHANGED")
        dispatcher = null
        activeTargetPackage = normalizedTarget

        val executor = AccessibilityGestureExecutor(
            config = GestureExecutionConfig(
                coordinateSpace = CoordinateSpace.PIXELS,
                swipeDeltaXPx = 0f,
                swipeDeltaYPx = -500f,
                swipeDurationMs = 220L
            )
        )

        dispatcher = AetherActionDispatcher(
            targetPackage = normalizedTarget,
            executor = executor,
            config = DispatcherConfig(
                emptyQueueDelayMs = 2L,
                errorDelayMs = 20L,
                stopOnExecutorError = true
            )
        ).also { it.start() }
    }

    @Synchronized
    fun emergencyStop(reason: String = "MANUAL_EMERGENCY_STOP") {
        dispatcher?.stop(reason)
        dispatcher = null
        activeTargetPackage = null
    }

    @Synchronized
    fun isRunning(): Boolean = dispatcher != null

    @Synchronized
    fun targetPackage(): String? = activeTargetPackage
}
