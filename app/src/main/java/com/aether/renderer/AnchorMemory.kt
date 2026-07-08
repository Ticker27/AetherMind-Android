package com.aether.renderer

import java.util.concurrent.ConcurrentHashMap

data class AnchorMemoryState(
    val packageName: String = "",
    val anchorCount: Int = 0,
    val stableAnchorCount: Int = 0,
    val primaryZone: String = "UNKNOWN",
    val anchorStability: Float = 0.0f,
    val lastScreenType: ScreenType = ScreenType.SCREEN_UNKNOWN
) {
    val stabilityLabel: String
        get() = when {
            anchorCount <= 0 -> "NO_ANCHOR"
            anchorStability >= 0.78f -> "ANCHOR_STABLE"
            anchorStability >= 0.48f -> "ANCHOR_WATCH"
            else -> "ANCHOR_LEARNING"
        }
}

object AnchorMemory {
    private val states = ConcurrentHashMap<String, MutableMap<String, Float>>()
    private val latest = ConcurrentHashMap<String, AnchorMemoryState>()

    fun update(packageName: String, anchors: List<NodeAnchor>, screenType: ScreenType): AnchorMemoryState {
        if (packageName.isBlank()) return AnchorMemoryState(lastScreenType = screenType)
        val memory = states.getOrPut(packageName) { mutableMapOf() }
        val seen = anchors.map { it.signature }.toSet()
        seen.forEach { signature ->
            val old = memory[signature] ?: 0.20f
            memory[signature] = (old + 0.16f).coerceAtMost(1.0f)
        }
        memory.keys.toList().forEach { signature ->
            if (signature !in seen) memory[signature] = ((memory[signature] ?: 0f) - 0.04f).coerceAtLeast(0f)
        }
        val stable = seen.count { (memory[it] ?: 0f) >= 0.66f }
        val avg = if (seen.isNotEmpty()) seen.map { memory[it] ?: 0f }.average().toFloat() else 0f
        val zone = anchors.groupingBy { it.positionKey }.eachCount().maxByOrNull { it.value }?.key ?: "UNKNOWN"
        val state = AnchorMemoryState(
            packageName = packageName,
            anchorCount = anchors.size,
            stableAnchorCount = stable,
            primaryZone = zone,
            anchorStability = avg.coerceIn(0f, 1f),
            lastScreenType = screenType
        )
        latest[packageName] = state
        return state
    }

    fun current(packageName: String): AnchorMemoryState? = latest[packageName]
}
