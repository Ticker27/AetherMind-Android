#pragma once

#include <string>

#include "aether/core/MindTypes.h"
#include "aether/core/SkillProfile.h"
#include "aether/world/WorldBuilder.h"
#include "aether/world/WorldStateValidator.h"
#include "aether/planning/ActionGenerator.h"
#include "aether/planning/Evaluator.h"
#include "aether/planning/Planner.h"
#include "aether/planning/DecisionPolicy.h"
#include "aether/physics/PhysicsKernel.h"

namespace aether {

// AetherMind is now the propose-only brain core.
// It may analyze, rank and explain candidates, but it must not execute actions
// or write real skill memory from mock/synthetic data.
class AetherMind {
public:
    explicit AetherMind(
        SkillLevel level,
        const std::string& memoryName = ""
    );

    MindOutput think(const MindInput& input);

private:
    SkillProfile skill;
    std::string memoryName;

    WorldBuilder worldBuilder;
    WorldStateValidator worldValidator;
    ActionGenerator actionGenerator;
    PhysicsKernel physicsKernel;
    Evaluator evaluator;
    Planner planner;
    DecisionPolicy decisionPolicy;
};

}
