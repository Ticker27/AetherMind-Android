package com.aether.renderer

import java.nio.ByteBuffer
import java.nio.ByteOrder

object NativeTrajectoryBridge {
    private const val EXPECTED_STATE_SIZE = 64
    private const val EXPECTED_STATE_LAYOUT_VERSION = 1

    init {
        System.loadLibrary("aether_jni")
    }

    external fun nativeStateSize(): Int
    external fun nativeStateLayoutVersion(): Int
    external fun nativePublishState(buffer: ByteBuffer): Boolean
    external fun nativeCopyLatestState(buffer: ByteBuffer): Boolean
    external fun nativePublishedCount(): Long

    val stateSize: Int by lazy {
        val nativeSize = nativeStateSize()
        require(nativeSize == EXPECTED_STATE_SIZE) {
            "Aether state ABI size mismatch: native=$nativeSize expected=$EXPECTED_STATE_SIZE"
        }
        nativeSize
    }

    val stateLayoutVersion: Int by lazy {
        val nativeVersion = nativeStateLayoutVersion()
        require(nativeVersion == EXPECTED_STATE_LAYOUT_VERSION) {
            "Aether state layout mismatch: native=$nativeVersion expected=$EXPECTED_STATE_LAYOUT_VERSION"
        }
        nativeVersion
    }

    fun verifyStateAbi() {
        stateSize
        stateLayoutVersion
    }

    fun allocateStateBuffer(): ByteBuffer {
        verifyStateAbi()
        return ByteBuffer
            .allocateDirect(stateSize)
            .order(ByteOrder.nativeOrder())
    }

    fun copyLatestState(buffer: ByteBuffer): Boolean {
        verifyStateAbi()
        require(buffer.isDirect) { "Aether state buffer must be direct" }
        require(buffer.capacity() >= stateSize) {
            "Aether state buffer capacity ${buffer.capacity()} is smaller than $stateSize"
        }
        buffer.position(0)
        return nativeCopyLatestState(buffer)
    }

    fun publishState(buffer: ByteBuffer): Boolean {
        verifyStateAbi()
        require(buffer.isDirect) { "Aether state buffer must be direct" }
        require(buffer.capacity() >= stateSize) {
            "Aether state buffer capacity ${buffer.capacity()} is smaller than $stateSize"
        }
        require(buffer.order() == ByteOrder.nativeOrder()) {
            "Aether state buffer must use native byte order"
        }
        require(buffer.getInt(0) == stateLayoutVersion) {
            "Aether state layout mismatch in buffer: buffer=${buffer.getInt(0)} expected=$stateLayoutVersion"
        }
        buffer.position(0)
        return nativePublishState(buffer)
    }
}
