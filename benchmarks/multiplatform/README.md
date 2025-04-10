# Compose Multiplatform benchmarks

## Run Desktop
- `./gradlew :benchmarks:run`

## Run native on iOS
Open the project in Fleet or Android Studio with KMM plugin installed and 
choose `iosApp` run configuration. Make sure that you build the app in `Release` configuration.
Alternatively you may open `iosApp/iosApp` project in XCode and run the app from there.

## Run native on MacOS
 - `./gradlew :benchmarks:runReleaseExecutableMacosArm64` (Works on Arm64 processors)
 - `./gradlew :benchmarks:runReleaseExecutableMacosX64` (Works on Intel processors)

## Run K/Wasm target in D8:
`./gradlew :benchmarks:wasmJsD8ProductionRun`

or with arguments:

`./gradlew :benchmarks:wasmJsD8ProductionRun -PrunArguments=benchmarks=AnimatedVisibility`

## To build and run a K/Wasm D8 distribution for Jetstream3-like:
`./gradlew :benchmarks:buildD8Distribution --rerun-tasks`

then in a distribution directory run using your D8 binary:

`~/.gradle/d8/v8-mac-arm64-rel-11.9.85/d8 --module launcher_jetstream3.mjs -- AnimatedVisibility 1000`

## Run in web browser:

Please run your browser with manual GC enabled before running the benchmark, like for Google Chrome:

`open -a Google\ Chrome --args --js-flags="--expose-gc"`

- `./gradlew clean :benchmarks:wasmJsBrowserProductionRun` (you can see the results printed on the page itself)