# Compose benchmarks for JVM vs Kotlin Native comparison

## Run Desktop
- `./gradlew :run`

## Run native on MacOS
 - `./gradlew runReleaseExecutableMacosArm64` (Works on Arm64 processors)
 - `./gradlew runReleaseExecutableMacosX64` (Works on Intel processors)

## Run in web browser:
- `./gradlew wasmJsBrowserProductionRun` (you can see the results printed on the page itself)