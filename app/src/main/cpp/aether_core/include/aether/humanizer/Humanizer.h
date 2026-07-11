#pragma once

#include <cstdint>
#include <string>

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
    std::uint64_t styleSeed = 0xA37B5F19D4C3B2A1ULL;  // persona fingerprint
    double cadenceBaseMs = 1200.0;                     // avg inter-shot delay
};

// Stable behavioral fingerprint. A human player has a CONSISTENT style, so the
// AI must emulate ONE person (not white-noise randomness each frame). This is
// the core of "leaves no trace": a recognizable, stable, human cadence + motion
// that varies organically shot-to-shot but is coherent across a session.
struct HumanPersona {
    std::uint64_t styleSeed = 0xA37B5F19D4C3B2A1ULL;
    double cadenceBias = 0.0;    // -0.6..0.6  slower / faster than average
    double curvatureBias = 0.18; // how curved the stroke path is
    double jitterBias = 1.0;     // micro waypoint jitter scale
    double slipBias = 1.0;       // imperfection (graded miss) scale
    double windupBias = 1.0;     // cue pull-back amount before strike
};

// Kinematic hint the executor consumes to move the "finger" like a human:
// curved path, eased wind-up / commit / follow-through, micro jitter.
// The BRAIN owns this policy; Kotlin merely actuates it on the touch screen.
struct HumanMotionProfile {
    double durationMs = 380.0;  // commit stroke duration (ms)
    double curvature = 0.16;    // perpendicular bow / stroke length (0 = straight)
    double jitterPx = 3.0;      // micro waypoint jitter (px)
    double windupScale = 0.18;  // pull-back fraction of stroke length
    double followScale = 0.10;  // follow-through fraction of stroke length
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

    // --- Human-like cadence + kinematics (brain-owned policy) ---
    // The AI plays like one stable person: a recognizable, varied-but-coherent
    // rhythm and stroke. difficulty in [0,1] (harder shots => longer think time,
    // more curve/jitter). Returned values are deterministic for a given persona.
    const HumanPersona& persona() const noexcept { return persona_; }

    double sampleCadenceMs(double difficulty) const noexcept;

    HumanMotionProfile motionProfile(double difficulty) const noexcept;

private:
    HumanizerConfig cfg;
    HumanPersona persona_;

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

// Free functions used by the integration runtime / JNI. They derive their
// behavior from a single global Humanizer persona so the whole app plays with
// one consistent human style.
HumanPersona makePersona(std::uint64_t seed) noexcept;
double humanCadenceMs(double difficulty) noexcept;
std::string humanMotionProfileJson(double difficulty) noexcept;

}
