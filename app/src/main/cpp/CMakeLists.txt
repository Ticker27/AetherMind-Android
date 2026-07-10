cmake_minimum_required(VERSION 3.22.1)

project(aether_mind_integrated LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Keep compile flags target-scoped. Do not set -fno-exceptions globally because
# aether_execution_core intentionally uses try/catch for allocation-safe status returns.
add_compile_options(
    -Wall
    -Wextra
    -ffunction-sections
    -fdata-sections
)

add_link_options(
    -Wl,--gc-sections
)

find_library(log-lib log)

# -----------------------------------------------------------------------------
# Existing AetherMind trajectory/native core
# -----------------------------------------------------------------------------
set(AETHER_PROJECT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/aether_core")
include("${AETHER_PROJECT_ROOT}/cmake/CoreSources.cmake")

add_library(aether_core STATIC
    ${AETHER_CORE_SOURCES}
)

set_target_properties(aether_core PROPERTIES
    POSITION_INDEPENDENT_CODE ON
)

target_include_directories(aether_core PUBLIC
    "${AETHER_PROJECT_ROOT}/include"
)

target_compile_features(aether_core PUBLIC cxx_std_17)

target_compile_definitions(aether_core PRIVATE
    AETHER_ENABLE_INTEGRATION_JNI
)

add_library(aether_jni SHARED
    trajectory/aether_trajectory_jni.cpp
)

target_include_directories(aether_jni PRIVATE
    "${AETHER_PROJECT_ROOT}/include"
)

target_link_libraries(aether_jni PRIVATE
    -Wl,--whole-archive
    aether_core
    -Wl,--no-whole-archive
    ${log-lib}
)

# -----------------------------------------------------------------------------
# Phase 1 AetherMind Execution Core ABI/JNI queue
# -----------------------------------------------------------------------------
add_library(aether_execution_core SHARED
    execution/ExecutionBridge.cpp
    execution/aether_execution_jni.cpp
)

target_include_directories(aether_execution_core PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}/execution"
)

target_compile_features(aether_execution_core PRIVATE cxx_std_17)

target_link_libraries(aether_execution_core PRIVATE
    ${log-lib}
)

# -----------------------------------------------------------------------------
# Phase 3 Aether Vision Lite - Ball Detection + Trajectory
# -----------------------------------------------------------------------------
add_library(aether_vision SHARED
    vision/VisionProcessorImpl.cpp
    vision/TrajectoryEngine.cpp
    vision/aether_vision_jni.cpp
)

target_include_directories(aether_vision PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}/vision"
)

target_compile_features(aether_vision PRIVATE cxx_std_17)

target_compile_options(aether_vision PRIVATE
    -O3
    -ffast-math
    -fno-exceptions
    -fno-rtti
    $<$<CONFIG:Release>:-funroll-loops>
)

target_link_libraries(aether_vision PRIVATE
    ${log-lib}
)
