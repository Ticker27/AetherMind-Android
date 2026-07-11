package com.aethermind.execution

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.aether.renderer.accessibility.AccessibilityStateMapper
import java.util.concurrent.atomic.AtomicReference

class AetherAccessibilityService : AccessibilityService() {
    private var stateMapper: AccessibilityStateMapper? = null

    companion object {
        private const val TAG = "AetherAccessibilitySvc"

        private val serviceRef = AtomicReference<AetherAccessibilityService?>(null)
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
                AccessibilityEvent.TYPE_VIEW_CLICKED

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
            stateMapper?.onEvent(event, root)
        } finally {
            root?.recycle()
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
