package com.aethermind.execution

import android.content.Context
import com.aethermind.ui.overlay.OverlayUiState

/**
 * AT168 Core Truth Recovery.
 *
 * Auto-play is surgically disabled. The class remains only so older UI wiring
 * compiles, but it never starts runtime dispatch, never computes swipes, and
 * never pushes commands into the native queue.
 */
class AutoPlayController(
    private val context: Context,
    private val statusSink: (String) -> Unit
) : AutoCloseable {

    fun onFrame(state: OverlayUiState) {
        (voidContext())
        if (state.autoPlayEnabled) {
            statusSink("LOCKED")
        } else {
            statusSink("OFF")
        }
    }

    fun stopRuntime(reason: String = "AUTO_PLAY_DISABLED") {
        (voidContext())
        AetherExecutionBridge.clearQueue()
        statusSink("LOCKED:$reason")
    }

    override fun close() {
        stopRuntime("AUTO_PLAY_CONTROLLER_CLOSED")
    }

    private fun voidContext() {
        // Keep constructor signature stable without using Android execution APIs.
        context.applicationContext
    }
}
