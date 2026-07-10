package com.aether.renderer

enum class WorldMemoryLayer {
    DEVICE_PROFILE,
    SCREEN_WORLD,
    NODE_ANCHOR,
    ANCHOR_MEMORY,
    SCREEN_TYPE,
    CONTEXT_ENGINE,
    BEHAVIOR_PROFILE,
    POLICY_V2
}

data class WorldMemoryLockStatus(
    val apiStatus: String = "FROZEN_PHASE4",
    val telemetryOnly: Boolean = true,
    val lockedLayers: List<WorldMemoryLayer> = WorldMemoryLayer.entries,
    val lockReason: String = "phase4_stable_baseline"
)

object WorldMemoryLock {
    const val API_STATUS = "FROZEN_PHASE4"
    const val TELEMETRY_ONLY = true
    const val WORLD_MEMORY_LOCKED = true
    const val AUTO_CONTROL_ENABLED = false

    val status = WorldMemoryLockStatus()

    fun isLocked(layer: WorldMemoryLayer): Boolean = layer in status.lockedLayers
}
