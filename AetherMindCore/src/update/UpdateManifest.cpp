#include "aether/update/UpdateManifest.h"

#include <cctype>
#include <sstream>
#include <string>

namespace aether {

static std::string trim(const std::string& input) {
    size_t first = 0;

    while (
        first < input.size() &&
        std::isspace(static_cast<unsigned char>(input[first]))
    ) {
        first++;
    }

    size_t last = input.size();

    while (
        last > first &&
        std::isspace(static_cast<unsigned char>(input[last - 1]))
    ) {
        last--;
    }

    return input.substr(first, last - first);
}

static bool parseBool(
    const std::string& value,
    bool& out
) {
    if (value == "yes" || value == "true" || value == "1") {
        out = true;
        return true;
    }

    if (value == "no" || value == "false" || value == "0") {
        out = false;
        return true;
    }

    return false;
}

static const char* boolText(bool value) {
    return value ? "yes" : "no";
}

static bool isKnownModule(const std::string& moduleName) {
    return
        moduleName == "memory" ||
        moduleName == "brain" ||
        moduleName == "planner" ||
        moduleName == "physics" ||
        moduleName == "config" ||
        moduleName == "core" ||
        moduleName == "test";
}

std::string UpdateManifestCodec::toText(
    const UpdateManifest& manifest
) {
    std::ostringstream out;

    out << AETHER_UPDATE_MAGIC << " "
        << manifest.manifestVersion << "\n";

    out << "package_id=" << manifest.packageId << "\n";
    out << "module=" << manifest.moduleName << "\n";
    out << "package_version=" << manifest.packageVersion << "\n";
    out << "requires_core=" << manifest.requiresCore << "\n";
    out << "memory_schema_from=" << manifest.memorySchemaFrom << "\n";
    out << "memory_schema_to=" << manifest.memorySchemaTo << "\n";
    out << "rollback=" << boolText(manifest.rollbackSupported) << "\n";
    out << "checksum=" << manifest.checksum << "\n";
    out << "description=" << manifest.description << "\n";
    out << "END_AETHER_UPDATE\n";

    return out.str();
}

bool UpdateManifestCodec::fromText(
    const std::string& text,
    UpdateManifest& manifest,
    std::string& error
) {
    std::istringstream input(text);

    std::string magic;
    int version = 0;

    if (!(input >> magic >> version)) {
        error = "missing manifest header";
        return false;
    }

    if (magic != AETHER_UPDATE_MAGIC) {
        error = "bad manifest magic";
        return false;
    }

    if (version != AETHER_UPDATE_MANIFEST_VERSION) {
        error = "unsupported manifest version";
        return false;
    }

    manifest = UpdateManifest{};
    manifest.manifestVersion = version;

    std::string line;
    std::getline(input, line);

    while (std::getline(input, line)) {
        line = trim(line);

        if (line.empty()) {
            continue;
        }

        if (line == "END_AETHER_UPDATE") {
            return true;
        }

        size_t eq = line.find('=');

        if (eq == std::string::npos) {
            error = "bad manifest line";
            return false;
        }

        std::string key = trim(line.substr(0, eq));
        std::string value = trim(line.substr(eq + 1));

        // Parse with manual error checking (exceptions disabled)
        bool parsed_ok = true;

        if (key == "package_id") {
            manifest.packageId = value;
        } else if (key == "module") {
            manifest.moduleName = value;
        } else if (key == "package_version") {
            manifest.packageVersion = value;
        } else if (key == "requires_core") {
            manifest.requiresCore = value;
        } else if (key == "memory_schema_from") {
            if (!value.empty() &&
                ((value[0] >= '0' && value[0] <= '9') || value[0] == '-')) {
                // manual parse to avoid exceptions
                int val = 0;
                bool neg = false;
                size_t i = 0;
                if (value[0] == '-') { neg = true; i = 1; }
                for (; i < value.size(); i++) {
                    if (value[i] >= '0' && value[i] <= '9') {
                        val = val * 10 + (value[i] - '0');
                    } else { parsed_ok = false; break; }
                }
                if (parsed_ok) manifest.memorySchemaFrom = neg ? -val : val;
            } else { parsed_ok = false; }
        } else if (key == "memory_schema_to") {
            if (!value.empty() &&
                ((value[0] >= '0' && value[0] <= '9') || value[0] == '-')) {
                int val = 0;
                bool neg = false;
                size_t i = 0;
                if (value[0] == '-') { neg = true; i = 1; }
                for (; i < value.size(); i++) {
                    if (value[i] >= '0' && value[i] <= '9') {
                        val = val * 10 + (value[i] - '0');
                    } else { parsed_ok = false; break; }
                }
                if (parsed_ok) manifest.memorySchemaTo = neg ? -val : val;
            } else { parsed_ok = false; }
        } else if (key == "rollback") {
            bool rollback = false;
            if (!parseBool(value, rollback)) {
                error = "bad rollback value";
                return false;
            }
            manifest.rollbackSupported = rollback;
        } else if (key == "checksum") {
            manifest.checksum = value;
        } else if (key == "description") {
            manifest.description = value;
        }

        if (!parsed_ok) {
            error = "bad manifest numeric value";
            return false;
        }
    }

    error = "missing END_AETHER_UPDATE";
    return false;
}

UpdateManifestCheckResult UpdateManifestCodec::validateForCore(
    const UpdateManifest& manifest,
    const std::string& coreVersion,
    int currentMemorySchema
) {
    UpdateManifestCheckResult result;

    result.touchesMemory = manifest.moduleName == "memory";
    result.schemaChange =
        manifest.memorySchemaFrom >= 0 &&
        manifest.memorySchemaTo >= 0 &&
        manifest.memorySchemaFrom != manifest.memorySchemaTo;

    result.rollbackRequired =
        result.touchesMemory || result.schemaChange;

    if (manifest.packageId.empty()) {
        result.message = "reject: missing package id";
        return result;
    }

    if (manifest.moduleName.empty()) {
        result.message = "reject: missing module";
        return result;
    }

    if (!isKnownModule(manifest.moduleName)) {
        result.message = "reject: unknown module";
        return result;
    }

    if (manifest.packageVersion.empty()) {
        result.message = "reject: missing package version";
        return result;
    }

    if (manifest.requiresCore != coreVersion) {
        result.message = "reject: core version mismatch";
        return result;
    }

    if (manifest.checksum.empty()) {
        result.message = "reject: missing checksum";
        return result;
    }

    if (
        manifest.memorySchemaFrom >= 0 &&
        manifest.memorySchemaFrom != currentMemorySchema
    ) {
        result.message = "reject: memory schema source mismatch";
        return result;
    }

    if (
        manifest.memorySchemaFrom >= 0 &&
        manifest.memorySchemaTo >= 0 &&
        manifest.memorySchemaTo < manifest.memorySchemaFrom
    ) {
        result.message = "reject: memory schema downgrade";
        return result;
    }

    if (result.rollbackRequired && !manifest.rollbackSupported) {
        result.message = "reject: rollback required but not supported";
        return result;
    }

    result.ok = true;
    result.message = "accept: manifest compatible";
    return result;
}

}
