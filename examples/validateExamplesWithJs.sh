#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Example must be buildable with dev builds, so they should have dev maven repo set up.

set -euo pipefail

skip_imageviewer_wasm=false
if [ "$#" -eq 3 ] && [ "$3" = "--skip-imageviewer-wasm" ]; then
    skip_imageviewer_wasm=true
    set -- "$1" "$2"
fi

if [ "$#" -gt 2 ]; then
    echo "Optionally specify Compose and Kotlin versions. For example: ./validateExamplesWithJs.sh 1.13.0-alpha03 2.3.20"
    exit 1
fi

version_args=()
if [ "$#" -eq 1 ]; then
    version_args=("-Pcompose.version=$1")
fi

if [ "$#" -eq 2 ]; then
    version_args=("-Pcompose.version=$1" "-Pkotlin.version=$2")
fi


runGradle() {
    local example="$1"
    local task="$2"
    pushd "$example"
    if [ "${#version_args[@]}" -gt 0 ]; then
        ./gradlew "$task" "${version_args[@]}"
    else
        ./gradlew "$task"
    fi
    popd
}

runGradle html/compose-bird build
runGradle html/landing build
runGradle html/with-react build
if [ "$skip_imageviewer_wasm" = false ]; then
    runGradle imageviewer :webApp:wasmJsBrowserDistribution
fi
