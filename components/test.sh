#!/bin/bash
cd "$(dirname "$0")" # Run always in current dir
set -euo pipefail # Fail fast

# Unit tests
./gradlew :resources:library:test
./gradlew :resources:library:desktopTest

# Android integration tests
./gradlew :resources:library:pixel5DebugAndroidTest

