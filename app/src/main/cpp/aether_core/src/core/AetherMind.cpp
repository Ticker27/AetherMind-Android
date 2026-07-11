#include "aether/core/AetherMind.h"
#include "aether/foundation/Vec2.h"

#include <sstream>
#include <vector>

namespace aether {

namespace {

const char* issueName(ValidationIssueCode code) noexcept {
    switch (code) {
        case ValidationIssueCode::MockSource: return "MOCK_SOURCE";
        case ValidationIssueCode::UnknownSource: return "UNKNOWN_SOURCE";
        case ValidationIssueCode::TableNotVerified: return "TABLE_NOT_VERIFIED";
        case ValidationIssueCode::CueBallMissing: return "CUE_BALL_MISSING";
        case ValidationIssueCode::NoObjectBalls: return "NO_OBJECT_BALLS";
        case ValidationIssueCode::BallOutOfBounds: return "BALL_OUT_OF_BOUNDS";
        case ValidationIssueCode::LowConfidenceWorld: return "LOW_CONFIDENCE_WORLD";
        case ValidationIssueCode::ExecutionLocked: return "EXECUTION_LOCKED";
        case ValidationIssueCode::None:
        default: return "NONE";
    }
}

std::string explainValidationOnly(
    const WorldState& world,
    const ValidationReport& report
) {
    std::ostringstream ss;
    ss << "PROPOSE_ONLY diagnostic: source="
       << perceptionSourceName(world.meta.source)
       << " confidence=" << report.confidence
       << " execution=LOCKED";

    for (const auto& issue : report.issues) {
        ss << " | " << issueName(issue.code) << ": " << issue.message;
    }

    return ss.str();
}

void forceNoExecution(MindOutput& output) noexcept {
    output.executedAction = Action{};
    output.executionAngleError = 0.0;
    output.executionPowerError = 0.0;
    output.executionBlunder = false;
    output.executedPocketed = false;
    output.executedCuePocketed = false;
    output.executedFirstHitId = -1;
    output.executedTargetPocketDistance = 999.0;
    output.executedPocketMargin = -999.0;
    output.executedConfidence = 0.0;
    output.executedTravelDistance = 0.0;
}

} // namespace

AetherMind::AetherMind(
    SkillLevel level,
    const std::string& memoryName_
)
    : skill(makeSkillProfile(level)),
      memoryName(memoryName_) {}

MindOutput AetherMind::think(const MindInput& input) {
    WorldState world = worldBuilder.build(input.perception);
    const ValidationReport validation = worldValidator.validate(world);

    MindOutput output;
    output.confidence = validation.confidence;
    output.risk = validation.usable ? 0.45 : 1.0;
    output.expectedReward = 0.0;
    output.targetId = -1;
    output.memorySummary = "memory disabled in diagnostic/propose-only mode";
    output.memoryConfidenceBias = 0.0;
    output.memoryRiskBias = 0.0;
    output.explanation = explainValidationOnly(world, validation);

    if (!validation.usable) {
        forceNoExecution(output);
        return output;
    }

    auto candidates = actionGenerator.generate(world, skill);
    auto plans = planner.buildPlans(
        world,
        candidates,
        physicsKernel,
        evaluator,
        skill
    );

    output = decisionPolicy.choose(plans, skill);
    forceNoExecution(output);

    std::ostringstream ss;
    ss << "PROPOSE_ONLY human-like candidate; no command emitted. "
       << "source=" << perceptionSourceName(world.meta.source)
       << " worldConfidence=" << validation.confidence
       << " execution=LOCKED | "
       << output.explanation;

    for (const auto& issue : validation.issues) {
        ss << " | " << issueName(issue.code) << ": " << issue.message;
    }

    output.explanation = ss.str();
    output.memorySummary = "memory write blocked; diagnostic trace only";
    output.memoryConfidenceBias = 0.0;
    output.memoryRiskBias = 0.0;
    return output;
}

}
