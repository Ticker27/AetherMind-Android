#pragma once

#include <random>

#include "aether/core/MindTypes.h"
#include "aether/core/SkillProfile.h"

namespace aether {

struct ExecutionResult {
    Action plannedAction;
    Action executedAction;

    double angleError = 0.0;
    double powerError = 0.0;
    bool blunder = false;
};

class ExecutionModel {
public:
    ExecutionModel();

    ExecutionResult execute(
        const Action& planned,
        const SkillProfile& skill
    );

private:
    std::mt19937 rng;
};

}
