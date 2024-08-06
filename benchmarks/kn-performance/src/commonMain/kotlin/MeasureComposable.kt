import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Surface
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource.Monotonic.markNow

const val nanosPerSecond = 1E9.toLong()

interface GraphicsContext {
    fun surface(width: Int, height: Int): Surface

    suspend fun awaitGPUCompletion()
}

expect fun runGC()

suspend inline fun preciseDelay(duration: Duration) {
    val liveDelay: Duration
    if (duration.inWholeMilliseconds > 1) {
        val delayMillis = duration.inWholeMilliseconds - 1
        delay(delayMillis)
        liveDelay = duration - delayMillis.milliseconds
    } else {
        liveDelay = duration
    }
    val start = markNow()
    while (start.elapsedNow() < liveDelay){}
}

@OptIn(ExperimentalTime::class, InternalComposeUiApi::class)
fun measureComposable(
    warmupCount: Int,
    frameCount: Int,
    width: Int,
    height: Int,
    targetFps: Int,
    graphicsContext: GraphicsContext?,
    content: @Composable () -> Unit
): BenchmarkResult = runBlocking {
    val scene = MultiLayerComposeScene(size = IntSize(width, height))
    try {
        val nanosPerFrame = (1.0 / targetFps.toDouble() * nanosPerSecond).toLong()
        scene.setContent(content)
        val surface = graphicsContext?.surface(width, height) ?: Surface.makeNull(width, height)
        val canvas = surface.canvas.asComposeCanvas()

        // warmup
        repeat(warmupCount) {
            scene.render(canvas, it * nanosPerFrame)
        }

        val frames = MutableList(frameCount) {
            BenchmarkFrame(Duration.INFINITE, Duration.INFINITE)
        }

        var nextVSync = Duration.ZERO
        var missedFrames = 0;

        runGC()

        val start = markNow()

        repeat(frameCount) {
            val frameStart = start + nextVSync

            scene.render(canvas, nextVSync.inWholeNanoseconds)
            surface.flushAndSubmit(false)

            val cpuTime = frameStart.elapsedNow()

            graphicsContext?.awaitGPUCompletion()

            val frameTime = frameStart.elapsedNow()

            frames[it] = BenchmarkFrame(cpuTime, frameTime - cpuTime)

            missedFrames += (frameTime.inWholeNanoseconds / nanosPerFrame).toInt()

            nextVSync = ((it + 1 + missedFrames) * nanosPerFrame).nanoseconds

            val timeUntilNextVSync = nextVSync - start.elapsedNow()

            if (timeUntilNextVSync > Duration.ZERO) {
                // Emulate waiting for next vsync
                preciseDelay(timeUntilNextVSync)
            }
        }

        BenchmarkResult(
            nanosPerFrame.nanoseconds,
            frames
        )
    } finally {
        scene.close()
    }
}
