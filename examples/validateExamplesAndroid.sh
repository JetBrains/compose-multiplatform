#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Example must be buildable with dev builds, so they should have dev maven repo set up.

set -euo pipefail

if [ "$#" -gt 2 ]; then
    echo "Optionally specify Compose and Kotlin versions. For example: ./validateExamplesAndroid.sh 1.13.0-alpha03 2.3.20"
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
    ./gradlew "$task" "${version_args[@]}" --rerun-tasks
    popd
}

# requires an emulator running or an Android device to be connected
runGradle chat installDebug
runGradle codeviewer installDebug
runGradle imageviewer installDebug
runGradle issues installDebug
runGradle graphics-2d installDebug
