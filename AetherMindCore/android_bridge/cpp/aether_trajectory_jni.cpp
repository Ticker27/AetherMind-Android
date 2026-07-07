#include <jni.h>

#include <cstddef>
#include <cstring>

#include "aether/memory/MemorySchema.h"
#include "aether/render/NativeTrajectoryBridge.h"

using aether::AETHER_PHYSICS_EXPERIENCE_STATE_VERSION;
using aether::PhysicsExperienceState;
using aether::globalTrajectoryBridge;

extern "C" JNIEXPORT jint JNICALL
Java_com_aether_renderer_NativeTrajectoryBridge_nativeStateSize(
    JNIEnv*,
    jclass
) {
    return static_cast<jint>(sizeof(PhysicsExperienceState));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_aether_renderer_NativeTrajectoryBridge_nativeStateLayoutVersion(
    JNIEnv*,
    jclass
) {
    return static_cast<jint>(AETHER_PHYSICS_EXPERIENCE_STATE_VERSION);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aether_renderer_NativeTrajectoryBridge_nativePublishState(
    JNIEnv* env,
    jclass,
    jobject directBuffer
) {
    if (!directBuffer) {
        return JNI_FALSE;
    }

    void* raw = env->GetDirectBufferAddress(directBuffer);
    jlong capacity = env->GetDirectBufferCapacity(directBuffer);

    if (!raw || capacity < static_cast<jlong>(sizeof(PhysicsExperienceState))) {
        return JNI_FALSE;
    }

    PhysicsExperienceState state;
    std::memcpy(
        &state,
        raw,
        sizeof(PhysicsExperienceState)
    );

    if (state.layoutVersion != AETHER_PHYSICS_EXPERIENCE_STATE_VERSION) {
        return JNI_FALSE;
    }

    globalTrajectoryBridge().publishState(state);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aether_renderer_NativeTrajectoryBridge_nativeCopyLatestState(
    JNIEnv* env,
    jclass,
    jobject directBuffer
) {
    if (!directBuffer) {
        return JNI_FALSE;
    }

    void* raw = env->GetDirectBufferAddress(directBuffer);
    jlong capacity = env->GetDirectBufferCapacity(directBuffer);

    if (!raw || capacity < static_cast<jlong>(sizeof(PhysicsExperienceState))) {
        return JNI_FALSE;
    }

    PhysicsExperienceState state;

    if (!globalTrajectoryBridge().copyLatestState(state)) {
        return JNI_FALSE;
    }

    if (state.layoutVersion != AETHER_PHYSICS_EXPERIENCE_STATE_VERSION) {
        return JNI_FALSE;
    }

    std::memcpy(
        raw,
        &state,
        sizeof(PhysicsExperienceState)
    );

    return JNI_TRUE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_aether_renderer_NativeTrajectoryBridge_nativePublishedCount(
    JNIEnv*,
    jclass
) {
    return static_cast<jlong>(
        globalTrajectoryBridge().publishedCount()
    );
}
