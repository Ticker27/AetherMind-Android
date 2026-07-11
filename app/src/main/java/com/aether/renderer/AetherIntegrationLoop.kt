package com.aether.renderer

import java.nio.ByteBuffer

object AetherIntegrationLoop {
    init {
        System.loadLibrary("aether_jni")
    }

    external fun nativeRunFrame(
        observerStateBuffer: ByteBuffer,
        outputStateBuffer: ByteBuffer,
        authorizationKey: String?
    ): Boolean

    external fun nativeOnKeyEvent(
        keyCode: Int,
        pressed: Boolean
    ): Boolean

    external fun nativeOnTouchEvent(
        action: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        eventTimeNanos: Long
    ): Boolean

    external fun nativeHudVisible(): Boolean
    external fun nativeAiActive(): Boolean
    external fun nativeLatestHudIntent(): Int

    /**
     * Native AI skill level used by the C++ planner/humanizer profile.
     * 0 = Beginner / Basic, 1 = Intermediate / Smart, 2 = Advanced / Pro.
     */
    external fun nativeSkillLevel(): Int

    /**
     * Updates the C++ runtime skill profile. Returns false only for an invalid
     * level or a native runtime failure. Execution remains locked elsewhere.
     */
    external fun nativeSetSkillLevel(level: Int): Boolean

    /**
     * Native auto-play switch. The UI must keep this disabled by default.
     * When enabled, Kotlin still applies package-scope and emergency-stop guards
     * before any Accessibility gesture is dispatched.
     */
    external fun nativeAutoPlayEnabled(): Boolean

    external fun nativeSetAutoPlayEnabled(enabled: Boolean): Boolean

    /**
     * Skill-adaptive auto-play policy exported by C++.
     * Basic = slower/softer, Smart = balanced, Pro = faster/stronger.
     */
    external fun nativeAutoPlayIntervalMs(): Int

    external fun nativeAutoPlaySwipePowerPx(): Float

    // AT167: brain-owned human-like cadence + gesture kinematics policy.
    external fun nativeHumanCadenceMs(difficulty: Float): Long
    external fun nativeHumanMotionProfile(difficulty: Float): String

    external fun nativeSetAccessibilitySnapshot(
        sequence: Long,
        packageName: String,
        className: String,
        nodeCount: Int,
        clickableCount: Int,
        textCount: Int,
        maxDepth: Int,
        eventType: Int,
        timestampMs: Long
    ): Boolean

    external fun nativeGetAccessibilityHandshakeStatus(): String

    external fun nativeGetNativeBridgeHealth(): String

    fun initializeAetherNative(): Boolean {
        val stateSize = NativeTrajectoryBridge.nativeStateSize()
        val layoutVersion = NativeTrajectoryBridge.nativeStateLayoutVersion()
        return stateSize == NativeTrajectoryBridge.PHYSICS_STATE_BYTES && layoutVersion > 0
    }

    fun runFrameStrict(
        observerStateBuffer: ByteBuffer,
        outputStateBuffer: ByteBuffer,
        authorizationKey: String?
    ): Boolean {
        if (!NativeTrajectoryBridge.isValidStateBuffer(observerStateBuffer)) return false
        if (!NativeTrajectoryBridge.isValidStateBuffer(outputStateBuffer)) return false
        return nativeRunFrame(observerStateBuffer, outputStateBuffer, authorizationKey)
    }
}
