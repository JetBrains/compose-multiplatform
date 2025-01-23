import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Surface
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.*
import kotlin.time.TimeSource.Monotonic.markNow
import kotlin.time.measureTime

const val nanosPerSecond = 1E9.toLong()

interface GraphicsContext {
    fun surface(width: Int, height: Int): Surface

    suspend fun awaitGPUCompletion()
}

expect fun runGC()

suspend inline fun preciseDelay(duration: Duration) {
    val liveDelay: Duration
    if (duration.inWholeMilliseconds > 2) {
        //experiments have shown that for precise delay we should do live delay at least 2 ms
        val delayMillis = duration.inWholeMilliseconds - 2
        val delayStart = markNow()
        delay(delayMillis)
        liveDelay = duration - delayStart.elapsedNow()
    } else {
        liveDelay = duration
    }
    val liveDelayStart = markNow()
    while (liveDelayStart.elapsedNow() < liveDelay){}
}

@OptIn(ExperimentalTime::class, InternalComposeUiApi::class)
suspend fun measureComposable(
    warmupCount: Int,
    frameCount: Int,
    width: Int,
    height: Int,
    targetFps: Int,
    graphicsContext: GraphicsContext?,
    content: @Composable () -> Unit
): BenchmarkResult  {
    val scene = CanvasLayersComposeScene(size = IntSize(width, height))
    try {
        val nanosPerFrame = (1.0 / targetFps.toDouble() * nanosPerSecond).toLong()
        scene.setContent(content)
        val surface = graphicsContext?.surface(width, height) ?: Surface.makeNull(width, height)
        val canvas = surface.canvas.asComposeCanvas()

        // warmup
        repeat(warmupCount) {
            scene.render(canvas, it * nanosPerFrame)
            surface.flushAndSubmit(false)
            graphicsContext?.awaitGPUCompletion()
        }

        runGC()

        var renderTime = Duration.ZERO
        var gpuTime = Duration.ZERO
        if (Args.isModeEnabled(Mode.CPU)) {
            renderTime = measureTime {
                repeat(frameCount) {
                    scene.render(canvas, it * nanosPerFrame)
                    surface.flushAndSubmit(false)
                    gpuTime += measureTime {
                        graphicsContext?.awaitGPUCompletion()
                    }
                }
            }
            renderTime -= gpuTime
        }

        val frames = MutableList(frameCount) {
            BenchmarkFrame(Duration.INFINITE, Duration.INFINITE)
        }

        if (Args.isModeEnabled(Mode.FRAMES)) {

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
        }

        return BenchmarkResult(
            nanosPerFrame.nanoseconds,
            renderTime,
            frames
        )
    } finally {
        scene.close()
        runGC() // cleanup for next benchmarks
    }
}
