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
