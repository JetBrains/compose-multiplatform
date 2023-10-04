import androidx.compose.runtime.Composable
import fallingballs.Time

object AndroidTime : Time {
    override fun now(): Long = System.nanoTime()
}

@Composable
fun MainView() {
    Graphics2D()
}
