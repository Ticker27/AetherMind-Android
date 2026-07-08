#pragma once

#include <vector>

#include "aether/core/SkillProfile.h"
#include "aether/planning/ActionGenerator.h"
#include "aether/planning/Evaluator.h"
#include "aether/physics/PhysicsKernel.h"
#include "aether/world/WorldState.h"

namespace aether {

struct RankedPlan {
    ActionCandidate candidate;
    SimulationResult simulation;
    PlanScore score;
};

class Planner {
public:
    std::vector<RankedPlan> buildPlans(
        const WorldState& world,
        const std::vector<ActionCandidate>& candidates,
        const PhysicsKernel& physics,
        const Evaluator& evaluator,
        const SkillProfile& skill
    ) const;

private:
    PhysicsConfig makePhysicsConfig(const SkillProfile& skill) const;
};

}
