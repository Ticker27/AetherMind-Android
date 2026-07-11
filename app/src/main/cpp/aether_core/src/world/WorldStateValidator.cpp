#include "aether/world/WorldStateValidator.h"

namespace aether {

namespace {

bool insideTable(const TableState& table, const BallState& ball) noexcept {
    if (!table.detected) {
        return true;
    }
    return ball.position.x >= table.min.x && ball.position.x <= table.max.x &&
           ball.position.y >= table.min.y && ball.position.y <= table.max.y;
}

}

ValidationReport WorldStateValidator::validate(const WorldState& world) const {
    ValidationReport report;
    report.confidence = world.quality.overallConfidence;
    report.diagnosticOnly = true;
    report.executionAllowed = false;

    if (world.meta.source == PerceptionSource::Unknown) {
        report.add(
            ValidationIssueCode::UnknownSource,
            "perception source is unknown; trust cannot be upgraded",
            0.75
        );
    }

    if (world.meta.source == PerceptionSource::Mock ||
        world.meta.source == PerceptionSource::SyntheticTest) {
        report.add(
            ValidationIssueCode::MockSource,
            "mock/synthetic perception may be used only for diagnostics",
            0.85
        );
    }

    if (!world.table.detected || world.table.confidence < 0.50) {
        report.add(
            ValidationIssueCode::TableNotVerified,
            "table geometry is not verified",
            0.70
        );
    }

    bool cueFound = false;
    int objectBallCount = 0;
    for (const auto& ball : world.balls) {
        if (ball.kind == BallKind::Cue || ball.id == 0) {
            cueFound = true;
        } else {
            ++objectBallCount;
        }

        if (!insideTable(world.table, ball)) {
            report.add(
                ValidationIssueCode::BallOutOfBounds,
                "ball is outside verified table bounds",
                0.55
            );
        }
    }

    if (!cueFound) {
        report.add(
            ValidationIssueCode::CueBallMissing,
            "cue ball is missing from WorldState",
            0.90
        );
    }

    if (objectBallCount == 0) {
        report.add(
            ValidationIssueCode::NoObjectBalls,
            "no object balls available for reasoning",
            0.70
        );
    }

    if (world.quality.overallConfidence < 0.35) {
        report.add(
            ValidationIssueCode::LowConfidenceWorld,
            "overall WorldState confidence is too low",
            0.65
        );
    }

    report.add(
        ValidationIssueCode::ExecutionLocked,
        "runtime is locked to propose-only diagnostic mode",
        1.0
    );

    report.usable = cueFound && objectBallCount > 0 && world.quality.overallConfidence >= 0.20;
    report.confidence = world.quality.overallConfidence;
    return report;
}

}
