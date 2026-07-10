#pragma once

#include "aether/pool/geometry/PoolGeometryTypes.h"

namespace aether::pool::geometry {

struct GhostBallResult {
    bool valid = false;
    Vec2 objectToPocketDirection{};
    Vec2 ghostBallCenter{};
    Vec2 contactPoint{};
    Vec2 collisionNormal{};
    double objectToPocketDistance = 0.0;
    GeometryFailureReason failureReason = GeometryFailureReason::INVALID_INPUT;
};

class GhostBallSolver {
public:
    GhostBallResult solve(const Ball& objectBall, const Pocket& targetPocket) const;
};

} // namespace aether::pool::geometry
