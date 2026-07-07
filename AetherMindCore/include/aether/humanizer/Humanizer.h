#pragma once

#include <cstdint>

#include "aether/core/MindTypes.h"
#include "aether/core/SkillProfile.h"
#include "aether/memory/MemorySchema.h"

namespace aether {

struct HumanizerConfig {
    double maxAngleErrorRadians = 0.055;
    double maxPowerError = 0.18;
    double maxSpinError = 0.10;
    double tremorRadians = 0.012;
    double smoothness = 0.72;
    std::uint32_t maxReactionDelayFrames = 3;
};

struct HumanizerState {
    std::uint64_t rng = 0xA37B5F19D4C3B2A1ULL;
    std::uint32_t delayFramesRemaining = 0;
    std::uint32_t sequence = 0;
    Action heldAction;
    Action previousAction;
    bool hasHeldAction = false;
    bool hasPreviousAction = false;

    // Phase 6.3 temporal coherence state. All fields are scalar and preallocated.
    double tremorX = 0.0;
    double tremorY = 0.0;
    double delayBudgetMs = 0.0;
};

struct HumanizedAction {
    Action action;
    double angleErrorRadians = 0.0;
    double powerError = 0.0;
    double spinXError = 0.0;
    double spinYError = 0.0;
    double tremorRadians = 0.0;
    double reactionDelayMs = 0.0;
    bool reactionDelayed = false;
    bool blunder = false;
};

class Humanizer {
public:
    explicit Humanizer(
        HumanizerConfig config = HumanizerConfig()
    ) noexcept;

    HumanizedAction apply(
        const Action& planned,
        const SkillProfile& skill,
        double confidence,
        double risk,
        HumanizerState& state
    ) const noexcept;

    PhysicsExperienceState applyTelemetry(
        const PhysicsExperienceState& planned,
        const SkillProfile& skill,
        HumanizerState& state
    ) const noexcept;

private:
    HumanizerConfig cfg;

    static std::uint64_t nextRandom(
        HumanizerState& state
    ) noexcept;

    static double uniformSigned(
        HumanizerState& state
    ) noexcept;

    static double clampUnit(
        double value
    ) noexcept;

    static Action rotateAction(
        const Action& action,
        double radians
    ) noexcept;

    static Action smoothAction(
        const Action& previous,
        const Action& current,
        double alpha
    ) noexcept;
};

}
