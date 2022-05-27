#!/bin/bash

cd "$(dirname "$0")"
cd ../frameworks/support

export JAVA_TOOLS_JAR="$PWD/../../external/tools.jar"
export ALLOW_PUBLIC_REPOS=1
export ANDROIDX_PROJECTS=COMPOSE

../../gradlew --no-daemon \
  -Pandroidx.compose.multiplatformEnabled=false \
  -Pjetbrains.compose.jsCompilerTestsEnabled=false \
  -Pandroidx.validateProjectStructure=false \
  -Pkotlin.compiler.execution.strategy="in-process" \
  :compose:animation:animation-core:animation-core-benchmark:testDebugUnitTest \
  :compose:animation:animation-core:animation-core-samples:testDebugUnitTest \
  :compose:animation:animation-core:testDebugUnitTest \
  :compose:animation:animation-graphics:animation-graphics-samples:testDebugUnitTest \
  :compose:animation:animation-graphics:testDebugUnitTest \
  :compose:animation:animation:animation-samples:testDebugUnitTest \
  :compose:animation:animation:integration-tests:animation-demos:testDebugUnitTest \
  :compose:animation:animation:testDebugUnitTest \
  :compose:benchmark-utils:benchmark-utils-benchmark:testDebugUnitTest \
  :compose:benchmark-utils:testDebugUnitTest \
  :compose:foundation:foundation-benchmark:testDebugUnitTest \
  :compose:foundation:foundation-layout:foundation-layout-benchmark:testDebugUnitTest \
  :compose:foundation:foundation-layout:foundation-layout-samples:testDebugUnitTest \
  :compose:foundation:foundation-layout:integration-tests:foundation-layout-demos:testDebugUnitTest \
  :compose:foundation:foundation-layout:testDebugUnitTest \
  :compose:foundation:foundation:foundation-samples:testDebugUnitTest \
  :compose:foundation:foundation:integration-tests:foundation-demos:testDebugUnitTest \
  :compose:foundation:foundation:testDebugUnitTest \
  :compose:integration-tests:demos:common:testDebugUnitTest \
  :compose:integration-tests:demos:testDebugUnitTest \
  :compose:integration-tests:docs-snippets:testDebugUnitTest \
  :compose:integration-tests:macrobenchmark-target:testDebugUnitTest \
  :compose:integration-tests:macrobenchmark:testDebugUnitTest \
  :compose:integration-tests:material-catalog:testDebugUnitTest \
  :compose:material3:material3-window-size-class:material3-window-size-class-samples:testDebugUnitTest \
  :compose:material3:material3-window-size-class:testDebugUnitTest \
  :compose:material3:material3:integration-tests:material3-catalog:testDebugUnitTest \
  :compose:material3:material3:integration-tests:material3-demos:testDebugUnitTest \
  :compose:material3:material3:material3-samples:testDebugUnitTest \
  :compose:material3:material3:testDebugUnitTest \
  :compose:material:material-benchmark:testDebugUnitTest \
  :compose:material:material-icons-core:material-icons-core-samples:testDebugUnitTest \
  :compose:material:material-icons-core:testDebugUnitTest \
  :compose:material:material-icons-extended-filled:testDebugUnitTest \
  :compose:material:material-icons-extended-outlined:testDebugUnitTest \
  :compose:material:material-icons-extended-rounded:testDebugUnitTest \
  :compose:material:material-icons-extended-sharp:testDebugUnitTest \
  :compose:material:material-icons-extended-twotone:testDebugUnitTest \
  :compose:material:material-icons-extended:testDebugUnitTest \
  :compose:material:material-ripple:testDebugUnitTest \
  :compose:material:material:integration-tests:material-catalog:testDebugUnitTest \
  :compose:material:material:integration-tests:material-demos:testDebugUnitTest \
  :compose:material:material:material-samples:testDebugUnitTest \
  :compose:material:material:testDebugUnitTest \
  :compose:runtime:runtime-livedata:runtime-livedata-samples:testDebugUnitTest \
  :compose:runtime:runtime-livedata:testDebugUnitTest \
  :compose:runtime:runtime-rxjava2:runtime-rxjava2-samples:testDebugUnitTest \
  :compose:runtime:runtime-rxjava2:testDebugUnitTest \
  :compose:runtime:runtime-rxjava3:runtime-rxjava3-samples:testDebugUnitTest \
  :compose:runtime:runtime-rxjava3:testDebugUnitTest \
  :compose:runtime:runtime-saveable:runtime-saveable-samples:testDebugUnitTest \
  :compose:runtime:runtime-saveable:testDebugUnitTest \
  :compose:runtime:runtime:benchmark:testDebugUnitTest \
  :compose:runtime:runtime:integration-tests:testDebugUnitTest \
  :compose:runtime:runtime:runtime-samples:testDebugUnitTest \
  :compose:runtime:runtime:testDebugUnitTest \
  :compose:test-utils:testDebugUnitTest \
  :compose:ui:ui-android-stubs:testDebugUnitTest \
  :compose:ui:ui-benchmark:testDebugUnitTest \
  :compose:ui:ui-geometry:testDebugUnitTest \
  :compose:ui:ui-graphics:testDebugUnitTest \
  :compose:ui:ui-graphics:ui-graphics-benchmark:test:testDebugUnitTest \
  :compose:ui:ui-graphics:ui-graphics-benchmark:testDebugUnitTest \
  :compose:ui:ui-graphics:ui-graphics-samples:testDebugUnitTest \
  :compose:ui:ui-inspection:testDebugUnitTest \
  :compose:ui:ui-test-font:testDebugUnitTest \
  :compose:ui:ui-test-manifest:integration-tests:testapp:testDebugUnitTest \
  :compose:ui:ui-test-manifest:testDebugUnitTest \
  :compose:ui:ui-test:ui-test-samples:testDebugUnitTest \
  :compose:ui:ui-text-google-fonts:testDebugUnitTest \
  :compose:ui:ui-text:testDebugUnitTest \
  :compose:ui:ui-text:ui-text-benchmark:testDebugUnitTest \
  :compose:ui:ui-text:ui-text-samples:testDebugUnitTest \
  :compose:ui:ui-tooling-data:testDebugUnitTest \
  :compose:ui:ui-tooling-preview:testDebugUnitTest \
  :compose:ui:ui-tooling:testDebugUnitTest \
  :compose:ui:ui-unit:testDebugUnitTest \
  :compose:ui:ui-unit:ui-unit-samples:testDebugUnitTest \
  :compose:ui:ui-util:testDebugUnitTest \
  :compose:ui:ui-viewbinding:testDebugUnitTest \
  :compose:ui:ui-viewbinding:ui-viewbinding-samples:testDebugUnitTest \
  :compose:ui:ui:integration-tests:ui-demos:testDebugUnitTest \
  :compose:ui:ui:ui-samples:testDebugUnitTest

#  :compose:compiler:compiler-hosted:integration-tests:testDebugUnitTest \ TODO Compile with errors
#  :compose:ui:ui:testDebugUnitTest \ TODO missing .robolectric
#  :compose:ui:ui-test-junit4:testDebugUnitTest \ TODO missing robolectric
#  :compose:ui:ui-test:testDebugUnitTest \ TODO missing robolectric
