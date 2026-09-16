#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Example must be buildable with dev builds, so they should have dev maven repo set up.

set -euo pipefail

if [ "$#" -gt 1 ]; then
    echo "Optionally specify a Compose version. For example: ./validateExamplesWeb.sh 1.13.0-alpha03"
    exit 1
fi

compose_args=()
if [ "$#" -eq 1 ]; then
    compose_args=("-Pcompose.version=$1")
fi


runGradle() {
    pushd "$1"
    ./gradlew "$2" "${compose_args[@]}"
    popd
}

runGradle html/compose-bird build
runGradle html/landing build
runGradle html/with-react build
runGradle imageviewer :webApp:wasmJsBrowserDistribution
