#include "aether/world/WorldBuilder.h"
#include "aether/foundation/Vec2.h"

namespace aether {

WorldState WorldBuilder::build(const PerceptionFrame& frame) const {
    WorldState world;

    double confidenceSum = 0.0;
    int count = 0;

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

        confidenceSum += o.confidence;
        count++;
    }

    double avg = count > 0 ? confidenceSum / static_cast<double>(count) : 0.0;
    world.uncertainty = 1.0 - clamp01(avg);

    return world;
}

}
