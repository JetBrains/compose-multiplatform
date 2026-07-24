import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Canvas

/**
 * Fake (no-op) implementation of [BenchmarkComposeScene] for Android.
 */
class BenchmarkComposeSceneImpl(width: Int, height: Int) : BenchmarkComposeScene {
    override fun setContent(content: @Composable () -> Unit) {}
    override fun render(canvas: Canvas, nanoTime: Long) {}
    override fun close() {}
}

fun createBenchmarkComposeScene(width: Int, height: Int): BenchmarkComposeScene =
    BenchmarkComposeSceneImpl(width, height)
