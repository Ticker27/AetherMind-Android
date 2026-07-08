package com.aether.renderer

object Phase4Lock {
    const val LABEL = "AT44_45_PHASE4_LOCK"
    const val PHASE = "4.4-4.5"
    const val APP_VERSION = "4.5.0"
    const val VERSION_CODE = 450
    const val CORE_VERSION = "7.1"
    const val PACKAGE_NAME = "com.aether.renderer"
    const val BASELINE_FROM = "AT41_43_CONTEXT_BATCH"
    const val API_STATUS = "FROZEN_PHASE4"
    const val TELEMETRY_ONLY = true
    const val AUTO_CONTROL_ENABLED = false
    const val WORLD_MODEL_LOCKED = true
    const val CONTEXT_ENGINE_LOCKED = true
    const val BEHAVIOR_PROFILE_LOCKED = true
    const val POLICY_V2_LOCKED = true

    fun labelBlock(): String = """
        LABEL: $LABEL
        PHASE: $PHASE
        APP_VERSION: $APP_VERSION
        VERSION_CODE: $VERSION_CODE
        CORE_VERSION: $CORE_VERSION
        PACKAGE_NAME: $PACKAGE_NAME
        BASELINE_FROM: $BASELINE_FROM
        API_STATUS: $API_STATUS
        TELEMETRY_ONLY: $TELEMETRY_ONLY
        AUTO_CONTROL_ENABLED: $AUTO_CONTROL_ENABLED
    """.trimIndent()
}
