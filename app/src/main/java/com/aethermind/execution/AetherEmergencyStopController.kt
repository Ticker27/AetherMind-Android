package com.aethermind.execution

import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object AetherEmergencyStopController {
    private const val TAG = "AetherEmergencyStop"

    private val stopped = AtomicBoolean(false)
    private val reason = AtomicReference<String?>(null)
    private val listeners = CopyOnWriteArrayList<(String) -> Unit>()

    fun isStopped(): Boolean = stopped.get()

    fun reason(): String? = reason.get()

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    fun reset() {
        reason.set(null)
        stopped.set(false)
        Log.i(TAG, "Emergency stop reset.")
    }

    fun stop(reasonText: String, clearNativeQueue: Boolean = true) {
        if (!stopped.compareAndSet(false, true)) {
            return
        }

        reason.set(reasonText)

        if (clearNativeQueue) {
            val status = runCatching {
                AetherExecutionBridge.clearQueue()
            }.getOrDefault(NativeExecutionStatus.ERROR_INTERNAL)

            Log.w(
                TAG,
                "Emergency stop triggered. reason=$reasonText clearQueueStatus=${NativeExecutionStatus.nameOf(status)}"
            )
        } else {
            Log.w(TAG, "Emergency stop triggered. reason=$reasonText")
        }

        listeners.forEach { listener ->
            runCatching { listener(reasonText) }
                .onFailure { Log.e(TAG, "Emergency stop listener failed.", it) }
        }
    }
}
