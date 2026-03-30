import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.withFrameNanos
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.TimeSource
import benchmarks.animation.AnimatedVisibility
import benchmarks.complexlazylist.components.MainUiNoImageUseModel
import benchmarks.multipleComponents.MultipleComponentsExample
import benchmarks.lazygrid.LazyGrid
import benchmarks.visualeffects.NYContent
import benchmarks.textlayout.TextLayout
import benchmarks.canvasdrawing.CanvasDrawing
import benchmarks.heavyshader.HeavyShader
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

enum class BenchmarkFrameTimeKind {
    CPU, GPU
}


fun BenchmarkFrameTimeKind.toPrettyPrintString(): String =
    when (this) {
        BenchmarkFrameTimeKind.CPU -> "CPU"
        BenchmarkFrameTimeKind.GPU -> "GPU"
    }

data class BenchmarkFrame(
    val cpuDuration: Duration,
    val gpuDuration: Duration
) {
    fun duration(kind: BenchmarkFrameTimeKind): Duration =
        when (kind) {
            BenchmarkFrameTimeKind.CPU -> cpuDuration
            BenchmarkFrameTimeKind.GPU -> gpuDuration
        }
}

@Serializable
data class BenchmarkConditions(
    val frameCount: Int,
    val warmupCount: Int
) {
    fun prettyPrint() {
        println("$frameCount frames (warmup $warmupCount)")
    }

    fun putFormattedValuesTo(map: MutableMap<String, String>) {
        map.put("Frames/warmup", "$frameCount/$warmupCount")
    }
}

@Serializable
data class FPSInfo(
    val fps: Double
) {

    fun prettyPrint() {
        println("Average FPS: $fps")
    }

    fun putFormattedValuesTo(map: MutableMap<String, String>) {
        map.put("Average FPS", fps.toString())
    }
}

@Serializable
data class FrameInfo(
    val cpuTime: Duration,
    val gpuTime: Duration,
) {
    val totalTime = cpuTime + gpuTime

    fun prettyPrint() {
        if (gpuTime == Duration.ZERO) {
            println("Average frame time: $totalTime")
        } else {
            println("CPU average frame time: $cpuTime")
            println("GPU average frame time: $gpuTime")
            println("TOTAL average frame time: $totalTime")
        }
    }

    fun putFormattedValuesTo(map: MutableMap<String, String>) {
        map.put("CPU avg frame (ms)", cpuTime.formatAsMilliseconds())
        map.put("GPU avg frame (ms)", gpuTime.formatAsMilliseconds())
        map.put("TOTAL avg frame (ms)", totalTime.formatAsMilliseconds())
    }
}

@Serializable
data class BenchmarkPercentileAverage(
    val percentile: Double,
    val average: Duration
)

@Serializable
data class MissedFrames(
    val count: Int,
    val ratio: Double
) {
    fun prettyPrint(description: String) {
        println(
            """
            Missed frames ($description):
                - count: $count
                - ratio: $ratio     
                            
            """.trimIndent()
        )
    }

    fun putFormattedValuesTo(description: String, map: MutableMap<String, String>) {
        map.put("Missed frames ($description)", "$ratio")
    }
}

private val json = Json { prettyPrint = true }

@Serializable
data class StartupTimeInfo(
    val timeToMain: Duration? = null,
    val timeFromMainToFirstFrame: Duration? = null,
    val timeOfFirstFrame: Duration,
    val timeToNthFrame: Duration,
    val nthFrameCount: Int,
    val longestFrames: List<Duration>
)

@Serializable
data class BenchmarkStats(
    val name: String,
    val frameBudget: Duration,
    val conditions: BenchmarkConditions,
    val averageFrameInfo: FrameInfo?,
    val averageFPSInfo: FPSInfo,
    val percentileCPUAverage: List<BenchmarkPercentileAverage>,
    val percentileGPUAverage: List<BenchmarkPercentileAverage>,
    val noBufferingMissedFrames: MissedFrames,
    val doubleBufferingMissedFrames: MissedFrames,
    val startupTimeInfo: StartupTimeInfo? = null,
) {
    fun prettyPrint() {
        println("# $name")
        val versionInfo = Config.versionInfo
        if (versionInfo != null) {
            println("Version: $versionInfo")
        }
        conditions.prettyPrint()
        println()
        startupTimeInfo?.let {
            println("StartupTimeInfo:")
            var timeToMain: Duration = Duration.ZERO
            it.timeToMain?.let { t ->
                timeToMain = t
                println("    - Time to main: ${t.inWholeMilliseconds} ms")
            }
            it.timeFromMainToFirstFrame?.let { t ->
                println("    - Time from main to first frame: ${t.inWholeMilliseconds} ms")
            }
            println("    - Time of first frame: ${it.timeOfFirstFrame.inWholeMilliseconds} ms")
            it.timeFromMainToFirstFrame?.let { t ->
                println("    - Total startup time: ${timeToMain.inWholeMilliseconds + t.inWholeMilliseconds + it.timeOfFirstFrame.inWholeMilliseconds} ms")
            }
            println("    - Time from first to ${it.nthFrameCount}th frame: ${it.timeToNthFrame.inWholeMilliseconds} ms")
            println("    - ${it.longestFrames.size} longest frames during startup: ${it.longestFrames.joinToString { f -> "${f.inWholeMilliseconds} ms" }}")
            println()
        }
        if (Config.isModeEnabled(Mode.SIMPLE)) {
            val frameInfo = requireNotNull(averageFrameInfo) { "frameInfo shouldn't be null with Mode.SIMPLE" }
            frameInfo.prettyPrint()
            println()
        }
        if (Config.isModeEnabled(Mode.VSYNC_EMULATION)) {
            percentileCPUAverage.prettyPrint(BenchmarkFrameTimeKind.CPU)
            println()
            percentileGPUAverage.prettyPrint(BenchmarkFrameTimeKind.GPU)
            println()
            noBufferingMissedFrames.prettyPrint("no buffering")
            doubleBufferingMissedFrames.prettyPrint("double buffering")
        }
        if (Config.isModeEnabled(Mode.REAL)) {
            averageFPSInfo.prettyPrint()
            averageFrameInfo?.prettyPrint()
            noBufferingMissedFrames.prettyPrint("estimation of real")
            println()
        }
    }

    fun putFormattedValuesTo(map: MutableMap<String, String>) {
        val versionInfo = Config.versionInfo
        if (versionInfo != null) {
            map.put("Version", versionInfo)
        }
        conditions.putFormattedValuesTo(map)
        startupTimeInfo?.let {
            it.timeToMain?.let { t ->
                map.put("Startup: Time to main (ms)", t.formatAsMilliseconds())
            }
            it.timeFromMainToFirstFrame?.let { t ->
                map.put("Startup: Time from main to first frame (ms)", t.formatAsMilliseconds())
            }
            map.put("Startup: Time of first frame (ms)", it.timeOfFirstFrame.formatAsMilliseconds())
            map.put("Startup: Time from first to ${it.nthFrameCount}th frame (ms)", it.timeToNthFrame.formatAsMilliseconds())
            it.longestFrames.forEachIndexed { index, duration ->
                map.put("Startup: Longest frame ${index + 1} (ms)", duration.formatAsMilliseconds())
            }
        }
        if (Config.isModeEnabled(Mode.SIMPLE)) {
            val frameInfo = requireNotNull(averageFrameInfo) { "frameInfo shouldn't be null with Mode.SIMPLE" }
            frameInfo.putFormattedValuesTo(map)
        }
        if (Config.isModeEnabled(Mode.VSYNC_EMULATION)) {
            percentileCPUAverage.putFormattedValuesTo(BenchmarkFrameTimeKind.CPU, map)
            percentileGPUAverage.putFormattedValuesTo(BenchmarkFrameTimeKind.GPU, map)
            noBufferingMissedFrames.putFormattedValuesTo("no buffering", map)
            doubleBufferingMissedFrames.putFormattedValuesTo("double buffering", map)
        }
        if (Config.isModeEnabled(Mode.REAL)) {
            averageFPSInfo.putFormattedValuesTo(map)
        }
    }

    private fun List<BenchmarkPercentileAverage>.prettyPrint(kind: BenchmarkFrameTimeKind) {
        forEach {
            println("Worst p${(it.percentile * 100).roundToInt()} ${kind.toPrettyPrintString()} (ms): ${it.average}")
        }
    }

    private fun List<BenchmarkPercentileAverage>.putFormattedValuesTo(
        kind: BenchmarkFrameTimeKind,
        map: MutableMap<String, String>
    ) {
        forEach {
            map.put(
                "Worst p${(it.percentile * 100).roundToInt()} ${kind.toPrettyPrintString()} (ms)",
                it.average.formatAsMilliseconds()
            )
        }
    }

    fun toJsonString(): String = json.encodeToString(serializer(), this)
}

class BenchmarkResult(
    private val name: String,
    private val frameBudget: Duration,
    private val conditions: BenchmarkConditions,
    private val averageFrameInfo: FrameInfo,
    private val averageFPSInfo: FPSInfo,
    private val frames: List<BenchmarkFrame>,
    private val startupTimeInfo: StartupTimeInfo? = null,
) {
    private fun percentileAverageFrameTime(percentile: Double, kind: BenchmarkFrameTimeKind): Duration {
        require(percentile in 0.0..1.0)

        val startIndex = ((frames.size - 1) * percentile).roundToInt()

        val percentileFrames = frames.sortedBy { it.duration(kind) }.subList(frames.size - startIndex - 1, frames.size)

        return averageDuration(percentileFrames) {
            it.duration(kind)
        }
    }

    fun generateStats(): BenchmarkStats {
        val noBufferingMissedFramesCount = frames.count {
            it.cpuDuration + it.gpuDuration > frameBudget
        }

        val doubleBufferingMissedFrames = frames.count {
            maxOf(it.cpuDuration, it.gpuDuration) > frameBudget
        }

        return BenchmarkStats(
            name,
            frameBudget,
            conditions,
            averageFrameInfo,
            averageFPSInfo,
            listOf(0.01, 0.02, 0.05, 0.1, 0.25, 0.5).map { percentile ->
                val average = percentileAverageFrameTime(percentile, BenchmarkFrameTimeKind.CPU)

                BenchmarkPercentileAverage(percentile, average)
            },
            listOf(0.01, 0.1, 0.5).map { percentile ->
                val average = percentileAverageFrameTime(percentile, BenchmarkFrameTimeKind.GPU)

                BenchmarkPercentileAverage(percentile, average)
            },
            MissedFrames(noBufferingMissedFramesCount, noBufferingMissedFramesCount / frames.size.toDouble()),
            MissedFrames(doubleBufferingMissedFrames, doubleBufferingMissedFrames / frames.size.toDouble()),
            startupTimeInfo = startupTimeInfo,
        )
    }

    private fun averageDuration(frames: List<BenchmarkFrame>, selector: (BenchmarkFrame) -> Duration): Duration =
        frames.fold(Duration.ZERO) { acc, frame ->
            acc + selector(frame)
        } / frames.size

}

private fun Duration.formatAsMilliseconds(): String = (inWholeMicroseconds / 1000.0).toString()

data class Benchmark(
    val name: String,
    val frameCount: Int,
    val content: @Composable () -> Unit
)

fun Benchmark(name: String, content: @Composable () -> Unit): Benchmark {
    return Benchmark(name, Config.getBenchmarkProblemSize(name, Config.frameCount), content)
}

fun getBenchmarks(): List<Benchmark> = listOf(
    Benchmark("AnimatedVisibility") { AnimatedVisibility() },
    Benchmark("LazyGrid") { LazyGrid() },
    Benchmark("LazyGrid-ItemLaunchedEffect") {
        LazyGrid(smoothScroll = false, withLaunchedEffectInItem = true)
    },
    Benchmark("LazyGrid-SmoothScroll") {
        LazyGrid(smoothScroll = true)
    },
    Benchmark("LazyGrid-SmoothScroll-ItemLaunchedEffect") {
        LazyGrid(smoothScroll = true, withLaunchedEffectInItem = true)
    },
    Benchmark("VisualEffects") {
        NYContent(1920, 1080)
    },
    Benchmark("LazyList") { MainUiNoImageUseModel() },
    Benchmark("MultipleComponents") { MultipleComponentsExample() },
    Benchmark("MultipleComponents-NoVectorGraphics") {
        MultipleComponentsExample(isVectorGraphicsSupported = false)
    },
    Benchmark("TextLayout") { TextLayout() },
    Benchmark("CanvasDrawing") { CanvasDrawing() },
    Benchmark("HeavyShader") { HeavyShader() }
).sortedBy { it.name }

suspend fun runBenchmark(
    benchmark: Benchmark,
    width: Int,
    height: Int,
    targetFps: Int,
    graphicsContext: GraphicsContext?,
    warmupCount: Int = Config.warmupCount,
) {
    if (Config.isBenchmarkEnabled(benchmark.name)) {
        val stats = measureComposable(
            name = benchmark.name,
            warmupCount = warmupCount,
            frameCount = benchmark.frameCount,
            width = width,
            height = height,
            targetFps = targetFps,
            graphicsContext = graphicsContext,
            content = benchmark.content
        ).generateStats()
        reportBenchmarkStats(stats)
    }
}

private fun reportBenchmarkStats(stats: BenchmarkStats, results: MutableList<BenchmarkStats>? = null) {
    if (results != null) {
        results.add(stats)
    }
    if (results == null || !Config.reportAtTheEnd) {
        stats.prettyPrint()
    }
    if (Config.saveStatsToJSON && isIosTarget) {
        println("JSON_START")
        println(stats.toJsonString())
        println("JSON_END")
    }
    if (Config.saveStats()) {
        saveBenchmarkStats(name = stats.name, stats = stats)
    }
}

suspend fun runBenchmarks(
    benchmarks: List<Benchmark>,
    width: Int = 1920,
    height: Int = 1080,
    targetFps: Int = 120,
    warmupCount: Int = Config.warmupCount,
    graphicsContext: GraphicsContext? = null
) {
    if (Config.listBenchmarks) {
        println("AVAILABLE_BENCHMARKS_START")
        benchmarks.forEach { println(it.name) }
        println("AVAILABLE_BENCHMARKS_END")
        return
    }
    println()
    println("Running emulating $targetFps FPS")
    println()
    for (benchmark in benchmarks) {
        runBenchmark(benchmark, width, height, targetFps, graphicsContext, warmupCount)
    }
}

expect fun getProcessStartTime(): TimeSource.Monotonic.ValueTimeMark?

expect val mainTime: TimeSource.Monotonic.ValueTimeMark

private enum class BenchmarkPhase {
    STARTUP,
    EMPTY_SCREEN,
    WARMUP,
    MEASUREMENT
}

@Composable
fun BenchmarkRunner(
    benchmarks: List<Benchmark>,
    deviceFrameRate: Int,
    onExit: () -> Unit
) {
    var currentBenchmarkIndex by remember { mutableStateOf(0) }
    var firstBenchmarkName by remember { mutableStateOf<String?>(null) }
    var phaseBeforeEmptyScreen by remember { mutableStateOf<BenchmarkPhase?>(null) }
    var phase by remember {
        mutableStateOf(
            when {
                Config.isModeEnabled(Mode.STARTUP) -> BenchmarkPhase.STARTUP
                Config.warmupCount > 0 -> BenchmarkPhase.WARMUP
                else -> BenchmarkPhase.MEASUREMENT
            }
        )
    }
    var startupTimeInfo by remember { mutableStateOf<StartupTimeInfo?>(null) }
    val results = remember { mutableListOf<BenchmarkStats>() }
    val nanosPerFrame = 1.seconds.inWholeNanoseconds / deviceFrameRate

    if (currentBenchmarkIndex == benchmarks.size) {
        if (Config.reportAtTheEnd) {
            results.forEach { it.prettyPrint() }
        }
        onExit()
        return
    }
    val benchmark = benchmarks[currentBenchmarkIndex]
    if (firstBenchmarkName == null && Config.isBenchmarkEnabled(benchmark.name)) {
        firstBenchmarkName = benchmark.name
    }

    if (Config.isBenchmarkEnabled(benchmark.name)) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (phase != BenchmarkPhase.EMPTY_SCREEN) {
                benchmark.content()
            }

            LaunchedEffect(benchmark.name, phase) {
                when (phase) {
                    BenchmarkPhase.STARTUP -> {
                        val processStart = getProcessStartTime()

                        // Time of the first frame
                        val firstFrameMeasureStart = TimeSource.Monotonic.markNow()
                        withFrameNanos { }
                        val firstFrameMark = TimeSource.Monotonic.markNow()
                        val timeOfFirstFrame = firstFrameMark - firstFrameMeasureStart

                        // Nth frame
                        val startupFramesCount = Config.startupFrameCount
                        val startupFrames = MutableList(startupFramesCount) { Duration.ZERO }
                        repeat(startupFramesCount) { i ->
                            val frameStart = TimeSource.Monotonic.markNow()
                            withFrameNanos { }
                            startupFrames[i] = frameStart.elapsedNow()
                        }
                        val nthFrameMark = TimeSource.Monotonic.markNow()
                        val timeToNthFrame = nthFrameMark - firstFrameMark

                        val capturedStartupTimeInfo = StartupTimeInfo(
                            timeToMain = processStart?.let { mainTime - it },
                            timeFromMainToFirstFrame = if (benchmark.name == firstBenchmarkName) firstFrameMark - mainTime else null,
                            timeOfFirstFrame = timeOfFirstFrame,
                            timeToNthFrame = timeToNthFrame,
                            nthFrameCount = startupFramesCount,
                            longestFrames = startupFrames.sortedDescending().take(Config.startupLongestFramesCount)
                        )
                        startupTimeInfo = capturedStartupTimeInfo

                        if (!Config.isModeEnabled(Mode.REAL)) {
                            val stats = BenchmarkResult(
                                name = benchmark.name,
                                frameBudget = nanosPerFrame.nanoseconds,
                                conditions = BenchmarkConditions(frameCount = startupFramesCount, warmupCount = 0),
                                averageFrameInfo = FrameInfo(timeOfFirstFrame, Duration.ZERO),
                                averageFPSInfo = FPSInfo(0.0),
                                frames = startupFrames.map { BenchmarkFrame(it, Duration.ZERO) },
                                startupTimeInfo = capturedStartupTimeInfo
                            ).generateStats()

                            reportBenchmarkStats(stats, results)
                            currentBenchmarkIndex++
                            startupTimeInfo = null
                        }
                        phaseBeforeEmptyScreen = BenchmarkPhase.STARTUP
                        phase = BenchmarkPhase.EMPTY_SCREEN
                    }
                    BenchmarkPhase.WARMUP -> {
                        repeat(Config.warmupCount) {
                            withFrameNanos { }
                        }
                        phaseBeforeEmptyScreen = BenchmarkPhase.WARMUP
                        phase = BenchmarkPhase.EMPTY_SCREEN
                    }
                    BenchmarkPhase.EMPTY_SCREEN -> {
                        delay(Config.emptyScreenDelay.milliseconds)
                        withFrameNanos { }
                        phase = when (phaseBeforeEmptyScreen) {
                            BenchmarkPhase.STARTUP ->
                                if (Config.isModeEnabled(Mode.REAL)) {
                                    if (Config.warmupCount > 0) BenchmarkPhase.WARMUP
                                    else BenchmarkPhase.MEASUREMENT
                                } else BenchmarkPhase.STARTUP
                            BenchmarkPhase.WARMUP ->
                                BenchmarkPhase.MEASUREMENT
                            BenchmarkPhase.MEASUREMENT ->
                                if (Config.isModeEnabled(Mode.STARTUP)) BenchmarkPhase.STARTUP
                                else if (Config.warmupCount > 0) BenchmarkPhase.WARMUP
                                else BenchmarkPhase.MEASUREMENT
                            else -> throw IllegalStateException("Unexpected phase: $phaseBeforeEmptyScreen")
                        }
                    }
                    BenchmarkPhase.MEASUREMENT -> {
                        val frames = MutableList(benchmark.frameCount) {
                            BenchmarkFrame(Duration.ZERO, Duration.ZERO)
                        }
                        // skip first frame waiting
                        withFrameNanos { }
                        val start = TimeSource.Monotonic.markNow()
                        repeat(benchmark.frameCount) {
                            val frameStart = TimeSource.Monotonic.markNow()
                            withFrameNanos { }
                            val rawFrameTime = frameStart.elapsedNow()
                            // rawFrameTime isn't reliable for missed-frame checks: small timing inaccuracies can push
                            // it just over the frame budget even when the frame would effectively fit.
                            // Instead, estimate how many vsync intervals the frame took by rounding the measured time to
                            // the nearest multiple of nanosPerFrame.
                            // Note: this is only an estimate — we can't determine here whether a frame was truly missed.
                            val normalizedFrameTime = ((rawFrameTime.inWholeNanoseconds + nanosPerFrame/2) / nanosPerFrame) * nanosPerFrame
                            frames[it] = BenchmarkFrame(normalizedFrameTime.nanoseconds, Duration.ZERO)
                        }
                        val duration = start.elapsedNow()
                        val stats = BenchmarkResult(
                            name = benchmark.name,
                            frameBudget = nanosPerFrame.nanoseconds,
                            conditions = BenchmarkConditions(benchmark.frameCount, warmupCount = Config.warmupCount),
                            averageFrameInfo = FrameInfo(duration / benchmark.frameCount, Duration.ZERO),
                            averageFPSInfo = FPSInfo(benchmark.frameCount.toDouble() / duration.toDouble(DurationUnit.SECONDS)),
                            frames = frames,
                            startupTimeInfo = startupTimeInfo
                        ).generateStats()
                        reportBenchmarkStats(stats, results)
                        currentBenchmarkIndex++
                        startupTimeInfo = null
                        phaseBeforeEmptyScreen = BenchmarkPhase.MEASUREMENT
                        phase = BenchmarkPhase.EMPTY_SCREEN
                    }
                }
            }
        }
    } else {
        LaunchedEffect(benchmark.name) {
            currentBenchmarkIndex++
        }
    }
}

suspend fun runBenchmarks(
    width: Int = 1920,
    height: Int = 1080,
    targetFps: Int = 120,
    warmupCount: Int = Config.warmupCount,
    graphicsContext: GraphicsContext? = null
) {
    runBenchmarks(getBenchmarks(), width, height, targetFps, warmupCount, graphicsContext)
}
