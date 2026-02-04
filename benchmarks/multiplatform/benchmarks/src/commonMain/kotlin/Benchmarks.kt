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
        println("CPU average frame time: $cpuTime")
        println("GPU average frame time: $gpuTime")
        println("TOTAL average frame time: $totalTime")
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
) {
    fun prettyPrint() {
        println("# $name")
        val versionInfo = Config.versionInfo
        if (versionInfo != null) {
            println("Version: $versionInfo")
        }
        conditions.prettyPrint()
        println()
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
            println()
        }
    }

    fun putFormattedValuesTo(map: MutableMap<String, String>) {
        val versionInfo = Config.versionInfo
        if (versionInfo != null) {
            map.put("Version", versionInfo)
        }
        conditions.putFormattedValuesTo(map)
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
            MissedFrames(doubleBufferingMissedFrames, doubleBufferingMissedFrames / frames.size.toDouble())
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
    }
)

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
        stats.prettyPrint()
        if (Config.saveStats()) {
            saveBenchmarkStats(name = benchmark.name, stats = stats)
        }
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
    println()
    println("Running emulating $targetFps FPS")
    println()
    for (benchmark in benchmarks) {
        runBenchmark(benchmark, width, height, targetFps, graphicsContext, warmupCount)
    }
}

@Composable
fun BenchmarkRunner(
    benchmarks: List<Benchmark>,
    deviceFrameRate: Int,
    onExit: () -> Unit
) {
    var currentBenchmarkIndex by remember { mutableStateOf(0) }
    var isWarmup by remember { mutableStateOf(Config.warmupCount > 0) }
    var isEmptyScreen by remember { mutableStateOf(false) }

    if (currentBenchmarkIndex == benchmarks.size) {
        onExit()
        return
    }
    val benchmark = benchmarks[currentBenchmarkIndex]
    if (Config.isBenchmarkEnabled(benchmark.name)) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!isEmptyScreen) {
                benchmark.content()
            }

            LaunchedEffect(benchmark.name, isWarmup, isEmptyScreen) {
                if (isWarmup) {
                    repeat(Config.warmupCount) {
                        withFrameNanos { }
                    }
                    isWarmup = false
                    isEmptyScreen = true
                } else if (isEmptyScreen) {
                    delay(Config.emptyScreenDelay.milliseconds)
                    withFrameNanos {  }
                    isEmptyScreen = false
                } else {
                    val start = TimeSource.Monotonic.markNow()
                    val frames = MutableList(benchmark.frameCount) {
                        BenchmarkFrame(Duration.ZERO, Duration.ZERO)
                    }
                    repeat(benchmark.frameCount) {
                        val frameStart = TimeSource.Monotonic.markNow()
                        withFrameNanos { }
                        frames[it] = BenchmarkFrame(frameStart.elapsedNow(), Duration.ZERO)
                    }
                    val duration = start.elapsedNow()
                    val stats = BenchmarkResult(
                        name = benchmark.name,
                        frameBudget = (1.seconds.inWholeNanoseconds / deviceFrameRate).nanoseconds,
                        conditions = BenchmarkConditions(benchmark.frameCount, 0),
                        averageFrameInfo = FrameInfo(duration / benchmark.frameCount, Duration.ZERO),
                        averageFPSInfo = FPSInfo(benchmark.frameCount.toDouble() / duration.toDouble(DurationUnit.SECONDS)),
                        frames = frames
                    ).generateStats()
                    stats.prettyPrint()
                    if (Config.saveStats()) {
                        saveBenchmarkStats(name = benchmark.name, stats = stats)
                    }
                    currentBenchmarkIndex++
                    isWarmup = Config.warmupCount > 0
                    isEmptyScreen = true
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
