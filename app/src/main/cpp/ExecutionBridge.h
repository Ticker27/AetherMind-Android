#pragma once

#include "ActionCommandSchema.h"

#include <cstddef>

namespace aether::execution {

// Pushes one command by value into the native queue.
// The command is copied into C++ ownership; no Java/Kotlin pointer is retained.
ExecutionStatus pushCommand(const ActionCommand& command) noexcept;

// Pops one command by value from the native queue.
// Returns QUEUE_EMPTY when there is no pending command.
ExecutionStatus popCommand(ActionCommand& outCommand) noexcept;

// Atomically clears all pending commands.
// "Atomic" here means no push/pop can interleave with clear because all queue
// mutations share the same mutex.
ExecutionStatus clearCommands() noexcept;

// Thread-safe queue size snapshot.
std::size_t commandQueueSize() noexcept;

} // namespace aether::execution
