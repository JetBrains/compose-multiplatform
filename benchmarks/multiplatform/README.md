# Compose Multiplatform benchmarks

This project contains performance benchmarks for Compose Multiplatform on various targets, 
including Desktop, iOS, MacOS, and Web (Kotlin/Wasm and Kotlin/JS). 
These benchmarks measure the performance of various Compose components and features, 
such as animations, lazy layouts, text rendering, and visual effects.

## Benchmark Modes

The benchmarks can be run in different modes, which determine how performance is measured and reported:

- **`SIMPLE`**: Measures basic frame times without considering VSync. Good for quick checks of raw rendering performance. Enabled by default if no modes are specified.
- **`VSYNC_EMULATION`**: Emulates VSync behavior to estimate missed frames and provide more realistic performance metrics (CPU/GPU percentiles). Enabled by default if no modes are specified.
- **`REAL`**: Runs the benchmark in a real-world scenario with actual VSync. This mode provides the most accurate results for user-perceived performance (FPS, actual missed frames)
              but may not catch performance regressions if a frame fits to budget. 
              Also requires a device with a real display (may have problems with headless devices). 

To enable specific modes, use the `modes` argument:
`modes=SIMPLE,VSYNC_EMULATION,REAL`

## Configuration Arguments

You can configure benchmark runs using arguments passed to the Gradle task (via `-PrunArguments="..."`) or to the `.main.kts` script. Arguments can also be set in `gradle.properties` using the `runArguments` property.

| Argument | Description                                                | Example |
|----------|------------------------------------------------------------|---------|
| `modes` | Comma-separated list of execution modes (`SIMPLE`, `VSYNC_EMULATION`, `REAL`). | `modes=REAL` |
| `benchmarks` | Comma-separated list of benchmarks to run. Can optionally specify problem size in parentheses. | `benchmarks=LazyGrid(100),AnimatedVisibility` |
| `disabledBenchmarks` | Comma-separated list of benchmarks to skip.                | `disabledBenchmarks=HeavyShader` |
| `warmupCount` | Number of warmup frames before starting measurements.      | `warmupCount=50` |
| `frameCount` | Number of frames to measure for each benchmark.            | `frameCount=500` |
| `emptyScreenDelay` | Delay in milliseconds between warmup and measurement (real mode only).| `emptyScreenDelay=1000` |
| `parallel` | (iOS only) Enable parallel rendering.                      | `parallel=true` |
| `saveStatsToCSV` | Save results to CSV files.                                 | `saveStatsToCSV=true` |
| `saveStatsToJSON` | Save results to JSON files.                                | `saveStatsToJSON=true` |
| `versionInfo` | Add version information to the report.                     | `versionInfo=1.2.3` |
| `reportAtTheEnd` | Print a summary report after all benchmarks are finished real mode only).| `reportAtTheEnd=true` |
| `listBenchmarks` | List all available benchmarks and exit.                    | `listBenchmarks=true` |

### Usage Example

```bash
./gradlew :benchmarks:run -PrunArguments="benchmarks=LazyGrid modes=REAL frameCount=200"
```

## Run Desktop
- `./gradlew :benchmarks:run`

## Run native on iOS
Open the project in Fleet or Android Studio with KMM plugin installed and
choose `iosApp` run configuration. Make sure that you build the app in `Release` configuration.
Alternatively you may open `iosApp/iosApp` project in XCode and run the app from there.

## Run iOS benchmarks via scripts
1. To run on device, open `iosApp/iosApp.xcodeproj` and properly configure the Signing section on the Signing & Capabilities project tab.
2. Use the following command to get list of all iOS devices:
- `xcrun xctrace list devices`
3. From the benchmarks directory run:
- `./run_ios_benchmarks.main.kts <DEVICE ID>` (supports all modes of running benchmarks, configured the same way as for other targets:  
                                               script arguments or `runArguments` property of `gradle.properties`)
- or `./iosApp/run_ios_benchmarks.sh <DEVICE ID>` (shell script supporting `real` mode benchmarks running with multiple attempts)

To run specific benchmarks:
- `./run_ios_benchmarks.main.kts <DEVICE ID> benchmarks=AnimatedVisibility,LazyGrid`

To run all benchmarks in a single process (faster, but may be less stable):
- `./run_ios_benchmarks.main.kts <DEVICE ID> separateProcess=false`

4. Results are saved in `benchmarks/build/benchmarks/text-reports/` (when using `.main.kts`) or `benchmarks_result/` (when using `.sh`).

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

### K/Wasm
- `./gradlew clean :benchmarks:wasmJsBrowserProductionRun` (you can see the results printed on the page itself)

### K/JS
- `./gradlew clean :benchmarks:jsBrowserProductionRun` (you can see the results printed on the page itself)


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
| TextLayout                               | [benchmarks/src/commonMain/kotlin/benchmarks/textlayout/TextLayout.kt](benchmarks/src/commonMain/kotlin/benchmarks/textlayout/TextLayout.kt)                         | Tests text layout and rendering performance by continuously scrolling column with big number of heady to layout items.                         |
| CanvasDrawing                            | [benchmarks/src/commonMain/kotlin/benchmarks/canvasdrawing/CanvasDrawing.kt](benchmarks/src/commonMain/kotlin/benchmarks/canvasdrawing/CanvasDrawing.kt)             | Tests Canvas drawing performance by scrolling items with massive amount of graphic shapes.                                                     |
| HeavyShader                              | [benchmarks/src/commonMain/kotlin/benchmarks/heavyshader/HeavyShader.kt](benchmarks/src/commonMain/kotlin/benchmarks/heavyshader/HeavyShader.kt)                     | Tests GPU shader performance by scrolling items with a complex GPU shader.                                                                     |
