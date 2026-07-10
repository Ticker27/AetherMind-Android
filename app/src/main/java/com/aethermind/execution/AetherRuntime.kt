package com.aethermind.execution

object AetherRuntime {
    private var dispatcher: AetherActionDispatcher? = null

    fun startForTargetPackage(targetPackage: String) {
        val executor = AccessibilityGestureExecutor(
            config = GestureExecutionConfig(
                coordinateSpace = CoordinateSpace.PIXELS,
                swipeDeltaXPx = 0f,
                swipeDeltaYPx = -500f,
                swipeDurationMs = 220L
            )
        )

        dispatcher = AetherActionDispatcher(
            targetPackage = targetPackage,
            executor = executor,
            config = DispatcherConfig(
                emptyQueueDelayMs = 2L,
                errorDelayMs = 20L,
                stopOnExecutorError = true
            )
        ).also { it.start() }
    }

    fun emergencyStop(reason: String = "MANUAL_EMERGENCY_STOP") {
        dispatcher?.stop(reason)
        dispatcher = null
    }
}
