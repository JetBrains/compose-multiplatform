import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.unit.Constraints
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

const val width = 640
const val height = 480

const val nanosPerSecond = 1E9.toLong()
const val nanosPerFrame = (0.16 * nanosPerSecond).toLong()

@OptIn(ExperimentalTime::class)
fun measureComposable(
    frameCount: Int = 100,
    content: @Composable () -> Unit
): Duration {

    val scene = ComposeScene()
    val times = try {
        scene.setContent(content)
        scene.constraints = Constraints.fixed(width, height)
        val surface = org.jetbrains.skia.Surface.makeNull(width, height)

        (1..200).map {
            kotlin.time.measureTime {
                var nanoTime = 0L
                repeat(frameCount) {
                    scene.render(surface.canvas, nanoTime)
                    nanoTime += nanosPerFrame
                }
            }.inWholeMilliseconds
        }
    } finally {
        scene.close()
    }
    return times.median().toDuration(DurationUnit.MILLISECONDS)
}

private fun List<Long>.median() = sorted()[size/ 2]