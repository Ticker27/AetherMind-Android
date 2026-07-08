package com.aether.renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class PhysicsExperienceStateView(
    val layoutVersion: Int,
    val flags: Int,
    val cueX: Float,
    val cueY: Float,
    val targetX: Float,
    val targetY: Float,
    val angleOffset: Float,
    val powerScale: Float,
    val velocityScale: Float,
    val errorMargin: Float,
    val confidenceBias: Float,
    val riskBias: Float,
    val cushionBounceCount: Int,
    val timestampNanos: Long
)

object PhysicsExperienceStateReader {
    private const val OFFSET_LAYOUT_VERSION = 0
    private const val OFFSET_FLAGS = 4
    private const val OFFSET_CUE_X = 8
    private const val OFFSET_CUE_Y = 12
    private const val OFFSET_TARGET_X = 16
    private const val OFFSET_TARGET_Y = 20
    private const val OFFSET_ANGLE_OFFSET = 24
    private const val OFFSET_POWER_SCALE = 28
    private const val OFFSET_VELOCITY_SCALE = 32
    private const val OFFSET_ERROR_MARGIN = 36
    private const val OFFSET_CONFIDENCE_BIAS = 40
    private const val OFFSET_RISK_BIAS = 44
    private const val OFFSET_CUSHION_BOUNCE_COUNT = 48
    private const val OFFSET_TIMESTAMP_NANOS = 56

    fun read(buffer: ByteBuffer): PhysicsExperienceStateView {
        NativeTrajectoryBridge.verifyStateAbi()
        require(buffer.capacity() >= NativeTrajectoryBridge.stateSize) {
            "Aether state buffer capacity ${buffer.capacity()} is smaller than ${NativeTrajectoryBridge.stateSize}"
        }
        require(buffer.order() == ByteOrder.nativeOrder()) {
            "Aether state buffer must use native byte order"
        }

        buffer.position(0)

        val layoutVersion = buffer.getInt(OFFSET_LAYOUT_VERSION)
        require(layoutVersion == NativeTrajectoryBridge.stateLayoutVersion) {
            "Aether state layout mismatch in buffer: buffer=$layoutVersion expected=${NativeTrajectoryBridge.stateLayoutVersion}"
        }

        return PhysicsExperienceStateView(
            layoutVersion = layoutVersion,
            flags = buffer.getInt(OFFSET_FLAGS),
            cueX = buffer.getFloat(OFFSET_CUE_X),
            cueY = buffer.getFloat(OFFSET_CUE_Y),
            targetX = buffer.getFloat(OFFSET_TARGET_X),
            targetY = buffer.getFloat(OFFSET_TARGET_Y),
            angleOffset = buffer.getFloat(OFFSET_ANGLE_OFFSET),
            powerScale = buffer.getFloat(OFFSET_POWER_SCALE),
            velocityScale = buffer.getFloat(OFFSET_VELOCITY_SCALE),
            errorMargin = buffer.getFloat(OFFSET_ERROR_MARGIN),
            confidenceBias = buffer.getFloat(OFFSET_CONFIDENCE_BIAS),
            riskBias = buffer.getFloat(OFFSET_RISK_BIAS),
            cushionBounceCount = buffer.getInt(OFFSET_CUSHION_BOUNCE_COUNT),
            timestampNanos = buffer.getLong(OFFSET_TIMESTAMP_NANOS)
        )
    }
}
