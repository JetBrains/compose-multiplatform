#!/bin/bash
cd "$(dirname "$0")" # Run always in current dir
set -euo pipefail # Fail fast

./gradlew :resources:library:desktopTest
./gradlew :resources:library:testAndroidHostTest
./gradlew :resources:library:iosSimulatorArm64Test
./gradlew :resources:library:wasmJsBrowserTest
./gradlew :resources:library:jsBrowserTest
