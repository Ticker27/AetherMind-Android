#include "aether/memory/ExperienceMemory.h"

#include <fstream>
#include <sstream>
#include <utility>

namespace aether {

static const char* skillName(SkillLevel level) {
    switch (level) {
        case SkillLevel::Beginner:
            return "beginner";
        case SkillLevel::Intermediate:
            return "intermediate";
        case SkillLevel::Advanced:
            return "advanced";
    }

    return "unknown";
}

static const char* actionTypeName(ActionType type) {
    switch (type) {
        case ActionType::None:
            return "none";
        case ActionType::Direct:
            return "direct";
        case ActionType::Safety:
            return "safety";
        case ActionType::Position:
            return "position";
        case ActionType::Exploratory:
            return "exploratory";
    }

    return "unknown";
}

std::string ExperienceMemory::keyFor(
    const Action& action,
    const SkillProfile& skill
) const {
    std::ostringstream ss;

    ss << skillName(skill.level)
       << ":"
       << actionTypeName(action.type)
       << ":target="
       << action.targetId;

    return ss.str();
}

void ExperienceMemory::record(
    const MindOutput& output,
    const SkillProfile& skill
) {
    std::string key = keyFor(output.plannedAction, skill);
    ExperienceStats& s = stats[key];

    s.attempts++;

    if (output.plannedPocketed) {
        s.plannedPocketed++;
    }

    if (output.executedPocketed) {
        s.executedPocketed++;
    }

    bool cleanPocket =
        output.executedPocketed &&
        !output.executedCuePocketed &&
        output.executedFirstHitId == output.targetId;

    if (cleanPocket) {
        s.cleanPocketSuccess++;
    }

    bool validFirstHit =
        output.executedFirstHitId == output.targetId;

    bool wrongFirstHit =
        output.executedFirstHitId >= 0 &&
        output.executedFirstHitId != output.targetId;

    bool noFirstHit =
        output.executedFirstHitId < 0;

    if (output.executedCuePocketed) {
        s.cuePocketed++;
    }

    if (wrongFirstHit) {
        s.wrongFirstHit++;
    }

    if (noFirstHit) {
        s.noFirstHit++;
    }

    if (output.plannedAction.type == ActionType::Safety) {
        s.safetyChosen++;

        bool controlledSafety =
            !output.executedPocketed &&
            !output.executedCuePocketed &&
            validFirstHit;

        if (controlledSafety) {
            s.safetyControlSuccess++;
        }
    }

    double n = static_cast<double>(s.attempts);

    s.averageRisk += (output.risk - s.averageRisk) / n;
    s.averageReward += (output.expectedReward - s.averageReward) / n;
    s.averageExecutionConfidence +=
        (output.executedConfidence - s.averageExecutionConfidence) / n;
    s.averageExecutedMargin +=
        (output.executedPocketMargin - s.averageExecutedMargin) / n;
}

double ExperienceMemory::confidenceBias(
    const Action& action,
    const SkillProfile& skill
) const {
    std::string key = keyFor(action, skill);
    auto it = stats.find(key);

    if (it == stats.end()) {
        return 0.0;
    }

    const ExperienceStats& s = it->second;

    if (s.attempts < 3) {
        return 0.0;
    }

    double attempts = static_cast<double>(s.attempts);

    if (action.type == ActionType::Direct) {
        double successRate =
            static_cast<double>(s.cleanPocketSuccess) / attempts;

        return (successRate - 0.50) * 0.14;
    }

    if (action.type == ActionType::Safety) {
        double controlRate =
            static_cast<double>(s.safetyControlSuccess) / attempts;

        return (controlRate - 0.50) * 0.10;
    }

    return 0.0;
}

double ExperienceMemory::riskBias(
    const Action& action,
    const SkillProfile& skill
) const {
    std::string key = keyFor(action, skill);
    auto it = stats.find(key);

    if (it == stats.end()) {
        return 0.0;
    }

    const ExperienceStats& s = it->second;

    if (s.attempts < 3) {
        return 0.0;
    }

    double attempts = static_cast<double>(s.attempts);

    if (action.type == ActionType::Direct) {
        double successRate =
            static_cast<double>(s.cleanPocketSuccess) / attempts;

        double cueRisk =
            static_cast<double>(s.cuePocketed) / attempts;

        double wrongHitRisk =
            static_cast<double>(s.wrongFirstHit + s.noFirstHit) / attempts;

        return
            (0.55 - successRate) * 0.16 +
            cueRisk * 0.10 +
            wrongHitRisk * 0.08;
    }

    if (action.type == ActionType::Safety) {
        double controlRate =
            static_cast<double>(s.safetyControlSuccess) / attempts;

        double failureRate = 1.0 - controlRate;

        return
            (0.45 - controlRate) * 0.10 +
            failureRate * 0.04;
    }

    return 0.0;
}

std::string ExperienceMemory::toText() const {
    std::ostringstream out;

    for (const auto& pair : stats) {
        const ExperienceStats& s = pair.second;

        out << pair.first << " "
            << s.attempts << " "
            << s.plannedPocketed << " "
            << s.executedPocketed << " "
            << s.cleanPocketSuccess << " "
            << s.safetyChosen << " "
            << s.safetyControlSuccess << " "
            << s.cuePocketed << " "
            << s.wrongFirstHit << " "
            << s.noFirstHit << " "
            << s.averageRisk << " "
            << s.averageReward << " "
            << s.averageExecutionConfidence << " "
            << s.averageExecutedMargin << "\n";
    }

    return out.str();
}

bool ExperienceMemory::fromText(const std::string& text) {
    std::istringstream input(text);
    std::unordered_map<std::string, ExperienceStats> loaded;

    std::string line;

    while (std::getline(input, line)) {
        if (line.empty()) {
            continue;
        }

        std::istringstream row(line);

        std::string key;
        ExperienceStats s;

        if (
            !(row >> key
                  >> s.attempts
                  >> s.plannedPocketed
                  >> s.executedPocketed
                  >> s.cleanPocketSuccess
                  >> s.safetyChosen
                  >> s.safetyControlSuccess
                  >> s.cuePocketed
                  >> s.wrongFirstHit
                  >> s.noFirstHit
                  >> s.averageRisk
                  >> s.averageReward
                  >> s.averageExecutionConfidence
                  >> s.averageExecutedMargin)
        ) {
            return false;
        }

        loaded[key] = s;
    }

    stats = std::move(loaded);
    return true;
}

bool ExperienceMemory::saveText(const std::string& path) const {
    std::ofstream out(path);

    if (!out) {
        return false;
    }

    out << toText();
    return static_cast<bool>(out);
}

bool ExperienceMemory::loadText(const std::string& path) {
    std::ifstream in(path);

    if (!in) {
        return false;
    }

    std::ostringstream buffer;
    buffer << in.rdbuf();

    return fromText(buffer.str());
}

std::string ExperienceMemory::summary() const {
    std::ostringstream ss;

    ss << "memory entries=" << stats.size();

    for (const auto& pair : stats) {
        const ExperienceStats& s = pair.second;

        ss << " | "
           << pair.first
           << " attempts="
           << s.attempts
           << " plannedPocketed="
           << s.plannedPocketed
           << " executedPocketed="
           << s.executedPocketed
           << " cleanPocketSuccess="
           << s.cleanPocketSuccess
           << " safetyChosen="
           << s.safetyChosen
           << " safetyControlSuccess="
           << s.safetyControlSuccess
           << " cuePocketed="
           << s.cuePocketed
           << " wrongFirstHit="
           << s.wrongFirstHit
           << " noFirstHit="
           << s.noFirstHit
           << " avgRisk="
           << s.averageRisk
           << " avgReward="
           << s.averageReward
           << " avgExecConf="
           << s.averageExecutionConfidence
           << " avgExecMargin="
           << s.averageExecutedMargin;
    }

    return ss.str();
}

}
