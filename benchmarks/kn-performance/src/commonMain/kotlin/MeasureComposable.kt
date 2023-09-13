import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.unit.Constraints
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Surface
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlinx.coroutines.*

const val nanosPerSecond = 1E9.toLong()
const val millisPerSecond = 1e3.toLong()
const val nanosPerMillisecond = nanosPerSecond / millisPerSecond

interface GraphicsContext {
    fun surface(width: Int, height: Int): Surface

    suspend fun awaitGPUCompletion()
}

@OptIn(ExperimentalTime::class)
fun measureComposable(
    frameCount: Int = 500,
    width: Int,
    height: Int,
    targetFps: Int,
    graphicsContext: GraphicsContext?,
    content: @Composable () -> Unit
): BenchmarkResult = runBlocking {
    val scene = ComposeScene()
    try {
        val nanosPerFrame = (1.0 / targetFps.toDouble() * nanosPerSecond).toLong()
        scene.setContent(content)
        scene.constraints = Constraints.fixed(width, height)
        val surface = graphicsContext?.surface(width, height) ?: Surface.makeNull(width, height)

        val frames = MutableList(frameCount) {
            BenchmarkFrame(Duration.INFINITE, Duration.INFINITE)
        }

        var nanoTime = 0L

        repeat(frameCount) {
            val frameTime = measureTime {
                val cpuTime = measureTime {
                    scene.render(surface.canvas, nanoTime)
                    surface.flushAndSubmit(false)
                }

                val gpuTime = measureTime {
                    graphicsContext?.awaitGPUCompletion()
                }

                frames[it] = BenchmarkFrame(cpuTime, gpuTime)
            }

            val actualNanosPerFrame = frameTime.inWholeNanoseconds
            val nanosUntilDeadline = nanosPerFrame - actualNanosPerFrame

            // Emulate waiting for next vsync
            if (nanosUntilDeadline > 0) {
                delay(nanosUntilDeadline / nanosPerMillisecond)
            }

            nanoTime += maxOf(actualNanosPerFrame, nanosPerFrame)
        }

        BenchmarkResult(
            nanosPerFrame.nanoseconds,
            frames
        )
    } finally {
        scene.close()
    }
}
