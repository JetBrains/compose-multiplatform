import androidx.compose.runtime.Composable
import benchmarks.animation.AnimatedVisibility
import benchmarks.lazygrid.LazyGrid
import benchmarks.visualeffects.NYContent
import kotlin.math.roundToInt
import kotlin.time.Duration

enum class BenchmarkFrameTimeKind {
    CPU, GPU
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

data class BenchmarkStats(
    val percentileCPUAverage: List<BenchmarkPercentileAverage>,
    val percentileGPUAverage: List<BenchmarkPercentileAverage>,
    val missedFramesCount: Int,
    val missedFramesRatio: Double
) {
    override fun toString(): String =
        """
        ${percentileCPUAverage.toPrettyPrintString()}
        
        ${percentileGPUAverage.toPrettyPrintString()}
        
        Missed frames count: $missedFramesCount
        Missed frames ratio: $missedFramesRatio
        """.trimIndent()

    private fun List<BenchmarkPercentileAverage>.toPrettyPrintString(): String =
        joinToString("\n") {
            "p${(it.percentile * 100).roundToInt()} CPU average: ${it.average}"
        }
}

class BenchmarkResult(
    private val frameBudget: Duration,
    private val frames: List<BenchmarkFrame>,
) {
    private fun percentileAverageFrameTime(percentile: Double, kind: BenchmarkFrameTimeKind): Duration {
        require(percentile in 0.0..1.0)

        val startIndex = ((frames.size - 1) * percentile).roundToInt()

        val percentileFrames = frames.sortedBy { it.duration(kind) }.subList(startIndex, frames.size)

        return averageDuration(percentileFrames) {
            it.duration(kind)
        }
    }

    fun generateStats(): BenchmarkStats {
        val missedFramesCount = frames.count {
            it.cpuDuration + it.gpuDuration > frameBudget
        }

        return BenchmarkStats(
            listOf(0.0, 0.5, 0.75, 0.9, 0.95, 0.97, 0.99).map { percentile ->
                val average = percentileAverageFrameTime(percentile, BenchmarkFrameTimeKind.CPU)

                BenchmarkPercentileAverage(percentile, average)
            },
            listOf(0.0, 0.5, 0.95).map { percentile ->
                val average = percentileAverageFrameTime(percentile, BenchmarkFrameTimeKind.GPU)

                BenchmarkPercentileAverage(percentile, average)
            },
            missedFramesCount,
            missedFramesCount.toDouble() / frames.size
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
    content: @Composable () -> Unit
): BenchmarkStats {
    val stats = measureComposable(frameCount, width, height, targetFps, graphicsContext, content).generateStats()

    println(
        """
        $name
        $stats
        
        """.trimIndent()
    )

    return stats
}

fun runBenchmarks(
    width: Int = 1920,
    height: Int = 1080,
    targetFps: Int = 60,
    graphicsContext: GraphicsContext? = null
) {
    runBenchmark("AnimatedVisibility", width, height, targetFps, 1000, graphicsContext) { AnimatedVisibility() }
    runBenchmark("LazyGrid", width, height, targetFps, 1000, graphicsContext) { LazyGrid() }
    runBenchmark("VisualEffects", width, height, targetFps, 1000, graphicsContext) { NYContent(width, height) }
}