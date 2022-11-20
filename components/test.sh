#!/bin/bash
set -euo pipefail

#test Android
./gradlew :resources:library:pixel5DebugAndroidTest
./gradlew :resources:demo:android:pixel5DebugAndroidTest

./gradlew :resources:library:androidTest

./gradlew :resources:library:desktopTest