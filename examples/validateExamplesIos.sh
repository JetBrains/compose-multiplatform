#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Example must be buildable with dev builds, so they should have dev maven repo set up.

set -euo pipefail

if [ "$#" -gt 1 ]; then
    echo "Optionally specify a Compose version. For example: ./validateExamplesIos.sh 1.13.0-alpha03"
    exit 1
fi

compose_args=()
if [ "$#" -eq 1 ]; then
    compose_args=("-Pcompose.version=$1")
fi


runGradle() {
    pushd "$1"
    echo "Validating $1"
    ./gradlew clean linkIosArm64 "${compose_args[@]}" --rerun-tasks || (echo "Failed $1" && exit 1)
    popd
}

runGradle chat
runGradle codeviewer
runGradle imageviewer
runGradle graphics-2d
