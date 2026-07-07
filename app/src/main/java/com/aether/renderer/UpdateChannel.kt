package com.aether.renderer

enum class UpdateChannelMode {
    STABLE,
    RC,
    INTERNAL
}

data class UpdateChannelState(
    val mode: UpdateChannelMode = UpdateChannelMode.STABLE,
    val updateAllowed: Boolean = true,
    val rollbackRequired: Boolean = false,
    val note: String = "stable_channel"
) {
    val badge: String
        get() = mode.name

    val compactLine: String
        get() = "$badge update=$updateAllowed rollback=$rollbackRequired"
}

object UpdateChannel {
    fun current(): UpdateChannelState = UpdateChannelState()
}
