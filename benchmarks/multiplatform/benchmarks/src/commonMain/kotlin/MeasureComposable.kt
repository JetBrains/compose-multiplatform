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
        }

        runGC()

        var renderTime = Duration.ZERO
        var gpuTime = Duration.ZERO
        if (Args.isModeEnabled(Mode.CPU)) {
            renderTime = measureTime {
                repeat(frameCount) {
                    scene.mimicSkikoRender(surface, it * nanosPerFrame, width, height)
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
            }
        }

        return BenchmarkResult(
            nanosPerFrame.nanoseconds,
            renderTime,
            frames
        )
    } finally {
        scene.close()
        surface.close()
        runGC() // cleanup for next benchmarks
    }
}

private val pictureRecorder = PictureRecorder()

/**
 * Mimic Skiko render logic from https://github.com/JetBrains/skiko/blob/eb1f04ec99d50ff0bdb2f592fdf49711a9251aa7/skiko/src/awtMain/kotlin/org/jetbrains/skiko/SkiaLayer.awt.kt#L531
 *
 * This is very simplified logic, and it still can differ from the real cases.
 *
 * Though one main function - rendering into picture - was affecting performance.
 *
 * Benchmarks showed an improvement by 10%, but there was a regression by 10%.
 *
 * Beware that this logic can be changed in some new version of Skiko.
 *
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
