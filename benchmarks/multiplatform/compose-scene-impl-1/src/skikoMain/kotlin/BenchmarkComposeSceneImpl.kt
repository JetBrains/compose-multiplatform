import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.IntSize

/**
 * Implementation of [BenchmarkComposeScene] for Compose Multiplatform versions before
 * the shared FrameRecomposer PR (compose-multiplatform-core#3012), i.e. ≤ 1.11.x.
 *
 * Uses [CanvasLayersComposeScene] and
 * [androidx.compose.ui.scene.ComposeScene.render] which are internal Compose UI API.
 */
@OptIn(InternalComposeUiApi::class)
class BenchmarkComposeSceneImpl(width: Int, height: Int) : BenchmarkComposeScene {
    private val scene = CanvasLayersComposeScene(size = IntSize(width, height))

    override fun setContent(content: @Composable () -> Unit) {
        scene.setContent(content)
    }

    override fun render(canvas: Canvas, nanoTime: Long) {
        scene.render(canvas, nanoTime)
    }

    override fun close() {
        scene.close()
    }
}

fun createBenchmarkComposeScene(width: Int, height: Int): BenchmarkComposeScene =
    BenchmarkComposeSceneImpl(width, height)
