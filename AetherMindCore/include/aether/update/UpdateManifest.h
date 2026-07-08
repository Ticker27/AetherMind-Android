#pragma once

#include <string>

namespace aether {

inline constexpr const char* AETHER_UPDATE_MAGIC = "AETHER_UPDATE";
inline constexpr int AETHER_UPDATE_MANIFEST_VERSION = 1;

struct UpdateManifest {
    int manifestVersion = AETHER_UPDATE_MANIFEST_VERSION;

    std::string packageId;
    std::string moduleName;
    std::string packageVersion;
    std::string requiresCore;

    int memorySchemaFrom = -1;
    int memorySchemaTo = -1;

    bool rollbackSupported = false;

    std::string checksum;
    std::string description;
};

struct UpdateManifestCheckResult {
    bool ok = false;
    bool touchesMemory = false;
    bool schemaChange = false;
    bool rollbackRequired = false;

    std::string message;
};

class UpdateManifestCodec {
public:
    static std::string toText(
        const UpdateManifest& manifest
    );

    static bool fromText(
        const std::string& text,
        UpdateManifest& manifest,
        std::string& error
    );

    static UpdateManifestCheckResult validateForCore(
        const UpdateManifest& manifest,
        const std::string& coreVersion,
        int currentMemorySchema
    );
};

}
