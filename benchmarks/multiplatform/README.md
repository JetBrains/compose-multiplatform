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


# Benchmarks description

| Benchmark Name                           | File Path                                                                                                                                                            | Description                                                                                                                                    |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| AnimatedVisibility                       | [benchmarks/src/commonMain/kotlin/benchmarks/animation/AnimatedVisibility.kt](benchmarks/src/commonMain/kotlin/benchmarks/animation/AnimatedVisibility.kt)           | Tests the performance of the AnimatedVisibility component by repeatedly toggling the visibility of a PNG image.                                |
| LazyGrid                                 | [benchmarks/src/commonMain/kotlin/benchmarks/lazygrid/LazyGrid.kt](benchmarks/src/commonMain/kotlin/benchmarks/lazygrid/LazyGrid.kt)                                 | Tests the performance of the LazyVerticalGrid component with 12,000 items and jumps to specific items multiple times while running.            |
| LazyGrid-ItemLaunchedEffect              | [benchmarks/src/commonMain/kotlin/benchmarks/lazygrid/LazyGrid.kt](benchmarks/src/commonMain/kotlin/benchmarks/lazygrid/LazyGrid.kt)                                 | Same as LazyGrid but adds a LaunchedEffect in each grid item that simulates an async task.                                                     |
| LazyGrid-SmoothScroll                    | [benchmarks/src/commonMain/kotlin/benchmarks/lazygrid/LazyGrid.kt](benchmarks/src/commonMain/kotlin/benchmarks/lazygrid/LazyGrid.kt)                                 | Same as LazyGrid but uses smooth scrolling instead of jumping to items.                                                                        |
| LazyGrid-SmoothScroll-ItemLaunchedEffect | [benchmarks/src/commonMain/kotlin/benchmarks/lazygrid/LazyGrid.kt](benchmarks/src/commonMain/kotlin/benchmarks/lazygrid/LazyGrid.kt)                                 | Combines smooth scrolling with LaunchedEffect in each item.                                                                                    |
| VisualEffects                            | [benchmarks/src/commonMain/kotlin/benchmarks/visualeffects/HappyNY.kt](benchmarks/src/commonMain/kotlin/benchmarks/visualeffects/HappyNY.kt)                         | Tests the performance of complex animations and visual effects including snow flakes, stars, and rocket particles.                             |
| LazyList                                 | [benchmarks/src/commonMain/kotlin/benchmarks/complexlazylist/components/MainUI.kt](benchmarks/src/commonMain/kotlin/benchmarks/complexlazylist/components/MainUI.kt) | Tests the performance of a complex LazyColumn implementation with features like pull-to-refresh, loading more items, and continuous scrolling. |
| MultipleComponents                       | [benchmarks/src/commonMain/kotlin/benchmarks/example1/Example1.kt](benchmarks/src/commonMain/kotlin/benchmarks/multipleComponents/MultipleComponents.kt)             | Tests the performance of a comprehensive UI that showcases various Compose components including layouts, animations, and styled text.          |
| MultipleComponents-NoVectorGraphics      | [benchmarks/src/commonMain/kotlin/benchmarks/example1/Example1.kt](benchmarks/src/commonMain/kotlin/benchmarks/multipleComponents/MultipleComponents.kt)             | Same as MultipleComponents but skips the Composables with vector graphics rendering.                                                           |
