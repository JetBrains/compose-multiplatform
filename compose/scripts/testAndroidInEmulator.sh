#!/bin/bash

echo "First need to start android emulator or connect device"

cd "$(dirname "$0")"
cd ../frameworks/support

export JAVA_TOOLS_JAR="$PWD/../../external/tools.jar"
export ALLOW_PUBLIC_REPOS=1
export ANDROIDX_PROJECTS=COMPOSE

select CURRENT_TASK in \
  ":compose:animation:animation-core:animation-core-samples:connectedDebugAndroidTest" \
  ":compose:animation:animation-core:connectedDebugAndroidTest" \
  ":compose:animation:animation-graphics:animation-graphics-samples:connectedDebugAndroidTest" \
  ":compose:animation:animation-graphics:connectedDebugAndroidTest" \
  ":compose:animation:animation:animation-samples:connectedDebugAndroidTest" \
  ":compose:animation:animation:connectedDebugAndroidTest" \
  ":compose:animation:animation:integration-tests:animation-demos:connectedDebugAndroidTest" \
  ":compose:benchmark-utils:connectedDebugAndroidTest" \
  ":compose:compiler:compiler-hosted:integration-tests:connectedDebugAndroidTest" \
  ":compose:foundation:foundation-layout:connectedDebugAndroidTest" \
  ":compose:foundation:foundation-layout:foundation-layout-samples:connectedDebugAndroidTest" \
  ":compose:foundation:foundation-layout:integration-tests:foundation-layout-demos:connectedDebugAndroidTest" \
  ":compose:foundation:foundation:connectedDebugAndroidTest" \
  ":compose:foundation:foundation:foundation-samples:connectedDebugAndroidTest" \
  ":compose:foundation:foundation:integration-tests:foundation-demos:connectedDebugAndroidTest" \
  ":compose:integration-tests:demos:common:connectedDebugAndroidTest" \
  ":compose:integration-tests:demos:connectedDebugAndroidTest" \
  ":compose:integration-tests:docs-snippets:connectedDebugAndroidTest" \
  ":compose:integration-tests:macrobenchmark-target:connectedDebugAndroidTest" \
  ":compose:integration-tests:macrobenchmark:connectedDebugAndroidTest" \
  ":compose:integration-tests:material-catalog:connectedDebugAndroidTest" \
  ":compose:material3:material3-window-size-class:connectedDebugAndroidTest" \
  ":compose:material3:material3-window-size-class:material3-window-size-class-samples:connectedDebugAndroidTest" \
  ":compose:material3:material3:connectedDebugAndroidTest" \
  ":compose:material3:material3:integration-tests:material3-catalog:connectedDebugAndroidTest" \
  ":compose:material3:material3:integration-tests:material3-demos:connectedDebugAndroidTest" \
  ":compose:material3:material3:material3-samples:connectedDebugAndroidTest" \
  ":compose:material:material-icons-core:connectedDebugAndroidTest" \
  ":compose:material:material-icons-core:material-icons-core-samples:connectedDebugAndroidTest" \
  ":compose:material:material-icons-extended-filled:connectedDebugAndroidTest" \
  ":compose:material:material-icons-extended-outlined:connectedDebugAndroidTest" \
  ":compose:material:material-icons-extended-rounded:connectedDebugAndroidTest" \
  ":compose:material:material-icons-extended-sharp:connectedDebugAndroidTest" \
  ":compose:material:material-icons-extended-twotone:connectedDebugAndroidTest" \
  ":compose:material:material-icons-extended:connectedDebugAndroidTest" \
  ":compose:material:material-ripple:connectedDebugAndroidTest" \
  ":compose:material:material:connectedDebugAndroidTest" \
  ":compose:material:material:integration-tests:material-catalog:connectedDebugAndroidTest" \
  ":compose:material:material:integration-tests:material-demos:connectedDebugAndroidTest" \
  ":compose:material:material:material-samples:connectedDebugAndroidTest" \
  ":compose:runtime:runtime-livedata:connectedDebugAndroidTest" \
  ":compose:runtime:runtime-livedata:runtime-livedata-samples:connectedDebugAndroidTest" \
  ":compose:runtime:runtime-rxjava2:connectedDebugAndroidTest" \
  ":compose:runtime:runtime-rxjava2:runtime-rxjava2-samples:connectedDebugAndroidTest" \
  ":compose:runtime:runtime-rxjava3:connectedDebugAndroidTest" \
  ":compose:runtime:runtime-rxjava3:runtime-rxjava3-samples:connectedDebugAndroidTest" \
  ":compose:runtime:runtime-saveable:connectedDebugAndroidTest" \
  ":compose:runtime:runtime-saveable:runtime-saveable-samples:connectedDebugAndroidTest" \
  ":compose:runtime:runtime:connectedDebugAndroidTest" \
  ":compose:runtime:runtime:integration-tests:connectedDebugAndroidTest" \
  ":compose:runtime:runtime:runtime-samples:connectedDebugAndroidTest" \
  ":compose:test-utils:connectedDebugAndroidTest" \
  ":compose:ui:ui-android-stubs:connectedDebugAndroidTest" \
  ":compose:ui:ui-geometry:connectedDebugAndroidTest" \
  ":compose:ui:ui-graphics:connectedDebugAndroidTest" \
  ":compose:ui:ui-graphics:ui-graphics-benchmark:test:connectedDebugAndroidTest" \
  ":compose:ui:ui-graphics:ui-graphics-samples:connectedDebugAndroidTest" \
  ":compose:ui:ui-inspection:connectedDebugAndroidTest" \
  ":compose:ui:ui-test-font:connectedDebugAndroidTest" \
  ":compose:ui:ui-test-junit4:connectedDebugAndroidTest" \
  ":compose:ui:ui-test-manifest:connectedDebugAndroidTest" \
  ":compose:ui:ui-test-manifest:integration-tests:testapp:connectedDebugAndroidTest" \
  ":compose:ui:ui-test:connectedDebugAndroidTest" \
  ":compose:ui:ui-test:ui-test-samples:connectedDebugAndroidTest" \
  ":compose:ui:ui-text-google-fonts:connectedDebugAndroidTest" \
  ":compose:ui:ui-text:connectedDebugAndroidTest" \
  ":compose:ui:ui-text:ui-text-samples:connectedDebugAndroidTest" \
  ":compose:ui:ui-tooling-data:connectedDebugAndroidTest" \
  ":compose:ui:ui-tooling-preview:connectedDebugAndroidTest" \
  ":compose:ui:ui-tooling:connectedDebugAndroidTest" \
  ":compose:ui:ui-unit:connectedDebugAndroidTest" \
  ":compose:ui:ui-unit:ui-unit-samples:connectedDebugAndroidTest" \
  ":compose:ui:ui-util:connectedDebugAndroidTest" \
  ":compose:ui:ui-viewbinding:connectedDebugAndroidTest" \
  ":compose:ui:ui-viewbinding:ui-viewbinding-samples:connectedDebugAndroidTest" \
  ":compose:ui:ui:connectedDebugAndroidTest" \
  ":compose:ui:ui:integration-tests:ui-demos:connectedDebugAndroidTest" \
  ":compose:ui:ui:ui-samples:connectedDebugAndroidTest"
do
  ../../gradlew -i --no-daemon \
    -Pandroidx.compose.multiplatformEnabled=false \
    -Pjetbrains.compose.jsCompilerTestsEnabled=false \
    -Pandroidx.validateProjectStructure=false \
    -Pkotlin.compiler.execution.strategy="in-process" \
    $CURRENT_TASK
  break
done
