#pragma once

#include <array>
#include <cstdint>

#include "aether/core/SkillProfile.h"
#include "aether/memory/MemorySchema.h"
#include "aether/strategy/Strategist.h"

namespace aether {

struct CemAction {
    float angle = 0.0f;
    float power = 1.0f;
    float spin = 0.0f;
};

struct CemCandidate {
    CemAction action;
    float score = -1000000.0f;
};

struct CemPlannerConfig {
    static constexpr std::uint32_t MaxPopulation = 48U;
    static constexpr std::uint32_t MaxElite = 8U;

    std::uint32_t population = 32U;
    std::uint32_t elite = 6U;
    std::uint32_t iterations = 3U;
    float smoothing = 0.62f;
};

struct CemPlanResult {
    CemAction action;
    float score = 0.0f;
    float confidence = 0.0f;
    float risk = 0.0f;
    std::uint32_t evaluatedCandidates = 0U;
};

class CemPlanner {
public:
    explicit CemPlanner(
        CemPlannerConfig config = CemPlannerConfig()
    ) noexcept;

    CemPlanResult plan(
        const PhysicsExperienceState& state,
        const Intent& intent,
        const SkillProfile& skill,
        std::uint64_t seed
    ) const noexcept;

    PhysicsExperienceState planTelemetry(
        const PhysicsExperienceState& state,
        const Intent& intent,
        const SkillProfile& skill,
        std::uint64_t seed
    ) const noexcept;

private:
    CemPlannerConfig cfg;

    static float clampRange(float value, float lo, float hi) noexcept;
    static float wrapPi(float value) noexcept;
    static float absFloat(float value) noexcept;
    static float sqrtApprox(float value) noexcept;

    static std::uint64_t nextRandom(std::uint64_t& state) noexcept;
    static float uniform01(std::uint64_t& state) noexcept;
    static float normal01(std::uint64_t& state) noexcept;

    static float scoreCandidate(
        const PhysicsExperienceState& state,
        const Intent& intent,
        const SkillProfile& skill,
        const CemAction& action
    ) noexcept;

    static void sortDescending(
        std::array<CemCandidate, CemPlannerConfig::MaxPopulation>& candidates,
        std::uint32_t count
    ) noexcept;
};

}
