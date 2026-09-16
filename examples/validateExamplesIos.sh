#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Example must be buildable with dev builds, so they should have dev maven repo set up.

set -euo pipefail

if [ "$#" -gt 2 ]; then
    echo "Optionally specify Compose and Kotlin versions. For example: ./validateExamplesIos.sh 1.13.0-alpha03 2.3.20"
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
    pushd "$example"
    echo "Validating $example"
    ./gradlew clean linkIosArm64 "${version_args[@]}" --rerun-tasks || (echo "Failed $example" && exit 1)
    popd
}

runGradle chat
runGradle codeviewer
runGradle imageviewer
runGradle graphics-2d
