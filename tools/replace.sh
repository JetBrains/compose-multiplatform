#!/bin/bash

# Replace hard-coded Compose version in Compose repo projects. Usage: ./replace.sh 1.0.0-rc6

ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"/..

declare -a files=(
  "templates/desktop-template/build.gradle.kts"
  "templates/web-template/build.gradle.kts"
  "templates/multiplatform-template/build.gradle.kts"
  "examples/codeviewer/build.gradle.kts"
  "examples/falling-balls/build.gradle.kts"
  "examples/falling-balls-web/build.gradle.kts"
  "examples/imageviewer/build.gradle.kts"
  "examples/intellij-plugin/build.gradle.kts"
  "examples/issues/build.gradle.kts"
  "examples/notepad/build.gradle.kts"
  "examples/todoapp/buildSrc/buildSrc/src/main/kotlin/Deps.kt"
  "examples/todoapp-lite/build.gradle.kts"
  "examples/widgets-gallery/build.gradle.kts"
  "templates/desktop-template/build.gradle.kts"
  "gradle-plugins/gradle.properties"
  "components/gradle.properties"
  "ci/compose-uber-jar/gradle.properties"
  "web/gradle.properties"
  "tutorials/Getting_Started/README.md"
  "tutorials/Web/Getting_Started/README.md"
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

replace() {
    for file in "${files[@]}"
    do
       $SED -i -e "s/$1/$2/g" "$ROOT/$file"
    done
}

replace 'id("org.jetbrains.compose") version ".*"' 'id("org.jetbrains.compose") version "'"$COMPOSE_VERSION"'"'
replace 'compose.version=.*' 'compose.version='"$COMPOSE_VERSION"''
replace 'COMPOSE_CORE_VERSION=.*' 'COMPOSE_CORE_VERSION='"$COMPOSE_VERSION"''
replace 'COMPOSE_WEB_VERSION=.*' 'COMPOSE_WEB_VERSION='"$COMPOSE_VERSION"''
replace '"org.jetbrains.compose:compose-gradle-plugin:.*"' '"org.jetbrains.compose:compose-gradle-plugin:'"$COMPOSE_VERSION"'"'
