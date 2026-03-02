#!/bin/bash

# Replace hard-coded Compose version in CMP published components. Usage: ./replace.sh 1.0.0-rc6

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/replace_version_common.sh"

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"/..

# Add folders which should contain up-to-date versions
declare -a folders=(
    "$ROOT/gradle-plugins"
    "$ROOT/html"
    "$ROOT/components"
)

if [ -z "$1" ]; then
echo "Specify Compose version. For example: ./replaceVersionInLibs.sh 1.2.0-beta02 1.7.10"
exit 1
fi

if [ -z "$2" ]; then
echo "Specify Kotlin version. For example: ./replaceVersionInLibs.sh 1.2.0-beta02 1.7.10"
exit 1
fi

COMPOSE_VERSION=$1
KOTLIN_VERSION=$2

if [[ $OSTYPE == 'darwin'* ]]; then
    SED=gsed
else
    SED=sed
fi



for folder in "${folders[@]}"
do
   replaceVersionInFolder $folder "**gradle.properties"
   replaceVersionInFolder $folder "**pom.xml"
   replaceVersionInFolder $folder "**README.md"
done
