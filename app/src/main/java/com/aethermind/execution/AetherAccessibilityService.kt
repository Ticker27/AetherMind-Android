package com.aethermind.execution

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aether.renderer.AetherIntegrationLoop
import com.aether.renderer.accessibility.AccessibilityStateMapper
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class AetherAccessibilityService : AccessibilityService() {
    private var stateMapper: AccessibilityStateMapper? = null

    companion object {
        private const val TAG = "AetherAccessibilitySvc"
        private const val EYE_TAG = "AetherNativeEye"

        private val serviceRef = AtomicReference<AetherAccessibilityService?>(null)
        private val eyeSequence = AtomicLong(0L)
        private val latestPackageRef = AtomicReference<String?>(null)

        fun currentService(): AetherAccessibilityService? = serviceRef.get()

        fun latestEventPackage(): String? = latestPackageRef.get()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceRef.set(this)
        stateMapper = AccessibilityStateMapper(this)

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED

            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50L

            flags =
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }

        Log.i(TAG, "Accessibility service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString()
        if (!packageName.isNullOrBlank()) {
            latestPackageRef.set(packageName)
        }

        // Accessibility is the Android-side eye/context shell. It observes the
        // active window and publishes normalized observer/world state through
        // AccessibilityStateMapper. Decision-making remains in the native C++
        // controller; this service does not compute shots and does not enqueue
        // screen actions.
        val root = runCatching { rootInActiveWindow }.getOrNull()
        try {
            publishNativeEyeSnapshot(event, root)
            stateMapper?.onEvent(event, root)
        } finally {
            root?.recycle()
        }
    }

    private fun publishNativeEyeSnapshot(event: AccessibilityEvent?, root: AccessibilityNodeInfo?) {
        if (event == null || !isUsefulEyeEvent(event.eventType)) return

        val metrics = if (root == null) EyeMetrics.EMPTY else countNodeTree(root, 0)
        val seq = eyeSequence.incrementAndGet()
        val packageName = event.packageName?.toString().orEmpty()
        val className = event.className?.toString().orEmpty()
        val timestampMs = if (event.eventTime > 0L) event.eventTime else SystemClock.uptimeMillis()

        val ok = runCatching {
            AetherIntegrationLoop.nativeSetAccessibilitySnapshot(
                sequence = seq,
                packageName = packageName,
                className = className,
                nodeCount = metrics.nodeCount,
                clickableCount = metrics.clickableCount,
                textCount = metrics.textCount,
                maxDepth = metrics.maxDepth,
                eventType = event.eventType,
                timestampMs = timestampMs
            )
        }.onFailure { throwable ->
            Log.w(EYE_TAG, "native snapshot failed seq=$seq pkg=$packageName", throwable)
        }.getOrDefault(false)

        Log.i(
            EYE_TAG,
            "sent seq=$seq pkg=$packageName nodes=${metrics.nodeCount} " +
                "click=${metrics.clickableCount} text=${metrics.textCount} " +
                "depth=${metrics.maxDepth} ok=$ok"
        )
    }

    private fun isUsefulEyeEvent(type: Int): Boolean {
        return type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            type == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            type == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
    }

    private fun countNodeTree(node: AccessibilityNodeInfo, depth: Int): EyeMetrics {
        var out = EyeMetrics(
            nodeCount = 1,
            clickableCount = if (node.isClickable) 1 else 0,
            textCount = if (!node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()) 1 else 0,
            maxDepth = depth
        )

        for (i in 0 until node.childCount) {
            val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
            try {
                out += countNodeTree(child, depth + 1)
            } finally {
                child.recycle()
            }
        }

        return out
    }

    private data class EyeMetrics(
        val nodeCount: Int,
        val clickableCount: Int,
        val textCount: Int,
        val maxDepth: Int
    ) {
        operator fun plus(other: EyeMetrics): EyeMetrics {
            return EyeMetrics(
                nodeCount = nodeCount + other.nodeCount,
                clickableCount = clickableCount + other.clickableCount,
                textCount = textCount + other.textCount,
                maxDepth = maxOf(maxDepth, other.maxDepth)
            )
        }

        companion object {
            val EMPTY = EyeMetrics(0, 0, 0, 0)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted.")
        AetherEmergencyStopController.stop(
            reasonText = "ACCESSIBILITY_SERVICE_INTERRUPTED",
            clearNativeQueue = true
        )
    }

    override fun onDestroy() {
        stateMapper = null
        serviceRef.compareAndSet(this, null)
        Log.w(TAG, "Accessibility service destroyed.")
        super.onDestroy()
    }
}
