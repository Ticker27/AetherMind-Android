#pragma once

#include "aether/world/WorldState.h"

namespace aether {

class WorldBuilder {
public:
    WorldState build(const PerceptionFrame& frame) const;
};

}
