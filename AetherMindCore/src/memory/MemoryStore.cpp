#include "aether/memory/MemoryStore.h"
#include "aether/memory/MemorySchema.h"

#include <filesystem>
#include <fstream>
#include <iomanip>
#include <mutex>
#include <sstream>
#include <utility>

namespace fs = std::filesystem;

namespace aether {

static std::shared_mutex& globalMemoryMutex() {
    static std::shared_mutex mutex;
    return mutex;
}

static std::uint64_t fnv1a64(const std::string& text) {
    constexpr std::uint64_t offset = 14695981039346656037ull;
    constexpr std::uint64_t prime = 1099511628211ull;

    std::uint64_t hash = offset;

    for (unsigned char c : text) {
        hash ^= static_cast<std::uint64_t>(c);
        hash *= prime;
    }

    return hash;
}

static std::string hex64(std::uint64_t value) {
    std::ostringstream out;

    out << std::hex
        << std::setw(16)
        << std::setfill('0')
        << value;

    return out.str();
}

static bool startsWith(
    const std::string& value,
    const std::string& prefix
) {
    return value.rfind(prefix, 0) == 0;
}

MemoryStore::MemoryStore(std::string rootPath_)
    : rootPath(std::move(rootPath_)) {}

MemoryStore::ReadLock MemoryStore::acquireReadLock() const {
    return ReadLock(globalMemoryMutex());
}

MemoryStore::WriteLock MemoryStore::acquireWriteLock() const {
    return WriteLock(globalMemoryMutex());
}

std::string MemoryStore::resolvePath(
    const std::string& memoryName
) const {
    fs::path p(memoryName);

    if (p.is_absolute()) {
        return memoryName;
    }

    if (memoryName.find('/') != std::string::npos) {
        return memoryName;
    }

    return (fs::path(rootPath) / memoryName).string();
}

std::string MemoryStore::backupPathFor(
    const std::string& path
) const {
    return path + ".bak";
}

bool MemoryStore::ensureRootUnlocked() const {
    std::error_code ec;
    fs::create_directories(rootPath, ec);
    return !ec;
}

bool MemoryStore::readWholeFileUnlocked(
    const std::string& path,
    std::string& text
) const {
    std::ifstream in(path, std::ios::binary);

    if (!in) {
        return false;
    }

    std::ostringstream buffer;
    buffer << in.rdbuf();

    if (!in.good() && !in.eof()) {
        return false;
    }

    text = buffer.str();
    return true;
}

std::string MemoryStore::makeExperienceDocumentUnlocked(
    const ExperienceMemory& memory
) const {
    std::string body = memory.toText();
    std::string checksum = hex64(fnv1a64(body));

    std::ostringstream out;

    out << AETHER_MEMORY_MAGIC << " "
        << AETHER_EXPERIENCE_KIND << " "
        << AETHER_EXPERIENCE_SCHEMA_VERSION << "\n";

    out << "checksum=" << checksum << "\n";
    out << "BEGIN_BODY\n";
    out << body;
    out << "END_BODY\n";

    return out.str();
}

bool MemoryStore::parseExperienceDocumentUnlocked(
    const std::string& document,
    ExperienceMemory& memory
) const {
    std::istringstream input(document);

    std::string magic;
    std::string kind;
    int version = 0;

    if (!(input >> magic >> kind >> version)) {
        return false;
    }

    if (magic != AETHER_MEMORY_MAGIC) {
        return false;
    }

    if (kind != AETHER_EXPERIENCE_KIND) {
        return false;
    }

    if (version != AETHER_EXPERIENCE_SCHEMA_VERSION) {
        return false;
    }

    std::string line;
    std::getline(input, line);

    if (!std::getline(input, line)) {
        ExperienceMemory loaded;

        if (!loaded.fromText("")) {
            return false;
        }

        memory = loaded;
        return true;
    }

    if (!startsWith(line, "checksum=")) {
        std::ostringstream legacyBody;
        legacyBody << line << "\n";
        legacyBody << input.rdbuf();

        ExperienceMemory loaded;

        if (!loaded.fromText(legacyBody.str())) {
            return false;
        }

        memory = loaded;
        return true;
    }

    std::string expectedChecksum =
        line.substr(std::string("checksum=").size());

    if (!std::getline(input, line)) {
        return false;
    }

    if (line != "BEGIN_BODY") {
        return false;
    }

    std::ostringstream body;

    while (std::getline(input, line)) {
        if (line == "END_BODY") {
            std::string bodyText = body.str();
            std::string actualChecksum = hex64(fnv1a64(bodyText));

            if (actualChecksum != expectedChecksum) {
                return false;
            }

            ExperienceMemory loaded;

            if (!loaded.fromText(bodyText)) {
                return false;
            }

            memory = loaded;
            return true;
        }

        body << line << "\n";
    }

    return false;
}

bool MemoryStore::writeAtomicUnlocked(
    const std::string& path,
    const std::string& text
) const {
    if (!ensureRootUnlocked()) {
        return false;
    }

    fs::path finalPath(path);
    fs::path parent = finalPath.parent_path();

    std::error_code ec;

    if (!parent.empty()) {
        fs::create_directories(parent, ec);

        if (ec) {
            return false;
        }
    }

    std::string tmpPath = path + ".tmp";
    std::string bakPath = backupPathFor(path);

    {
        std::ofstream out(
            tmpPath,
            std::ios::binary | std::ios::trunc
        );

        if (!out) {
            return false;
        }

        out.write(
            text.data(),
            static_cast<std::streamsize>(text.size())
        );

        out.flush();

        if (!out) {
            return false;
        }
    }

    std::string tmpText;

    if (!readWholeFileUnlocked(tmpPath, tmpText)) {
        fs::remove(tmpPath, ec);
        return false;
    }

    if (fnv1a64(tmpText) != fnv1a64(text)) {
        fs::remove(tmpPath, ec);
        return false;
    }

    ExperienceMemory probe;

    if (!parseExperienceDocumentUnlocked(tmpText, probe)) {
        fs::remove(tmpPath, ec);
        return false;
    }

    if (fs::exists(finalPath)) {
        fs::copy_file(
            finalPath,
            bakPath,
            fs::copy_options::overwrite_existing,
            ec
        );

        if (ec) {
            fs::remove(tmpPath);
            return false;
        }
    }

    ec.clear();

    fs::rename(tmpPath, finalPath, ec);

    if (ec) {
        fs::remove(tmpPath);
        return false;
    }

    return true;
}

MemoryStoreResult MemoryStore::saveExperienceLocked(
    const std::string& memoryName,
    const ExperienceMemory& memory
) const {
    MemoryStoreResult result;
    result.path = resolvePath(memoryName);

    std::string document =
        makeExperienceDocumentUnlocked(memory);

    if (!writeAtomicUnlocked(result.path, document)) {
        result.ok = false;
        result.message = "save failed";
        return result;
    }

    result.ok = true;
    result.message = "save ok";
    return result;
}

MemoryStoreResult MemoryStore::loadExperienceLocked(
    const std::string& memoryName,
    ExperienceMemory& memory
) const {
    MemoryStoreResult result;
    result.path = resolvePath(memoryName);

    std::string document;

    if (readWholeFileUnlocked(result.path, document)) {
        ExperienceMemory loaded;

        if (parseExperienceDocumentUnlocked(document, loaded)) {
            memory = loaded;

            result.ok = true;
            result.usedBackup = false;
            result.message = "load ok";
            return result;
        }
    }

    std::string bakPath = backupPathFor(result.path);

    if (readWholeFileUnlocked(bakPath, document)) {
        ExperienceMemory loaded;

        if (parseExperienceDocumentUnlocked(document, loaded)) {
            memory = loaded;

            result.ok = true;
            result.usedBackup = true;
            result.message = "load ok from backup";
            return result;
        }
    }

    result.ok = false;
    result.message = "no valid memory found";
    return result;
}

MemoryStoreResult MemoryStore::saveExperience(
    const std::string& memoryName,
    const ExperienceMemory& memory
) const {
    WriteLock lock = acquireWriteLock();
    return saveExperienceLocked(memoryName, memory);
}

MemoryStoreResult MemoryStore::loadExperience(
    const std::string& memoryName,
    ExperienceMemory& memory
) const {
    ReadLock lock = acquireReadLock();
    return loadExperienceLocked(memoryName, memory);
}

bool MemoryStore::verifyPrimaryExperienceLocked(
    const std::string& memoryName
) const {
    std::string path = resolvePath(memoryName);
    std::string document;

    if (!readWholeFileUnlocked(path, document)) {
        return false;
    }

    ExperienceMemory loaded;
    return parseExperienceDocumentUnlocked(document, loaded);
}

bool MemoryStore::restoreBackupLocked(
    const std::string& memoryName
) const {
    std::string path = resolvePath(memoryName);
    std::string bakPath = backupPathFor(path);

    std::error_code ec;

    if (!fs::exists(bakPath)) {
        return false;
    }

    fs::copy_file(
        bakPath,
        path,
        fs::copy_options::overwrite_existing,
        ec
    );

    return !ec;
}

}
