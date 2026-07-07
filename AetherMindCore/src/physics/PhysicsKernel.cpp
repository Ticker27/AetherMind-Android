#include "aether/physics/PhysicsKernel.h"
#include "aether/foundation/Vec2.h"

#include <algorithm>
#include <cmath>

namespace aether {

static double pointSegmentDistance(
    const Vec2& a,
    const Vec2& b,
    const Vec2& p
) {
    Vec2 ab = b - a;
    double abLen2 = ab.lengthSquared();

    if (abLen2 <= 1e-12) {
        return distance(a, p);
    }

    double t = dot(p - a, ab) / abLen2;
    t = clamp(t, 0.0, 1.0);

    Vec2 closest = a + ab * t;
    return distance(closest, p);
}

std::vector<SimObject> PhysicsKernel::copyObjects(const WorldState& world) const {
    std::vector<SimObject> objects;

    for (const auto& o : world.objects) {
        SimObject s;
        s.id = o.id;
        s.type = o.type;
        s.position = o.position;
        s.previousPosition = o.position;
        s.velocity = o.velocity;
        s.radius = o.radius;
        s.active = o.active;

        objects.push_back(s);
    }

    return objects;
}

void PhysicsKernel::applyAction(
    std::vector<SimObject>& objects,
    const Action& action
) const {
    for (auto& o : objects) {
        if (o.id == 0 && o.type == ObjectType::Ball && o.active) {
            o.velocity = action.direction.normalized() * action.power;
            return;
        }
    }
}

void PhysicsKernel::stepMotion(
    std::vector<SimObject>& objects,
    const PhysicsConfig& config,
    double& travelDistance
) const {
    double retentionPerSecond = clamp(config.friction, 0.01, 0.999);
    double damping = std::pow(retentionPerSecond, config.timeStep);

    for (auto& o : objects) {
        if (!o.active || o.type != ObjectType::Ball) {
            continue;
        }

        Vec2 before = o.position;
        o.previousPosition = before;

        o.position = o.position + o.velocity * config.timeStep;

        travelDistance += distance(before, o.position);

        if (o.position.x - o.radius < config.minX) {
            o.position.x = config.minX + o.radius;
            o.velocity.x = std::abs(o.velocity.x) * config.railBounce;
        }

        if (o.position.x + o.radius > config.maxX) {
            o.position.x = config.maxX - o.radius;
            o.velocity.x = -std::abs(o.velocity.x) * config.railBounce;
        }

        if (o.position.y - o.radius < config.minY) {
            o.position.y = config.minY + o.radius;
            o.velocity.y = std::abs(o.velocity.y) * config.railBounce;
        }

        if (o.position.y + o.radius > config.maxY) {
            o.position.y = config.maxY - o.radius;
            o.velocity.y = -std::abs(o.velocity.y) * config.railBounce;
        }

        o.velocity = o.velocity * damping;

        if (o.velocity.length() < 0.002) {
            o.velocity = Vec2{};
        }
    }
}

void PhysicsKernel::resolveBallCollisions(
    std::vector<SimObject>& objects,
    SimulationResult& result
) const {
    for (size_t i = 0; i < objects.size(); ++i) {
        SimObject& a = objects[i];

        if (!a.active || a.type != ObjectType::Ball) {
            continue;
        }

        for (size_t j = i + 1; j < objects.size(); ++j) {
            SimObject& b = objects[j];

            if (!b.active || b.type != ObjectType::Ball) {
                continue;
            }

            Vec2 delta = b.position - a.position;
            double dist = delta.length();
            double minDist = a.radius + b.radius;

            if (dist <= 1e-9 || dist > minDist) {
                continue;
            }

            Vec2 normal = delta / dist;

            double overlap = minDist - dist;
            a.position -= normal * (overlap * 0.5);
            b.position += normal * (overlap * 0.5);

            double va = dot(a.velocity, normal);
            double vb = dot(b.velocity, normal);

            double impulse = vb - va;

            a.velocity += normal * impulse;
            b.velocity -= normal * impulse;

            result.collisionCount++;

            if (result.firstHitId < 0) {
                if (a.id == 0 && b.id != 0) {
                    result.firstHitId = b.id;
                } else if (b.id == 0 && a.id != 0) {
                    result.firstHitId = a.id;
                }
            }
        }
    }
}

void PhysicsKernel::detectPockets(
    SimulationResult& result,
    const std::vector<SimObject>& pockets,
    const PhysicsConfig& config
) const {
    for (auto& o : result.objects) {
        if (!o.active || o.type != ObjectType::Ball) {
            continue;
        }

        for (const auto& p : pockets) {
            if (p.type != ObjectType::Pocket) {
                continue;
            }

            double pocketR = p.radius > 0.0 ? p.radius : config.pocketRadius;

            double speed = o.velocity.length();
            double captureBoost = o.radius * 0.85;

            if (speed > 0.75) {
                captureBoost *= 0.65;
            }

            double effectivePocketRadius = pocketR + captureBoost;

            double centerDistance = distance(o.position, p.position);
            double sweptDistance = pointSegmentDistance(
                o.previousPosition,
                o.position,
                p.position
            );

            double bestDistance = std::min(centerDistance, sweptDistance);
            double margin = effectivePocketRadius - bestDistance;

            if (o.id == result.targetId) {
                if (bestDistance < result.targetClosestPocketDistance) {
                    result.targetClosestPocketDistance = bestDistance;
                    result.targetEffectivePocketRadius = effectivePocketRadius;
                    result.targetPocketMargin = margin;
                }
            }

            if (bestDistance <= effectivePocketRadius) {
                o.active = false;
                o.velocity = Vec2{};

                result.pocketedIds.push_back(o.id);

                if (o.id == 0) {
                    result.cuePocketed = true;
                }

                if (o.id == result.targetId) {
                    result.targetPocketed = true;
                    result.targetClosestPocketDistance = bestDistance;
                    result.targetEffectivePocketRadius = effectivePocketRadius;
                    result.targetPocketMargin = margin;
                }

                break;
            }
        }
    }
}

void PhysicsKernel::updateTargetDiagnostics(
    SimulationResult& result,
    const std::vector<SimObject>& pockets
) const {
    for (const auto& o : result.objects) {
        if (o.id != result.targetId || o.type != ObjectType::Ball) {
            continue;
        }

        result.targetFinalPosition = o.position;

        double bestDistance = 999.0;
        double bestEffectiveRadius = 0.0;
        double bestMargin = -999.0;

        for (const auto& p : pockets) {
            if (p.type != ObjectType::Pocket) {
                continue;
            }

            double pocketR = p.radius > 0.0 ? p.radius : 0.055;
            double effectiveRadius = pocketR + o.radius * 0.85;
            double d = distance(o.position, p.position);
            double margin = effectiveRadius - d;

            if (d < bestDistance) {
                bestDistance = d;
                bestEffectiveRadius = effectiveRadius;
            }

            if (margin > bestMargin) {
                bestMargin = margin;
            }
        }

        if (bestDistance < result.targetClosestPocketDistance) {
            result.targetClosestPocketDistance = bestDistance;
            result.targetEffectivePocketRadius = bestEffectiveRadius;
            result.targetPocketMargin = bestMargin;
        }

        return;
    }
}

double PhysicsKernel::totalEnergy(const std::vector<SimObject>& objects) const {
    double e = 0.0;

    for (const auto& o : objects) {
        if (o.active && o.type == ObjectType::Ball) {
            e += o.velocity.lengthSquared();
        }
    }

    return e;
}

SimulationResult PhysicsKernel::simulate(
    const WorldState& world,
    const Action& action
) const {
    PhysicsConfig config;
    return simulate(world, action, config);
}

SimulationResult PhysicsKernel::simulate(
    const WorldState& world,
    const Action& action,
    const PhysicsConfig& config
) const {
    SimulationResult result;
    result.objects = copyObjects(world);
    result.targetId = action.targetId;

    std::vector<SimObject> pockets;
    for (const auto& o : result.objects) {
        if (o.type == ObjectType::Pocket) {
            pockets.push_back(o);
        }
    }

    applyAction(result.objects, action);

    double t = 0.0;

    while (t < config.maxTime) {
        stepMotion(result.objects, config, result.travelDistance);
        resolveBallCollisions(result.objects, result);
        detectPockets(result, pockets, config);

        result.remainingEnergy = totalEnergy(result.objects);

        if (result.remainingEnergy < 0.00001) {
            break;
        }

        t += config.timeStep;
    }

    updateTargetDiagnostics(result, pockets);

    result.confidence = result.targetPocketed ? 0.85 : 0.35;

    if (result.firstHitId == result.targetId) {
        result.confidence += 0.10;
    }

    if (result.collisionCount == 0) {
        result.confidence *= 0.50;
    }

    if (result.cuePocketed) {
        result.confidence *= 0.35;
    }

    result.confidence = clamp01(result.confidence);

    return result;
}

}
