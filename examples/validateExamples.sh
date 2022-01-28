#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Don't add examples, which don't depend on maven.pkg.jetbrains.space, because they won't be able to compile.

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
runGradle imageviewer package
runGradle issues package
runGradle notepad package
runGradle todoapp-lite package
runGradle visual-effects package
runGradle web-compose-bird build
runGradle web-landing build
runGradle web-with-react build
runGradle widgets-gallery package
