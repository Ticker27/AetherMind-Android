#include "aether/runtime/IntegrationLoop.h"

#include <atomic>
#include <cstdint>
#include <cstring>

#include "aether/humanizer/Humanizer.h"
#include "aether/planning/CemPlanner.h"
#include "aether/render/NativeTrajectoryBridge.h"
#include "aether/security/OmnisGate.h"

#if defined(AETHER_ENABLE_INTEGRATION_JNI)
#include <jni.h>
#endif

namespace aether {

namespace {

constexpr std::uint32_t FLAG_INTEGRATION_LOOP = 0x00000400U;
constexpr std::uint32_t FLAG_AUTHORIZED_EXECUTION = 0x00000800U;
constexpr std::uint32_t FLAG_TELEMETRY_ONLY = 0x00001000U;
constexpr std::uint32_t FLAG_AI_ACTIVE = 0x00002000U;
constexpr std::uint32_t FLAG_HUD_VISIBLE = 0x00004000U;
constexpr std::uint32_t FLAG_INTENT_SHIFT = 24U;
constexpr std::uint32_t FLAG_INTENT_MASK = 0x03000000U;

constexpr std::int32_t KEYCODE_VOLUME_UP = 24;
constexpr std::int32_t TOUCH_ACTION_DOWN = 0;
constexpr std::uint64_t DOUBLE_TAP_WINDOW_NANOS = 300000000ULL;
constexpr float CENTER_TAP_RADIUS_RATIO = 0.18f;

struct RuntimeSwitchState {
    std::atomic<bool> hudVisible;
    std::atomic<bool> aiActive;
    std::atomic<std::uint64_t> lastCenterTapNanos;

    RuntimeSwitchState() noexcept
        : hudVisible(true),
          aiActive(true),
          lastCenterTapNanos(0ULL) {}
};

RuntimeSwitchState& runtimeSwitchState() noexcept {
    static RuntimeSwitchState state;
    return state;
}

IntegrationLoop& runtimeIntegrationLoop() noexcept {
    static IntegrationLoop loop;
    return loop;
}

SkillProfile& runtimeSkillProfile() noexcept {
    static SkillProfile skill = makeSkillProfile(SkillLevel::Intermediate);
    return skill;
}

PhysicsExperienceState normalizeObserverState(
    const PhysicsExperienceState& observerState
) noexcept {
    PhysicsExperienceState out = observerState;
    out.layoutVersion = AETHER_PHYSICS_EXPERIENCE_STATE_VERSION;
    return out;
}

bool readStateFromDirectBuffer(
#if defined(AETHER_ENABLE_INTEGRATION_JNI)
    JNIEnv* env,
    jobject directBuffer,
#else
    void*,
    void*,
#endif
    PhysicsExperienceState& out
) noexcept {
#if defined(AETHER_ENABLE_INTEGRATION_JNI)
    if (env == nullptr || directBuffer == nullptr) {
        return false;
    }

    void* raw = env->GetDirectBufferAddress(directBuffer);
    const jlong capacity = env->GetDirectBufferCapacity(directBuffer);

    if (raw == nullptr || capacity < static_cast<jlong>(sizeof(PhysicsExperienceState))) {
        return false;
    }

    std::memcpy(&out, raw, sizeof(PhysicsExperienceState));
    return out.layoutVersion == AETHER_PHYSICS_EXPERIENCE_STATE_VERSION;
#else
    (void)out;
    return false;
#endif
}

bool writeStateToDirectBuffer(
#if defined(AETHER_ENABLE_INTEGRATION_JNI)
    JNIEnv* env,
    jobject directBuffer,
#else
    void*,
    void*,
#endif
    const PhysicsExperienceState& state
) noexcept {
#if defined(AETHER_ENABLE_INTEGRATION_JNI)
    if (env == nullptr || directBuffer == nullptr) {
        return false;
    }

    void* raw = env->GetDirectBufferAddress(directBuffer);
    const jlong capacity = env->GetDirectBufferCapacity(directBuffer);

    if (raw == nullptr || capacity < static_cast<jlong>(sizeof(PhysicsExperienceState))) {
        return false;
    }

    std::memcpy(raw, &state, sizeof(PhysicsExperienceState));
    return true;
#else
    (void)state;
    return false;
#endif
}

PhysicsExperienceState stampIntentFlag(
    PhysicsExperienceState state,
    IntentType intent
) noexcept {
    state.flags &= ~FLAG_INTENT_MASK;
    state.flags |= (static_cast<std::uint32_t>(intent) << FLAG_INTENT_SHIFT) & FLAG_INTENT_MASK;
    return state;
}

IntentHudTelemetry makeHudTelemetry(
    IntentType intent,
    bool omnisAuthorized,
    bool hudVisible,
    bool aiActive
) noexcept {
    IntentHudTelemetry hud = IntegrationLoop::hudForIntent(intent, omnisAuthorized);

    if (!hudVisible) {
        hud.alpha = 0U;
    }

    if (aiActive) {
        hud.omnisAuthorized = omnisAuthorized;
    }

    return hud;
}

bool isCenterTap(
    float x,
    float y,
    float width,
    float height
) noexcept {
    if (width <= 0.0f || height <= 0.0f) {
        return false;
    }

    const float cx = width * 0.5f;
    const float cy = height * 0.5f;
    const float dx = x - cx;
    const float dy = y - cy;
    const float minAxis = width < height ? width : height;
    const float radius = minAxis * CENTER_TAP_RADIUS_RATIO;

    return (dx * dx + dy * dy) <= (radius * radius);
}

} // namespace

IntegrationLoop::IntegrationLoop() noexcept
    : sequence(0ULL) {}

IntentHudTelemetry IntegrationLoop::hudForIntent(
    IntentType intent,
    bool omnisAuthorized
) noexcept {
    IntentHudTelemetry hud;
    hud.intent = intent;
    hud.omnisAuthorized = omnisAuthorized;

    if (omnisAuthorized) {
        hud.color = HudIntentColor::HolographicChroma;
        hud.label = "OMNIS";
        hud.red = 180U;
        hud.green = 255U;
        hud.blue = 255U;
        hud.alpha = 255U;
        return hud;
    }

    switch (intent) {
        case IntentType::Offensive:
            hud.color = HudIntentColor::GoldAmber;
            hud.label = "OFFENSIVE";
            hud.red = 255U;
            hud.green = 191U;
            hud.blue = 0U;
            hud.alpha = 255U;
            break;

        case IntentType::SafetyPlay:
            hud.color = HudIntentColor::DeepPurple;
            hud.label = "SAFETY_PLAY";
            hud.red = 82U;
            hud.green = 45U;
            hud.blue = 128U;
            hud.alpha = 255U;
            break;

        case IntentType::Defensive:
            hud.color = HudIntentColor::DefensiveBlue;
            hud.label = "DEFENSIVE";
            hud.red = 64U;
            hud.green = 128U;
            hud.blue = 255U;
            hud.alpha = 255U;
            break;

        case IntentType::Positioning:
        default:
            hud.color = HudIntentColor::PositioningGreen;
            hud.label = "POSITIONING";
            hud.red = 64U;
            hud.green = 210U;
            hud.blue = 144U;
            hud.alpha = 255U;
            break;
    }

    return hud;
}

IntegrationLoopResult IntegrationLoop::classifyOnlyFrame(
    const PhysicsExperienceState& observerState,
    const SkillProfile& skill
) noexcept {
    IntegrationLoopResult result;
    result.observerState = normalizeObserverState(observerState);
    result.executionState = result.observerState;
    result.observerAccepted = true;

    Strategist strategist;
    result.intent = strategist.selectIntent(result.observerState, skill);

    RuntimeSwitchState& switches = runtimeSwitchState();
    result.hud = makeHudTelemetry(
        result.intent.type,
        false,
        switches.hudVisible.load(std::memory_order_relaxed),
        switches.aiActive.load(std::memory_order_relaxed)
    );

    result.executionState = stampIntentFlag(result.executionState, result.intent.type);
    result.executionState.flags |= FLAG_TELEMETRY_ONLY;
    if (switches.hudVisible.load(std::memory_order_relaxed)) {
        result.executionState.flags |= FLAG_HUD_VISIBLE;
    }
    if (switches.aiActive.load(std::memory_order_relaxed)) {
        result.executionState.flags |= FLAG_AI_ACTIVE;
    }

    return result;
}

IntegrationLoopResult IntegrationLoop::executeAutoPlayFrame(
    const PhysicsExperienceState& observerState,
    const SkillProfile& skill,
    const char* authorizationKey
) noexcept {
    IntegrationLoopResult result;
    result.observerState = normalizeObserverState(observerState);
    result.executionState = result.observerState;
    result.observerAccepted = true;

    RuntimeSwitchState& switches = runtimeSwitchState();
    const bool hudVisible = switches.hudVisible.load(std::memory_order_relaxed);
    const bool aiActive = switches.aiActive.load(std::memory_order_relaxed);

    Strategist strategist;
    result.intent = strategist.selectIntent(result.observerState, skill);

    const bool authorized = OmnisGate::autoPlayTrigger(authorizationKey);
    result.authorized = authorized;
    result.hud = makeHudTelemetry(result.intent.type, authorized, hudVisible, aiActive);

    // Hard fail-closed contract:
    // Without authorization, or while AI is idle, the loop may publish telemetry
    // for the HUD/aim visualization only. It must not publish auto-play output.
    if (!authorized || !aiActive) {
        result.executionState = stampIntentFlag(result.executionState, result.intent.type);
        result.executionState.flags |= FLAG_TELEMETRY_ONLY;
        if (hudVisible) {
            result.executionState.flags |= FLAG_HUD_VISIBLE;
        }
        if (aiActive) {
            result.executionState.flags |= FLAG_AI_ACTIVE;
        }
        result.executed = false;
        globalTrajectoryBridge().publishState(result.executionState);
        return result;
    }

    CemPlanner planner;
    Humanizer humanizer;
    HumanizerState humanizerState;

    const std::uint64_t seed =
        result.observerState.timestampNanos ^
        (++sequence * 0x9E3779B97F4A7C15ULL);

    const PhysicsExperienceState planned = planner.planTelemetry(
        result.observerState,
        result.intent,
        skill,
        seed
    );

    result.executionState = humanizer.applyTelemetry(
        planned,
        skill,
        humanizerState
    );

    result.executionState = stampIntentFlag(result.executionState, result.intent.type);
    result.executionState.flags |= FLAG_INTEGRATION_LOOP;
    result.executionState.flags |= FLAG_AUTHORIZED_EXECUTION;
    result.executionState.flags |= FLAG_AI_ACTIVE;
    if (hudVisible) {
        result.executionState.flags |= FLAG_HUD_VISIBLE;
    }
    result.executionState.layoutVersion = AETHER_PHYSICS_EXPERIENCE_STATE_VERSION;
    result.executed = true;

    globalTrajectoryBridge().publishState(result.executionState);
    return result;
}

// Native hardware trigger listener. KEYCODE_VOLUME_UP toggles HUD visibility.
bool integrationLoopOnKeyEvent(
    std::int32_t keyCode,
    bool pressed
) noexcept {
    if (!pressed || keyCode != KEYCODE_VOLUME_UP) {
        return false;
    }

    RuntimeSwitchState& switches = runtimeSwitchState();
    const bool current = switches.hudVisible.load(std::memory_order_relaxed);
    switches.hudVisible.store(!current, std::memory_order_relaxed);
    return true;
}

// Native touch listener. A double-tap near the display center toggles AI Active/Idle.
bool integrationLoopOnTouchEvent(
    std::int32_t action,
    float x,
    float y,
    float width,
    float height,
    std::uint64_t eventTimeNanos
) noexcept {
    if (action != TOUCH_ACTION_DOWN || !isCenterTap(x, y, width, height)) {
        return false;
    }

    RuntimeSwitchState& switches = runtimeSwitchState();
    const std::uint64_t previous = switches.lastCenterTapNanos.exchange(
        eventTimeNanos,
        std::memory_order_relaxed
    );

    if (previous != 0ULL && eventTimeNanos >= previous &&
        eventTimeNanos - previous <= DOUBLE_TAP_WINDOW_NANOS) {
        const bool current = switches.aiActive.load(std::memory_order_relaxed);
        switches.aiActive.store(!current, std::memory_order_relaxed);
        switches.lastCenterTapNanos.store(0ULL, std::memory_order_relaxed);
        return true;
    }

    return false;
}

bool integrationLoopHudVisible() noexcept {
    return runtimeSwitchState().hudVisible.load(std::memory_order_relaxed);
}

bool integrationLoopAiActive() noexcept {
    return runtimeSwitchState().aiActive.load(std::memory_order_relaxed);
}

} // namespace aether

#if defined(AETHER_ENABLE_INTEGRATION_JNI)

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aether_renderer_AetherIntegrationLoop_nativeOnKeyEvent(
    JNIEnv*,
    jclass,
    jint keyCode,
    jboolean pressed
) {
    return aether::integrationLoopOnKeyEvent(
        static_cast<std::int32_t>(keyCode),
        pressed == JNI_TRUE
    ) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aether_renderer_AetherIntegrationLoop_nativeOnTouchEvent(
    JNIEnv*,
    jclass,
    jint action,
    jfloat x,
    jfloat y,
    jfloat width,
    jfloat height,
    jlong eventTimeNanos
) {
    return aether::integrationLoopOnTouchEvent(
        static_cast<std::int32_t>(action),
        static_cast<float>(x),
        static_cast<float>(y),
        static_cast<float>(width),
        static_cast<float>(height),
        static_cast<std::uint64_t>(eventTimeNanos)
    ) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aether_renderer_AetherIntegrationLoop_nativeHudVisible(
    JNIEnv*,
    jclass
) {
    return aether::integrationLoopHudVisible() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aether_renderer_AetherIntegrationLoop_nativeAiActive(
    JNIEnv*,
    jclass
) {
    return aether::integrationLoopAiActive() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_aether_renderer_AetherIntegrationLoop_nativeRunFrame(
    JNIEnv* env,
    jclass,
    jobject observerStateBuffer,
    jobject outputStateBuffer,
    jstring authorizationKey
) {
    aether::PhysicsExperienceState observerState;
    if (!aether::readStateFromDirectBuffer(env, observerStateBuffer, observerState)) {
        return JNI_FALSE;
    }

    const char* keyChars = nullptr;
    if (authorizationKey != nullptr) {
        keyChars = env->GetStringUTFChars(authorizationKey, nullptr);
    }

    const aether::IntegrationLoopResult result =
        aether::runtimeIntegrationLoop().executeAutoPlayFrame(
            observerState,
            aether::runtimeSkillProfile(),
            keyChars
        );

    if (keyChars != nullptr) {
        env->ReleaseStringUTFChars(authorizationKey, keyChars);
    }

    if (!aether::writeStateToDirectBuffer(env, outputStateBuffer, result.executionState)) {
        return JNI_FALSE;
    }

    return result.observerAccepted ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_aether_renderer_AetherIntegrationLoop_nativeLatestHudIntent(
    JNIEnv*,
    jclass
) {
    aether::PhysicsExperienceState latest;
    if (!aether::globalTrajectoryBridge().copyLatestState(latest)) {
        return static_cast<jint>(aether::IntentType::Positioning);
    }

    const std::uint32_t raw = (latest.flags & aether::FLAG_INTENT_MASK) >> aether::FLAG_INTENT_SHIFT;
    if (raw > static_cast<std::uint32_t>(aether::IntentType::Positioning)) {
        return static_cast<jint>(aether::IntentType::Positioning);
    }

    return static_cast<jint>(raw);
}

#endif // AETHER_ENABLE_INTEGRATION_JNI
