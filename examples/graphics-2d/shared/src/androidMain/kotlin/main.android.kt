import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import fallingballs.Time

object AndroidTime : Time {
    override fun now(): Long = System.nanoTime()
}

@Composable
fun MainView() {
    Graphics2D()
}
