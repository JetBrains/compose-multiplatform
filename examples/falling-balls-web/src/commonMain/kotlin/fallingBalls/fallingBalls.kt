package org.jetbrains.compose.demo.falling.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import org.jetbrains.compose.demo.falling.Game
import org.jetbrains.compose.common.material.Text
import org.jetbrains.compose.common.foundation.layout.Column
import org.jetbrains.compose.common.material.Slider
import org.jetbrains.compose.common.foundation.layout.Row
import org.jetbrains.compose.common.foundation.layout.Box
import org.jetbrains.compose.common.material.Button
import org.jetbrains.compose.common.ui.Modifier
import org.jetbrains.compose.common.ui.unit.em
import org.jetbrains.compose.common.ui.unit.dp
import org.jetbrains.compose.common.foundation.layout.offset
import org.jetbrains.compose.common.foundation.layout.width
import org.jetbrains.compose.common.ui.layout.onSizeChanged
import org.jetbrains.compose.common.ui.background
import org.jetbrains.compose.common.foundation.border
import org.jetbrains.compose.common.ui.size
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.foundation.layout.fillMaxHeight
import org.jetbrains.compose.common.foundation.layout.fillMaxWidth

@Composable
fun fallingBalls(game: Game) {
    Column(Modifier.fillMaxWidth().fillMaxHeight(1f)) {
        Box() {
            Text(
                "Catch balls!${if (game.finished) " Game over!" else ""}",
                size = 1.8f.em,
                color = Color(218, 120, 91)
            )
        }
        Box() {
            Text(
                "Score: ${game.score} Time: ${game.elapsed / 1_000_000} Blocks: ${game.numBlocks}",
                size = 1.8f.em
            )
        }
        Row() {
            if (!game.started) {
                Slider(
                    value = game.numBlocks / 20f,
                    onValueChange = { game.numBlocks = (it * 20f).toInt().coerceAtLeast(1) },
                    modifier = Modifier.width(100.dp)
                )
            }
            Button(
                Modifier
                    .border(2.dp, Color(255, 215, 0))
                    .background(Color.Yellow),
                onClick = {
                    game.started = !game.started
                    if (game.started) {
                        game.start()
                    }
                }
            ) {
                Text(if (game.started) "Stop" else "Start", size = 2f.em)
            }
            if (game.started) {
                Button(
                    Modifier
                        .offset(10.dp, 0.dp)
                        .border(2.dp, Color(255, 215, 0))
                        .background(Color.Yellow),
                    onClick = {
                        game.togglePause()
                    }
                ) {
                    Text(if (game.paused) "Resume" else "Pause", size = 2f.em)
                }
            }
        }

        if (game.started) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(1f)
                    .size(game.width.dp, game.height.dp)
                    .onSizeChanged {
                        game.size = it
                    }
            ) {
                game.pieces.forEachIndexed { index, piece ->
                    Piece(index, piece)
                }
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
