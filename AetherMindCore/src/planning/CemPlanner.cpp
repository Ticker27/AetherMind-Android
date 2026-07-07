#include "aether/planning/CemPlanner.h"

#include <cmath>

namespace aether {

namespace {
constexpr float PI_F = 3.14159265358979323846f;
constexpr float TWO_PI_F = 6.28318530717958647692f;
}

CemPlanner::CemPlanner(CemPlannerConfig config) noexcept
    : cfg(config) {
    if (cfg.population > CemPlannerConfig::MaxPopulation) {
        cfg.population = CemPlannerConfig::MaxPopulation;
    }
    if (cfg.elite > CemPlannerConfig::MaxElite) {
        cfg.elite = CemPlannerConfig::MaxElite;
    }
    if (cfg.elite == 0U) {
        cfg.elite = 1U;
    }
    if (cfg.population < cfg.elite) {
        cfg.population = cfg.elite;
    }
    if (cfg.iterations == 0U) {
        cfg.iterations = 1U;
    }
    cfg.smoothing = clampRange(cfg.smoothing, 0.0f, 1.0f);
}

float CemPlanner::clampRange(float value, float lo, float hi) noexcept {
    return value < lo ? lo : (value > hi ? hi : value);
}

float CemPlanner::wrapPi(float value) noexcept {
    while (value > PI_F) {
        value -= TWO_PI_F;
    }
    while (value < -PI_F) {
        value += TWO_PI_F;
    }
    return value;
}

float CemPlanner::absFloat(float value) noexcept {
    return value < 0.0f ? -value : value;
}

float CemPlanner::sqrtApprox(float value) noexcept {
    return value <= 0.0f ? 0.0f : std::sqrt(value);
}

std::uint64_t CemPlanner::nextRandom(std::uint64_t& state) noexcept {
    std::uint64_t x = state == 0ULL ? 0x9E3779B97F4A7C15ULL : state;
    x ^= x >> 12U;
    x ^= x << 25U;
    x ^= x >> 27U;
    state = x;
    return x * 2685821657736338717ULL;
}

float CemPlanner::uniform01(std::uint64_t& state) noexcept {
    const std::uint64_t value = nextRandom(state);
    return static_cast<float>(static_cast<double>(value >> 11U) * (1.0 / 9007199254740992.0));
}

float CemPlanner::normal01(std::uint64_t& state) noexcept {
    const float u1 = clampRange(uniform01(state), 0.000001f, 0.999999f);
    const float u2 = uniform01(state);
    return std::sqrt(-2.0f * std::log(u1)) * std::cos(TWO_PI_F * u2);
}

float CemPlanner::scoreCandidate(
    const PhysicsExperienceState& state,
    const Intent& intent,
    const SkillProfile& skill,
    const CemAction& action
) noexcept {
    const float dx = state.targetPosition.x - state.cuePosition.x;
    const float dy = state.targetPosition.y - state.cuePosition.y;
    const float distance = sqrtApprox(dx * dx + dy * dy);
    const float desiredAngle = wrapPi(std::atan2(dy, dx) + state.angleOffset);
    const float angleError = absFloat(wrapPi(action.angle - desiredAngle));
    const float powerTarget = clampRange(distance * 1.65f * state.powerScale, 0.10f, 1.0f);
    const float powerError = absFloat(action.power - powerTarget);
    const float spinPenalty = absFloat(action.spin) * 0.08f;
    const float telemetryRisk = clampRange(0.50f + state.riskBias + state.errorMargin, 0.0f, 1.0f);
    const float skillSafety = clampRange(static_cast<float>(skill.safetyAwareness), 0.0f, 1.0f);
    const float skillPosition = clampRange(static_cast<float>(skill.positionPlanning), 0.0f, 1.0f);

    float firstOrderTravel = action.power * (1.0f - telemetryRisk * 0.22f);
    float progress = 1.0f - clampRange(absFloat(firstOrderTravel - powerTarget) / 0.95f, 0.0f, 1.0f);
    float alignment = 1.0f - clampRange(angleError / 0.75f, 0.0f, 1.0f);
    float control = 1.0f - clampRange(powerError / 0.75f + spinPenalty, 0.0f, 1.0f);

    float score = alignment * 1.40f + progress * 0.75f + control * 0.55f;

    switch (intent.type) {
        case IntentType::Offensive:
            score += alignment * 0.65f + action.power * 0.25f;
            score -= telemetryRisk * 0.18f;
            break;

        case IntentType::Defensive:
            score += (1.0f - telemetryRisk) * 0.25f;
            score += (1.0f - clampRange(action.power, 0.0f, 1.0f)) * 0.32f;
            score -= angleError * 0.35f;
            break;

        case IntentType::SafetyPlay:
            score += skillSafety * 0.42f;
            score += (1.0f - absFloat(action.power - intent.powerMean)) * 0.28f;
            score -= action.power * telemetryRisk * 0.20f;
            break;

        case IntentType::Positioning:
            score += skillPosition * 0.38f;
            score += (1.0f - absFloat(action.spin - intent.spinMean)) * 0.18f;
            score -= absFloat(action.power - intent.powerMean) * 0.25f;
            break;
    }

    score += clampRange(intent.confidence, 0.0f, 1.0f) * 0.18f;
    score -= telemetryRisk * (1.0f - clampRange(static_cast<float>(skill.riskTolerance), 0.0f, 1.0f)) * 0.30f;

    return score;
}

void CemPlanner::sortDescending(
    std::array<CemCandidate, CemPlannerConfig::MaxPopulation>& candidates,
    std::uint32_t count
) noexcept {
    for (std::uint32_t i = 1U; i < count; ++i) {
        CemCandidate key = candidates[i];
        std::uint32_t j = i;
        while (j > 0U && candidates[j - 1U].score < key.score) {
            candidates[j] = candidates[j - 1U];
            --j;
        }
        candidates[j] = key;
    }
}

CemPlanResult CemPlanner::plan(
    const PhysicsExperienceState& state,
    const Intent& intent,
    const SkillProfile& skill,
    std::uint64_t seed
) const noexcept {
    std::array<CemCandidate, CemPlannerConfig::MaxPopulation> candidates{};

    float meanAngle = intent.angleMean;
    float meanPower = intent.powerMean;
    float meanSpin = intent.spinMean;

    float stdAngle = intent.angleStd;
    float stdPower = intent.powerStd;
    float stdSpin = intent.spinStd;

    CemCandidate globalBest;
    globalBest.action.angle = meanAngle;
    globalBest.action.power = meanPower;
    globalBest.action.spin = meanSpin;
    globalBest.score = scoreCandidate(state, intent, skill, globalBest.action);

    std::uint64_t rng = seed ^ (static_cast<std::uint64_t>(state.timestampNanos) + 0xA5A5A5A55A5A5A5AULL);
    std::uint32_t evaluated = 1U;

    const std::uint32_t population = cfg.population;
    const std::uint32_t elite = cfg.elite <= population ? cfg.elite : population;

    for (std::uint32_t iter = 0U; iter < cfg.iterations; ++iter) {
        for (std::uint32_t i = 0U; i < population; ++i) {
            CemAction action;
            if (i == 0U) {
                action.angle = meanAngle;
                action.power = meanPower;
                action.spin = meanSpin;
            } else {
                action.angle = wrapPi(meanAngle + normal01(rng) * stdAngle);
                action.power = clampRange(meanPower + normal01(rng) * stdPower, 0.04f, 1.0f);
                action.spin = clampRange(meanSpin + normal01(rng) * stdSpin, -1.0f, 1.0f);
            }

            candidates[i].action = action;
            candidates[i].score = scoreCandidate(state, intent, skill, action);
            ++evaluated;
        }

        sortDescending(candidates, population);

        if (candidates[0].score > globalBest.score) {
            globalBest = candidates[0];
        }

        float angleX = 0.0f;
        float angleY = 0.0f;
        float powerSum = 0.0f;
        float spinSum = 0.0f;

        for (std::uint32_t i = 0U; i < elite; ++i) {
            angleX += std::cos(candidates[i].action.angle);
            angleY += std::sin(candidates[i].action.angle);
            powerSum += candidates[i].action.power;
            spinSum += candidates[i].action.spin;
        }

        const float invElite = 1.0f / static_cast<float>(elite);
        const float eliteAngle = std::atan2(angleY * invElite, angleX * invElite);
        const float elitePower = powerSum * invElite;
        const float eliteSpin = spinSum * invElite;

        float angleVar = 0.0f;
        float powerVar = 0.0f;
        float spinVar = 0.0f;

        for (std::uint32_t i = 0U; i < elite; ++i) {
            const float da = wrapPi(candidates[i].action.angle - eliteAngle);
            const float dp = candidates[i].action.power - elitePower;
            const float ds = candidates[i].action.spin - eliteSpin;
            angleVar += da * da;
            powerVar += dp * dp;
            spinVar += ds * ds;
        }

        meanAngle = wrapPi(meanAngle * cfg.smoothing + eliteAngle * (1.0f - cfg.smoothing));
        meanPower = clampRange(meanPower * cfg.smoothing + elitePower * (1.0f - cfg.smoothing), 0.04f, 1.0f);
        meanSpin = clampRange(meanSpin * cfg.smoothing + eliteSpin * (1.0f - cfg.smoothing), -1.0f, 1.0f);

        stdAngle = clampRange(sqrtApprox(angleVar * invElite), 0.010f, intent.angleStd);
        stdPower = clampRange(sqrtApprox(powerVar * invElite), 0.012f, intent.powerStd);
        stdSpin = clampRange(sqrtApprox(spinVar * invElite), 0.020f, intent.spinStd);
    }

    CemPlanResult result;
    result.action = globalBest.action;
    result.score = globalBest.score;
    result.confidence = clampRange(0.30f + globalBest.score * 0.18f + intent.confidence * 0.25f, 0.0f, 1.0f);
    result.risk = clampRange(0.50f + state.riskBias + state.errorMargin * 0.40f - result.confidence * 0.22f, 0.0f, 1.0f);
    result.evaluatedCandidates = evaluated;
    return result;
}

PhysicsExperienceState CemPlanner::planTelemetry(
    const PhysicsExperienceState& state,
    const Intent& intent,
    const SkillProfile& skill,
    std::uint64_t seed
) const noexcept {
    PhysicsExperienceState out = state;
    const CemPlanResult result = plan(state, intent, skill, seed);

    out.angleOffset = wrapPi(result.action.angle - intent.angleMean);
    out.powerScale = clampRange(result.action.power, 0.0f, 1.6f);
    out.velocityScale = clampRange(result.action.power * (1.0f + absFloat(result.action.spin) * 0.08f), 0.0f, 2.0f);
    out.confidenceBias = clampRange(result.confidence - 0.50f, -0.50f, 0.50f);
    out.riskBias = clampRange(result.risk - 0.50f, -0.50f, 0.50f);
    out.errorMargin = clampRange(1.0f - result.confidence + result.risk * 0.20f, 0.0f, 1.0f);
    out.flags |= 0x00000100U;
    out.flags |= (static_cast<std::uint32_t>(intent.type) & 0x0FU) << 12U;
    out.reserved0 = result.evaluatedCandidates;
    out.layoutVersion = AETHER_PHYSICS_EXPERIENCE_STATE_VERSION;

    return out;
}

}
