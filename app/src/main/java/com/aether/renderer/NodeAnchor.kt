package com.aether.renderer

import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale
import kotlin.math.abs

data class NodeAnchor(
    val anchorId: String,
    val packageName: String,
    val className: String,
    val roleGuess: String,
    val textHash: Int,
    val contentHash: Int,
    val bounds: NormalizedBounds,
    val clickable: Boolean,
    val visible: Boolean,
    val depth: Int,
    val timestampNanos: Long
) {
    val positionKey: String get() = bounds.zone
    val signature: String get() = "$roleGuess:$textHash:$contentHash:$positionKey"

    companion object {
        fun from(
            node: AccessibilityNodeInfo,
            packageName: String,
            bounds: NormalizedBounds,
            depth: Int,
            timestampNanos: Long,
            allowTextHash: Boolean = true
        ): NodeAnchor {
            val cls = node.className?.toString().orEmpty()
            val text = if (allowTextHash) node.text?.toString().orEmpty().trim().lowercase(Locale.US) else ""
            val desc = if (allowTextHash) node.contentDescription?.toString().orEmpty().trim().lowercase(Locale.US) else ""
            val role = when {
                node.isEditable -> "INPUT"
                node.isCheckable -> "CHECK"
                node.isClickable && text.isNotBlank() -> "BUTTON_TEXT"
                node.isClickable -> "BUTTON"
                text.isNotBlank() -> "TEXT"
                else -> cls.substringAfterLast('.').ifBlank { "NODE" }.uppercase(Locale.US)
            }
            val id = listOf(packageName, role, bounds.zone, quantize(bounds.centerX), quantize(bounds.centerY)).joinToString(":")
            return NodeAnchor(
                anchorId = id,
                packageName = packageName,
                className = cls,
                roleGuess = role,
                textHash = text.hashCode(),
                contentHash = desc.hashCode(),
                bounds = bounds,
                clickable = node.isClickable,
                visible = node.isVisibleToUser,
                depth = depth,
                timestampNanos = timestampNanos
            )
        }

        private fun quantize(value: Float): Int = (abs(value).coerceIn(0f, 1f) * 20f).toInt()
    }
}
