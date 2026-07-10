cmake_minimum_required(VERSION 3.22.1)

project(aether_execution_core LANGUAGES CXX)

add_library(
    aether_execution_core
    SHARED
    ExecutionBridge.cpp
    aether_execution_jni.cpp
)

target_compile_features(
    aether_execution_core
    PRIVATE
    cxx_std_17
)

target_include_directories(
    aether_execution_core
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_options(
    aether_execution_core
    PRIVATE
    -Wall
    -Wextra
    -Wpedantic
)
