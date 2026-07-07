#pragma once

#include <array>
#include <atomic>
#include <cstdint>

#include "aether/memory/MemorySchema.h"
#include "aether/core/SkillProfile.h"
#include "aether/humanizer/Humanizer.h"
#include "aether/planning/CemPlanner.h"
#include "aether/strategy/Strategist.h"

namespace aether {

class NativeTrajectoryBridge {
public:
    NativeTrajectoryBridge();

    void publishState(
        const PhysicsExperienceState& state
    );

    bool copyLatestState(
        PhysicsExperienceState& out
    ) const;

    bool copyLatestStrategicState(
        PhysicsExperienceState& out,
        const SkillProfile& skill
    );

    std::uint64_t publishedCount() const;

    std::uint32_t activeSlotIndex() const;

private:
    static constexpr std::uint32_t SLOT_COUNT = 2;

    std::array<PhysicsExperienceState, SLOT_COUNT> slots;
    mutable std::array<std::atomic<std::uint32_t>, SLOT_COUNT> readerCounts;

    std::atomic<std::uint32_t> activeIndex;
    std::atomic<std::uint64_t> publishCounter;
    std::atomic<bool> hasPublished;
    std::atomic_flag publishLock = ATOMIC_FLAG_INIT;

    Strategist strategist;
    CemPlanner planner;
    Humanizer humanizer;
    HumanizerState humanizerState;

    void lockPublisher() noexcept;
    void unlockPublisher() noexcept;
};

NativeTrajectoryBridge& globalTrajectoryBridge();

}
