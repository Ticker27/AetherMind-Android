#pragma once

#include <cstdint>
#include <vector>
#include "aether/foundation/Vec2.h"

namespace aether {

// AT168 Core Truth Recovery
// WorldState is the only input the brain is allowed to reason from.
// Perception may be mock, synthetic, accessibility-only, or real vision; the
// source must be explicit so trust/learning/execution cannot be accidentally
// upgraded from fake data.
enum class PerceptionSource {
    Unknown,
    Mock,
    SyntheticTest,
    RealVision,
    AccessibilityOnly
};

enum class CoordinateSpace {
    Unknown,
    NormalizedTable,
    ScreenPixels
};

enum class ObjectType {
    Unknown,
    Ball,
    Pocket,
    Rail,
    Obstacle
};

enum class BallKind {
    Unknown,
    Cue,
    ObjectBall
};

struct WorldStateMeta {
    std::uint64_t sequence = 0;
    std::uint64_t timestampNanos = 0;
    PerceptionSource source = PerceptionSource::Unknown;
    CoordinateSpace coordinateSpace = CoordinateSpace::NormalizedTable;

    bool allowLearning = false;
    bool allowPlanning = false;
    bool allowExecution = false;
};

struct TableState {
    bool detected = false;
    Vec2 min = Vec2(0.0, 0.0);
    Vec2 max = Vec2(1.0, 1.0);
    double confidence = 0.0;
};

struct BallState {
    int id = -1;
    BallKind kind = BallKind::Unknown;
    Vec2 position;
    Vec2 velocity;
    double radius = 0.025;
    double confidence = 0.0;
    bool visible = true;
    bool stable = true;
};

struct PocketState {
    int id = -1;
    Vec2 position;
    double radius = 0.04;
    double confidence = 0.0;
};

struct StateQuality {
    double overallConfidence = 0.0;
    double uncertainty = 1.0;
    bool tableReliable = false;
    bool ballsReliable = false;
    bool pocketsReliable = false;
    bool cueReliable = false;
    int missingCriticalObjects = 0;
    int unstableObjects = 0;
};

struct PerceptionObject {
    int id = -1;
    ObjectType type = ObjectType::Unknown;
    Vec2 position;
    Vec2 velocity;
    double radius = 0.025;
    double confidence = 1.0;
};

struct PerceptionFrame {
    std::uint64_t sequence = 0;
    std::uint64_t timestampNanos = 0;
    PerceptionSource source = PerceptionSource::Unknown;
    CoordinateSpace coordinateSpace = CoordinateSpace::NormalizedTable;
    std::vector<PerceptionObject> objects;
    double globalConfidence = 1.0;
};

// Kept for legacy planner compatibility. New code should prefer BallState,
// PocketState and TableState through WorldState below.
struct ObjectState {
    int id = -1;
    ObjectType type = ObjectType::Unknown;
    Vec2 position;
    Vec2 velocity;
    double radius = 0.025;
    double confidence = 1.0;
    bool active = true;
};

struct WorldState {
    WorldStateMeta meta;

    TableState table;
    std::vector<BallState> balls;
    std::vector<PocketState> pockets;

    // Legacy compatibility view used by older planners. This must remain a
    // derivative view, not the canonical brain model.
    std::vector<ObjectState> objects;
    double uncertainty = 1.0;

    StateQuality quality;

    const ObjectState* findObject(int id) const {
        for (const auto& o : objects) {
            if (o.id == id) {
                return &o;
            }
        }
        return nullptr;
    }

    const ObjectState* cueBall() const {
        return findObject(0);
    }
};

inline const char* perceptionSourceName(PerceptionSource source) noexcept {
    switch (source) {
        case PerceptionSource::Mock: return "MOCK";
        case PerceptionSource::SyntheticTest: return "SYNTHETIC_TEST";
        case PerceptionSource::RealVision: return "REAL_VISION";
        case PerceptionSource::AccessibilityOnly: return "ACCESSIBILITY_ONLY";
        case PerceptionSource::Unknown:
        default: return "UNKNOWN";
    }
}

}
