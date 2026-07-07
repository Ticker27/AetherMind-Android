#pragma once

#include <string>

#include "aether/core/MindTypes.h"
#include "aether/core/SkillProfile.h"
#include "aether/world/WorldBuilder.h"
#include "aether/planning/ActionGenerator.h"
#include "aether/planning/Evaluator.h"
#include "aether/planning/Planner.h"
#include "aether/planning/DecisionPolicy.h"
#include "aether/physics/PhysicsKernel.h"
#include "aether/cognition/ExecutionModel.h"
#include "aether/memory/ExperienceMemory.h"
#include "aether/memory/MemoryStore.h"

namespace aether {

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
    ActionGenerator actionGenerator;
    PhysicsKernel physicsKernel;
    Evaluator evaluator;
    Planner planner;
    DecisionPolicy decisionPolicy;
    ExecutionModel executionModel;
    ExperienceMemory experienceMemory;
    MemoryStore memoryStore;
};

}
