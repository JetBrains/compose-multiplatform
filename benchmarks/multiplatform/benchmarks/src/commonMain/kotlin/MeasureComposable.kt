import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Surface
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.*
import org.jetbrains.skia.Color
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.Rect
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

/**
 * Some of the benchmarks involved an asynchronous fetch operation for resources when running in a browser.
 * To let the fetch operation result be handled by compose, the benchmark loop must yield the event loop.
 * Otherwise, such benchmark do not run a part of workload, making the stats less meaningful.
 *
 * It makes sense for all platforms, since there could be some work scheduled to run on the same Thread
 * as the benchmarks runner. But without yielding, it won't be dispatched.
 */
private suspend inline fun yieldEventLoop() {
    yield()
}

@OptIn(ExperimentalTime::class, InternalComposeUiApi::class)
internal suspend fun measureComposable(
    name: String,
    warmupCount: Int,
    frameCount: Int,
    width: Int,
    height: Int,
    targetFps: Int,
    graphicsContext: GraphicsContext?,
    content: @Composable () -> Unit
): BenchmarkResult  {
    val surface = graphicsContext?.surface(width, height) ?: Surface.makeNull(width, height)
    val scene = CanvasLayersComposeScene(size = IntSize(width, height))
    try {
        val nanosPerFrame = (1.0 / targetFps.toDouble() * nanosPerSecond).toLong()
        scene.setContent(content)

        // warmup
        repeat(warmupCount) {
            scene.mimicSkikoRender(surface, it * nanosPerFrame, width, height)
            surface.flushAndSubmit(false)
            graphicsContext?.awaitGPUCompletion()
            yieldEventLoop()
        }

        runGC()

        var cpuTotalTime = Duration.ZERO
        var gpuTotalTime = Duration.ZERO
        if (Config.isModeEnabled(Mode.SIMPLE)) {
            cpuTotalTime = measureTime {
                repeat(frameCount) {
                    scene.mimicSkikoRender(surface, it * nanosPerFrame, width, height)
                    surface.flushAndSubmit(false)
                    gpuTotalTime += measureTime {
                        graphicsContext?.awaitGPUCompletion()
                    }
                    yieldEventLoop()
                }
            }
            cpuTotalTime -= gpuTotalTime
        }

        val frames = MutableList(frameCount) {
            BenchmarkFrame(Duration.INFINITE, Duration.INFINITE)
        }

        if (Config.isModeEnabled(Mode.VSYNC_EMULATION)) {
            var nextVSync = Duration.ZERO
            var missedFrames = 0;

            runGC()

            val start = markNow()

            repeat(frameCount) {
                val frameStart = start + nextVSync

                scene.mimicSkikoRender(surface, it * nextVSync.inWholeNanoseconds, width, height)
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

                yieldEventLoop()
            }
        }

        return BenchmarkResult(
            name,
            nanosPerFrame.nanoseconds,
            BenchmarkConditions(frameCount, warmupCount),
            FrameInfo(cpuTotalTime / frameCount, gpuTotalTime / frameCount),
            frames,
        )
    } finally {
        scene.close()
        surface.close()
        runGC() // cleanup for next benchmarks
    }
}

private val pictureRecorder = PictureRecorder()

/**
 * Mimic Skiko render logic from
 * https://github.com/JetBrains/skiko/blob/eb1f04ec99d50ff0bdb2f592fdf49711a9251aa7/skiko/src/awtMain/kotlin/org/jetbrains/skiko/SkiaLayer.awt.kt#L531
 *
 * This is a simplified logic and it still can differ from the real cases:
 * - Rendering into an intermediate picture. Benchmarks can show incorrect results without it.
 *   For example, we had a case when they showed an improvement by 10%,
 *   but there was a regression by 10%
 * - Clearing the canvas each frame
 *
 * Beware that this logic can be changed in some new version of Skiko.
 * If Skiko stops using `picture`, we need to remove it here too.
 */
@OptIn(InternalComposeUiApi::class)
fun ComposeScene.mimicSkikoRender(surface: Surface, time: Long, width: Int, height: Int) {
    val pictureCanvas = pictureRecorder.beginRecording(Rect(0f, 0f, width.toFloat(), height.toFloat()))
    render(pictureCanvas.asComposeCanvas(), time)
    val picture = pictureRecorder.finishRecordingAsPicture()

    surface.canvas.clear(Color.TRANSPARENT)
    surface.canvas.drawPicture(picture)
    picture.close()
}
