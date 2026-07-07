#pragma once

#include <cstdint>
#include <type_traits>

namespace aether {

inline constexpr const char* AETHER_MEMORY_MAGIC = "AETHER_MEMORY";
inline constexpr const char* AETHER_EXPERIENCE_KIND = "EXPERIENCE";
inline constexpr int AETHER_EXPERIENCE_SCHEMA_VERSION = 1;

inline constexpr std::uint32_t AETHER_PHYSICS_EXPERIENCE_STATE_VERSION = 1;

struct alignas(8) PhysicsVector2D {
    float x = 0.0f;
    float y = 0.0f;
};

struct alignas(8) PhysicsExperienceState {
    std::uint32_t layoutVersion = AETHER_PHYSICS_EXPERIENCE_STATE_VERSION;
    std::uint32_t flags = 0;

    PhysicsVector2D cuePosition;
    PhysicsVector2D targetPosition;

    float angleOffset = 0.0f;
    float powerScale = 1.0f;
    float velocityScale = 1.0f;
    float errorMargin = 0.0f;

    float confidenceBias = 0.0f;
    float riskBias = 0.0f;

    std::uint32_t cushionBounceCount = 0;
    std::uint32_t reserved0 = 0;

    std::uint64_t timestampNanos = 0;
};

static_assert(
    std::is_trivially_copyable<PhysicsVector2D>::value,
    "PhysicsVector2D must be trivially copyable"
);

static_assert(
    std::is_trivially_copyable<PhysicsExperienceState>::value,
    "PhysicsExperienceState must be trivially copyable"
);

static_assert(
    alignof(PhysicsExperienceState) == 8,
    "PhysicsExperienceState must be 8-byte aligned"
);

static_assert(
    sizeof(PhysicsExperienceState) == 64,
    "PhysicsExperienceState ABI size must remain stable"
);

}
