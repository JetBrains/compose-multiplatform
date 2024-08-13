import androidx.compose.runtime.Composable
import benchmarks.animation.AnimatedVisibility
import benchmarks.lazygrid.LazyGrid
import benchmarks.visualeffects.NYContent
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
}

data class BenchmarkStats(
    val frameBudget: Duration,
    val percentileCPUAverage: List<BenchmarkPercentileAverage>,
    val percentileGPUAverage: List<BenchmarkPercentileAverage>,
    val noBufferingMissedFrames: MissedFrames,
    val doubleBufferingMissedFrames: MissedFrames
) {
    fun prettyPrint() {
        percentileCPUAverage.prettyPrint(BenchmarkFrameTimeKind.CPU)
        println()
        percentileGPUAverage.prettyPrint(BenchmarkFrameTimeKind.GPU)
        println()
        noBufferingMissedFrames.prettyPrint("no buffering")
        doubleBufferingMissedFrames.prettyPrint("double buffering")
    }

    private fun List<BenchmarkPercentileAverage>.prettyPrint(kind: BenchmarkFrameTimeKind) {
        forEach {
            println("Worst p${(it.percentile * 100).roundToInt()} ${kind.toPrettyPrintString()} average: ${it.average}")
        }
    }
}

class BenchmarkResult(
    private val frameBudget: Duration,
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

fun runBenchmark(
    name: String,
    width: Int,
    height: Int,
    targetFps: Int,
    frameCount: Int,
    graphicsContext: GraphicsContext?,
    warmupCount: Int = 100,
    content: @Composable () -> Unit
): BenchmarkStats {
    val stats = measureComposable(warmupCount, frameCount, width, height, targetFps, graphicsContext, content).generateStats()

    println(name)
    stats.prettyPrint()
    println()

    return stats
}

fun runBenchmarks(
    width: Int = 1920,
    height: Int = 1080,
    targetFps: Int = 120,
    graphicsContext: GraphicsContext? = null
) {
    println("Running emulating $targetFps FPS")
    runBenchmark("AnimatedVisibility", width, height, targetFps, 1000, graphicsContext) { AnimatedVisibility() }
    runBenchmark("LazyGrid", width, height, targetFps, 1000, graphicsContext) { LazyGrid() }
    runBenchmark("VisualEffects", width, height, targetFps, 1000, graphicsContext) { NYContent(width, height) }
}