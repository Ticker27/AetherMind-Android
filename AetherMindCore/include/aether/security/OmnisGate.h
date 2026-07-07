#pragma once

#include <cstddef>

namespace aether {

class OmnisGate {
public:
    static constexpr const char* RequiredKey = "AETHER_ARCHITECT_2026";

    static bool autoPlayTrigger(const char* authorizationKey) noexcept;

private:
    static bool constantTimeEquals(
        const char* lhs,
        const char* rhs
    ) noexcept;
};

}
