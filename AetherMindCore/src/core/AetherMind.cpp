#include "aether/core/AetherMind.h"
#include "aether/foundation/Vec2.h"

#include <vector>

namespace aether {

AetherMind::AetherMind(
    SkillLevel level,
    const std::string& memoryName_
)
    : skill(makeSkillProfile(level)),
      memoryName(memoryName_),
      memoryStore("data/state") {
    if (!memoryName.empty()) {
        memoryStore.loadExperience(memoryName, experienceMemory);
    }
}

static void applyMemoryBiasToPlans(
    std::vector<RankedPlan>& plans,
    const ExperienceMemory& memory,
    const SkillProfile& skill
) {
    for (auto& plan : plans) {
        double confidenceBias =
            memory.confidenceBias(plan.candidate.action, skill);

        double riskBias =
            memory.riskBias(plan.candidate.action, skill);

        plan.score.confidence =
            clamp01(plan.score.confidence + confidenceBias);

        plan.score.risk =
            clamp01(plan.score.risk + riskBias);

        plan.score.score = clamp01(
            plan.score.score +
            confidenceBias * 0.32 -
            riskBias * 0.50
        );
    }
}

MindOutput AetherMind::think(const MindInput& input) {
    WorldState world = worldBuilder.build(input.perception);

    auto candidates = actionGenerator.generate(world, skill);

    auto plans = planner.buildPlans(
        world,
        candidates,
        physicsKernel,
        evaluator,
        skill
    );

    applyMemoryBiasToPlans(
        plans,
        experienceMemory,
        skill
    );

    MindOutput output = decisionPolicy.choose(plans, skill);

    output.memoryConfidenceBias =
        experienceMemory.confidenceBias(output.plannedAction, skill);

    output.memoryRiskBias =
        experienceMemory.riskBias(output.plannedAction, skill);

    ExecutionResult executed =
        executionModel.execute(output.plannedAction, skill);

    output.plannedAction = executed.plannedAction;
    output.executedAction = executed.executedAction;
    output.executionAngleError = executed.angleError;
    output.executionPowerError = executed.powerError;
    output.executionBlunder = executed.blunder;

    SimulationResult executedSim =
        physicsKernel.simulate(world, output.executedAction);

    output.executedPocketed = executedSim.targetPocketed;
    output.executedCuePocketed = executedSim.cuePocketed;
    output.executedFirstHitId = executedSim.firstHitId;
    output.executedTargetPocketDistance = executedSim.targetClosestPocketDistance;
    output.executedPocketMargin = executedSim.targetPocketMargin;
    output.executedConfidence = executedSim.confidence;
    output.executedTravelDistance = executedSim.travelDistance;

    experienceMemory.record(output, skill);

    if (!memoryName.empty()) {
        memoryStore.saveExperience(memoryName, experienceMemory);
    }

    output.memorySummary = experienceMemory.summary();

    return output;
}

}
