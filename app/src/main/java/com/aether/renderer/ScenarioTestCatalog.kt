package com.aether.renderer

enum class ScenarioRiskBand {
    LOW,
    WATCH,
    CAUTION,
    LOCKED
}

data class ScenarioCase(
    val id: String,
    val screenType: String,
    val expectedBand: ScenarioRiskBand,
    val note: String
)

object ScenarioTestCatalog {
    val cases: List<ScenarioCase> = listOf(
        ScenarioCase("SCN-001", "SCREEN_HOME", ScenarioRiskBand.LOW, "baseline home screen"),
        ScenarioCase("SCN-002", "SCREEN_LIST", ScenarioRiskBand.WATCH, "dense list with stable anchors"),
        ScenarioCase("SCN-003", "SCREEN_FORM", ScenarioRiskBand.CAUTION, "form input boundary"),
        ScenarioCase("SCN-004", "SCREEN_DIALOG", ScenarioRiskBand.CAUTION, "modal confirmation"),
        ScenarioCase("SCN-005", "SCREEN_ERROR", ScenarioRiskBand.LOCKED, "error or unsafe screen")
    )

    fun total(): Int = cases.size
}
