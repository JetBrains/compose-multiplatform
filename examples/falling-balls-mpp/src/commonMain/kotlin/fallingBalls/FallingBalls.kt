import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.jetbrains.skiko.KotlinBackend

@Composable
fun FallingBalls(game: Game) {
    val density = LocalDensity.current
    Column {
        Text(
            "Catch balls!${if (game.finished) " Game over!" else ""}",
            fontSize = 1.8f.em,
            color = Color(218, 120, 91)
        )
        Text("Score: ${game.score} Time: ${game.elapsed / 1_000_000} Blocks: ${game.numBlocks}", fontSize = 1.8f.em)
        Row {
            if (!game.started) {
                Slider(
                    value = game.numBlocks / 20f,
                    onValueChange = { game.numBlocks = (it * 20f).toInt().coerceAtLeast(1) },
                    modifier = Modifier.width(100.dp)
                )
            }
            Button(
                modifier = Modifier
                    .border(2.dp, Color(255, 215, 0))
                    .background(Color.Yellow),
                onClick = {
                    game.started = !game.started
                    if (game.started) {
                        game.start()
                    }
                }
            ) {
                Text(if (game.started) "Stop" else "Start", fontSize = 2f.em)
            }
            if (game.started) {
                Button(
                    modifier = Modifier
                        .offset(10.dp, 0.dp)
                        .border(2.dp, Color(255, 215, 0))
                        .background(Color.Yellow),
                    onClick = {
                    game.togglePause()
                }) {
                    Text(if (game.paused) "Resume" else "Pause", fontSize = 2f.em)
                }
            }
        }
        if (game.started) {
            Box(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(1f)
                .onSizeChanged {
                    with(density) {
                        game.width = it.width.toDp()
                        game.height = it.height.toDp()
                    }
                }
            ) {
                game.pieces.forEachIndexed { index, piece -> Piece(index, piece) }
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos {
                    if (game.started && !game.paused && !game.finished)
                        game.update(it)
                }
            }
        }
    }
}

