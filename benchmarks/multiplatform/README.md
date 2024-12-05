# Compose Multiplatform benchmarks

## Run Desktop
- `./gradlew :benchmarks:run`

## Run native on MacOS
 - `./gradlew :benchmarks:runReleaseExecutableMacosArm64` (Works on Arm64 processors)
 - `./gradlew :benchmarks:runReleaseExecutableMacosX64` (Works on Intel processors)

## Run in web browser:

Please run your browser with manual GC enabled before running the benchmark, like for Google Chrome:

`open -a Google\ Chrome --args --js-flags="--expose-gc"`

- `./gradlew :benchmarks:wasmJsBrowserProductionRun` (you can see the results printed on the page itself)