# Shared native source inventory for desktop and Android/NDK builds.
# Consumers may define AETHER_PROJECT_ROOT before including this file.

if(NOT DEFINED AETHER_PROJECT_ROOT)
    get_filename_component(AETHER_PROJECT_ROOT "${CMAKE_CURRENT_LIST_DIR}/.." ABSOLUTE)
endif()

set(AETHER_BASE_CORE_SOURCES
    "${AETHER_PROJECT_ROOT}/src/core/AetherMind.cpp"
    "${AETHER_PROJECT_ROOT}/src/world/WorldBuilder.cpp"
    "${AETHER_PROJECT_ROOT}/src/world/WorldStateValidator.cpp"
    "${AETHER_PROJECT_ROOT}/src/planning/ActionGenerator.cpp"
    "${AETHER_PROJECT_ROOT}/src/planning/Evaluator.cpp"
    "${AETHER_PROJECT_ROOT}/src/planning/CemPlanner.cpp"
    "${AETHER_PROJECT_ROOT}/src/planning/Planner.cpp"
    "${AETHER_PROJECT_ROOT}/src/planning/DecisionPolicy.cpp"
    "${AETHER_PROJECT_ROOT}/src/cognition/ExecutionModel.cpp"
    "${AETHER_PROJECT_ROOT}/src/memory/ExperienceMemory.cpp"
    "${AETHER_PROJECT_ROOT}/src/memory/MemoryStore.cpp"
    "${AETHER_PROJECT_ROOT}/src/update/UpdateManifest.cpp"
    "${AETHER_PROJECT_ROOT}/src/update/UpdateManager.cpp"
    "${AETHER_PROJECT_ROOT}/src/runtime/BrainRuntime.cpp"
    "${AETHER_PROJECT_ROOT}/src/runtime/IntegrationLoop.cpp"
    "${AETHER_PROJECT_ROOT}/src/runtime/OmnisGate.cpp"
    "${AETHER_PROJECT_ROOT}/src/render/NativeTrajectoryBridge.cpp"
    "${AETHER_PROJECT_ROOT}/src/strategy/Strategist.cpp"
    "${AETHER_PROJECT_ROOT}/src/humanizer/Humanizer.cpp"
    "${AETHER_PROJECT_ROOT}/src/physics/PhysicsKernel.cpp"
    "${AETHER_PROJECT_ROOT}/src/storage/StorageManager.cpp"
)

# AT145 Pool Geometry Core: deterministic, add-only educational geometry layer.
# This module is pure C++17 math. It has no JNI, Android, rendering, OS, or execution authority.
set(POOL_GEOMETRY_SOURCES
    "${AETHER_PROJECT_ROOT}/src/pool/geometry/GhostBallSolver.cpp"
    "${AETHER_PROJECT_ROOT}/src/pool/geometry/CutAngleSolver.cpp"
    "${AETHER_PROJECT_ROOT}/src/pool/geometry/PoolGeometryEngine.cpp"
)

set(AETHER_CORE_SOURCES
    ${AETHER_BASE_CORE_SOURCES}
    ${POOL_GEOMETRY_SOURCES}
)
