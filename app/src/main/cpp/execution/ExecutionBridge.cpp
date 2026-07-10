#include "ExecutionBridge.h"

#include <cmath>
#include <deque>
#include <mutex>
#include <new>

namespace aether::execution {
namespace {

// Single process-local queue.
// All reads/writes must acquire gQueueMutex before touching gCommandQueue.
std::mutex gQueueMutex;
std::deque<ActionCommand> gCommandQueue;

bool isValidCommandType(ActionCommandType type) noexcept {
    return type == ActionCommandType::TAP ||
           type == ActionCommandType::SWIPE;
}

ExecutionStatus validateCommand(const ActionCommand& command) noexcept {
    if (!isValidCommandType(command.type)) {
        return ExecutionStatus::ERROR_INVALID_COMMAND_TYPE;
    }

    // Reject NaN/Inf so downstream execution code never receives poisoned values.
    if (!std::isfinite(command.x) || !std::isfinite(command.y)) {
        return ExecutionStatus::ERROR_INVALID_COORDINATE;
    }

    return ExecutionStatus::OK;
}

} // namespace

ExecutionStatus pushCommand(const ActionCommand& command) noexcept {
    const ExecutionStatus validation = validateCommand(command);
    if (validation != ExecutionStatus::OK) {
        return validation;
    }

    try {
        // Mutex protects the queue from concurrent producers/consumers.
        // lock_guard releases automatically even if push_back throws.
        std::lock_guard<std::mutex> lock(gQueueMutex);
        gCommandQueue.push_back(command);
        return ExecutionStatus::OK;
    } catch (const std::bad_alloc&) {
        return ExecutionStatus::ERROR_ALLOCATION_FAILED;
    } catch (...) {
        return ExecutionStatus::ERROR_INTERNAL;
    }
}

ExecutionStatus popCommand(ActionCommand& outCommand) noexcept {
    // The whole pop sequence is one critical section:
    // check empty -> read front -> remove front.
    // This prevents races where another thread modifies the queue between steps.
    std::lock_guard<std::mutex> lock(gQueueMutex);

    if (gCommandQueue.empty()) {
        return ExecutionStatus::QUEUE_EMPTY;
    }

    outCommand = gCommandQueue.front();
    gCommandQueue.pop_front();

    return ExecutionStatus::OK;
}

ExecutionStatus clearCommands() noexcept {
    // Clear is atomic relative to push/pop because it uses the same mutex.
    // No command can be inserted or removed while this critical section runs.
    std::lock_guard<std::mutex> lock(gQueueMutex);
    gCommandQueue.clear();

    return ExecutionStatus::OK;
}

std::size_t commandQueueSize() noexcept {
    std::lock_guard<std::mutex> lock(gQueueMutex);
    return gCommandQueue.size();
}

} // namespace aether::execution
