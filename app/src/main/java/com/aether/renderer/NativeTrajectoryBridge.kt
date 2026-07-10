package com.aether.renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder

object NativeTrajectoryBridge {
    const val PHYSICS_STATE_BYTES: Int = 64

    init {
        System.loadLibrary("aether_jni")
    }

    external fun nativeStateSize(): Int
    external fun nativeStateLayoutVersion(): Int
    external fun nativePublishState(directBuffer: ByteBuffer): Boolean
    external fun nativeCopyLatestState(directBuffer: ByteBuffer): Boolean
    external fun nativePublishedCount(): Long

    fun newStateBuffer(): ByteBuffer =
        ByteBuffer.allocateDirect(PHYSICS_STATE_BYTES).order(ByteOrder.nativeOrder())

    fun isValidStateBuffer(buffer: ByteBuffer): Boolean =
        buffer.isDirect && buffer.capacity() >= PHYSICS_STATE_BYTES
}
