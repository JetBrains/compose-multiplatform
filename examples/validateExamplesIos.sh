#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Don't add examples, which don't depend on maven.pkg.jetbrains.space, because they won't be able to compile.

set -euo pipefail

if [ "$#" -ne 2 ]; then
echo "Specify Compose and Kotlin version. For example: ./validateExamplesIos.sh 1.1.1 1.6.10"
exit 1
fi
COMPOSE_VERSION=$1
KOTLIN_VERSION=$2

ARCH="$(uname -m)"
isX86="$ARCH"=="x86_64"

linkCmd=""

if [ isX86 ]; then
    linkCmd="linkIosX64"
else
    linkCmd="linkIosArm64"
fi


runGradle() {
    pushd $1
    echo "Validating $1"
    ./gradlew clean $linkCmd -Pcompose.version=$COMPOSE_VERSION -Pkotlin.version=$KOTLIN_VERSION --rerun-tasks || (echo "Failed $1" && exit 1)
    popd
}

runGradle chat
runGradle codeviewer
runGradle imageviewer
runGradle graphics-2d
runGradle jetsnack
