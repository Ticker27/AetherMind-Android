package com.aether.renderer

data class RegressionCheck(
    val id: String,
    val area: String,
    val requirement: String,
    val status: String = "LOCKED"
)

object RegressionTestPlan {
    val checks: List<RegressionCheck> = listOf(
        RegressionCheck("REG-001", "runtime", "Runtime bus flows remain present"),
        RegressionCheck("REG-002", "privacy", "Raw UI text is not exported"),
        RegressionCheck("REG-003", "safety", "Auto-control remains disabled"),
        RegressionCheck("REG-004", "native", "JNI symbol checks remain in CI"),
        RegressionCheck("REG-005", "ux", "Sanitized status export remains available")
    )

    fun lockedCount(): Int = checks.count { it.status == "LOCKED" }
}
