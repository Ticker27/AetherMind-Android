#pragma once

#include <shared_mutex>
#include <mutex>
#include <string>

#include "aether/memory/ExperienceMemory.h"

namespace aether {

struct MemoryStoreResult {
    bool ok = false;
    bool usedBackup = false;
    std::string path;
    std::string message;
};

class MemoryStore {
public:
    using ReadLock = std::shared_lock<std::shared_mutex>;
    using WriteLock = std::unique_lock<std::shared_mutex>;

    explicit MemoryStore(std::string rootPath = "data/state");

    ReadLock acquireReadLock() const;
    WriteLock acquireWriteLock() const;

    MemoryStoreResult saveExperience(
        const std::string& memoryName,
        const ExperienceMemory& memory
    ) const;

    MemoryStoreResult loadExperience(
        const std::string& memoryName,
        ExperienceMemory& memory
    ) const;

    MemoryStoreResult saveExperienceLocked(
        const std::string& memoryName,
        const ExperienceMemory& memory
    ) const;

    MemoryStoreResult loadExperienceLocked(
        const std::string& memoryName,
        ExperienceMemory& memory
    ) const;

    bool verifyPrimaryExperienceLocked(
        const std::string& memoryName
    ) const;

    bool restoreBackupLocked(
        const std::string& memoryName
    ) const;

    std::string resolvePath(
        const std::string& memoryName
    ) const;

private:
    std::string rootPath;

    bool ensureRootUnlocked() const;

    bool writeAtomicUnlocked(
        const std::string& path,
        const std::string& text
    ) const;

    bool readWholeFileUnlocked(
        const std::string& path,
        std::string& text
    ) const;

    bool parseExperienceDocumentUnlocked(
        const std::string& document,
        ExperienceMemory& memory
    ) const;

    std::string makeExperienceDocumentUnlocked(
        const ExperienceMemory& memory
    ) const;

    std::string backupPathFor(
        const std::string& path
    ) const;
};

}
