#pragma once

#include "aether/world/ValidationReport.h"
#include "aether/world/WorldState.h"

namespace aether {

class WorldStateValidator {
public:
    ValidationReport validate(const WorldState& world) const;
};

}
