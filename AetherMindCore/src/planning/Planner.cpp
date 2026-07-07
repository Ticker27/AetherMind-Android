#include "aether/planning/Planner.h"
#include "aether/foundation/Vec2.h"

#include <cmath>
#include <vector>

namespace aether {

static Vec2 rotateVecPlanner(const Vec2& v, double angle) {
    double c = std::cos(angle);
    double s = std::sin(angle);

    return Vec2(
        v.x * c - v.y * s,
        v.x * s + v.y * c
    ).normalized();
}

PhysicsConfig Planner::makePhysicsConfig(const SkillProfile& skill) const {
    PhysicsConfig config;

    config.timeStep = 0.008;
    config.maxTime = 4.0;

    config.friction = 0.35;
    config.railBounce = 0.82;

    if (skill.level == SkillLevel::Beginner) {
        config.timeStep = 0.012;
        config.maxTime = 2.5;
    } else if (skill.level == SkillLevel::Intermediate) {
        config.timeStep = 0.008;
        config.maxTime = 3.5;
    } else {
        config.timeStep = 0.005;
        config.maxTime = 5.0;
    }

    return config;
}

static double angleProbeSize(const SkillProfile& skill) {
    switch (skill.level) {
        case SkillLevel::Beginner:
            return 0.035;
        case SkillLevel::Intermediate:
            return 0.018;
        case SkillLevel::Advanced:
            return 0.008;
    }

    return 0.018;
}

static double powerProbeSize(const SkillProfile& skill) {
    switch (skill.level) {
        case SkillLevel::Beginner:
            return 0.10;
        case SkillLevel::Intermediate:
            return 0.055;
        case SkillLevel::Advanced:
            return 0.025;
    }

    return 0.055;
}

static void computeRobustness(
    SimulationResult& base,
    const WorldState& world,
    const ActionCandidate& candidate,
    const PhysicsKernel& physics,
    const PhysicsConfig& config,
    const SkillProfile& skill
) {
    std::vector<Action> probes;

    double da = angleProbeSize(skill);
    double dp = powerProbeSize(skill);

    Action left = candidate.action;
    left.direction = rotateVecPlanner(left.direction, -da);
    probes.push_back(left);

    Action right = candidate.action;
    right.direction = rotateVecPlanner(right.direction, da);
    probes.push_back(right);

    Action soft = candidate.action;
    soft.power = clamp(soft.power - dp, 0.02, 1.0);
    probes.push_back(soft);

    Action hard = candidate.action;
    hard.power = clamp(hard.power + dp, 0.02, 1.0);
    probes.push_back(hard);

    int pocketed = 0;

    for (const auto& action : probes) {
        SimulationResult r = physics.simulate(world, action, config);

        if (
            r.targetPocketed &&
            !r.cuePocketed &&
            r.firstHitId == r.targetId
        ) {
            pocketed++;
        }
    }

    base.robustnessSamples = static_cast<int>(probes.size());
    base.robustnessPocketed = pocketed;
    base.robustness = probes.empty()
        ? 0.0
        : static_cast<double>(pocketed) / static_cast<double>(probes.size());
}

static double marginScore(const SimulationResult& r) {
    return clamp01((r.targetPocketMargin + 0.015) / 0.070);
}

static double localObjective(const SimulationResult& r) {
    double pocket = r.targetPocketed ? 1.0 : 0.0;
    double firstHit = r.firstHitId == r.targetId ? 1.0 : 0.0;
    double cuePenalty = r.cuePocketed ? 1.0 : 0.0;

    double proximity = clamp01(
        1.0 - r.targetClosestPocketDistance / 0.14
    );

    double margin = marginScore(r);

    return
        pocket * 1.70 +
        r.robustness * 1.55 +
        margin * 1.20 +
        firstHit * 0.35 +
        proximity * 0.30 -
        cuePenalty * 2.00;
}

static void refineLocallyIfUseful(
    RankedPlan& plan,
    const WorldState& world,
    const PhysicsKernel& physics,
    const PhysicsConfig& config,
    const SkillProfile& skill
) {
    if (skill.level != SkillLevel::Advanced) {
        return;
    }

    bool worthRefining =
        plan.simulation.targetPocketed ||
        plan.simulation.targetClosestPocketDistance < 0.16;

    if (!worthRefining) {
        return;
    }

    ActionCandidate bestCandidate = plan.candidate;
    SimulationResult bestSimulation = plan.simulation;
    double bestScore = localObjective(bestSimulation);

    const std::vector<double> angleOffsets = {
        -0.050, -0.040, -0.032, -0.024, -0.018,
        -0.012, -0.008, -0.004,
         0.000,
         0.004,  0.008,  0.012,
         0.018,  0.024,  0.032,  0.040,  0.050
    };

    const std::vector<double> powerScales = {
        0.88, 0.92, 0.96, 1.00, 1.04, 1.08
    };

    for (double angleOffset : angleOffsets) {
        for (double powerScale : powerScales) {
            ActionCandidate probe = plan.candidate;

            probe.action.direction =
                rotateVecPlanner(plan.candidate.action.direction, angleOffset);

            probe.action.power =
                clamp(plan.candidate.action.power * powerScale, 0.02, 1.0);

            SimulationResult sim =
                physics.simulate(world, probe.action, config);

            computeRobustness(
                sim,
                world,
                probe,
                physics,
                config,
                skill
            );

            double score = localObjective(sim);

            if (score > bestScore + 1e-9) {
                bestScore = score;
                bestCandidate = probe;
                bestSimulation = sim;
            }
        }
    }

    double oldScore = localObjective(plan.simulation);

    if (bestScore > oldScore + 1e-9) {
        bestCandidate.reason += " center-refined";
        plan.candidate = bestCandidate;
        plan.simulation = bestSimulation;
    }
}

std::vector<RankedPlan> Planner::buildPlans(
    const WorldState& world,
    const std::vector<ActionCandidate>& candidates,
    const PhysicsKernel& physics,
    const Evaluator& evaluator,
    const SkillProfile& skill
) const {
    std::vector<RankedPlan> plans;
    plans.reserve(candidates.size());

    PhysicsConfig config = makePhysicsConfig(skill);

    for (const auto& candidate : candidates) {
        RankedPlan plan;
        plan.candidate = candidate;
        plan.simulation = physics.simulate(world, candidate.action, config);

        computeRobustness(
            plan.simulation,
            world,
            candidate,
            physics,
            config,
            skill
        );

        refineLocallyIfUseful(
            plan,
            world,
            physics,
            config,
            skill
        );

        plan.score = evaluator.evaluate(
            world,
            plan.candidate,
            plan.simulation,
            skill
        );

        plans.push_back(plan);
    }

    return plans;
}

}
