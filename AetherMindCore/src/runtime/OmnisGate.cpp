#include "aether/security/OmnisGate.h"

#include <cstring>

namespace aether {

bool OmnisGate::constantTimeEquals(
    const char* lhs,
    const char* rhs
) noexcept {
    if (lhs == nullptr || rhs == nullptr) {
        return false;
    }

    const std::size_t lhsLen = std::strlen(lhs);
    const std::size_t rhsLen = std::strlen(rhs);

    unsigned char diff = static_cast<unsigned char>(lhsLen ^ rhsLen);
    const std::size_t count = lhsLen < rhsLen ? lhsLen : rhsLen;

    for (std::size_t i = 0U; i < count; ++i) {
        diff |= static_cast<unsigned char>(lhs[i] ^ rhs[i]);
    }

    return diff == 0U;
}

bool OmnisGate::autoPlayTrigger(
    const char* authorizationKey
) noexcept {
    if (!constantTimeEquals(authorizationKey, RequiredKey)) {
        return false;
    }

    return true;
}

}
