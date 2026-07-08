#pragma once

#include <iosfwd>
#include <string>

#include "aether/memory/ExperienceMemory.h"
#include "aether/memory/MemoryStore.h"
#include "aether/update/UpdateManifest.h"

namespace aether {

struct UpdatePackage {
    UpdateManifest manifest;
    std::string memoryName;
    std::string payloadText;

    bool forceVerifyFailure = false;
};

struct UpdateApplyResult {
    bool ok = false;
    bool staged = false;
    bool committed = false;
    bool rolledBack = false;

    std::string message;
};

class UpdateManager {
public:
    explicit UpdateManager(
        MemoryStore memoryStore = MemoryStore("data/state")
    );

    UpdateApplyResult applyMemoryPackage(
        const UpdatePackage& package,
        const std::string& coreVersion,
        int currentMemorySchema,
        std::ostream& log
    ) const;

    static std::string checksumForPayload(
        const std::string& payloadText
    );

private:
    MemoryStore memoryStore;

    bool stageMemoryOffLock(
        const UpdatePackage& package,
        const std::string& coreVersion,
        int currentMemorySchema,
        ExperienceMemory& stagedMemory,
        std::string& error,
        std::ostream& log
    ) const;

    void corruptPrimaryForTestLocked(
        const std::string& memoryName
    ) const;
};

}
