#include "aether/storage/StorageManager.h"

#include <filesystem>
#include <fstream>

namespace fs = std::filesystem;

namespace aether {

StorageManager::StorageManager(std::string rootPath_)
    : rootPath(std::move(rootPath_)) {}

bool StorageManager::initialize() {
    return
        ensureDirectory(rootPath) &&
        ensureDirectory(rootPath + "/skill") &&
        ensureDirectory(rootPath + "/config") &&
        ensureDirectory(rootPath + "/state") &&
        ensureDirectory(rootPath + "/cache") &&
        ensureDirectory(rootPath + "/logs") &&
        ensureDirectory(rootPath + "/tmp") &&
        clearTemp() &&
        cleanLogs();
}

bool StorageManager::shutdown() {
    return clearTemp();
}

bool StorageManager::clearTemp() {
    return removeFilesInDirectory(rootPath + "/tmp");
}

bool StorageManager::cleanLogs() {
    return removeFilesInDirectory(rootPath + "/logs");
}

bool StorageManager::saveBinaryAtomic(
    const std::string& relativePath,
    const std::vector<unsigned char>& data
) const {
    fs::path finalPath = fs::path(rootPath) / relativePath;
    fs::path parent = finalPath.parent_path();

    if (!parent.empty()) {
        fs::create_directories(parent);
    }

    fs::path tmpPath = finalPath;
    tmpPath += ".tmp";

    fs::path bakPath = finalPath;
    bakPath += ".bak";

    {
        std::ofstream out(tmpPath, std::ios::binary);
        if (!out) {
            return false;
        }

        out.write(
            reinterpret_cast<const char*>(data.data()),
            static_cast<std::streamsize>(data.size())
        );

        if (!out) {
            return false;
        }
    }

    std::error_code ec;

    if (fs::exists(finalPath)) {
        fs::rename(finalPath, bakPath, ec);
        if (ec) {
            return false;
        }
    }

    fs::rename(tmpPath, finalPath, ec);
    if (ec) {
        if (fs::exists(bakPath)) {
            fs::rename(bakPath, finalPath);
        }
        return false;
    }

    fs::remove(bakPath, ec);
    return true;
}

std::string StorageManager::root() const {
    return rootPath;
}

bool StorageManager::ensureDirectory(const std::string& path) const {
    std::error_code ec;
    fs::create_directories(path, ec);
    return !ec;
}

bool StorageManager::removeFilesInDirectory(const std::string& path) const {
    std::error_code ec;

    if (!fs::exists(path)) {
        fs::create_directories(path, ec);
        return !ec;
    }

    for (const auto& entry : fs::directory_iterator(path, ec)) {
        if (ec) {
            return false;
        }

        fs::remove_all(entry.path(), ec);
        if (ec) {
            return false;
        }
    }

    return true;
}

}
