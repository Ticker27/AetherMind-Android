package com.aether.renderer

data class IntentTransitionStep(
    val index: Int,
    val scenarioId: String,
    val band: IntentReadinessBand,
    val expectedIntent: StrategicIntentType,
    val actualIntent: StrategicIntentType,
    val risk: Float,
    val confidence: Float,
    val matched: Boolean,
    val reason: String
) {
    val compactLine: String
        get() = "#$index:$scenarioId ${band.name} expected=$expectedIntent actual=$actualIntent ok=$matched c=${confidence.fmt2()} r=${risk.fmt2()}"
}

data class IntentTransitionTrace(
    val steps: List<IntentTransitionStep> = emptyList(),
    val transitionCount: Int = 0,
    val distinctBands: Set<IntentReadinessBand> = emptySet(),
    val distinctIntents: Set<StrategicIntentType> = emptySet(),
    val passed: Boolean = false,
    val reason: String = "transition_trace_waiting"
) {
    val compactLine: String
        get() = "transitions=$transitionCount bands=${distinctBands.joinToString(",")} intents=${distinctIntents.joinToString(",")} pass=$passed"
}

object IntentTransitionTracer {
    fun trace(results: List<PlannerScenarioResult>): IntentTransitionTrace {
        val steps = results.mapIndexed { index, result ->
            val rule = IntentTransitionRules.classify(result.state)
            IntentTransitionStep(
                index = index,
                scenarioId = result.scenario.id,
                band = rule.band,
                expectedIntent = rule.expectedIntent,
                actualIntent = result.intent.type,
                risk = result.state.risk,
                confidence = result.state.confidence,
                matched = rule.expectedIntent == result.intent.type,
                reason = rule.reason
            )
        }
        return build(steps)
    }

    fun traceStates(states: List<Pair<String, PhysicsExperienceState>>): IntentTransitionTrace {
        val steps = states.mapIndexed { index, pair ->
            val rule = IntentTransitionRules.classify(pair.second)
            val actual = StrategicIntentEngine.decide(pair.second).type
            IntentTransitionStep(
                index = index,
                scenarioId = pair.first,
                band = rule.band,
                expectedIntent = rule.expectedIntent,
                actualIntent = actual,
                risk = pair.second.risk,
                confidence = pair.second.confidence,
                matched = rule.expectedIntent == actual,
                reason = rule.reason
            )
        }
        return build(steps)
    }

    private fun build(steps: List<IntentTransitionStep>): IntentTransitionTrace {
        val bands = steps.map { it.band }.toSet()
        val intents = steps.map { it.actualIntent }.toSet()
        val transitions = steps.zipWithNext().count { it.first.band != it.second.band || it.first.actualIntent != it.second.actualIntent }
        val passed = steps.isNotEmpty() && steps.all { it.matched } && bands.size >= 3 && intents.size >= 3 && transitions >= 2
        return IntentTransitionTrace(
            steps = steps,
            transitionCount = transitions,
            distinctBands = bands,
            distinctIntents = intents,
            passed = passed,
            reason = if (passed) "ready_caution_safety_transition_verified" else "intent_transition_not_diverse_or_not_matched"
        )
    }
}
