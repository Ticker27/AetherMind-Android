#include "aether/runtime/BrainRuntime.h"

#include <algorithm>
#include <cmath>
#include <iostream>
#include <memory>

namespace aether {

static float clamp01f(float value) {
    return std::max(0.0f, std::min(1.0f, value));
}

static float distance2d(
    const PhysicsVector2D& a,
    const PhysicsVector2D& b
) {
    float dx = a.x - b.x;
    float dy = a.y - b.y;

    return std::sqrt(dx * dx + dy * dy);
}

BrainRuntime::BrainRuntime(
    MemoryStore& memoryStore_
)
    : memoryStore(memoryStore_),
      cycleCounter(0),
      publishedVersion(0) {}

BrainRuntime::SnapshotPtr BrainRuntime::prepareSnapshot(
    const PhysicsExperienceState& state,
    std::uint64_t version
) {
    auto snapshot =
        std::make_shared<BrainRuntimeSnapshot>();

    snapshot->state = state;
    snapshot->version = version;

    return snapshot;
}

void BrainRuntime::publishPreparedSnapshotLocked(
    const SnapshotPtr& preparedSnapshot
) {
    std::atomic_store_explicit(
        &currentSnapshot,
        preparedSnapshot,
        std::memory_order_release
    );

    publishedVersion.store(
        preparedSnapshot ? preparedSnapshot->version : 0,
        std::memory_order_release
    );
}

void BrainRuntime::publishSnapshot(
    const PhysicsExperienceState& state,
    std::uint64_t version,
    std::ostream* log
) {
    SnapshotPtr prepared =
        prepareSnapshot(state, version);

    if (log) {
        *log << "[SNAPSHOT] Prepared version="
             << version
             << "\n";
    }

    {
        auto lock = memoryStore.acquireWriteLock();

        if (log) {
            *log << "[LOCK ACQUIRED] Runtime snapshot swap version="
                 << version
                 << "\n";
        }

        publishPreparedSnapshotLocked(prepared);
    }

    if (log) {
        *log << "[LOCK RELEASED] Runtime snapshot swap version="
             << version
             << "\n";
    }
}

BrainInferenceOutput BrainRuntime::runInference(
    const BrainInferenceInput& input
) const {
    SnapshotPtr snapshot;

    {
        auto lock = memoryStore.acquireReadLock();

        snapshot = std::atomic_load_explicit(
            &currentSnapshot,
            std::memory_order_acquire
        );
    }

    BrainInferenceOutput output;
    output.cycle =
        cycleCounter.fetch_add(
            1,
            std::memory_order_relaxed
        ) + 1;

    if (!snapshot) {
        output.hasSnapshot = false;
        return output;
    }

    output.hasSnapshot = true;
    output.snapshotVersion = snapshot->version;

    const PhysicsExperienceState* state =
        &snapshot->state;

    float cueTargetDistance =
        distance2d(
            state->cuePosition,
            state->targetPosition
        );

    float observedDrift =
        distance2d(
            state->targetPosition,
            input.observedState.targetPosition
        );

    float anglePenalty =
        std::min(1.0f, std::fabs(state->angleOffset) * 8.0f);

    float powerPenalty =
        std::min(1.0f, std::fabs(state->powerScale - 1.0f) * 2.0f);

    float bouncePenalty =
        std::min(
            1.0f,
            static_cast<float>(state->cushionBounceCount) * 0.18f
        );

    float marginValue =
        clamp01f(0.5f + state->errorMargin * 8.0f);

    output.aimScore = clamp01f(
        0.72f -
        cueTargetDistance * 0.18f -
        observedDrift * 0.25f -
        anglePenalty * 0.12f -
        powerPenalty * 0.08f -
        bouncePenalty * 0.06f +
        marginValue * 0.16f
    );

    output.confidence = clamp01f(
        output.aimScore +
        state->confidenceBias
    );

    output.risk = clamp01f(
        1.0f -
        output.aimScore +
        state->riskBias
    );

    return output;
}

std::uint64_t BrainRuntime::inferenceCount() const {
    return cycleCounter.load(std::memory_order_relaxed);
}

std::uint64_t BrainRuntime::latestSnapshotVersion() const {
    return publishedVersion.load(std::memory_order_acquire);
}

}
