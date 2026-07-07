#pragma once

#include "aether/core/SkillProfile.h"
#include "aether/planning/ActionGenerator.h"
#include "aether/physics/PhysicsKernel.h"
#include "aether/world/WorldState.h"

namespace aether {

struct PlanScore {
    double score = 0.0;
    double confidence = 0.0;
    double risk = 0.0;
    double reward = 0.0;

    double pocketReward = 0.0;
    double safetyPenalty = 0.0;
    double physicsConfidence = 0.0;
};

class Evaluator {
public:
    PlanScore evaluate(
        const WorldState& world,
        const ActionCandidate& candidate,
        const SimulationResult& simulation,
        const SkillProfile& skill
    ) const;
};

}
