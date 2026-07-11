#pragma once

#include <string>
#include <vector>

namespace aether {

enum class ValidationIssueCode {
    None,
    MockSource,
    UnknownSource,
    TableNotVerified,
    CueBallMissing,
    NoObjectBalls,
    BallOutOfBounds,
    LowConfidenceWorld,
    ExecutionLocked
};

struct ValidationIssue {
    ValidationIssueCode code = ValidationIssueCode::None;
    std::string message;
    double severity = 0.0;
};

struct ValidationReport {
    bool usable = false;
    bool diagnosticOnly = true;
    bool executionAllowed = false;
    double confidence = 0.0;
    std::vector<ValidationIssue> issues;

    void add(ValidationIssueCode code, const std::string& message, double severity) {
        issues.push_back(ValidationIssue{code, message, severity});
    }
};

}
