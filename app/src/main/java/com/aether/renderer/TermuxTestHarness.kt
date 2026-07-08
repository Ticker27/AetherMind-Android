package com.aether.renderer

data class TermuxTestHarnessState(
    val harnessLabel: String = "TERMUX_EXTERNAL_TEST_HARNESS",
    val zipIntegrityCheck: Boolean = true,
    val sourceLayoutCheck: Boolean = true,
    val labelCheck: Boolean = true,
    val privacyCheck: Boolean = true,
    val qaScriptCheck: Boolean = true,
    val optionalAndroidBuildCheck: Boolean = true,
    val requiredCommand: String = "sh termux/run_all_termux_checks.sh",
    val optionalBuildCommand: String = "sh termux/04_optional_android_build.sh"
)

object TermuxTestHarness {
    fun current(): TermuxTestHarnessState = TermuxTestHarnessState()
}
