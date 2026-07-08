#pragma once

#include <vector>
#include "aether/foundation/Vec2.h"

namespace aether {

enum class ObjectType {
    Unknown,
    Ball,
    Pocket,
    Rail,
    Obstacle
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
    std::vector<PerceptionObject> objects;
    double globalConfidence = 1.0;
};

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
    std::vector<ObjectState> objects;
    double uncertainty = 0.0;

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

}
