#include "aether/planning/DecisionPolicy.h"
#include "aether/foundation/Vec2.h"

#include <algorithm>
#include <cmath>
#include <sstream>

namespace aether {

DecisionPolicy::DecisionPolicy()
    : rng(1337) {}

MindOutput DecisionPolicy::choose(
    const std::vector<RankedPlan>& plans,
    const SkillProfile& skill
) {
    MindOutput out;

    if (plans.empty()) {
        out.explanation = "no plan available";
        return out;
    }

    std::vector<size_t> order(plans.size());
    for (size_t i = 0; i < plans.size(); ++i) {
        order[i] = i;
    }

    std::sort(order.begin(), order.end(), [&](size_t a, size_t b) {
        return plans[a].score.score > plans[b].score.score;
    });

    size_t chosenIndex = order.front();

    if (skill.level == SkillLevel::Beginner) {
        int topK = static_cast<int>(std::min<size_t>(3, order.size()));
        std::vector<double> weights;
        weights.reserve(static_cast<size_t>(topK));

        for (int i = 0; i < topK; ++i) {
            double w = std::max(0.001, plans[order[static_cast<size_t>(i)]].score.score);
            weights.push_back(w);
        }

        std::discrete_distribution<int> dist(weights.begin(), weights.end());
        chosenIndex = order[static_cast<size_t>(dist(rng))];
    } else {
        for (size_t idx : order) {
            const RankedPlan& p = plans[idx];

            double requiredRobustness =
                skill.level == SkillLevel::Advanced ? 0.50 : 0.25;

            bool cleanPocket =
                p.simulation.targetPocketed &&
                !p.simulation.cuePocketed &&
                p.simulation.firstHitId == p.simulation.targetId &&
                p.simulation.robustness >= requiredRobustness &&
                p.score.risk < 0.45;

            if (cleanPocket) {
                chosenIndex = idx;
                break;
            }
        }
    }

    const RankedPlan& chosen = plans[chosenIndex];

    out.plannedAction = chosen.candidate.action;
    out.executedAction = out.plannedAction;

    out.confidence = chosen.score.confidence;
    out.risk = chosen.score.risk;
    out.expectedReward = chosen.score.reward;
    out.targetId = chosen.candidate.action.targetId;

    out.plannedPocketed = chosen.simulation.targetPocketed;
    out.plannedCuePocketed = chosen.simulation.cuePocketed;
    out.plannedFirstHitId = chosen.simulation.firstHitId;
    out.plannedTargetPocketDistance = chosen.simulation.targetClosestPocketDistance;
    out.plannedPocketMargin = chosen.simulation.targetPocketMargin;

    std::ostringstream ss;

    ss << "selected "
       << chosen.candidate.reason
       << " target="
       << out.targetId
       << " confidence="
       << out.confidence
       << " risk="
       << out.risk
       << " score="
       << chosen.score.score
       << " pocketed="
       << (chosen.simulation.targetPocketed ? "yes" : "no")
       << " cuePocketed="
       << (chosen.simulation.cuePocketed ? "yes" : "no")
       << " firstHit="
       << chosen.simulation.firstHitId
       << " collisions="
       << chosen.simulation.collisionCount
       << " targetPocketDist="
       << chosen.simulation.targetClosestPocketDistance
       << " margin="
       << chosen.simulation.targetPocketMargin
       << " effPocketR="
       << chosen.simulation.targetEffectivePocketRadius
       << " robustness="
       << chosen.simulation.robustness
       << " robustHits="
       << chosen.simulation.robustnessPocketed
       << "/"
       << chosen.simulation.robustnessSamples
       << " travel="
       << chosen.simulation.travelDistance;

    out.explanation = ss.str();

    return out;
}

}
