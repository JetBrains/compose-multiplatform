import androidx.compose.runtime.Composable
import benchmarks.animation.AnimatedVisibility
import benchmarks.complexlazylist.components.MainUiNoImageUseModel
import benchmarks.example1.Example1
import benchmarks.lazygrid.LazyGrid
import benchmarks.visualeffects.NYContent
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
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

data class BenchmarkPercentileAverage(
    val percentile: Double,
    val average: Duration
)

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

data class BenchmarkStats(
    val frameBudget: Duration,
    val conditions: BenchmarkConditions,
    val averageFrameInfo: FrameInfo?,
    val percentileCPUAverage: List<BenchmarkPercentileAverage>,
    val percentileGPUAverage: List<BenchmarkPercentileAverage>,
    val noBufferingMissedFrames: MissedFrames,
    val doubleBufferingMissedFrames: MissedFrames
) {
    fun prettyPrint() {
        val versionInfo = Args.versionInfo
        if (versionInfo != null) {
            println("Version: $versionInfo")
        }
        conditions.prettyPrint()
        println()
        if (Args.isModeEnabled(Mode.SIMPLE)) {
            val frameInfo = requireNotNull(averageFrameInfo) { "frameInfo shouldn't be null with Mode.SIMPLE" }
            frameInfo.prettyPrint()
            println()
        }
        if (Args.isModeEnabled(Mode.VSYNC_EMULATION)) {
            percentileCPUAverage.prettyPrint(BenchmarkFrameTimeKind.CPU)
            println()
            percentileGPUAverage.prettyPrint(BenchmarkFrameTimeKind.GPU)
            println()
            noBufferingMissedFrames.prettyPrint("no buffering")
            doubleBufferingMissedFrames.prettyPrint("double buffering")
        }
    }

    fun putFormattedValuesTo(map: MutableMap<String, String>) {
        val versionInfo = Args.versionInfo
        if (versionInfo != null) {
            map.put("Version", versionInfo)
        }
        conditions.putFormattedValuesTo(map)
        if (Args.isModeEnabled(Mode.SIMPLE)) {
            val frameInfo = requireNotNull(averageFrameInfo) { "frameInfo shouldn't be null with Mode.SIMPLE" }
            frameInfo.putFormattedValuesTo(map)
        }
        if (Args.isModeEnabled(Mode.VSYNC_EMULATION)) {
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
}

class BenchmarkResult(
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
    if (Args.isBenchmarkEnabled(name)) {
        println("# $name")
        val stats = measureComposable(warmupCount, Args.getBenchmarkProblemSize(name, frameCount), width, height, targetFps, graphicsContext, content).generateStats()
        stats.prettyPrint()
        saveBenchmarkStatsOnDisk(name, stats)
    }
}

suspend fun runBenchmarks(
    width: Int = 1920,
    height: Int = 1080,
    targetFps: Int = 120,
    graphicsContext: GraphicsContext? = null
) {
    println()
    println("Running emulating $targetFps FPS")
    println()
    runBenchmark("AnimatedVisibility", width, height, targetFps, 1000, graphicsContext) { AnimatedVisibility() }
    runBenchmark("LazyGrid", width, height, targetFps, 1000, graphicsContext) { LazyGrid() }
    runBenchmark("VisualEffects", width, height, targetFps, 1000, graphicsContext) { NYContent(width, height) }
    runBenchmark("LazyList", width, height, targetFps, 1000, graphicsContext) { MainUiNoImageUseModel()}
    runBenchmark("Example1", width, height, targetFps, 1000, graphicsContext) { Example1() }
 }
