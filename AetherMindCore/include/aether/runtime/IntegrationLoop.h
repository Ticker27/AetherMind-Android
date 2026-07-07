#pragma once

#include <cstdint>

#include "aether/core/SkillProfile.h"
#include "aether/memory/MemorySchema.h"
#include "aether/strategy/Strategist.h"

namespace aether {

enum class HudIntentColor : std::uint8_t {
    Neutral = 0,
    GoldAmber = 1,
    DeepPurple = 2,
    DefensiveBlue = 3,
    PositioningGreen = 4,
    HolographicChroma = 5
};

struct IntentHudTelemetry {
    IntentType intent = IntentType::Positioning;
    HudIntentColor color = HudIntentColor::Neutral;
    const char* label = "POSITIONING";
    std::uint8_t red = 128U;
    std::uint8_t green = 128U;
    std::uint8_t blue = 128U;
    std::uint8_t alpha = 255U;
    bool omnisAuthorized = false;
};

struct IntegrationLoopResult {
    PhysicsExperienceState observerState;
    PhysicsExperienceState executionState;
    Intent intent;
    IntentHudTelemetry hud;
    bool observerAccepted = false;
    bool authorized = false;
    bool executed = false;
};

class IntegrationLoop {
public:
    IntegrationLoop() noexcept;

    IntegrationLoopResult executeAutoPlayFrame(
        const PhysicsExperienceState& observerState,
        const SkillProfile& skill,
        const char* authorizationKey
    ) noexcept;

    IntegrationLoopResult classifyOnlyFrame(
        const PhysicsExperienceState& observerState,
        const SkillProfile& skill
    ) noexcept;

    static IntentHudTelemetry hudForIntent(
        IntentType intent,
        bool omnisAuthorized
    ) noexcept;

private:
    std::uint64_t sequence;
};

}
