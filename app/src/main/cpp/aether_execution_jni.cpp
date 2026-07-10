#include "ActionCommandSchema.h"
#include "ExecutionBridge.h"

#include <jni.h>

#include <cstring>
#include <limits>

namespace {

using aether::execution::ActionCommand;
using aether::execution::ExecutionStatus;
using aether::execution::kActionCommandSize;

jint toJniStatus(ExecutionStatus status) noexcept {
    return static_cast<jint>(status);
}

ExecutionStatus validateDirectBuffer(JNIEnv* env, jobject buffer, void** outAddress) noexcept {
    if (buffer == nullptr) {
        return ExecutionStatus::ERROR_NULL_BUFFER;
    }

    // GetDirectBufferCapacity returns the capacity of the direct buffer memory region.
    // A negative value or null address is treated as non-direct/unsupported access.
    const jlong capacity = env->GetDirectBufferCapacity(buffer);
    if (capacity < 0) {
        return ExecutionStatus::ERROR_NON_DIRECT_BUFFER;
    }

    if (capacity < static_cast<jlong>(kActionCommandSize)) {
        return ExecutionStatus::ERROR_INSUFFICIENT_CAPACITY;
    }

    void* address = env->GetDirectBufferAddress(buffer);
    if (address == nullptr) {
        return ExecutionStatus::ERROR_NON_DIRECT_BUFFER;
    }

    *outAddress = address;
    return ExecutionStatus::OK;
}

ExecutionStatus readCommandFromDirectBuffer(
    JNIEnv* env,
    jobject buffer,
    ActionCommand& outCommand
) noexcept {
    void* address = nullptr;
    const ExecutionStatus validation = validateDirectBuffer(env, buffer, &address);
    if (validation != ExecutionStatus::OK) {
        return validation;
    }

    // Memory access rule:
    // Do not reinterpret_cast and dereference ActionCommand* directly.
    // memcpy avoids strict-aliasing and alignment issues from JVM-provided memory.
    //
    // The raw pointer is used only during this JNI call and is never stored.
    std::memcpy(&outCommand, address, kActionCommandSize);

    return ExecutionStatus::OK;
}

ExecutionStatus writeCommandToDirectBuffer(
    JNIEnv* env,
    jobject buffer,
    const ActionCommand& command
) noexcept {
    void* address = nullptr;
    const ExecutionStatus validation = validateDirectBuffer(env, buffer, &address);
    if (validation != ExecutionStatus::OK) {
        return validation;
    }

    // Writes a by-value command snapshot into the caller-provided DirectByteBuffer.
    // No native ownership of the buffer or raw pointer is retained.
    std::memcpy(address, &command, kActionCommandSize);

    return ExecutionStatus::OK;
}

} // namespace

extern "C" JNIEXPORT jint JNICALL
Java_com_aethermind_execution_AetherExecutionNative_nativePushCommand(
    JNIEnv* env,
    jclass,
    jobject commandBuffer
) {
    ActionCommand command{};

    const ExecutionStatus readStatus =
        readCommandFromDirectBuffer(env, commandBuffer, command);

    if (readStatus != ExecutionStatus::OK) {
        return toJniStatus(readStatus);
    }

    return toJniStatus(aether::execution::pushCommand(command));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_aethermind_execution_AetherExecutionNative_nativePopCommand(
    JNIEnv* env,
    jclass,
    jobject outCommandBuffer
) {
    ActionCommand command{};

    const ExecutionStatus popStatus = aether::execution::popCommand(command);
    if (popStatus != ExecutionStatus::OK) {
        return toJniStatus(popStatus);
    }

    return toJniStatus(writeCommandToDirectBuffer(env, outCommandBuffer, command));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_aethermind_execution_AetherExecutionNative_nativeClearQueue(
    JNIEnv*,
    jclass
) {
    return toJniStatus(aether::execution::clearCommands());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_aethermind_execution_AetherExecutionNative_nativeCommandSize(
    JNIEnv*,
    jclass
) {
    return static_cast<jint>(kActionCommandSize);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_aethermind_execution_AetherExecutionNative_nativeQueueSize(
    JNIEnv*,
    jclass
) {
    const std::size_t size = aether::execution::commandQueueSize();

    if (size > static_cast<std::size_t>(std::numeric_limits<jint>::max())) {
        return std::numeric_limits<jint>::max();
    }

    return static_cast<jint>(size);
}
