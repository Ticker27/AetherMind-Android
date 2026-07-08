package com.aether.renderer

data class RollbackState(
    val rollbackAvailable: Boolean = true,
    val rollbackPoint: String = "AT90_95_QA_LOCK",
    val reason: String = "baseline_available",
    val safeToRollback: Boolean = true
) {
    val compactLine: String
        get() = "rollback=$rollbackAvailable point=$rollbackPoint"
}

object RollbackSystem {
    fun current(): RollbackState = RollbackState()
}
