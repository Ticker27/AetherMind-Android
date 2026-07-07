#pragma once

#include <cstdint>

#include "aether/core/SkillProfile.h"
#include "aether/memory/MemorySchema.h"

namespace aether {

enum class IntentType : std::uint8_t {
    Offensive = 0,
    Defensive = 1,
    SafetyPlay = 2,
    Positioning = 3
};

struct Intent {
    IntentType type = IntentType::Positioning;

    float confidence = 0.0f;
    float urgency = 0.0f;
    float riskTolerance = 0.0f;

    float targetX = 0.0f;
    float targetY = 0.0f;

    float angleMean = 0.0f;
    float powerMean = 1.0f;
    float spinMean = 0.0f;

    float angleStd = 0.10f;
    float powerStd = 0.18f;
    float spinStd = 0.20f;

    std::uint32_t flags = 0U;
};

class Strategist {
public:
    Intent selectIntent(
        const PhysicsExperienceState& state,
        const SkillProfile& skill
    ) const noexcept;

private:
    static float clamp01(float value) noexcept;
    static float clampSigned(float value) noexcept;
};

const char* intentTypeName(IntentType type) noexcept;

}
