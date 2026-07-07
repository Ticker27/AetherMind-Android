#pragma once

#include <atomic>
#include <cstdint>
#include <iosfwd>
#include <memory>

#include "aether/memory/MemorySchema.h"
#include "aether/memory/MemoryStore.h"

namespace aether {

struct BrainRuntimeSnapshot {
    PhysicsExperienceState state;
    std::uint64_t version = 0;
};

struct BrainInferenceInput {
    PhysicsExperienceState observedState;
    float deltaSeconds = 0.016667f;
};

struct BrainInferenceOutput {
    bool hasSnapshot = false;

    std::uint64_t snapshotVersion = 0;
    std::uint64_t cycle = 0;

    float aimScore = 0.0f;
    float confidence = 0.0f;
    float risk = 0.0f;
};

class BrainRuntime {
public:
    using SnapshotPtr = std::shared_ptr<const BrainRuntimeSnapshot>;

    explicit BrainRuntime(
        MemoryStore& memoryStore
    );

    static SnapshotPtr prepareSnapshot(
        const PhysicsExperienceState& state,
        std::uint64_t version
    );

    void publishSnapshot(
        const PhysicsExperienceState& state,
        std::uint64_t version,
        std::ostream* log = nullptr
    );

    void publishPreparedSnapshotLocked(
        const SnapshotPtr& preparedSnapshot
    );

    BrainInferenceOutput runInference(
        const BrainInferenceInput& input
    ) const;

    std::uint64_t inferenceCount() const;
    std::uint64_t latestSnapshotVersion() const;

private:
    MemoryStore& memoryStore;

    mutable std::shared_ptr<const BrainRuntimeSnapshot> currentSnapshot;

    mutable std::atomic<std::uint64_t> cycleCounter;
    std::atomic<std::uint64_t> publishedVersion;
};

}
