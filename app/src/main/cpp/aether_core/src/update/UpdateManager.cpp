#include "aether/update/UpdateManager.h"

#include <filesystem>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <utility>

namespace fs = std::filesystem;

namespace aether {

static std::uint64_t fnv1a64Update(const std::string& text) {
    constexpr std::uint64_t offset = 14695981039346656037ull;
    constexpr std::uint64_t prime = 1099511628211ull;

    std::uint64_t hash = offset;

    for (unsigned char c : text) {
        hash ^= static_cast<std::uint64_t>(c);
        hash *= prime;
    }

    return hash;
}

static std::string hex64Update(std::uint64_t value) {
    std::ostringstream out;

    out << std::hex
        << std::setw(16)
        << std::setfill('0')
        << value;

    return out.str();
}

UpdateManager::UpdateManager(
    MemoryStore memoryStore_
)
    : memoryStore(std::move(memoryStore_)) {}

std::string UpdateManager::checksumForPayload(
    const std::string& payloadText
) {
    return hex64Update(fnv1a64Update(payloadText));
}

bool UpdateManager::stageMemoryOffLock(
    const UpdatePackage& package,
    const std::string& coreVersion,
    int currentMemorySchema,
    ExperienceMemory& stagedMemory,
    std::string& error,
    std::ostream& log
) const {
    log << "[UPDATE] Parsing package="
        << package.manifest.packageId
        << "\n";

    UpdateManifestCheckResult manifestResult =
        UpdateManifestCodec::validateForCore(
            package.manifest,
            coreVersion,
            currentMemorySchema
        );

    if (!manifestResult.ok) {
        error = manifestResult.message;
        return false;
    }

    if (package.manifest.moduleName != "memory") {
        error = "reject: UpdateManager only accepts memory package";
        return false;
    }

    if (package.memoryName.empty()) {
        error = "reject: missing memory name";
        return false;
    }

    log << "[UPDATE] Calculating checksum\n";

    std::string actualChecksum =
        checksumForPayload(package.payloadText);

    if (actualChecksum != package.manifest.checksum) {
        error = "reject: payload checksum mismatch";
        return false;
    }

    log << "[UPDATE] Staging memory\n";

    ExperienceMemory staged;

    if (!staged.fromText(package.payloadText)) {
        error = "reject: staged memory parse failed";
        return false;
    }

    stagedMemory = staged;
    return true;
}

void UpdateManager::corruptPrimaryForTestLocked(
    const std::string& memoryName
) const {
    std::string path = memoryStore.resolvePath(memoryName);

    std::ofstream out(
        path,
        std::ios::binary | std::ios::trunc
    );

    out << "CORRUPTED_AFTER_COMMIT\n";
}

UpdateApplyResult UpdateManager::applyMemoryPackage(
    const UpdatePackage& package,
    const std::string& coreVersion,
    int currentMemorySchema,
    std::ostream& log
) const {
    UpdateApplyResult result;

    ExperienceMemory stagedMemory;
    std::string error;

    if (
        !stageMemoryOffLock(
            package,
            coreVersion,
            currentMemorySchema,
            stagedMemory,
            error,
            log
        )
    ) {
        result.ok = false;
        result.message = error;
        return result;
    }

    result.staged = true;

    auto lock = memoryStore.acquireWriteLock();
    log << "[LOCK ACQUIRED] Committing memory="
        << package.memoryName
        << "\n";

    MemoryStoreResult saveResult =
        memoryStore.saveExperienceLocked(
            package.memoryName,
            stagedMemory
        );

    if (!saveResult.ok) {
        lock.unlock();
        log << "[LOCK RELEASED] commit_failed\n";

        result.ok = false;
        result.message = saveResult.message;
        return result;
    }

    result.committed = true;

    if (package.forceVerifyFailure) {
        corruptPrimaryForTestLocked(package.memoryName);
    }

    log << "[UPDATE] Verifying commit\n";

    bool verified =
        memoryStore.verifyPrimaryExperienceLocked(
            package.memoryName
        );

    if (!verified) {
        log << "[ROLLBACK] Restoring backup\n";

        bool restored =
            memoryStore.restoreBackupLocked(
                package.memoryName
            );

        lock.unlock();
        log << "[LOCK RELEASED] rollback_complete\n";

        result.ok = false;
        result.rolledBack = restored;
        result.message = restored
            ? "verify failed; rollback ok"
            : "verify failed; rollback failed";

        return result;
    }

    lock.unlock();
    log << "[LOCK RELEASED] commit_ok\n";

    result.ok = true;
    result.message = "commit ok";
    return result;
}

}
