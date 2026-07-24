# This file contains common functions for replaceVersion.sh and replaceVersionInLibs.sh scripts

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
    replaceVersion '^compose.tests.kotlin.version=.*' 'compose.tests.kotlin.version='"$KOTLIN_VERSION"'' $1
    replaceVersion '^compose.tests.compiler.compatible.kotlin.version=.*' 'compose.tests.compiler.compatible.kotlin.version='"$KOTLIN_VERSION"'' $1
    replaceVersion '^compose.tests.js.compiler.compatible.kotlin.version=.*' 'compose.tests.js.compiler.compatible.kotlin.version='"$KOTLIN_VERSION"'' $1
    replaceVersion 'kotlin("multiplatform") version ".*"' 'kotlin("multiplatform") version "'"$KOTLIN_VERSION"'"' $1
    replaceVersion 'kotlin("jvm") version ".*"' 'kotlin("jvm") version "'"$KOTLIN_VERSION"'"' $1
}

replaceVersionInFolder() {
    find $1 -wholename $2 -not -path "**/build/*" -not -path "**/.gradle**" | while read file; do replaceVersionInFile "$file"; done
}