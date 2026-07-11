package com.aethermind.execution

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class ActionCommandType(val raw: Int) {
    TAP(1),
    SWIPE(2);

    companion object {
        fun fromRaw(raw: Int): ActionCommandType? {
            return entries.firstOrNull { it.raw == raw }
        }
    }
}

data class ActionCommand(
    val x: Float,
    val y: Float,
    val type: ActionCommandType,
    val reserved: Int = 0,
    val timestampNanos: Long
) {
    companion object {
        const val OFFSET_X = 0
        const val OFFSET_Y = 4
        const val OFFSET_TYPE = 8
        const val OFFSET_RESERVED = 12
        const val OFFSET_TIMESTAMP_NANOS = 16
        const val ABI_SIZE_BYTES = 24

        fun readFrom(buffer: ByteBuffer): ActionCommand {
            require(buffer.isDirect) { "ActionCommand buffer must be DirectByteBuffer." }
            require(buffer.capacity() >= ABI_SIZE_BYTES) {
                "Buffer capacity ${buffer.capacity()} is smaller than $ABI_SIZE_BYTES."
            }

            buffer.order(ByteOrder.nativeOrder())

            val rawType = buffer.getInt(OFFSET_TYPE)
            val type = requireNotNull(ActionCommandType.fromRaw(rawType)) {
                "Invalid ActionCommandType raw value: $rawType"
            }

            return ActionCommand(
                x = buffer.getFloat(OFFSET_X),
                y = buffer.getFloat(OFFSET_Y),
                type = type,
                reserved = buffer.getInt(OFFSET_RESERVED),
                timestampNanos = buffer.getLong(OFFSET_TIMESTAMP_NANOS)
            )
        }

        fun writeTo(buffer: ByteBuffer, command: ActionCommand) {
            require(buffer.isDirect) { "ActionCommand buffer must be DirectByteBuffer." }
            require(buffer.capacity() >= ABI_SIZE_BYTES) {
                "Buffer capacity ${buffer.capacity()} is smaller than $ABI_SIZE_BYTES."
            }

            buffer.order(ByteOrder.nativeOrder())
            buffer.putFloat(OFFSET_X, command.x)
            buffer.putFloat(OFFSET_Y, command.y)
            buffer.putInt(OFFSET_TYPE, command.type.raw)
            buffer.putInt(OFFSET_RESERVED, command.reserved)
            buffer.putLong(OFFSET_TIMESTAMP_NANOS, command.timestampNanos)
        }
    }
}

object NativeExecutionStatus {
    const val OK = 0
    const val QUEUE_EMPTY = 1

    const val ERROR_NULL_BUFFER = -1
    const val ERROR_NON_DIRECT_BUFFER = -2
    const val ERROR_INSUFFICIENT_CAPACITY = -3
    const val ERROR_INVALID_COMMAND_TYPE = -4
    const val ERROR_INVALID_COORDINATE = -5
    const val ERROR_ALLOCATION_FAILED = -6
    const val ERROR_INTERNAL = -7
    const val ERROR_EXECUTION_LOCKED = -8

    fun nameOf(code: Int): String {
        return when (code) {
            OK -> "OK"
            QUEUE_EMPTY -> "QUEUE_EMPTY"
            ERROR_NULL_BUFFER -> "ERROR_NULL_BUFFER"
            ERROR_NON_DIRECT_BUFFER -> "ERROR_NON_DIRECT_BUFFER"
            ERROR_INSUFFICIENT_CAPACITY -> "ERROR_INSUFFICIENT_CAPACITY"
            ERROR_INVALID_COMMAND_TYPE -> "ERROR_INVALID_COMMAND_TYPE"
            ERROR_INVALID_COORDINATE -> "ERROR_INVALID_COORDINATE"
            ERROR_ALLOCATION_FAILED -> "ERROR_ALLOCATION_FAILED"
            ERROR_INTERNAL -> "ERROR_INTERNAL"
            ERROR_EXECUTION_LOCKED -> "ERROR_EXECUTION_LOCKED"
            else -> "UNKNOWN_STATUS_$code"
        }
    }
}

object AetherExecutionNative {
    init {
        System.loadLibrary("aether_execution_core")
    }

    @JvmStatic
    external fun nativePushCommand(commandBuffer: ByteBuffer): Int

    @JvmStatic
    external fun nativePopCommand(outCommandBuffer: ByteBuffer): Int

    @JvmStatic
    external fun nativeClearQueue(): Int

    @JvmStatic
    external fun nativeCommandSize(): Int

    @JvmStatic
    external fun nativeQueueSize(): Int
}

object AetherExecutionBridge {
    val commandSizeBytes: Int by lazy {
        AetherExecutionNative.nativeCommandSize()
    }

    fun newCommandBuffer(): ByteBuffer {
        return ByteBuffer
            .allocateDirect(commandSizeBytes)
            .order(ByteOrder.nativeOrder())
    }

    fun pushCommand(command: ActionCommand): Int {
        val buffer = newCommandBuffer()
        ActionCommand.writeTo(buffer, command)
        return AetherExecutionNative.nativePushCommand(buffer)
    }

    fun popCommand(outCommandBuffer: ByteBuffer): Int {
        return AetherExecutionNative.nativePopCommand(outCommandBuffer)
    }

    fun clearQueue(): Int {
        return AetherExecutionNative.nativeClearQueue()
    }

    fun queueSize(): Int {
        return AetherExecutionNative.nativeQueueSize()
    }
}
