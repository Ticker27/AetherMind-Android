#include "aether/pool/geometry/CutAngleSolver.h"

namespace aether::pool::geometry {

CutAngleResult CutAngleSolver::compute(
    const Ball& cueBall,
    const Vec2& ghostBallCenter,
    const Vec2& objectToPocketDirection
) const {
    CutAngleResult result{};

    if (!isValidBall(cueBall) ||
        !isFinite(ghostBallCenter) ||
        !isFinite(objectToPocketDirection) ||
        !canNormalize(objectToPocketDirection)) {
        result.valid = false;
        result.failureReason = GeometryFailureReason::INVALID_INPUT;
        return result;
    }

    const Vec2 cueToGhost = subtract(ghostBallCenter, cueBall.center);
    const double cueToGhostDistance = length(cueToGhost);

    if (cueToGhostDistance <= EPSILON || !isFinite(cueToGhostDistance)) {
        result.valid = false;
        result.failureReason = GeometryFailureReason::ZERO_LENGTH_CUE_TO_GHOST;
        return result;
    }

    const Vec2 cueToGhostDirection = normalizeOrZero(cueToGhost);
    const Vec2 objectDirection = normalizeOrZero(objectToPocketDirection);

    const double cosine = clampDouble(dot(cueToGhostDirection, objectDirection), -1.0, 1.0);
    const double cutAngleDegrees = radiansToDegrees(std::acos(cosine));

    result.valid = true;
    result.cueToGhostDirection = cueToGhostDirection;
    result.cueToGhostDistance = cueToGhostDistance;
    result.cutAngleDegrees = cutAngleDegrees;
    result.failureReason = GeometryFailureReason::NONE;

    return result;
}

} // namespace aether::pool::geometry
