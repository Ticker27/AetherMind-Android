package com.aether.renderer

data class SceneSnapshot(
    val packageName: String = "",
    val className: String = "",
    val eventType: Int = 0,
    val nodeCount: Int = 0,
    val clickableCount: Int = 0,
    val textNodeCount: Int = 0,
    val visibleCount: Int = 0,
    val maxDepth: Int = 0,
    val timestampNanos: Long = 0L
) {
    val density: String
        get() = when {
            nodeCount >= 80 || textNodeCount >= 40 -> "DENSE"
            nodeCount >= 25 || clickableCount >= 8 -> "ACTIVE"
            nodeCount > 0 -> "LIGHT"
            else -> "EMPTY"
        }

    val targetLabel: String
        get() = packageName.substringAfterLast('.').ifBlank { "unknown" }
}
