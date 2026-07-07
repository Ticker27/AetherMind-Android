cmake_minimum_required(VERSION 3.22)

project(aether_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Android production flags: keep the native loop lean and deterministic.
add_compile_options(
    -O3
    -fno-exceptions
    -fno-rtti
    -ffunction-sections
    -fdata-sections
    -Wall
    -Wextra
)

add_link_options(
    -Wl,--gc-sections
)

get_filename_component(AETHER_PROJECT_ROOT "${CMAKE_CURRENT_LIST_DIR}/../.." ABSOLUTE)
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
    aether_trajectory_jni.cpp
)

target_include_directories(aether_jni PRIVATE
    "${AETHER_PROJECT_ROOT}/include"
)

find_library(log-lib log)

# JNI symbols implemented inside static-library object files can be stripped
# when no C++ object directly references them. Keep all aether_core objects
# while producing libaether_jni.so.
target_link_libraries(aether_jni PRIVATE
    -Wl,--whole-archive
    aether_core
    -Wl,--no-whole-archive
    ${log-lib}
)
