#include "aether/cognition/ExecutionModel.h"
#include "aether/foundation/Vec2.h"

#include <cmath>

namespace aether {

ExecutionModel::ExecutionModel()
    : rng(2026) {}

ExecutionResult ExecutionModel::execute(
    const Action& planned,
    const SkillProfile& skill
) {
    ExecutionResult result;
    result.plannedAction = planned;
    result.executedAction = planned;

    double angleNoiseStd = (1.0 - skill.aimAccuracy) * 0.075;
    double powerNoiseStd = (1.0 - skill.powerControl) * 0.18;

    std::normal_distribution<double> angleNoise(0.0, angleNoiseStd);
    std::normal_distribution<double> powerNoise(0.0, powerNoiseStd);
    std::uniform_real_distribution<double> chance(0.0, 1.0);

    result.angleError = angleNoise(rng);
    result.powerError = powerNoise(rng);

    if (chance(rng) < skill.blunderChance) {
        result.blunder = true;

        std::normal_distribution<double> blunderAngle(0.0, 0.18);
        std::normal_distribution<double> blunderPower(0.0, 0.22);

        result.angleError += blunderAngle(rng);
        result.powerError += blunderPower(rng);
    }

    double angle = std::atan2(
        planned.direction.y,
        planned.direction.x
    );

    angle += result.angleError;

    result.executedAction.direction = Vec2(
        std::cos(angle),
        std::sin(angle)
    ).normalized();

    result.executedAction.power =
        clamp(planned.power + result.powerError, 0.02, 1.0);

    return result;
}

}
