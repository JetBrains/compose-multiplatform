import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

object AndroidTime : Time {
    override fun now(): Long = System.nanoTime()
}

@Composable
fun MainView() {
    val game = remember { Game(AndroidTime) }
    FallingBalls(game)
}