package com.aethermind.execution

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

data class DispatcherConfig(
    val emptyQueueDelayMs: Long = 2L,
    val errorDelayMs: Long = 20L,
    val stopOnExecutorError: Boolean = true
)

class AetherActionDispatcher(
    targetPackage: String,
    private val executor: ScreenActionExecutor,
    foregroundPackageProvider: ForegroundPackageProvider = AccessibilityForegroundPackageProvider(),
    private val config: DispatcherConfig = DispatcherConfig()
) : AutoCloseable {
    companion object {
        private const val TAG = "AetherDispatcher"
    }

    private val packageGuard = PackageScopeGuard(
        targetPackage = targetPackage,
        foregroundPackageProvider = foregroundPackageProvider
    )

    private val running = AtomicBoolean(false)

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    private var dispatcherJob: Job? = null

    fun start() {
        if (!running.compareAndSet(false, true)) {
            Log.w(TAG, "Dispatcher already running.")
            return
        }

        AetherEmergencyStopController.reset()

        dispatcherJob = scope.launch {
            runDispatcherLoop()
        }

        Log.i(TAG, "Dispatcher started.")
    }

    fun stop(reason: String = "STOP_REQUESTED") {
        if (!running.compareAndSet(true, false)) {
            return
        }

        AetherEmergencyStopController.stop(
            reasonText = reason,
            clearNativeQueue = true
        )

        dispatcherJob?.cancel()
        dispatcherJob = null

        Log.w(TAG, "Dispatcher stopped. reason=$reason")
    }

    override fun close() {
        stop("DISPATCHER_CLOSED")
    }

    private suspend fun runDispatcherLoop() {
        val commandBuffer = AetherExecutionBridge.newCommandBuffer()

        try {
            while (
                running.get() &&
                !AetherEmergencyStopController.isStopped() &&
                scope.coroutineContext.isActive
            ) {
                val status = AetherExecutionBridge.popCommand(commandBuffer)

                when (status) {
                    NativeExecutionStatus.OK -> {
                        handleCommand(commandBuffer)
                    }

                    NativeExecutionStatus.QUEUE_EMPTY -> {
                        delay(config.emptyQueueDelayMs)
                    }

                    else -> {
                        Log.e(
                            TAG,
                            "nativePopCommand failed. status=${NativeExecutionStatus.nameOf(status)}"
                        )
                        delay(config.errorDelayMs)
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            Log.e(TAG, "Dispatcher loop crashed.", t)
            AetherEmergencyStopController.stop(
                reasonText = "DISPATCHER_LOOP_CRASHED: ${t.message}",
                clearNativeQueue = true
            )
        } finally {
            running.set(false)
            AetherExecutionBridge.clearQueue()
            Log.w(TAG, "Dispatcher loop exited.")
        }
    }

    private suspend fun handleCommand(commandBuffer: ByteBuffer) {
        val command = runCatching {
            ActionCommand.readFrom(commandBuffer)
        }.getOrElse { error ->
            Log.e(TAG, "Failed to decode ActionCommand.", error)
            AetherEmergencyStopController.stop(
                reasonText = "COMMAND_DECODE_FAILED: ${error.message}",
                clearNativeQueue = true
            )
            return
        }

        // Critical safety gate:
        // Always verify foreground package after dequeue and before real screen execution.
        if (!packageGuard.verifyOrStop()) {
            running.set(false)
            return
        }

        runCatching {
            executor.execute(command)
        }.onFailure { error ->
            Log.e(TAG, "Command execution failed. command=$command", error)

            if (config.stopOnExecutorError) {
                AetherEmergencyStopController.stop(
                    reasonText = "EXECUTOR_ERROR: ${error.message}",
                    clearNativeQueue = true
                )
                running.set(false)
            }
        }
    }
}
