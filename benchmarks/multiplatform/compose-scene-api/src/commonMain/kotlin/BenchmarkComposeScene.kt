import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Canvas

/**
 * An abstraction over Compose internal scene API used by benchmarks.
 *
 * Different Compose versions may change the internal API surface
 * (e.g. [androidx.compose.ui.scene.CanvasLayersComposeScene] or
 * [androidx.compose.ui.scene.ComposeScene.render]).
 * Implementations of this interface encapsulate those version-specific
 * details so the benchmark code depends only on the stable contract
 * defined here.
 */
interface BenchmarkComposeScene {
    fun setContent(content: @Composable () -> Unit)
    fun render(canvas: Canvas, nanoTime: Long)
    fun close()
}
