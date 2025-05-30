import androidx.compose.runtime.Composable
import benchmarks.animation.AnimatedVisibility
import benchmarks.complexlazylist.components.MainUiNoImageUseModel
import benchmarks.multipleComponents.MultipleComponentsExample
import benchmarks.lazygrid.LazyGrid
import benchmarks.visualeffects.NYContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.time.Duration

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
    val percentileCPUAverage: List<BenchmarkPercentileAverage>,
    val percentileGPUAverage: List<BenchmarkPercentileAverage>,
    val noBufferingMissedFrames: MissedFrames,
    val doubleBufferingMissedFrames: MissedFrames,
) {
    fun prettyPrint() {
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

suspend fun runBenchmark(
    name: String,
    width: Int,
    height: Int,
    targetFps: Int,
    frameCount: Int,
    graphicsContext: GraphicsContext?,
    warmupCount: Int = 100,
    content: @Composable () -> Unit
) {
    if (Config.isBenchmarkEnabled(name)) {
        println("# $name")
        val stats = measureComposable(
            name = name,
            warmupCount = warmupCount,
            frameCount = Config.getBenchmarkProblemSize(name, frameCount),
            width = width,
            height = height,
            targetFps = targetFps,
            graphicsContext = graphicsContext,
            content = content
        ).generateStats()
        stats.prettyPrint()
        if (Config.saveStats()) {
            saveBenchmarkStats(name = name, stats = stats)
        }
    }
}

suspend fun runBenchmarks(
    width: Int = 1920,
    height: Int = 1080,
    targetFps: Int = 120,
    warmupCount: Int = 100,
    graphicsContext: GraphicsContext? = null
) {
    println()
    println("Running emulating $targetFps FPS")
    println()
    runBenchmark("AnimatedVisibility", width, height, targetFps, 1000, graphicsContext, warmupCount) { AnimatedVisibility() }
    runBenchmark("LazyGrid", width, height, targetFps, 1000, graphicsContext, warmupCount) { LazyGrid() }
    runBenchmark("LazyGrid-ItemLaunchedEffect", width, height, targetFps, 1000, graphicsContext, warmupCount) {
        LazyGrid(smoothScroll = false, withLaunchedEffectInItem = true)
    }
    runBenchmark("LazyGrid-SmoothScroll", width, height, targetFps, 1000, graphicsContext, warmupCount) {
        LazyGrid(smoothScroll = true)
    }
    runBenchmark("LazyGrid-SmoothScroll-ItemLaunchedEffect", width, height, targetFps, 1000, graphicsContext, warmupCount) {
        LazyGrid(smoothScroll = true, withLaunchedEffectInItem = true)
    }
    runBenchmark("VisualEffects", width, height, targetFps, 1000, graphicsContext, warmupCount) { NYContent(width, height) }
    runBenchmark("LazyList", width, height, targetFps, 1000, graphicsContext, warmupCount) { MainUiNoImageUseModel()}
    runBenchmark("MultipleComponents", width, height, targetFps, 1000, graphicsContext, warmupCount) { MultipleComponentsExample() }
    runBenchmark("MultipleComponents-NoVectorGraphics", width, height, targetFps, 1000, graphicsContext, warmupCount) {
        MultipleComponentsExample(isVectorGraphicsSupported = false)
    }
}
