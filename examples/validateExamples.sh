#!/bin/bash

# Script to build all examples, to verify if they can compile

set -euo pipefail

if [ -z "$@" ]; then
echo "Specify Compose version. For example: ./validateExamples.sh 1.0.0"
exit 1
fi
COMPOSE_VERSION=$@

runGradle() {
    pushd $1
    ./gradlew $2 -Pcompose.version=$COMPOSE_VERSION
    popd
}

runGradle codeviewer package
runGradle falling-balls-web build
runGradle imageviewer package
runGradle intellij-plugin build
runGradle issues package
runGradle notepad package
runGradle todoapp-lite package
runGradle visual-effects package
runGradle web-compose-bird build
runGradle web-landing build
runGradle web-with-react build
runGradle widgets-gallery package
