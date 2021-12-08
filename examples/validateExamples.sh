#!/bin/bash

# Script to build all examples, to verify if they can compile

set -euo pipefail

runGradle() {
    pushd $1
    ./gradlew $2
    popd
}

runGradle codeviewer package
runGradle falling-balls package
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
