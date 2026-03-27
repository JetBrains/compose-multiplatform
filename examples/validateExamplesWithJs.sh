#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Example must be buildable with dev builds, so they should have dev maven repo set up.

set -euo pipefail

if [ "$#" -ne 2 ]; then
echo "Specify Compose and Kotlin version. For example: ./validateExamplesWithJs.sh 1.1.1 1.6.10"
exit 1
fi
COMPOSE_VERSION=$1
KOTLIN_VERSION=$2


runGradle() {
    pushd $1
    ./gradlew $2 -Pcompose.version=$COMPOSE_VERSION -Pkotlin.version=$KOTLIN_VERSION
    popd
}

runGradle html/compose-bird build
runGradle html/landing build
runGradle html/with-react build
runGradle imageviewer :webApp:wasmJsBrowserDistribution
