#!/bin/bash

# Replace hard-coded Compose version in Compose repo projects. Usage: ./replace.sh 1.0.0-rc6

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"/..

# Add folders which should contain up-to-date versions
declare -a folders=(
    "templates"
    "examples"
    "gradle-plugins"
    "components"
    "ci"
    "web"
    "tutorials"
)

if [ -z "$@" ]; then
echo "Specify Compose version. For example: ./replace.sh 1.0.0-rc6"
exit 1
fi
COMPOSE_VERSION=$@

if [[ $OSTYPE == 'darwin'* ]]; then
    SED=gsed
else
    SED=sed
fi

replaceCompose() {
    $SED -i -e "s/$1/$2/g" $3
}

replaceComposeInFile() {
    echo "Replace in $1"
    replaceCompose '^compose.version=.*' 'compose.version='"$COMPOSE_VERSION"'' $1
    replaceCompose '^COMPOSE_CORE_VERSION=.*' 'COMPOSE_CORE_VERSION='"$COMPOSE_VERSION"'' $1
    replaceCompose '^COMPOSE_WEB_VERSION=.*' 'COMPOSE_WEB_VERSION='"$COMPOSE_VERSION"'' $1
    replaceCompose 'id("org.jetbrains.compose") version ".*"' 'id("org.jetbrains.compose") version "'"$COMPOSE_VERSION"'"' $1
    replaceCompose '"org.jetbrains.compose:compose-gradle-plugin:.*"' '"org.jetbrains.compose:compose-gradle-plugin:'"$COMPOSE_VERSION"'"' $1
}

replaceComposeInFolder() {
    find $ROOT/$1 -wholename $2 -not -path "**/build**" -not -path "**/.gradle**" | while read file; do replaceComposeInFile "$file"; done
}

for folder in "${folders[@]}"
do
   replaceComposeInFolder $folder "**gradle.properties"
   replaceComposeInFolder $folder "**README.md"
done
