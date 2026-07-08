#pragma once

#include <string>
#include <unordered_map>

#include "aether/core/MindTypes.h"
#include "aether/core/SkillProfile.h"

namespace aether {

struct ExperienceStats {
    int attempts = 0;

    int plannedPocketed = 0;
    int executedPocketed = 0;
    int cleanPocketSuccess = 0;

    int safetyChosen = 0;
    int safetyControlSuccess = 0;

    int cuePocketed = 0;
    int wrongFirstHit = 0;
    int noFirstHit = 0;

    double averageRisk = 0.0;
    double averageReward = 0.0;
    double averageExecutionConfidence = 0.0;
    double averageExecutedMargin = 0.0;
};

class ExperienceMemory {
public:
    void record(
        const MindOutput& output,
        const SkillProfile& skill
    );

    double confidenceBias(
        const Action& action,
        const SkillProfile& skill
    ) const;

    double riskBias(
        const Action& action,
        const SkillProfile& skill
    ) const;

    std::string toText() const;
    bool fromText(const std::string& text);

    bool saveText(const std::string& path) const;
    bool loadText(const std::string& path);

    std::string summary() const;

private:
    std::unordered_map<std::string, ExperienceStats> stats;

    std::string keyFor(
        const Action& action,
        const SkillProfile& skill
    ) const;
};

}
