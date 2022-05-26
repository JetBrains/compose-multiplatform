#!/bin/bash
set -euo pipefail
set -o xtrace

echo "First need to start android emulator or connect device"

cd "$(dirname "$0")"
cd ../frameworks/support

export OUT_DIR=`pwd`/out
export JAVA_TOOLS_JAR=../../external/tools.jar
export ALLOW_PUBLIC_REPOS=1
export ANDROIDX_PROJECTS=COMPOSE

../../gradlew -i --no-daemon \
  -Pandroidx.compose.multiplatformEnabled=false \
  -Pjetbrains.compose.jsCompilerTestsEnabled=false \
  -Pandroidx.validateProjectStructure=false \
  -Pkotlin.compiler.execution.strategy="in-process" \
  compose:ui:ui:connectedDebugAndroidTest

