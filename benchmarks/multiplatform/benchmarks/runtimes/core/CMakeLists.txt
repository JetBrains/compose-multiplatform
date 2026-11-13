cmake_minimum_required(VERSION 3.24)

project(skiko_sdl_core LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

find_package(SDL2 CONFIG QUIET)
if(TARGET SDL2::SDL2)
    set(SKIKO_SDL_TARGET SDL2::SDL2)
else()
    find_path(
        SDL2_INCLUDE_DIR
        SDL.h
        HINTS /opt/homebrew/include /usr/local/include
        PATH_SUFFIXES SDL2
    )
    find_library(
        SDL2_LIBRARY
        NAMES SDL2
        HINTS /opt/homebrew/lib /usr/local/lib
    )
    if(NOT SDL2_INCLUDE_DIR OR NOT SDL2_LIBRARY)
        message(FATAL_ERROR "SDL2 was not found. Install it with: brew install sdl2")
    endif()
    add_library(skiko_sdl2 UNKNOWN IMPORTED)
    set_target_properties(
        skiko_sdl2
        PROPERTIES
            IMPORTED_LOCATION "${SDL2_LIBRARY}"
            INTERFACE_INCLUDE_DIRECTORIES "${SDL2_INCLUDE_DIR}"
    )
    set(SKIKO_SDL_TARGET skiko_sdl2)
endif()

add_library(
    skiko_sdl_core STATIC
    skiko_sdl_core.cpp
)

target_include_directories(
    skiko_sdl_core
    PUBLIC
        "${CMAKE_CURRENT_SOURCE_DIR}"
        "${CMAKE_CURRENT_SOURCE_DIR}/../abi"
)

target_compile_definitions(
    skiko_sdl_core
    PUBLIC
        GL_SILENCE_DEPRECATION
        SDL_MAIN_HANDLED
)

target_link_libraries(
    skiko_sdl_core
    PUBLIC
        "${SKIKO_SDL_TARGET}"
)

if(APPLE)
    target_link_libraries(skiko_sdl_core PUBLIC "-framework OpenGL")
endif()
