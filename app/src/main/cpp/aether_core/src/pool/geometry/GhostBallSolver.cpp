#include "aether/pool/geometry/GhostBallSolver.h"

namespace aether::pool::geometry {

GhostBallResult GhostBallSolver::solve(const Ball& objectBall, const Pocket& targetPocket) const {
    GhostBallResult result{};

    if (!isValidBall(objectBall) || !isValidPocket(targetPocket)) {
        result.valid = false;
        result.failureReason = GeometryFailureReason::INVALID_INPUT;
        return result;
    }

    const Vec2 objectToPocket = subtract(targetPocket.center, objectBall.center);
    const double objectToPocketDistance = length(objectToPocket);

    if (objectToPocketDistance <= EPSILON || !isFinite(objectToPocketDistance)) {
        result.valid = false;
        result.failureReason = GeometryFailureReason::ZERO_LENGTH_OBJECT_TO_POCKET;
        return result;
    }

    const Vec2 objectToPocketDirection = normalizeOrZero(objectToPocket);

    const Vec2 ghostBallCenter = subtract(
        objectBall.center,
        multiply(objectToPocketDirection, objectBall.radius * 2.0)
    );

    const Vec2 contactPoint = subtract(
        objectBall.center,
        multiply(objectToPocketDirection, objectBall.radius)
    );

    const Vec2 collisionNormal = normalizeOrZero(
        subtract(objectBall.center, ghostBallCenter)
    );

    result.valid = true;
    result.objectToPocketDirection = objectToPocketDirection;
    result.ghostBallCenter = ghostBallCenter;
    result.contactPoint = contactPoint;
    result.collisionNormal = collisionNormal;
    result.objectToPocketDistance = objectToPocketDistance;
    result.failureReason = GeometryFailureReason::NONE;

    return result;
}

} // namespace aether::pool::geometry
