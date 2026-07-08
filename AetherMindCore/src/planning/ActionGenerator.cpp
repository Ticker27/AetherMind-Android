#include "aether/planning/ActionGenerator.h"
#include "aether/foundation/Vec2.h"
#include <algorithm>
#include <sstream>
#include <cmath>

namespace aether {

static Vec2 rotateVec(const Vec2& v, double angle) {
    double c = std::cos(angle);
    double s = std::sin(angle);

    return Vec2(
        v.x * c - v.y * s,
        v.x * s + v.y * c
    ).normalized();
}

static std::vector<double> angleOffsetsForSkill(const SkillProfile& skill) {
    switch (skill.level) {
        case SkillLevel::Beginner:
            return {0.0};

        case SkillLevel::Intermediate:
            return {-0.030, 0.0, 0.030};

        case SkillLevel::Advanced:
            return {-0.090, -0.070, -0.050, -0.035, -0.020, -0.010, 0.0, 0.010, 0.020, 0.035, 0.050, 0.070, 0.090};
    }

    return {0.0};
}

static std::vector<double> powerScalesForSkill(const SkillProfile& skill) {
    switch (skill.level) {
        case SkillLevel::Beginner:
            return {1.0};

        case SkillLevel::Intermediate:
            return {0.92, 1.0, 1.08};

        case SkillLevel::Advanced:
            return {0.86, 0.94, 1.0, 1.06, 1.14};
    }

    return {1.0};
}

static std::vector<double> safetyAngleOffsets(const SkillProfile& skill) {
    switch (skill.level) {
        case SkillLevel::Beginner:
            return {0.0};

        case SkillLevel::Intermediate:
            return {-0.040, 0.0, 0.040};

        case SkillLevel::Advanced:
            return {-0.070, -0.040, -0.020, 0.0, 0.020, 0.040, 0.070};
    }

    return {0.0};
}

static std::vector<double> safetyPowers(const SkillProfile& skill) {
    switch (skill.level) {
        case SkillLevel::Beginner:
            return {0.35};

        case SkillLevel::Intermediate:
            return {0.28, 0.38, 0.48};

        case SkillLevel::Advanced:
            return {0.22, 0.30, 0.38, 0.48, 0.58};
    }

    return {0.35};
}

std::vector<ActionCandidate> ActionGenerator::generate(
    const WorldState& world,
    const SkillProfile& skill
) const {
    std::vector<ActionCandidate> result;

    const ObjectState* cue = world.cueBall();
    if (!cue) {
        return result;
    }

    std::vector<const ObjectState*> balls;
    std::vector<const ObjectState*> pockets;

    for (const auto& o : world.objects) {
        if (!o.active) continue;

        if (o.type == ObjectType::Ball && o.id != 0) {
            balls.push_back(&o);
        } else if (o.type == ObjectType::Pocket) {
            pockets.push_back(&o);
        }
    }

    const auto angleOffsets = angleOffsetsForSkill(skill);
    const auto powerScales = powerScalesForSkill(skill);

    for (const auto* ball : balls) {
        for (const auto* pocket : pockets) {
            Vec2 ballToPocket = pocket->position - ball->position;
            double ballToPocketDist = ballToPocket.length();

            if (ballToPocketDist <= 1e-6) {
                continue;
            }

            Vec2 targetToPocket = ballToPocket.normalized();

            Vec2 ghostBall = ball->position - targetToPocket * (ball->radius * 2.0);
            Vec2 cueToGhost = ghostBall - cue->position;

            double cueToGhostDist = cueToGhost.length();
            if (cueToGhostDist <= 1e-6) {
                continue;
            }

            Vec2 baseDirection = cueToGhost.normalized();

            double straightness = clamp01(dot(baseDirection, targetToPocket));
            double totalPathDistance = cueToGhostDist + ballToPocketDist;

            double basePower =
                totalPathDistance * 1.85 +
                (1.0 - straightness) * 0.25;

            if (skill.level == SkillLevel::Beginner) {
                basePower *= 0.95;
            } else if (skill.level == SkillLevel::Intermediate) {
                basePower *= 1.05;
            } else {
                basePower *= 1.12;
            }

            double confidence = clamp01(
                (cue->confidence + ball->confidence + pocket->confidence) / 3.0
            );

            double distancePenalty = clamp01(totalPathDistance / 1.8);
            double anglePenalty = 1.0 - straightness;

            double baseDifficulty = clamp01(
                anglePenalty * 0.65 +
                distancePenalty * 0.35
            );

            double basePrior = clamp01(
                confidence * 0.45 +
                straightness * 0.35 +
                (1.0 - distancePenalty) * 0.20
            );

            for (double angleOffset : angleOffsets) {
                for (double powerScale : powerScales) {
                    ActionCandidate c;

                    c.action.direction = rotateVec(baseDirection, angleOffset);
                    c.action.power = clamp(basePower * powerScale, 0.18, 1.0);
                    c.action.spinX = 0.0;
                    c.action.spinY = 0.0;
                    c.action.type = ActionType::Direct;
                    c.action.targetId = ball->id;

                    double offsetPenalty = clamp01(std::abs(angleOffset) / 0.08);
                    double powerPenalty = clamp01(std::abs(powerScale - 1.0) / 0.25);

                    c.estimatedDifficulty = clamp01(
                        baseDifficulty +
                        offsetPenalty * 0.08 +
                        powerPenalty * 0.04
                    );

                    c.priorScore = clamp01(
                        basePrior -
                        offsetPenalty * 0.035 -
                        powerPenalty * 0.020
                    );

                    std::ostringstream ss;
                    ss << "micro direct ball="
                       << ball->id
                       << " pocket="
                       << pocket->id
                       << " path="
                       << totalPathDistance
                       << " straightness="
                       << straightness
                       << " angleOffset="
                       << angleOffset
                       << " powerScale="
                       << powerScale;

                    c.reason = ss.str();

                    result.push_back(c);
                }
            }
        }
    }

    const auto safetyOffsets = safetyAngleOffsets(skill);
    const auto safetyPowerList = safetyPowers(skill);

    for (const auto* ball : balls) {
        Vec2 cueToBall = ball->position - cue->position;
        double dist = cueToBall.length();

        if (dist <= 1e-6) {
            continue;
        }

        Vec2 baseSafetyDirection = cueToBall.normalized();

        double confidence = clamp01(
            (cue->confidence + ball->confidence) * 0.5
        );

        for (double angleOffset : safetyOffsets) {
            for (double power : safetyPowerList) {
                ActionCandidate c;

                c.action.direction = rotateVec(baseSafetyDirection, angleOffset);
                c.action.power = clamp(power, 0.05, 0.80);
                c.action.spinX = 0.0;
                c.action.spinY = 0.0;
                c.action.type = ActionType::Safety;
                c.action.targetId = ball->id;

                double offsetPenalty = clamp01(std::abs(angleOffset) / 0.10);
                double distancePenalty = clamp01(dist / 1.2);

                c.estimatedDifficulty = clamp01(
                    0.20 +
                    offsetPenalty * 0.10 +
                    distancePenalty * 0.15
                );

                c.priorScore = clamp01(
                    confidence * 0.38 +
                    skill.safetyAwareness * 0.38 +
                    (1.0 - distancePenalty) * 0.12 +
                    (1.0 - offsetPenalty) * 0.12
                );

                std::ostringstream ss;
                ss << "safety ball="
                   << ball->id
                   << " angleOffset="
                   << angleOffset
                   << " power="
                   << power;

                c.reason = ss.str();

                result.push_back(c);
            }
        }
    }

    std::sort(result.begin(), result.end(), [](const ActionCandidate& a, const ActionCandidate& b) {
        return a.priorScore > b.priorScore;
    });

    if (static_cast<int>(result.size()) > skill.candidateLimit) {
        result.resize(static_cast<size_t>(skill.candidateLimit));
    }

    return result;
}

}
