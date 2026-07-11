#include "aether/humanizer/Humanizer.h"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdio>

namespace aether {

namespace {
constexpr double PI = 3.14159265358979323846264338327950288;
constexpr double FRAME_MS = 16.666666666666668;

struct CalibrationPoint {
    double skillLevel;
    double errorMargin;
    double tremorIntensity;
    double reactionDelayMs;
    double smoothness;
    double blunderScale;
};

// Phase 6.3 humanizer calibration table.
// skillLevel:          0.0 novice -------------------------- 1.0 pro
// errorMargin:         high -------------------------------- near zero
// tremorIntensity:     high -------------------------------- negligible
// reactionDelayMs:     high -------------------------------- minimal
// All values are deterministic scalar parameters; no heap allocation.
constexpr CalibrationPoint HUMANIZER_CALIBRATION[] = {
    {0.00, 1.000, 1.000, 180.0, 0.40, 1.00},
    {0.25, 0.620, 0.680, 115.0, 0.55, 0.72},
    {0.55, 0.310, 0.330,  58.0, 0.72, 0.42},
    {0.80, 0.110, 0.120,  28.0, 0.84, 0.20},
    {1.00, 0.018, 0.020,   8.0, 0.92, 0.06},
};

static double clampDouble(
    double value,
    double lo,
    double hi
) noexcept {
    return std::max(lo, std::min(hi, value));
}

static float clampFloat(
    float value,
    float lo,
    float hi
) noexcept {
    return std::max(lo, std::min(hi, value));
}

static double lerp(
    double a,
    double b,
    double t
) noexcept {
    return a + (b - a) * t;
}

static CalibrationPoint interpolateCalibration(
    double skillLevel
) noexcept {
    const double s = clampDouble(skillLevel, 0.0, 1.0);

    for (std::size_t i = 1U;
         i < sizeof(HUMANIZER_CALIBRATION) / sizeof(HUMANIZER_CALIBRATION[0]);
         ++i) {
        const CalibrationPoint& prev = HUMANIZER_CALIBRATION[i - 1U];
        const CalibrationPoint& next = HUMANIZER_CALIBRATION[i];

        if (s <= next.skillLevel) {
            const double span = next.skillLevel - prev.skillLevel;
            const double t = span > 0.0 ? (s - prev.skillLevel) / span : 0.0;

            return CalibrationPoint{
                s,
                lerp(prev.errorMargin, next.errorMargin, t),
                lerp(prev.tremorIntensity, next.tremorIntensity, t),
                lerp(prev.reactionDelayMs, next.reactionDelayMs, t),
                lerp(prev.smoothness, next.smoothness, t),
                lerp(prev.blunderScale, next.blunderScale, t),
            };
        }
    }

    return HUMANIZER_CALIBRATION[
        (sizeof(HUMANIZER_CALIBRATION) / sizeof(HUMANIZER_CALIBRATION[0])) - 1U
    ];
}

static double normalizedSkillLevel(
    const SkillProfile& skill
) noexcept {
    const double explicitLevel = clampDouble(skill.skillLevel, 0.0, 1.0);

    if (explicitLevel > 0.0 || skill.level == SkillLevel::Beginner) {
        return explicitLevel;
    }

    if (skill.level == SkillLevel::Intermediate) {
        return 0.55;
    }

    if (skill.level == SkillLevel::Advanced) {
        return 1.0;
    }

    return clampDouble(
        skill.aimAccuracy * 0.35 +
        skill.powerControl * 0.25 +
        skill.consistency * 0.25 +
        skill.physicsUnderstanding * 0.15,
        0.0,
        1.0
    );
}

static std::uint32_t delayFramesFromMs(
    double delayMs,
    std::uint32_t maxFrames
) noexcept {
    const double frames = delayMs / FRAME_MS;
    const std::uint32_t rounded = static_cast<std::uint32_t>(frames + 0.5);
    return std::min(maxFrames, rounded);
}

}

Humanizer::Humanizer(
    HumanizerConfig config
) noexcept
    : cfg(config),
      persona_(makePersona(config.styleSeed)) {}

std::uint64_t Humanizer::nextRandom(
    HumanizerState& state
) noexcept {
    std::uint64_t x = state.rng;

    x ^= x >> 12U;
    x ^= x << 25U;
    x ^= x >> 27U;

    state.rng = x;

    return x * 2685821657736338717ULL;
}

double Humanizer::uniformSigned(
    HumanizerState& state
) noexcept {
    const std::uint64_t value = nextRandom(state);
    const double unit =
        static_cast<double>(value >> 11U) *
        (1.0 / 9007199254740992.0);

    return unit * 2.0 - 1.0;
}

double Humanizer::clampUnit(
    double value
) noexcept {
    return clampDouble(value, 0.0, 1.0);
}

Action Humanizer::rotateAction(
    const Action& action,
    double radians
) noexcept {
    Action out = action;

    const double c = std::cos(radians);
    const double s = std::sin(radians);

    out.direction = Vec2(
        action.direction.x * c - action.direction.y * s,
        action.direction.x * s + action.direction.y * c
    ).normalized();

    return out;
}

Action Humanizer::smoothAction(
    const Action& previous,
    const Action& current,
    double alpha
) noexcept {
    Action out = current;
    const double beta = 1.0 - alpha;

    out.direction = Vec2(
        previous.direction.x * alpha + current.direction.x * beta,
        previous.direction.y * alpha + current.direction.y * beta
    ).normalized();

    out.power = clampDouble(
        previous.power * alpha + current.power * beta,
        0.0,
        1.0
    );

    out.spinX = clampDouble(
        previous.spinX * alpha + current.spinX * beta,
        -1.0,
        1.0
    );

    out.spinY = clampDouble(
        previous.spinY * alpha + current.spinY * beta,
        -1.0,
        1.0
    );

    return out;
}

HumanizedAction Humanizer::apply(
    const Action& planned,
    const SkillProfile& skill,
    double confidence,
    double risk,
    HumanizerState& state
) const noexcept {
    HumanizedAction result;

    const CalibrationPoint cal = interpolateCalibration(
        normalizedSkillLevel(skill)
    );

    const double aim = clampUnit(skill.aimAccuracy);
    const double powerControl = clampUnit(skill.powerControl);
    const double consistency = clampUnit(skill.consistency);
    const double confidenceTerm = clampUnit(confidence);
    const double riskTerm = clampUnit(risk);

    const double instability = clampUnit(
        (1.0 - consistency) * 0.50 +
        (1.0 - confidenceTerm) * 0.32 +
        riskTerm * 0.18
    );

    if (
        state.hasHeldAction &&
        state.delayFramesRemaining > 0U
    ) {
        result.action = state.heldAction;
        result.reactionDelayed = true;
        result.reactionDelayMs = state.delayBudgetMs;
        --state.delayFramesRemaining;
        state.delayBudgetMs = std::max(0.0, state.delayBudgetMs - FRAME_MS);
        return result;
    }

    result.action = planned;
    state.heldAction = planned;
    state.hasHeldAction = true;

    const double delayPressure = clampUnit(
        cal.errorMargin * 0.55 + instability * 0.45
    );

    const double reactionRoll =
        (uniformSigned(state) + 1.0) * 0.5;

    if (
        cfg.maxReactionDelayFrames > 0U &&
        reactionRoll < delayPressure * 0.38
    ) {
        const double jitter =
            0.75 + ((uniformSigned(state) + 1.0) * 0.25);

        const double requestedMs = cal.reactionDelayMs * jitter;
        const std::uint32_t delayFrames = delayFramesFromMs(
            requestedMs,
            cfg.maxReactionDelayFrames
        );

        state.delayFramesRemaining = delayFrames;
        state.delayBudgetMs = static_cast<double>(delayFrames) * FRAME_MS;
    }

    const bool blunder =
        ((uniformSigned(state) + 1.0) * 0.5) <
        clampUnit(skill.blunderChance * cal.blunderScale * (0.45 + instability));

    const double blunderErrorScale = blunder ? 2.25 : 1.0;

    const double angleError =
        uniformSigned(state) *
        cfg.maxAngleErrorRadians *
        cal.errorMargin *
        (1.0 - aim * 0.72) *
        (0.55 + instability) *
        blunderErrorScale;

    // Stateful tremor: first-order filtered deterministic noise. This preserves
    // temporal coherence and avoids robotic per-frame white-noise jitter.
    const double tremorTargetX = uniformSigned(state);
    const double tremorTargetY = uniformSigned(state);
    const double tremorFollow = clampDouble(0.18 + cal.tremorIntensity * 0.32, 0.12, 0.50);

    state.tremorX = lerp(state.tremorX, tremorTargetX, tremorFollow);
    state.tremorY = lerp(state.tremorY, tremorTargetY, tremorFollow);

    const double tremor =
        (state.tremorX * 0.70 + state.tremorY * 0.30) *
        cfg.tremorRadians *
        cal.tremorIntensity *
        (1.0 - consistency * 0.65) *
        (0.45 + riskTerm);

    const double powerError =
        uniformSigned(state) *
        cfg.maxPowerError *
        cal.errorMargin *
        (1.0 - powerControl * 0.70) *
        (0.50 + instability) *
        blunderErrorScale;

    const double spinScale = cal.errorMargin * (1.0 - consistency * 0.55);

    const double spinXError =
        uniformSigned(state) *
        cfg.maxSpinError *
        spinScale *
        (0.30 + riskTerm);

    const double spinYError =
        uniformSigned(state) *
        cfg.maxSpinError *
        spinScale *
        (0.30 + riskTerm);

    result.action = rotateAction(
        result.action,
        angleError + tremor
    );

    result.action.power = clampDouble(
        result.action.power + powerError,
        0.0,
        1.0
    );

    result.action.spinX = clampDouble(
        result.action.spinX + spinXError,
        -1.0,
        1.0
    );

    result.action.spinY = clampDouble(
        result.action.spinY + spinYError,
        -1.0,
        1.0
    );

    if (state.hasPreviousAction) {
        const double alpha = clampDouble(
            cfg.smoothness * cal.smoothness * consistency,
            0.0,
            0.97
        );

        result.action = smoothAction(
            state.previousAction,
            result.action,
            alpha
        );
    }

    state.previousAction = result.action;
    state.hasPreviousAction = true;
    ++state.sequence;

    result.angleErrorRadians = angleError + tremor;
    result.powerError = powerError;
    result.spinXError = spinXError;
    result.spinYError = spinYError;
    result.tremorRadians = tremor;
    result.reactionDelayMs = state.delayBudgetMs;
    result.blunder = blunder;

    return result;
}

PhysicsExperienceState Humanizer::applyTelemetry(
    const PhysicsExperienceState& planned,
    const SkillProfile& skill,
    HumanizerState& state
) const noexcept {
    PhysicsExperienceState out = planned;

    Action action;
    action.direction = Vec2(1.0, 0.0);
    action.power = clampDouble(
        static_cast<double>(planned.powerScale),
        0.0,
        1.0
    );

    const HumanizedAction h = apply(
        action,
        skill,
        static_cast<double>(planned.confidenceBias + 0.5f),
        static_cast<double>(planned.riskBias + 0.5f),
        state
    );

    out.angleOffset = clampFloat(
        planned.angleOffset + static_cast<float>(h.angleErrorRadians),
        static_cast<float>(-PI),
        static_cast<float>(PI)
    );

    out.powerScale = clampFloat(
        static_cast<float>(h.action.power),
        0.0f,
        1.6f
    );

    out.velocityScale = clampFloat(
        planned.velocityScale + static_cast<float>(h.powerError),
        0.0f,
        2.0f
    );

    out.errorMargin = clampFloat(
        planned.errorMargin +
            static_cast<float>(
                std::fabs(h.angleErrorRadians) +
                std::fabs(h.powerError) * 0.25 +
                std::fabs(h.tremorRadians) * 0.50 +
                h.reactionDelayMs * 0.0005
            ),
        0.0f,
        1.0f
    );

    if (h.reactionDelayed) {
        out.flags |= 0x00000001U;
    }

    if (h.blunder) {
        out.flags |= 0x00000002U;
    }

    out.layoutVersion = AETHER_PHYSICS_EXPERIENCE_STATE_VERSION;

    return out;
}

namespace {

// Single global persona so the entire app plays with ONE consistent human
// style (the heart of "leaves no trace"). gCadenceState supplies deterministic
// RNG for inter-shot cadence sampling.
Humanizer gHumanizer;
HumanizerState gCadenceState;

} // namespace

HumanPersona aether::makePersona(std::uint64_t seed) noexcept {
    HumanPersona p;
    std::uint64_t x = seed ? seed : 0xA37B5F19D4C3B2A1ULL;

    auto rnd = [&x]() -> double {
        x ^= x >> 12U;
        x ^= x << 25U;
        x ^= x >> 27U;
        const std::uint64_t v = x * 2685821657736338717ULL;
        return (static_cast<double>(v >> 11U) * (1.0 / 9007199254740992.0)) * 2.0 - 1.0;
    };

    p.styleSeed = seed;
    p.cadenceBias = rnd() * 0.6;
    p.curvatureBias = clampDouble(0.12 + (rnd() * 0.5 + 0.5) * 0.22, 0.05, 0.45);
    p.jitterBias = clampDouble(0.7 + (rnd() * 0.5 + 0.5) * 0.8, 0.4, 1.6);
    p.slipBias = clampDouble(0.8 + (rnd() * 0.5 + 0.5) * 0.5, 0.5, 1.5);
    p.windupBias = clampDouble(0.85 + (rnd() * 0.5 + 0.5) * 0.4, 0.6, 1.4);
    return p;
}

double Humanizer::sampleCadenceMs(double difficulty) const noexcept {
    const double d = clampDouble(difficulty, 0.0, 1.0);

    // Harder shots => longer, more variable "thinking" before the strike.
    const double base = 850.0 + d * 950.0;            // 850..1800 ms
    const double u1 = (uniformSigned(gCadenceState) + 1.0) * 0.5;
    const double u2 = (uniformSigned(gCadenceState) + 1.0) * 0.5;
    const double spread = u1 * 0.6 + u2 * 0.4;        // 0..1, slightly skewed
    const double factor = 0.70 + spread * 0.95;       // 0.70..1.65
    const double cadence = base * factor * (1.0 + persona_.cadenceBias * 0.25);
    return clampDouble(cadence, 550.0, 3200.0);
}

HumanMotionProfile Humanizer::motionProfile(double difficulty) const noexcept {
    const double d = clampDouble(difficulty, 0.0, 1.0);

    HumanMotionProfile p;
    p.durationMs = 300.0 + d * 280.0;                 // 300..580 commit stroke
    p.curvature = clampDouble(persona_.curvatureBias * (0.7 + d * 0.6), 0.04, 0.5);
    p.jitterPx = 2.0 + persona_.jitterBias * 3.0;
    p.windupScale = clampDouble(persona_.windupBias * 0.16, 0.06, 0.30);
    p.followScale = clampDouble(persona_.windupBias * 0.10, 0.04, 0.20);
    return p;
}

double aether::humanCadenceMs(double difficulty) noexcept {
    return gHumanizer.sampleCadenceMs(difficulty);
}

std::string aether::humanMotionProfileJson(double difficulty) noexcept {
    const HumanMotionProfile p = gHumanizer.motionProfile(difficulty);
    char buf[192];
    std::snprintf(
        buf, sizeof(buf),
        "{\"durationMs\":%.1f,\"curvature\":%.3f,\"jitterPx\":%.2f,"
        "\"windupScale\":%.3f,\"followScale\":%.3f}",
        p.durationMs, p.curvature, p.jitterPx, p.windupScale, p.followScale
    );
    return std::string(buf);
}

}
