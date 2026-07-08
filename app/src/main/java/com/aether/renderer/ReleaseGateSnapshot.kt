package com.aether.renderer

data class ReleaseGateSnapshot(
    val label: String = FinalManifest.LABEL,
    val phase: String = FinalManifest.PHASE,
    val appVersion: String = FinalManifest.APP_VERSION,
    val versionCode: Int = FinalManifest.VERSION_CODE,
    val coreVersion: String = FinalManifest.CORE_VERSION,
    val packageName: String = FinalManifest.PACKAGE_NAME,
    val apiStatus: String = FinalManifest.API_STATUS,
    val telemetryOnly: Boolean = FinalStabilityLock.TELEMETRY_ONLY,
    val autoControlEnabled: Boolean = FinalStabilityLock.AUTO_CONTROL_ENABLED,
    val executionEnabled: Boolean = FinalStabilityLock.EXECUTION_ENABLED,
    val skillMode: String = FinalStabilityLock.SKILL_MODE,
    val rawUiTextStored: Boolean = FinalStabilityLock.RAW_UI_TEXT_STORED,
    val rawUiTextExported: Boolean = FinalStabilityLock.RAW_UI_TEXT_EXPORTED,
    val externalExportAllowed: Boolean = FinalStabilityLock.EXTERNAL_EXPORT_ALLOWED,
    val modulesLocked: Int = FinalManifest.MODULES.size
) {
    val passed: Boolean
        get() = label == "AT131_140_FINAL_STABILITY_LOCK" &&
            appVersion == "14.0.0" &&
            versionCode == 1400 &&
            packageName == "com.aether.renderer" &&
            apiStatus == "FINAL_STABILITY_LOCK" &&
            telemetryOnly &&
            !autoControlEnabled &&
            !executionEnabled &&
            skillMode == "PROPOSE_ONLY" &&
            !rawUiTextStored &&
            !rawUiTextExported &&
            !externalExportAllowed
}
