import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers

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

@OptIn(InternalComposeUiApi::class)
fun createBenchmarkComposeScene(width: Int, height: Int): BenchmarkComposeScene {
    registerSkikoComposeImplementation()
    return BenchmarkComposeSceneImpl(width, height)
}
