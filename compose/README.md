![Project in Android Studio](screenshots/studio.png)

Composite build of [Compose-jb sources](https://github.com/JetBrains/androidx)

## Download submodules after downloading the main project:
```bash
git submodule update --init --recursive
```
Set this property to always update submodules on git checkout/pull/reset:
```bash
git config --global submodule.recurse true
```

## General requirements
- Java 11 (should be specified in JAVA_HOME)
- [macOs/Linux] Android SDK downloaded via `./scripts/downloadAndroidSdk`
- [Windows] Android SDK downloaded from Android Studio and specified in ANDROID_SDK_ROOT. Required components:
  - Android 12
  - Build-Tools 30.0.3
  - NDK 21.3.6528147 (in folder $androidSdk/ndk, not in $androidSdk/ndk/$version)
  - CMake 3.10.2.4988404 (in folder $androidSdk/cmake, not in $androidSdk/cmake/$version)

## Developing in IDE
1. Download Android Studio from [the official site](https://developer.android.com/studio/archive) (it is mandatory to use the version, written [here](https://github.com/JetBrains/androidx/blob/jb-main/gradle/libs.versions.toml#L11)). As an alternative you can use IDEA, which is compatible with [this AGP version](https://github.com/JetBrains/androidx/blob/jb-main/gradle/libs.versions.toml#L5), or you can disable Android plugin in IDEA plugins, to develop non-Android targets.
2. Set environment variables in the global script (`~/.zshrc` for macOs), or in the terminal, before starting IDE also from this terminal:
```
export ALLOW_PUBLIC_REPOS=1
export JAVA_TOOLS_JAR=$PWD/external/tools.jar
export ANDROIDX_PROJECTS=COMPOSE
export JAVA_HOME=<JDK_home> // it should point to Java 11 and contain /include/jvmti.h
```
3. Set gradle properties in ~/.gradle/gradle.properties:
```
androidx.compose.multiplatformEnabled=true
# note that https://android.googlesource.com/platform/frameworks/support build doesn't work with jetbrains.compose.jsCompilerTestsEnabled)
jetbrains.compose.jsCompilerTestsEnabled=true
androidx.validateProjectStructure=false
```
4. Open `compose` folder in Android Studio (not `compose/frameworks/support`)
5. Download a custom Gradle 7.2 and specify it in `Settings -> Build, Execution, Deployment -> Build Tools -> Gradle` (because Android Studio will pick the wrong Gradle in the subproject instead of the Gradle in the root project)
6. Specify Gradle JDK 11 in `... -> Build Tools -> Gradle`
7. [macOs/Linux] Specify Android SDK pointed to a folder, downloaded via `./scripts/downloadAndroidSdk`

## Scripts
Publish artifacts to the local directory `out/androidx/build/support_repo/org/jetbrains/compose`:
```
export COMPOSE_CUSTOM_VERSION=0.0.0-custom
./scripts/publish
```
(on Windows it doesn't build at the moment, the issue is in the line `packageInspector(project, project(":compose:ui:ui-inspection")` in `compose/frameworks/support/compose/ui/ui/build.gradle`)

Publish extended icons:
```bash
./scripts/publishExtendedIcons
```

### Run tests

Run tests for Desktop:
```bash
./scripts/testDesktop
```

Run tests for Web:
```bash
./scripts/testWeb
```

Run tests for Android:
```bash
./scripts/testAndroidUnit.sh
```
```bash
./scripts/testAndroidInEmulator.sh # first run android simulator or connect device
```

### Run samples
Run jvm desktop sample:
```bash
./scripts/runGradle run
```
another jvm desktop samples: 
```bash
./scripts/runGradle run1 run2 run3 run4 runWindowApi runVsync
```

Run wasm sample:
```bash
./scripts/runGradle runMppJs
```

Run native macos sample:
```bash
./scripts/runGradle runMppMacos
```

## Multiplatform build

```console
./scripts/downloadAndroidSdk
export COMPOSE_CUSTOM_VERSION=1.1.0-beta04
./scripts/publishToMavenLocal -Pcompose.platforms=all
./scripts/publishGradlePluginToMavenLocal
./scripts/publishWebComponentsToMavenLocal
```
`-Pcompose.platforms=all` could be replace with comma-separated list of platforms, such as `js,jvm,androidDebug,androidRelease,macosx64,uikitx64`.

