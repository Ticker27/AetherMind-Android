#pragma once

#include <string>
#include "aether/foundation/Vec2.h"
#include "aether/world/WorldState.h"

namespace aether {

enum class ActionType {
    None,
    Direct,
    Safety,
    Position,
    Exploratory
};

struct Action {
    Vec2 direction;
    double power = 0.0;
    double spinX = 0.0;
    double spinY = 0.0;
    ActionType type = ActionType::None;
    int targetId = -1;
};

struct MindInput {
    PerceptionFrame perception;
};

struct MindOutput {
    Action plannedAction;
    Action executedAction;

    double confidence = 0.0;
    double risk = 0.0;
    double expectedReward = 0.0;

    double executionAngleError = 0.0;
    double executionPowerError = 0.0;
    bool executionBlunder = false;

    bool plannedPocketed = false;
    bool executedPocketed = false;
    bool plannedCuePocketed = false;
    bool executedCuePocketed = false;

    int plannedFirstHitId = -1;
    int executedFirstHitId = -1;

    double plannedTargetPocketDistance = 999.0;
    double executedTargetPocketDistance = 999.0;

    double plannedPocketMargin = -999.0;
    double executedPocketMargin = -999.0;

    double executedConfidence = 0.0;
    double executedTravelDistance = 0.0;

    std::string memorySummary;
    double memoryConfidenceBias = 0.0;
    double memoryRiskBias = 0.0;

    int targetId = -1;
    std::string explanation;
};

}
