#!/usr/bin/env bash
set -euo pipefail

# This script is for educational purposes only.
# Its purpose is to demonstrate how to compile & run Compose Desktop App without Gradle.
# Prefer running Gradle directly if your project uses Gradle.

mkdir -p deps

function mavenDep {
  REPO=$1
  GROUP=$2
  ARTIFACT=$3
  VERSION=$4
  FILE="$ARTIFACT-$VERSION.jar"
  if [ ! -f "deps/$FILE" ]; then
    wget -P deps/ "$REPO/$GROUP/$ARTIFACT/$VERSION/$FILE"
  fi
}

# Define which platform the code is running on. This is required to load the correct native code.
PLATFORM=macos-arm64
# The Compose compiler must be compatible with the selected version of Kotlin.
# See https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-compatibility-and-versioning.html#use-a-developer-version-of-compose-multiplatform-compiler
# for which version of the compiler to use.
COMPOSE_COMPILER_VERSION=1.5.10.1
# See https://github.com/JetBrains/compose-multiplatform/releases for available versions of Compose
COMPOSE_VERSION=1.6.1
# To know which Skiko version corresponds to the used Compose version, search after "skiko" in
# https://repo1.maven.org/maven2/org/jetbrains/compose/ui/ui-desktop/1.6.1/ui-desktop-1.6.1.pom
# (replace 1.6.1) with the version of compose used.
SKIKO_VERSION=0.7.97
KOTLIN_VERSION=1.9.23
COROUTINES_VERSION=1.8.0
ANDROIDX_COLLECTION_VERSION=1.4.0
GOOGLE_REPO="https://maven.google.com/"
MAVEN_CENTRAL="https://repo1.maven.org/maven2"

mavenDep "$MAVEN_CENTRAL" "org/jetbrains/skiko" "skiko-awt" "$SKIKO_VERSION"
mavenDep "$MAVEN_CENTRAL" "org/jetbrains/skiko" "skiko-awt-runtime-$PLATFORM" "$SKIKO_VERSION"
mavenDep "$MAVEN_CENTRAL" "org/jetbrains/compose/compiler" "compiler-hosted" "$COMPOSE_COMPILER_VERSION"
mavenDep "$MAVEN_CENTRAL" "org/jetbrains/compose" "compose-full" "$COMPOSE_VERSION"
mavenDep "$MAVEN_CENTRAL" "org/jetbrains/kotlinx" "kotlinx-coroutines-core-jvm" "$COROUTINES_VERSION"
mavenDep "$GOOGLE_REPO" "androidx/collection" "collection-jvm" "$ANDROIDX_COLLECTION_VERSION"

mavenDep "$MAVEN_CENTRAL" "org/jetbrains/kotlin" "kotlin-stdlib" "$KOTLIN_VERSION"
if [ ! -f "deps/kotlin-compiler-$KOTLIN_VERSION.zip" ]; then
  wget -P deps/ "https://github.com/JetBrains/kotlin/releases/download/v$KOTLIN_VERSION/kotlin-compiler-$KOTLIN_VERSION.zip"
  pushd deps
  unzip kotlin-compiler-$KOTLIN_VERSION.zip
  popd
fi

OUT_DIR=out/
if [ -d "$OUT_DIR" ]; then
  rm -rf $OUT_DIR
fi

COMPILE_CLASSPATH="deps/compose-full-$COMPOSE_VERSION.jar"
RUNTIME_CLASSPATH="deps/collection-jvm-$ANDROIDX_COLLECTION_VERSION.jar:deps/skiko-awt-$SKIKO_VERSION.jar:deps/skiko-awt-runtime-$PLATFORM-$SKIKO_VERSION.jar:deps/kotlin-stdlib-$KOTLIN_VERSION.jar:deps/kotlinx-coroutines-core-jvm-$COROUTINES_VERSION.jar:$COMPILE_CLASSPATH"
SOURCES=$(find src/main -name "*.kt"|paste -sd " " -)
JAVA_OPTS=-Xmx1G deps/kotlinc/bin/kotlinc-jvm \
-language-version 1.9 \
-Xmulti-platform \
-Xplugin="deps/compiler-hosted-$COMPOSE_COMPILER_VERSION.jar" \
-cp "$COMPILE_CLASSPATH" \
-d $OUT_DIR $SOURCES

java -cp "out/:result.jar:$RUNTIME_CLASSPATH" MainKt
