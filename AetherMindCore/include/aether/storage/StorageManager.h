#pragma once

#include <string>
#include <vector>

namespace aether {

class StorageManager {
public:
    explicit StorageManager(std::string rootPath = "data");

    bool initialize();
    bool shutdown();

    bool clearTemp();
    bool cleanLogs();

    bool saveBinaryAtomic(
        const std::string& relativePath,
        const std::vector<unsigned char>& data
    ) const;

    std::string root() const;

private:
    std::string rootPath;

    bool ensureDirectory(const std::string& path) const;
    bool removeFilesInDirectory(const std::string& path) const;
};

}
