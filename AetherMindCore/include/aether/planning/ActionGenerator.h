#pragma once

#include <string>
#include <vector>

#include "aether/core/MindTypes.h"
#include "aether/core/SkillProfile.h"
#include "aether/world/WorldState.h"

namespace aether {

struct ActionCandidate {
    Action action;
    double estimatedDifficulty = 0.0;
    double priorScore = 0.0;
    std::string reason;
};

class ActionGenerator {
public:
    std::vector<ActionCandidate> generate(
        const WorldState& world,
        const SkillProfile& skill
    ) const;
};

}
