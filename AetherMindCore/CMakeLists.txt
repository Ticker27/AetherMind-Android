cmake_minimum_required(VERSION 3.16)

project(AetherMindCore LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(AETHER_PROJECT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}")
include("${CMAKE_CURRENT_SOURCE_DIR}/cmake/CoreSources.cmake")

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

add_executable(aether_console
    tools/aether_console.cpp
)

target_link_libraries(aether_console PRIVATE
    aether_core
)
