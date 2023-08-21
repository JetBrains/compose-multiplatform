import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.unit.Constraints
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Surface
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

const val nanosPerSecond = 1E9.toLong()

interface GraphicsContext {
    fun surface(width: Int, height: Int): Surface

    fun waitUntilGPUFinishes()
}

@OptIn(ExperimentalTime::class)
fun measureComposable(
    frameCount: Int = 500,
    width: Int,
    height: Int,
    targetFps: Int,
    graphicsContext: GraphicsContext?,
    content: @Composable () -> Unit
): BenchmarkResult {
    val scene = ComposeScene()
    try {
        val nanosPerFrame = (1.0 / targetFps.toDouble() * nanosPerSecond).toLong()
        scene.setContent(content)
        scene.constraints = Constraints.fixed(width, height)
        val surface = graphicsContext?.surface(width, height) ?: Surface.makeNull(width, height)

        var nanoTime = 0L

        val frames = MutableList(frameCount) {
            BenchmarkFrame(Duration.INFINITE, Duration.INFINITE)
        }

        repeat(frameCount) {
            val cpuTime = measureTime {
                scene.render(surface.canvas, nanoTime)
            }

            val gpuTime = measureTime {
                graphicsContext?.waitUntilGPUFinishes()
            }

            frames[it] = BenchmarkFrame(cpuTime, gpuTime)

            nanoTime += nanosPerFrame
        }

        return BenchmarkResult(
            nanosPerFrame.nanoseconds,
            frames
        )
    } finally {
        scene.close()
    }
}
