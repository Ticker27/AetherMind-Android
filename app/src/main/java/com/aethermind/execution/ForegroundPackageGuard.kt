package com.aethermind.execution

import android.util.Log

interface ForegroundPackageProvider {
    fun currentForegroundPackage(): String?
}

class AccessibilityForegroundPackageProvider : ForegroundPackageProvider {
    override fun currentForegroundPackage(): String? {
        val service = AetherAccessibilityService.currentService()

        val rootPackage = service
            ?.rootInActiveWindow
            ?.packageName
            ?.toString()

        return rootPackage ?: AetherAccessibilityService.latestEventPackage()
    }
}

class PackageScopeGuard(
    private val targetPackage: String,
    private val foregroundPackageProvider: ForegroundPackageProvider
) {
    companion object {
        private const val TAG = "AetherPackageGuard"
    }

    fun verifyOrStop(): Boolean {
        val currentPackage = foregroundPackageProvider.currentForegroundPackage()

        if (currentPackage == targetPackage) {
            return true
        }

        Log.e(
            TAG,
            "Package guard failed. target=$targetPackage foreground=$currentPackage. Clearing queue and stopping dispatcher."
        )

        AetherEmergencyStopController.stop(
            reasonText = "PACKAGE_GUARD_FAILED target=$targetPackage foreground=$currentPackage",
            clearNativeQueue = true
        )

        return false
    }
}
