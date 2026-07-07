package com.aether.renderer

import java.nio.ByteBuffer

object PhysicsStateBuffer {
    const val LAYOUT_VERSION: Int = 1

    const val OFFSET_LAYOUT_VERSION = 0
    const val OFFSET_FLAGS = 4
    const val OFFSET_CUE_X = 8
    const val OFFSET_CUE_Y = 12
    const val OFFSET_TARGET_X = 16
    const val OFFSET_TARGET_Y = 20
    const val OFFSET_ANGLE_OFFSET = 24
    const val OFFSET_POWER_SCALE = 28
    const val OFFSET_VELOCITY_SCALE = 32
    const val OFFSET_ERROR_MARGIN = 36
    const val OFFSET_CONFIDENCE_BIAS = 40
    const val OFFSET_RISK_BIAS = 44
    const val OFFSET_CUSHION_BOUNCE_COUNT = 48
    const val OFFSET_RESERVED0 = 52
    const val OFFSET_TIMESTAMP_NANOS = 56

    const val FLAG_TELEMETRY_ONLY: Int = 0x00001000
    const val FLAG_AI_ACTIVE: Int = 0x00002000
    const val FLAG_HUD_VISIBLE: Int = 0x00004000
    const val FLAG_INTENT_MASK: Int = 0x03000000
    const val FLAG_INTENT_SHIFT: Int = 24

    fun write(
        buffer: ByteBuffer,
        flags: Int,
        cueX: Float,
        cueY: Float,
        targetX: Float,
        targetY: Float,
        angleOffset: Float,
        powerScale: Float,
        velocityScale: Float,
        errorMargin: Float,
        confidenceBias: Float,
        riskBias: Float,
        cushionBounceCount: Int,
        timestampNanos: Long
    ) {
        buffer.clear()
        buffer.putInt(OFFSET_LAYOUT_VERSION, LAYOUT_VERSION)
        buffer.putInt(OFFSET_FLAGS, flags)
        buffer.putFloat(OFFSET_CUE_X, cueX)
        buffer.putFloat(OFFSET_CUE_Y, cueY)
        buffer.putFloat(OFFSET_TARGET_X, targetX)
        buffer.putFloat(OFFSET_TARGET_Y, targetY)
        buffer.putFloat(OFFSET_ANGLE_OFFSET, angleOffset)
        buffer.putFloat(OFFSET_POWER_SCALE, powerScale)
        buffer.putFloat(OFFSET_VELOCITY_SCALE, velocityScale)
        buffer.putFloat(OFFSET_ERROR_MARGIN, errorMargin)
        buffer.putFloat(OFFSET_CONFIDENCE_BIAS, confidenceBias)
        buffer.putFloat(OFFSET_RISK_BIAS, riskBias)
        buffer.putInt(OFFSET_CUSHION_BOUNCE_COUNT, cushionBounceCount)
        buffer.putInt(OFFSET_RESERVED0, 0)
        buffer.putLong(OFFSET_TIMESTAMP_NANOS, timestampNanos)
    }

    fun readTelemetry(buffer: ByteBuffer): RuntimeTelemetry {
        val flags = buffer.getInt(OFFSET_FLAGS)
        return RuntimeTelemetry(
            flags = flags,
            intent = (flags and FLAG_INTENT_MASK) ushr FLAG_INTENT_SHIFT,
            hudVisible = (flags and FLAG_HUD_VISIBLE) != 0,
            aiActive = (flags and FLAG_AI_ACTIVE) != 0,
            telemetryOnly = (flags and FLAG_TELEMETRY_ONLY) != 0,
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
            timestampNanos = buffer.getLong(OFFSET_TIMESTAMP_NANOS)
        )
    }
}

data class RuntimeTelemetry(
    val flags: Int,
    val intent: Int,
    val hudVisible: Boolean,
    val aiActive: Boolean,
    val telemetryOnly: Boolean,
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
    val timestampNanos: Long
) {
    val intentLabel: String
        get() = UiReadability.nativeIntentLabel(intent)
}
