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

PLATFORM=macos-x64
SKIKO_VERSION=0.1.18
# __KOTLIN_COMPOSE_VERSION__
KOTLIN_VERSION=1.4.20
# __LATEST_COMPOSE_RELEASE_VERSION__
COMPOSE_VERSION=0.2.0-build132
COROUTINES_VERSION=1.3.6
COLLECTIONS_VERSION=0.3
SPACE_REPO="https://public.jetbrains.space/p/compose/packages/maven/"
MAVEN_CENTRAL="https://repo1.maven.org/maven2"
BINTRAY_KOTLINX="https://dl.bintray.com/kotlin/kotlinx"

mavenDep "$SPACE_REPO" "org/jetbrains/skiko" "skiko-jvm-runtime-$PLATFORM" "$SKIKO_VERSION"
mavenDep "$SPACE_REPO" "org/jetbrains/compose" "compose-full" "$COMPOSE_VERSION"
mavenDep "$SPACE_REPO" "org/jetbrains/compose" "compose-compiler-hosted" "$COMPOSE_VERSION"
mavenDep "$BINTRAY_KOTLINX" "org/jetbrains/kotlinx" "kotlinx-collections-immutable-jvm" "$COLLECTIONS_VERSION"
mavenDep "$MAVEN_CENTRAL" "org/jetbrains/kotlinx" "kotlinx-coroutines-core" "$COROUTINES_VERSION"
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

COMPILE_CLASSPATH="deps/compose-full-$COMPOSE_VERSION.jar:deps/skiko-jvm-runtime-$PLATFORM-$SKIKO_VERSION.jar:deps/kotlinx-coroutines-core-$COROUTINES_VERSION.jar"
RUNTIME_CLASSPATH="deps/kotlinx-collections-immutable-jvm-$COLLECTIONS_VERSION.jar:deps/kotlin-stdlib-$KOTLIN_VERSION.jar:$COMPILE_CLASSPATH"
SOURCES=$(find src/main -name "*.kt"|paste -sd " " -)
JAVA_OPTS=-Xmx1G deps/kotlinc/bin/kotlinc-jvm \
-jvm-target 1.8 \
-Xuse-ir \
-Xmulti-platform \
-Xplugin="deps/compose-compiler-hosted-$COMPOSE_VERSION.jar" \
-cp "$COMPILE_CLASSPATH" \
-d $OUT_DIR $SOURCES

#jar cf result.jar -C "$OUT_DIR" . -C "src/main/resources" .
java -cp "out/:result.jar:$RUNTIME_CLASSPATH" MainKt
