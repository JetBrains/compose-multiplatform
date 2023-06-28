import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.unit.Constraints
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

const val width = 640
const val height = 480

const val nanosPerSecond = 1E9.toLong()
const val nanosPerFrame = (0.16 * nanosPerSecond).toLong()

@OptIn(ExperimentalTime::class)
fun measureComposable(
    frameCount: Int = 1000,
    content: @Composable () -> Unit
): Duration {
    val scene = ComposeScene()
    try {
        scene.setContent(content)
        scene.constraints = Constraints.fixed(width, height)
        val surface = org.jetbrains.skia.Surface.makeNull(width, height)
        return kotlin.time.measureTime {
            var nanoTime = 0L
            repeat(frameCount) {
                scene.render(surface.canvas, nanoTime)
                nanoTime += nanosPerFrame
            }
        }
    } finally {
        scene.close()
    }
}
