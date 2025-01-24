#!/bin/bash

# Replace hard-coded Compose version in Compose repo projects. Usage: ./replace.sh 1.0.0-rc6

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"/..

# Add folders which should contain up-to-date versions
declare -a folders=(
    "$ROOT/examples"
    "$ROOT/gradle-plugins"
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

if [[ $OSTYPE == 'darwin'* ]]; then
    SED=gsed
else
    SED=sed
fi

replaceVersion() {
    $SED -i -e "s/$1/$2/g" $3
}

replaceVersionInFile() {
    echo "Replace in $1"
    replaceVersion '^compose.version=.*' 'compose.version='"$COMPOSE_VERSION"'' $1
    replaceVersion '<compose.version>.*<\/compose.version>' '<compose.version>'"$COMPOSE_VERSION"'<\/compose.version>' $1
    replaceVersion '^COMPOSE_CORE_VERSION=.*' 'COMPOSE_CORE_VERSION='"$COMPOSE_VERSION"'' $1
    replaceVersion '^COMPOSE_WEB_VERSION=.*' 'COMPOSE_WEB_VERSION='"$COMPOSE_VERSION"'' $1
    replaceVersion 'id("org.jetbrains.compose") version ".*"' 'id("org.jetbrains.compose") version "'"$COMPOSE_VERSION"'"' $1
    replaceVersion '"org.jetbrains.compose:compose-gradle-plugin:.*"' '"org.jetbrains.compose:compose-gradle-plugin:'"$COMPOSE_VERSION"'"' $1
    replaceVersion '^kotlin.version=.*' 'kotlin.version='"$KOTLIN_VERSION"'' $1
    replaceVersion '<kotlin.version>.*<\/kotlin.version>' '<kotlin.version>'"$KOTLIN_VERSION"'<\/kotlin.version>' $1
    replaceVersion '^compose.tests.compiler.compatible.kotlin.version=.*' 'compose.tests.compiler.compatible.kotlin.version='"$KOTLIN_VERSION"'' $1
    replaceVersion '^compose.tests.js.compiler.compatible.kotlin.version=.*' 'compose.tests.js.compiler.compatible.kotlin.version='"$KOTLIN_VERSION"'' $1
    replaceVersion 'kotlin("multiplatform") version ".*"' 'kotlin("multiplatform") version "'"$KOTLIN_VERSION"'"' $1
    replaceVersion 'kotlin("jvm") version ".*"' 'kotlin("jvm") version "'"$KOTLIN_VERSION"'"' $1
}

replaceVersionInFolder() {
    find $1 -wholename $2 -not -path "**/build**" -not -path "**/.gradle**" | while read file; do replaceVersionInFile "$file"; done
}

for folder in "${folders[@]}"
do
   replaceVersionInFolder $folder "**gradle.properties"
   replaceVersionInFolder $folder "**pom.xml"
   replaceVersionInFolder $folder "**README.md"
done
