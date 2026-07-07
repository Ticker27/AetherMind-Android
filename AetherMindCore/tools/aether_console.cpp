#include "aether/core/CoreVersion.h"
#include "aether/memory/MemorySchema.h"
#include "aether/render/NativeTrajectoryBridge.h"
#include "aether/humanizer/Humanizer.h"
#include "aether/runtime/IntegrationLoop.h"
#include "aether/security/OmnisGate.h"

#include <iostream>

using namespace aether;

static PhysicsExperienceState makeState(
    float cueX,
    float cueY,
    float targetX,
    float targetY,
    float confidenceBias,
    float riskBias,
    std::uint64_t stamp
) {
    PhysicsExperienceState s;

    s.cuePosition.x = cueX;
    s.cuePosition.y = cueY;

    s.targetPosition.x = targetX;
    s.targetPosition.y = targetY;

    s.angleOffset = 0.01f;
    s.powerScale = 1.0f;
    s.velocityScale = 1.0f;
    s.errorMargin = 0.04f;

    s.confidenceBias = confidenceBias;
    s.riskBias = riskBias;
    s.cushionBounceCount = 0;
    s.timestampNanos = stamp;

    return s;
}

int main() {
    std::cout << "PHASE=7.1_STRATEGIC_INTEGRATION_FINALIZED\n";
    std::cout << "CORE=" << AETHER_CORE_VERSION << "\n";
    std::cout << "PhysicsExperienceState.size="
              << sizeof(PhysicsExperienceState)
              << "\n";

    std::cout << "PhysicsExperienceState.align="
              << alignof(PhysicsExperienceState)
              << "\n";

    NativeTrajectoryBridge bridge;

    PhysicsExperienceState state1 =
        makeState(
            0.20f,
            0.50f,
            0.55f,
            0.50f,
            0.04f,
            -0.03f,
            1
        );

    PhysicsExperienceState state2 =
        makeState(
            0.22f,
            0.48f,
            0.62f,
            0.44f,
            0.08f,
            -0.06f,
            2
        );

    Humanizer humanizer;
    HumanizerState humanizerState;
    SkillProfile skill = makeSkillProfile(SkillLevel::Intermediate);

    PhysicsExperienceState humanized1 =
        humanizer.applyTelemetry(state1, skill, humanizerState);

    PhysicsExperienceState humanized2 =
        humanizer.applyTelemetry(state2, skill, humanizerState);

    bridge.publishState(humanized1);
    bridge.publishState(humanized2);

    PhysicsExperienceState latest;

    bool ok =
        bridge.copyLatestStrategicState(latest, skill);

    IntegrationLoop loop;
    const IntegrationLoopResult denied = loop.executeAutoPlayFrame(
        state2,
        skill,
        "BAD_KEY"
    );

    const IntegrationLoopResult authorized = loop.executeAutoPlayFrame(
        state2,
        skill,
        OmnisGate::RequiredKey
    );

    const IntegrationLoopResult classified = loop.classifyOnlyFrame(
        state2,
        skill
    );

    bool passed =
        ok &&
        bridge.publishedCount() == 2 &&
        latest.timestampNanos == 2 &&
        latest.cuePosition.x == 0.22f &&
        latest.targetPosition.x == 0.62f &&
        latest.layoutVersion == AETHER_PHYSICS_EXPERIENCE_STATE_VERSION &&
        sizeof(PhysicsExperienceState) == 64 &&
        alignof(PhysicsExperienceState) == 8 &&
        (latest.flags & 0x00000300U) == 0x00000300U &&
        denied.observerAccepted &&
        !denied.authorized &&
        !denied.executed &&
        authorized.authorized &&
        authorized.executed &&
        (authorized.executionState.flags & 0x00000C00U) == 0x00000C00U &&
        classified.observerAccepted &&
        classified.hud.label != nullptr;

    std::cout << "BRIDGE_COPY ok="
              << (ok ? "yes" : "no")
              << "\n";

    std::cout << "PUBLISHED_COUNT="
              << bridge.publishedCount()
              << "\n";

    std::cout << "LATEST_STAMP="
              << latest.timestampNanos
              << "\n";

    std::cout << "HUMANIZER_FLAGS="
              << latest.flags
              << "\n";

    std::cout << "HUMANIZER_ERROR_MARGIN="
              << latest.errorMargin
              << "\n";

    std::cout << "DENIED_EXECUTED="
              << (denied.executed ? "yes" : "no")
              << "\n";

    std::cout << "AUTHORIZED_EXECUTED="
              << (authorized.executed ? "yes" : "no")
              << "\n";

    std::cout << "HUD_LABEL="
              << authorized.hud.label
              << "\n";

    std::cout << "ANDROID_BRIDGE_FILES=android_bridge/cpp,android_bridge/kotlin\n";

    std::cout << "PHASE7_1_INTEGRATION_RESULT="
              << (passed ? "PASS" : "FAIL")
              << "\n";

    return passed ? 0 : 1;
}
