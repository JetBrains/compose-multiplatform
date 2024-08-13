import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

const val width = 640
const val height = 480

const val nanosPerSecond = 1E9.toLong()
const val nanosPerFrame = (0.16 * nanosPerSecond).toLong()

@OptIn(ExperimentalTime::class, InternalComposeUiApi::class)
fun measureComposable(
    frameCount: Int = 1000,
    content: @Composable () -> Unit
): Duration {
    val scene = MultiLayerComposeScene(size = IntSize(width, height))
    try {
        scene.setContent(content)
        val surface = org.jetbrains.skia.Surface.makeNull(width, height)
        val canvas = surface.canvas.asComposeCanvas()
        return kotlin.time.measureTime {
            var nanoTime = 0L
            repeat(frameCount) {
                scene.render(canvas, nanoTime)
                nanoTime += nanosPerFrame
            }
        }
    } finally {
        scene.close()
    }
}
