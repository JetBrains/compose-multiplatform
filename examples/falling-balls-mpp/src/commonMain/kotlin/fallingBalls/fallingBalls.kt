/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.material.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Slider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun fallingBalls(game: Game) {
    Column(Modifier.fillMaxWidth().fillMaxHeight(1f)) {
        Box() {
            Text(
                "Catch balls!${if (game.finished) " Game over!" else ""}",
                fontSize = 1.8f.em,
                color = Color(218, 120, 91)
            )
        }
        Box() {
            Text(
                "Score: ${game.score} Time: ${game.elapsed / 1_000_000} Blocks: ${game.numBlocks}",
                fontSize = 1.8f.em
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
                    }
                ) {
                    Text(if (game.paused) "Resume" else "Pause", fontSize = 2f.em)
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
