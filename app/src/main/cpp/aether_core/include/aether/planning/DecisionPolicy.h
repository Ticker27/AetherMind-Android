#pragma once

#include <random>
#include <vector>

#include "aether/core/MindTypes.h"
#include "aether/core/SkillProfile.h"
#include "aether/planning/Planner.h"

namespace aether {

class DecisionPolicy {
public:
    DecisionPolicy();

    MindOutput choose(
        const std::vector<RankedPlan>& plans,
        const SkillProfile& skill
    );

private:
    std::mt19937 rng;
};

}
