package com.aether.renderer

object Phase5Lock {
    const val LABEL = "AT54_55_PHASE5_LOCK"
    const val PHASE = "5.4-5.5"
    const val APP_VERSION = "5.5.0"
    const val VERSION_CODE = 550
    const val CORE_VERSION = "7.1"
    const val API_STATUS = "FROZEN_PHASE5"
    const val TELEMETRY_ONLY = true
    const val AUTO_CONTROL_ENABLED = false
    const val LEARNING_OVERRIDE_SAFETY = false
    const val LEARNING_CAN_RAISE_RISK = true
    const val LEARNING_CAN_LOWER_SAFETY = false
    const val LOCK_SCOPE = "LEARNING_LOOP_MEMORY_TRUST_FEEDBACK_POLICY"

    fun statusLine(): String {
        return "$LABEL $API_STATUS telemetryOnly=$TELEMETRY_ONLY autoControl=$AUTO_CONTROL_ENABLED scope=$LOCK_SCOPE"
    }
}
