#include "aether/render/NativeTrajectoryBridge.h"

#include <thread>

namespace aether {

NativeTrajectoryBridge::NativeTrajectoryBridge()
    : slots{},
      readerCounts{},
      activeIndex(0),
      publishCounter(0),
      hasPublished(false),
      publishLock(ATOMIC_FLAG_INIT),
      strategist(),
      planner(),
      humanizer(),
      humanizerState() {
    for (auto& count : readerCounts) {
        count.store(0, std::memory_order_relaxed);
    }
}

void NativeTrajectoryBridge::lockPublisher() noexcept {
    while (publishLock.test_and_set(std::memory_order_acquire)) {
        std::this_thread::yield();
    }
}

void NativeTrajectoryBridge::unlockPublisher() noexcept {
    publishLock.clear(std::memory_order_release);
}

void NativeTrajectoryBridge::publishState(
    const PhysicsExperienceState& state
) {
    lockPublisher();

    const std::uint32_t current = activeIndex.load(std::memory_order_acquire);
    const std::uint32_t next = current ^ 1U;

    while (readerCounts[next].load(std::memory_order_acquire) != 0U) {
        std::this_thread::yield();
    }

    slots[next] = state;

    activeIndex.store(next, std::memory_order_release);
    hasPublished.store(true, std::memory_order_release);

    publishCounter.fetch_add(
        1,
        std::memory_order_relaxed
    );

    unlockPublisher();
}

bool NativeTrajectoryBridge::copyLatestState(
    PhysicsExperienceState& out
) const {
    if (!hasPublished.load(std::memory_order_acquire)) {
        return false;
    }

    for (;;) {
        const std::uint32_t index = activeIndex.load(std::memory_order_acquire);

        readerCounts[index].fetch_add(1U, std::memory_order_acquire);

        if (index == activeIndex.load(std::memory_order_acquire)) {
            out = slots[index];

            readerCounts[index].fetch_sub(1U, std::memory_order_release);
            return true;
        }

        readerCounts[index].fetch_sub(1U, std::memory_order_release);
        std::this_thread::yield();
    }
}


bool NativeTrajectoryBridge::copyLatestStrategicState(
    PhysicsExperienceState& out,
    const SkillProfile& skill
) {
    PhysicsExperienceState latest;

    if (!copyLatestState(latest)) {
        return false;
    }

    const Intent intent = strategist.selectIntent(latest, skill);

    const PhysicsExperienceState planned = planner.planTelemetry(
        latest,
        intent,
        skill,
        publishedCount() ^ latest.timestampNanos
    );

    out = humanizer.applyTelemetry(
        planned,
        skill,
        humanizerState
    );

    out.flags |= 0x00000200U;
    out.layoutVersion = AETHER_PHYSICS_EXPERIENCE_STATE_VERSION;

    return true;
}

std::uint64_t NativeTrajectoryBridge::publishedCount() const {
    return publishCounter.load(
        std::memory_order_relaxed
    );
}

std::uint32_t NativeTrajectoryBridge::activeSlotIndex() const {
    return activeIndex.load(std::memory_order_acquire);
}

NativeTrajectoryBridge& globalTrajectoryBridge() {
    static NativeTrajectoryBridge bridge;
    return bridge;
}

}
