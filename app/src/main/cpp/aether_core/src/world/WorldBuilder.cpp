#include "aether/world/WorldBuilder.h"
#include "aether/foundation/Vec2.h"

namespace aether {

WorldState WorldBuilder::build(const PerceptionFrame& frame) const {
    WorldState world;
    world.meta.sequence = frame.sequence;
    world.meta.timestampNanos = frame.timestampNanos;
    world.meta.source = frame.source;
    world.meta.coordinateSpace = frame.coordinateSpace;

    // AT168 truth contract: mock/synthetic data may help diagnostics, but it
    // may not create real learning, real planning authority, or execution.
    world.meta.allowLearning = false;
    world.meta.allowExecution = false;
    world.meta.allowPlanning = frame.source == PerceptionSource::RealVision;

    double confidenceSum = 0.0;
    int count = 0;
    int activeBalls = 0;
    int activePockets = 0;

    for (const auto& p : frame.objects) {
        if (p.confidence <= 0.05) {
            continue;
        }

        ObjectState o;
        o.id = p.id;
        o.type = p.type;
        o.position = p.position;
        o.velocity = p.velocity;
        o.radius = p.radius;
        o.confidence = clamp01(p.confidence * frame.globalConfidence);
        o.active = true;

        world.objects.push_back(o);

        if (p.type == ObjectType::Ball) {
            BallState b;
            b.id = p.id;
            b.kind = p.id == 0 ? BallKind::Cue : BallKind::ObjectBall;
            b.position = p.position;
            b.velocity = p.velocity;
            b.radius = p.radius;
            b.confidence = o.confidence;
            b.visible = true;
            b.stable = true;
            world.balls.push_back(b);
            ++activeBalls;
        } else if (p.type == ObjectType::Pocket) {
            PocketState pocket;
            pocket.id = p.id;
            pocket.position = p.position;
            pocket.radius = p.radius;
            pocket.confidence = o.confidence;
            world.pockets.push_back(pocket);
            ++activePockets;
        }

        confidenceSum += o.confidence;
        count++;
    }

    const double avg = count > 0 ? confidenceSum / static_cast<double>(count) : 0.0;
    world.uncertainty = 1.0 - clamp01(avg);

    // Until a real table detector writes explicit geometry, treat normalized
    // table bounds as an unverified diagnostic fallback only.
    world.table.detected = false;
    world.table.min = Vec2(0.0, 0.0);
    world.table.max = Vec2(1.0, 1.0);
    world.table.confidence = 0.0;

    world.quality.overallConfidence = clamp01(avg * frame.globalConfidence);
    world.quality.uncertainty = world.uncertainty;
    world.quality.tableReliable = world.table.detected && world.table.confidence >= 0.70;
    world.quality.ballsReliable = activeBalls > 0 && avg >= 0.35;
    world.quality.pocketsReliable = activePockets >= 4;
    world.quality.cueReliable = world.cueBall() != nullptr;
    world.quality.missingCriticalObjects = world.quality.cueReliable ? 0 : 1;
    world.quality.unstableObjects = 0;

    return world;
}

}
