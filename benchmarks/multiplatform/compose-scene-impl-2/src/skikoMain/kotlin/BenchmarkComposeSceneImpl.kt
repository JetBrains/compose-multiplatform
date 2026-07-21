import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers

/**
 * Implementation of [BenchmarkComposeScene] for Compose Multiplatform versions after
 * the shared FrameRecomposer PR (compose-multiplatform-core#3012), i.e. ≥ 1.12.0.
 *
 * Uses [FrameRecomposer] + [CanvasLayersComposeScene] with explicit
 * [FrameRecomposer.performFrame], [ComposeScene.measureAndLayout], and [ComposeScene.draw]
 * phase calls which replaced the old single [ComposeScene.render] method.
 */
@OptIn(InternalComposeUiApi::class)
class BenchmarkComposeSceneImpl(width: Int, height: Int) : BenchmarkComposeScene {
    private val frameRecomposer = FrameRecomposer(Dispatchers.Unconfined)
    private val scene = CanvasLayersComposeScene(
        frameRecomposer = frameRecomposer,
        size = IntSize(width, height)
    )

    override fun setContent(content: @Composable () -> Unit) {
        scene.setContent(content = content)
    }

    override fun render(canvas: Canvas, nanoTime: Long) {
        frameRecomposer.performFrame(nanoTime)
        scene.measureAndLayout()
        scene.draw(canvas)
    }

    override fun close() {
        scene.close()
        frameRecomposer.close()
    }
}

fun createBenchmarkComposeScene(width: Int, height: Int): BenchmarkComposeScene =
    BenchmarkComposeSceneImpl(width, height)
