#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Don't add examples, which don't depend on maven.pkg.jetbrains.space, because they won't be able to compile.

set -euo pipefail

if [ "$#" -ne 2 ]; then
echo "Specify Compose and Kotlin version. For example: ./validateExamples.sh 1.1.1 1.6.10"
exit 1
fi
COMPOSE_VERSION=$1
KOTLIN_VERSION=$2


runGradle() {
    pushd $1
    ./gradlew $2 -Pcompose.version=$COMPOSE_VERSION -Pkotlin.version=$KOTLIN_VERSION --rerun-tasks
    popd
}

runGradle chat-mpp packageDistributionForCurrentOS
runGradle codeviewer packageDistributionForCurrentOS
runGradle falling-balls-mpp packageDistributionForCurrentOS
runGradle imageviewer packageDistributionForCurrentOS
runGradle minesweeper packageDistributionForCurrentOS
# runGradle todoapp-lite packageDistributionForCurrentOS  # Disabled due to scrollbars until upgrade to compose 1.4.0
runGradle visual-effects packageDistributionForCurrentOS
runGradle widgets-gallery packageDistributionForCurrentOS
