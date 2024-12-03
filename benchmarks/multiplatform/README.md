# Compose Multiplatform benchmarks

## Run Desktop
- `./gradlew :run`

## Run native on MacOS
 - `./gradlew runReleaseExecutableMacosArm64` (Works on Arm64 processors)
 - `./gradlew runReleaseExecutableMacosX64` (Works on Intel processors)

## Run in web browser:

Please run your browser with manual GC enabled before running the benchmark, like for Google Chrome:

`open -a Google\ Chrome --args --js-flags="--expose-gc"`

- `./gradlew wasmJsBrowserProductionRun` (you can see the results printed on the page itself)