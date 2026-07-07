#include "aether/strategy/Strategist.h"

#include <cmath>

namespace aether {

namespace {
constexpr float PI_F = 3.14159265358979323846f;

static float clampRange(float value, float lo, float hi) noexcept {
    return value < lo ? lo : (value > hi ? hi : value);
}

static float safeHypot(float x, float y) noexcept {
    return std::sqrt(x * x + y * y);
}
}

float Strategist::clamp01(float value) noexcept {
    return clampRange(value, 0.0f, 1.0f);
}

float Strategist::clampSigned(float value) noexcept {
    return clampRange(value, -1.0f, 1.0f);
}

Intent Strategist::selectIntent(
    const PhysicsExperienceState& state,
    const SkillProfile& skill
) const noexcept {
    Intent intent;

    const float dx = state.targetPosition.x - state.cuePosition.x;
    const float dy = state.targetPosition.y - state.cuePosition.y;
    const float distance = safeHypot(dx, dy);

    const float confidence = clamp01(0.50f + state.confidenceBias);
    const float risk = clamp01(0.50f + state.riskBias + state.errorMargin * 0.45f);
    const float skillRisk = clamp01(static_cast<float>(skill.riskTolerance));
    const float safety = clamp01(static_cast<float>(skill.safetyAwareness));
    const float position = clamp01(static_cast<float>(skill.positionPlanning));

    intent.confidence = confidence;
    intent.urgency = clamp01(1.0f - distance);
    intent.riskTolerance = skillRisk;
    intent.targetX = state.targetPosition.x;
    intent.targetY = state.targetPosition.y;
    intent.angleMean = clampRange(std::atan2(dy, dx) + state.angleOffset, -PI_F, PI_F);

    const float basePower = clamp01(distance * 1.65f * state.powerScale);
    intent.powerMean = clampRange(basePower, 0.12f, 1.0f);
    intent.spinMean = 0.0f;

    intent.angleStd = clampRange(0.18f - confidence * 0.08f + risk * 0.06f, 0.025f, 0.28f);
    intent.powerStd = clampRange(0.22f - confidence * 0.08f + risk * 0.05f, 0.040f, 0.30f);
    intent.spinStd = clampRange(0.18f + position * 0.08f, 0.05f, 0.30f);

    const bool degradedTelemetry =
        state.layoutVersion != AETHER_PHYSICS_EXPERIENCE_STATE_VERSION ||
        confidence < 0.32f ||
        state.errorMargin > 0.34f;

    const bool highRisk = risk > 0.68f || state.cushionBounceCount > 2U;
    const bool clearAttack = confidence > 0.60f && risk < (0.72f + skillRisk * 0.10f) && distance > 0.045f;
    const bool reposition = distance <= 0.045f || position > 0.72f;

    if (degradedTelemetry) {
        intent.type = IntentType::Defensive;
        intent.flags |= 0x00000001U;
    } else if (highRisk && safety >= skillRisk) {
        intent.type = IntentType::SafetyPlay;
        intent.flags |= 0x00000002U;
    } else if (clearAttack) {
        intent.type = IntentType::Offensive;
        intent.flags |= 0x00000004U;
    } else if (reposition) {
        intent.type = IntentType::Positioning;
        intent.flags |= 0x00000008U;
    } else {
        intent.type = IntentType::SafetyPlay;
        intent.flags |= 0x00000010U;
    }

    switch (intent.type) {
        case IntentType::Offensive:
            intent.powerMean = clampRange(intent.powerMean * (1.06f + skillRisk * 0.10f), 0.18f, 1.0f);
            intent.angleStd = clampRange(intent.angleStd * 0.72f, 0.018f, 0.16f);
            intent.powerStd = clampRange(intent.powerStd * 0.76f, 0.030f, 0.22f);
            break;

        case IntentType::Defensive:
            intent.powerMean = clampRange(intent.powerMean * 0.72f, 0.10f, 0.68f);
            intent.angleStd = clampRange(intent.angleStd * 1.15f, 0.04f, 0.32f);
            intent.powerStd = clampRange(intent.powerStd * 0.90f, 0.035f, 0.24f);
            break;

        case IntentType::SafetyPlay:
            intent.powerMean = clampRange(0.24f + safety * 0.24f + distance * 0.18f, 0.14f, 0.70f);
            intent.angleStd = clampRange(intent.angleStd * 1.25f, 0.05f, 0.34f);
            intent.powerStd = clampRange(intent.powerStd * 0.80f, 0.030f, 0.20f);
            break;

        case IntentType::Positioning:
            intent.powerMean = clampRange(0.34f + position * 0.22f, 0.16f, 0.74f);
            intent.spinMean = clampSigned((position - 0.50f) * 0.50f);
            intent.spinStd = clampRange(intent.spinStd * 1.20f, 0.08f, 0.34f);
            break;
    }

    return intent;
}

const char* intentTypeName(IntentType type) noexcept {
    switch (type) {
        case IntentType::Offensive: return "OFFENSIVE";
        case IntentType::Defensive: return "DEFENSIVE";
        case IntentType::SafetyPlay: return "SAFETY_PLAY";
        case IntentType::Positioning: return "POSITIONING";
    }

    return "POSITIONING";
}

}
