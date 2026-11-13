cmake_minimum_required(VERSION 3.24)

project(wasmtime_sdl_runner LANGUAGES CXX)

if(NOT APPLE)
    message(WARNING "Non-Apple platform detected; build may require platform-specific OpenGL/EGL configuration.")
endif()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Add shared SDL core
add_subdirectory("${CMAKE_CURRENT_SOURCE_DIR}/../core" "${CMAKE_CURRENT_BINARY_DIR}/skiko_sdl_core")

if(NOT WASMTIME_ROOT)
    if(DEFINED ENV{WASMTIME_ROOT})
        set(WASMTIME_ROOT "$ENV{WASMTIME_ROOT}" CACHE PATH "Root of an unpacked Wasmtime C API release" FORCE)
    elseif(DEFINED ENV{WASMTIME_DIR})
        set(WASMTIME_ROOT "$ENV{WASMTIME_DIR}" CACHE PATH "Root of an unpacked Wasmtime C API release" FORCE)
    elseif(EXISTS "/Users/dustin.feucht/tools/wasmtime-v49.0.1-aarch64-macos-c-api")
        set(WASMTIME_ROOT "/Users/dustin.feucht/tools/wasmtime-v49.0.1-aarch64-macos-c-api" CACHE PATH "Root of an unpacked Wasmtime C API release" FORCE)
    else()
        file(GLOB WASMTIME_TOOL_CANDIDATES "$ENV{HOME}/tools/wasmtime-*-c-api" "/Users/dustin.feucht/tools/wasmtime-*-c-api")
        if(WASMTIME_TOOL_CANDIDATES)
            list(GET WASMTIME_TOOL_CANDIDATES 0 FIRST_WASMTIME_CANDIDATE)
            set(WASMTIME_ROOT "${FIRST_WASMTIME_CANDIDATE}" CACHE PATH "Root of an unpacked Wasmtime C API release" FORCE)
        else()
            set(WASMTIME_ROOT "" CACHE PATH "Root of an unpacked Wasmtime C API release")
        endif()
    endif()
endif()

if(NOT SKIA_ROOT)
    if(DEFINED ENV{SKIA_DIR})
        set(SKIA_ROOT "$ENV{SKIA_DIR}" CACHE PATH "Skia source tree" FORCE)
    elseif(DEFINED ENV{SKIA_ROOT})
        set(SKIA_ROOT "$ENV{SKIA_ROOT}" CACHE PATH "Skia source tree" FORCE)
    elseif(EXISTS "/Users/dustin.feucht/projects/skia")
        set(SKIA_ROOT "/Users/dustin.feucht/projects/skia" CACHE PATH "Skia source tree" FORCE)
    else()
        set(SKIA_ROOT "" CACHE PATH "Skia source tree")
    endif()
endif()

if(CMAKE_SYSTEM_PROCESSOR MATCHES "arm64|aarch64")
    set(SKIA_ARCH "arm64")
else()
    set(SKIA_ARCH "x64")
endif()

if(NOT SKIA_LIBRARY)
    if(DEFINED ENV{SKIA_LIBRARY})
        set(SKIA_LIBRARY "$ENV{SKIA_LIBRARY}" CACHE FILEPATH "Release-mode Skia archive" FORCE)
    elseif(SKIA_ROOT)
        if(EXISTS "${SKIA_ROOT}/out/Release-macos-${SKIA_ARCH}-gl/libskia.a")
            set(SKIA_LIBRARY "${SKIA_ROOT}/out/Release-macos-${SKIA_ARCH}-gl/libskia.a" CACHE FILEPATH "Release-mode Skia archive" FORCE)
        elseif(EXISTS "${SKIA_ROOT}/out/Release-macos-${SKIA_ARCH}/libskia.a")
            set(SKIA_LIBRARY "${SKIA_ROOT}/out/Release-macos-${SKIA_ARCH}/libskia.a" CACHE FILEPATH "Release-mode Skia archive" FORCE)
        endif()
    endif()
endif()

find_path(
    WASMTIME_INCLUDE_DIR
    wasmtime.h
    HINTS "${WASMTIME_ROOT}/include" /opt/homebrew/include /usr/local/include
)

find_library(
    WASMTIME_LIBRARY
    NAMES wasmtime
    HINTS "${WASMTIME_ROOT}/lib" /opt/homebrew/lib /usr/local/lib
)

if(NOT WASMTIME_INCLUDE_DIR OR NOT WASMTIME_LIBRARY)
    include(FetchContent)
    if(APPLE)
        if(CMAKE_SYSTEM_PROCESSOR MATCHES "arm64|aarch64")
            set(WASMTIME_URL "https://github.com/bytecodealliance/wasmtime/releases/download/v29.0.1/wasmtime-v29.0.1-aarch64-macos-c-api.tar.xz")
        else()
            set(WASMTIME_URL "https://github.com/bytecodealliance/wasmtime/releases/download/v29.0.1/wasmtime-v29.0.1-x86_64-macos-c-api.tar.xz")
        endif()
    else()
        if(CMAKE_SYSTEM_PROCESSOR MATCHES "arm64|aarch64")
            set(WASMTIME_URL "https://github.com/bytecodealliance/wasmtime/releases/download/v29.0.1/wasmtime-v29.0.1-aarch64-linux-c-api.tar.xz")
        else()
            set(WASMTIME_URL "https://github.com/bytecodealliance/wasmtime/releases/download/v29.0.1/wasmtime-v29.0.1-x86_64-linux-c-api.tar.xz")
        endif()
    endif()

    message(STATUS "Downloading Wasmtime C-API from ${WASMTIME_URL}...")
    FetchContent_Declare(
        wasmtime_c_api
        URL "${WASMTIME_URL}"
    )
    FetchContent_MakeAvailable(wasmtime_c_api)

    set(WASMTIME_INCLUDE_DIR "${wasmtime_c_api_SOURCE_DIR}/include")
    if(EXISTS "${wasmtime_c_api_SOURCE_DIR}/lib/libwasmtime.dylib")
        set(WASMTIME_LIBRARY "${wasmtime_c_api_SOURCE_DIR}/lib/libwasmtime.dylib")
    elseif(EXISTS "${wasmtime_c_api_SOURCE_DIR}/lib/libwasmtime.so")
        set(WASMTIME_LIBRARY "${wasmtime_c_api_SOURCE_DIR}/lib/libwasmtime.so")
    elseif(EXISTS "${wasmtime_c_api_SOURCE_DIR}/lib/libwasmtime.a")
        set(WASMTIME_LIBRARY "${wasmtime_c_api_SOURCE_DIR}/lib/libwasmtime.a")
    endif()
endif()

add_executable(
    wasmtime-sdl-runner
    src/main.cpp
    src/skiko_host.cc
    src/kotlin_app_host.cc
)

target_include_directories(
    wasmtime-sdl-runner
    PRIVATE
        "${WASMTIME_INCLUDE_DIR}"
        "${SKIA_ROOT}"
        "${CMAKE_CURRENT_SOURCE_DIR}/src"
        "${CMAKE_CURRENT_SOURCE_DIR}/generated"
        "${CMAKE_CURRENT_SOURCE_DIR}/../abi"
        "${CMAKE_CURRENT_SOURCE_DIR}/../core"
)

target_compile_definitions(
    wasmtime-sdl-runner
    PRIVATE
        NDEBUG
        SK_GANESH
        SK_GL
        SK_BUILD_FOR_SKIKO
        SK_ASSUME_GL=1
        GL_SILENCE_DEPRECATION
        SDL_MAIN_HANDLED
)

target_compile_options(
    wasmtime-sdl-runner
    PRIVATE
        -Wall
        -Wextra
        -fno-rtti
)

target_link_libraries(
    wasmtime-sdl-runner
    PRIVATE
        skiko_sdl_core
        "${WASMTIME_LIBRARY}"
)

if(SKIA_LIBRARY AND EXISTS "${SKIA_LIBRARY}")
    target_link_libraries(wasmtime-sdl-runner PRIVATE "${SKIA_LIBRARY}")
endif()

if(APPLE)
    target_link_libraries(wasmtime-sdl-runner PRIVATE "-framework OpenGL")
endif()
