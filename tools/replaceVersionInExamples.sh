#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/replace_version_common.sh"

# Replace hard-coded Compose version in Compose repo projects. Usage: ./replace.sh 1.0.0-rc6

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"/..

# Add folders which should contain up-to-date versions
declare -a folders=(
    "$ROOT/examples"
    "$ROOT/ci"
    "$ROOT/tutorials"
)

if [ ! -z "$COMPOSE_TEMPLATES_FOLDER" ]; then
folders+=(
    "$COMPOSE_TEMPLATES_FOLDER/compose-multiplatform-desktop-template"
    "$COMPOSE_TEMPLATES_FOLDER/compose-multiplatform-html-library-template"
    "$COMPOSE_TEMPLATES_FOLDER/compose-multiplatform-ios-android-template"
    "$COMPOSE_TEMPLATES_FOLDER/compose-multiplatform-template"
)
fi

if [ -z "$1" ]; then
echo "Specify Compose version. For example: ./replaceVersion.sh 1.2.0-beta02 1.7.10"
exit 1
fi

if [ -z "$2" ]; then
echo "Specify Kotlin version. For example: ./replaceVersion.sh 1.2.0-beta02 1.7.10"
exit 1
fi

COMPOSE_VERSION=$1
KOTLIN_VERSION=$2

for folder in "${folders[@]}"
do
   replaceVersionInFolder $folder "**gradle.properties"
   replaceVersionInFolder $folder "**pom.xml"
   replaceVersionInFolder $folder "**README.md"
done
