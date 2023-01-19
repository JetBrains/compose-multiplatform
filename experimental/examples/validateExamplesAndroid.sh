#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Don't add examples, which don't depend on maven.pkg.jetbrains.space, because they won't be able to compile.

set -euo pipefail

if [ "$#" -ne 2 ]; then
echo "Specify Compose and Kotlin version. For example: ./validateExamplesAndroid.sh 1.1.1 1.6.10"
exit 1
fi
COMPOSE_VERSION=$1
KOTLIN_VERSION=$2


runGradle() {
    pushd $1
    ./gradlew $2 -Pcompose.version=$COMPOSE_VERSION -Pkotlin.version=$KOTLIN_VERSION --rerun-tasks
    popd
}

runGradle chat-mpp installDebug
runGradle codeviewer installDebug
runGradle falling-balls-mpp installDebug
runGradle imageviewer installDebug
runGradle minesweeper installDebug
runGradle todoapp-lite installDebug
runGradle visual-effects installDebug
runGradle widgets-gallery installDebug
