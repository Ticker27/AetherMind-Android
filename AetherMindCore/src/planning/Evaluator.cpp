#include "aether/planning/Evaluator.h"
#include "aether/foundation/Vec2.h"

namespace aether {

PlanScore Evaluator::evaluate(
    const WorldState& world,
    const ActionCandidate& candidate,
    const SimulationResult& simulation,
    const SkillProfile& skill
) const {
    PlanScore p;

    double worldConfidence = clamp01(1.0 - world.uncertainty);
    double difficulty = clamp01(candidate.estimatedDifficulty);

    double executionConfidence =
        skill.aimAccuracy * 0.30 +
        skill.powerControl * 0.22 +
        skill.physicsUnderstanding * 0.28 +
        worldConfidence * 0.20;

    double firstHitTarget =
        simulation.firstHitId == simulation.targetId ? 1.0 : 0.0;

    double noHitPenalty =
        simulation.collisionCount == 0 ? 1.0 : 0.0;

    double wrongFirstHitPenalty =
        simulation.firstHitId >= 0 && simulation.firstHitId != simulation.targetId ? 1.0 : 0.0;

    double travelPenalty = clamp01(simulation.travelDistance / 3.0);

    p.physicsConfidence = clamp01(simulation.confidence);
    p.safetyPenalty = simulation.cuePocketed ? 1.0 : 0.0;

    if (candidate.action.type == ActionType::Safety) {
        double targetFarFromPocket = clamp01(
            simulation.targetClosestPocketDistance / 0.42
        );

        double targetNotPocketed = simulation.targetPocketed ? 0.0 : 1.0;
        double cueSafe = simulation.cuePocketed ? 0.0 : 1.0;

        double safetySkill =
            skill.safetyAwareness * 0.65 +
            skill.positionPlanning * 0.35;

        p.pocketReward = 0.0;

        p.confidence = clamp01(
            executionConfidence * 0.28 +
            firstHitTarget * 0.22 +
            cueSafe * 0.14 +
            candidate.priorScore * 0.10 +
            safetySkill * 0.16 +
            targetFarFromPocket * 0.10
        );

        p.risk = clamp01(
            noHitPenalty * 0.42 +
            wrongFirstHitPenalty * 0.36 +
            p.safetyPenalty * 0.55 +
            simulation.targetPocketed * 0.25 +
            difficulty * 0.18 +
            travelPenalty * 0.08
        );

        p.reward = clamp01(
            firstHitTarget * 0.14 +
            cueSafe * 0.12 +
            targetNotPocketed * 0.08 +
            targetFarFromPocket * (0.10 + safetySkill * 0.12) +
            safetySkill * 0.13
        );

        p.score = clamp01(
            p.reward * 0.58 +
            p.confidence * 0.22 -
            p.risk * 0.56
        );

        return p;
    }

    double marginScore = clamp01(
        (simulation.targetPocketMargin + 0.015) / 0.075
    );

    double pocketQuality = simulation.targetPocketed
        ? clamp01(0.35 + marginScore * 0.65)
        : 0.0;

    p.pocketReward = pocketQuality;

    double proximityReward = clamp01(
        1.0 - simulation.targetClosestPocketDistance / 0.60
    );

    double robustnessValue = clamp01(simulation.robustness);

    double fragilePocketPenalty = 0.0;
    if (simulation.targetPocketed) {
        fragilePocketPenalty =
            (1.0 - robustnessValue) * 0.65 +
            (1.0 - marginScore) * 0.35;
    }

    double robustnessSensitivity =
        skill.consistency * 0.28 +
        skill.physicsUnderstanding * 0.22;

    p.confidence = clamp01(
        executionConfidence * 0.34 +
        p.physicsConfidence * 0.22 +
        candidate.priorScore * 0.07 +
        firstHitTarget * 0.10 +
        robustnessValue * 0.17 +
        marginScore * 0.10
    );

    p.risk = clamp01(
        difficulty * 0.18 +
        world.uncertainty * 0.10 +
        travelPenalty * 0.08 +
        p.safetyPenalty * 0.30 +
        noHitPenalty * 0.35 +
        wrongFirstHitPenalty * 0.25 +
        fragilePocketPenalty * robustnessSensitivity
    );

    double positionValue =
        skill.positionPlanning * (1.0 - travelPenalty);

    double safetyValue =
        skill.safetyAwareness * (1.0 - p.safetyPenalty);

    p.reward = clamp01(
        p.pocketReward * 0.26 +
        robustnessValue * 0.30 +
        marginScore * 0.14 +
        firstHitTarget * 0.08 +
        proximityReward * 0.08 +
        candidate.priorScore * 0.06 +
        positionValue * 0.04 +
        safetyValue * 0.04
    );

    p.score = clamp01(
        p.reward * 0.60 +
        p.confidence * 0.32 -
        p.risk * (1.0 - skill.riskTolerance) * 0.58
    );

    return p;
}

}
