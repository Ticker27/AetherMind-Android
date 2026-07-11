#pragma once

#include <cstdint>
#include <cstddef>
#include <type_traits>

namespace aether::execution {

// Fixed-width command type for ABI compatibility across Android ABIs.
// Kotlin/Java side should write this as UInt/Int32 using ByteOrder.nativeOrder().
enum class ActionCommandType : std::uint32_t {
    TAP   = 1u,
    SWIPE = 2u
};

// Status values returned to Kotlin through JNI.
// Non-negative values are normal outcomes; negative values are errors.
enum class ExecutionStatus : std::int32_t {
    OK = 0,
    QUEUE_EMPTY = 1,

    ERROR_NULL_BUFFER = -1,
    ERROR_NON_DIRECT_BUFFER = -2,
    ERROR_INSUFFICIENT_CAPACITY = -3,
    ERROR_INVALID_COMMAND_TYPE = -4,
    ERROR_INVALID_COORDINATE = -5,
    ERROR_ALLOCATION_FAILED = -6,
    ERROR_INTERNAL = -7,
    ERROR_EXECUTION_LOCKED = -8
};

// ABI-stable command schema.
//
// Layout, bytes:
//   0  - 3   : float x
//   4  - 7   : float y
//   8  - 11  : uint32 command type
//   12 - 15  : reserved padding/version field
//   16 - 23  : uint64 timestampNanos
//
// Size is intentionally fixed to 24 bytes.
// Do not reorder fields without bumping the schema contract.
struct alignas(8) ActionCommand final {
    float x;
    float y;
    ActionCommandType type;
    std::uint32_t reserved;
    std::uint64_t timestampNanos;
};

static_assert(sizeof(ActionCommand) == 24, "ActionCommand ABI size must remain 24 bytes.");
static_assert(alignof(ActionCommand) == 8, "ActionCommand ABI alignment must remain 8 bytes.");
static_assert(std::is_standard_layout<ActionCommand>::value,
              "ActionCommand must remain standard-layout for ABI compatibility.");
static_assert(std::is_trivially_copyable<ActionCommand>::value,
              "ActionCommand must remain trivially copyable for safe memcpy from DirectByteBuffer.");

constexpr std::size_t kActionCommandSize = sizeof(ActionCommand);

} // namespace aether::execution
