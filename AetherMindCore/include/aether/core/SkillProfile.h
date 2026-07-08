#pragma once

namespace aether {

enum class SkillLevel {
    Beginner,
    Intermediate,
    Advanced
};

struct SkillProfile {
    SkillLevel level = SkillLevel::Beginner;

    // Normalized humanizer calibration scalar.
    // 0.0 = novice, 1.0 = pro.
    double skillLevel = 0.0;

    double aimAccuracy = 0.45;
    double powerControl = 0.40;
    double physicsUnderstanding = 0.35;

    int searchDepth = 1;
    int candidateLimit = 8;
    int simulationBudget = 120;

    double riskTolerance = 0.30;
    double positionPlanning = 0.20;
    double safetyAwareness = 0.25;

    double learningRate = 0.03;
    double consistency = 0.45;
    double blunderChance = 0.12;
};

inline SkillProfile makeSkillProfile(SkillLevel level) {
    SkillProfile s;
    s.level = level;

    if (level == SkillLevel::Beginner) {
        s.skillLevel = 0.0;
        s.aimAccuracy = 0.45;
        s.powerControl = 0.40;
        s.physicsUnderstanding = 0.35;
        s.searchDepth = 1;
        s.candidateLimit = 8;
        s.simulationBudget = 120;
        s.riskTolerance = 0.30;
        s.positionPlanning = 0.20;
        s.safetyAwareness = 0.25;
        s.learningRate = 0.03;
        s.consistency = 0.45;
        s.blunderChance = 0.12;
    } else if (level == SkillLevel::Intermediate) {
        s.skillLevel = 0.55;
        s.aimAccuracy = 0.68;
        s.powerControl = 0.64;
        s.physicsUnderstanding = 0.62;
        s.searchDepth = 2;
        s.candidateLimit = 24;
        s.simulationBudget = 960;
        s.riskTolerance = 0.50;
        s.positionPlanning = 0.55;
        s.safetyAwareness = 0.55;
        s.learningRate = 0.08;
        s.consistency = 0.68;
        s.blunderChance = 0.045;
    } else {
        s.skillLevel = 1.0;
        s.aimAccuracy = 0.88;
        s.powerControl = 0.84;
        s.physicsUnderstanding = 0.86;
        s.searchDepth = 3;
        s.candidateLimit = 160;
        s.simulationBudget = 12000;
        s.riskTolerance = 0.68;
        s.positionPlanning = 0.82;
        s.safetyAwareness = 0.78;
        s.learningRate = 0.14;
        s.consistency = 0.88;
        s.blunderChance = 0.012;
    }

    return s;
}

}
