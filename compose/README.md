![Project in Android Studio](screenshots/studio.png)

Composite build of [Compose-jb sources](https://github.com/JetBrains/androidx)

## Download submodules after downloading the main project:
```
git submodule update --init
```
Set this property to always update submodules on git checkout/pull/reset:
```
git config --global submodule.recurse true
```

## General requirements
- Java 11 (should be specified in JAVA_HOME)
- Android SDK (should be specified in ANDROID_SDK_ROOT. Can be downloaded via `./scripts/downloadAndroidSdk` on Linux/macOS)
  - Android 11
  - Build-Tools 30.0.3
  - NDK 21.3.6528147 (in folder $androidSdk/ndk, not in $androidSdk/ndk/$version)
  - CMake 3.10.2.4988404 (in folder $androidSdk/cmake, not in $androidSdk/cmake/$version)

## Requirements to develop in IDE
- Android Studio Arctic Fox | 2020.3.1 Canary 15
- Custom Gradle 7.1 specified in `Settings -> Build, Execution, Deployment -> Build Tools -> Gradle` (because Android Studio will pick the wrong Gradle in the subproject instead of the Gradle in the root project)
- Specified Gradle JDK 11 in `... -> Build Tools -> Gradle`
- Environment variables:
```
export ALLOW_PUBLIC_REPOS=1
export JAVA_TOOLS_JAR=$PWD/external/tools.jar
export ANDROIDX_PROJECTS=COMPOSE
export COMPOSE_CUSTOM_GROUP=org.jetbrains.compose
```
- Gradle properties in ~/.gradle/gradle.properties:
```
androidx.compose.multiplatformEnabled=true
androidx.compose.jsCompilerTestsEnabled=true
```

## Scripts
Publish artifacts to the local directory `out/androidx/build/support_repo/org/jetbrains/compose`:
```
./scripts/publish
```
Publish artifacts without Android Layout Inspector (use it on Windows):
```
./scripts/publishWithoutInspector
```
Publish extended icons:
```
./scripts/publishExtendedIcons
```

Run tests for Desktop:
```
./scripts/testDesktop
```

Run tests for Web:
```
./scripts/testWeb
```