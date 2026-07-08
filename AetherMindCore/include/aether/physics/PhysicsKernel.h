#pragma once

#include <vector>

#include "aether/core/MindTypes.h"
#include "aether/world/WorldState.h"

namespace aether {

struct SimObject {
    int id = -1;
    ObjectType type = ObjectType::Unknown;
    Vec2 position;
    Vec2 previousPosition;
    Vec2 velocity;
    double radius = 0.025;
    bool active = true;
};

struct SimulationResult {
    std::vector<SimObject> objects;
    std::vector<int> pocketedIds;

    int targetId = -1;
    int firstHitId = -1;

    bool targetPocketed = false;
    bool cuePocketed = false;

    int collisionCount = 0;

    double travelDistance = 0.0;
    double remainingEnergy = 0.0;
    double confidence = 0.0;

    double targetClosestPocketDistance = 999.0;
    double targetEffectivePocketRadius = 0.0;
    double targetPocketMargin = -999.0;

    Vec2 targetFinalPosition;

    double robustness = 0.0;
    int robustnessSamples = 0;
    int robustnessPocketed = 0;
};

struct PhysicsConfig {
    double timeStep = 0.008;
    double maxTime = 4.0;
    double friction = 0.35;
    double railBounce = 0.82;

    double minX = 0.0;
    double maxX = 1.0;
    double minY = 0.0;
    double maxY = 1.0;

    double pocketRadius = 0.055;
};

class PhysicsKernel {
public:
    SimulationResult simulate(
        const WorldState& world,
        const Action& action
    ) const;

    SimulationResult simulate(
        const WorldState& world,
        const Action& action,
        const PhysicsConfig& config
    ) const;

private:
    std::vector<SimObject> copyObjects(const WorldState& world) const;

    void applyAction(
        std::vector<SimObject>& objects,
        const Action& action
    ) const;

    void stepMotion(
        std::vector<SimObject>& objects,
        const PhysicsConfig& config,
        double& travelDistance
    ) const;

    void resolveBallCollisions(
        std::vector<SimObject>& objects,
        SimulationResult& result
    ) const;

    void detectPockets(
        SimulationResult& result,
        const std::vector<SimObject>& pockets,
        const PhysicsConfig& config
    ) const;

    void updateTargetDiagnostics(
        SimulationResult& result,
        const std::vector<SimObject>& pockets
    ) const;

    double totalEnergy(const std::vector<SimObject>& objects) const;
};

}
