package org.jetbrains.compose.demo.falling

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import kotlin.random.Random

class Game {
    private var previousTimeNanos: Long = Long.MAX_VALUE
    private val colors = arrayOf(
        Color.Red, Color.Blue, Color.Cyan,
        Color.Magenta, Color.Yellow, Color.Black
    )
    private var startTime = 0L

    var size by mutableStateOf(Pair(0.dp, 0.dp))

    var pieces = mutableStateListOf<PieceData>()
        private set

    var elapsed by mutableStateOf(0L)
    var score by mutableStateOf(0)
    private var clicked by mutableStateOf(0)

    var started by mutableStateOf(false)
    var paused by mutableStateOf(false)
    var finished by mutableStateOf(false)

    var numBlocks by mutableStateOf(5)

    fun start() {
        previousTimeNanos = System.nanoTime()
        startTime = previousTimeNanos
        clicked = 0
        started = true
        finished = false
        paused = false
        pieces.clear()
        repeat(numBlocks) { index ->
            pieces.add(PieceData(this, index * 1.5f + 5f, colors[index % colors.size]).also { piece ->
                piece.position = Random.nextDouble(0.0, 100.0).toFloat()
            })
        }
    }

    fun togglePause() {
        paused = !paused
        previousTimeNanos = System.nanoTime()
    }

    fun update(nanos: Long) {
        val dt = (nanos - previousTimeNanos).coerceAtLeast(0)
        previousTimeNanos = nanos
        elapsed = nanos - startTime
        pieces.forEach { it.update(dt) }
    }

    fun clicked(piece: PieceData) {
        score += piece.velocity.toInt()
        clicked++
        if (clicked == numBlocks) {
            finished = true
        }
    }
}

@Composable
@Preview
fun FallingBallsGame() {
    val game = remember { Game() }
    val density = LocalDensity.current
    Column {
        Text(
            "Catch balls!${if (game.finished) " Game over!" else ""}",
            fontSize = 50.sp,
            color = Color(218, 120, 91)
        )
        Text("Score ${game.score} Time ${game.elapsed / 1_000_000} Blocks ${game.numBlocks}", fontSize = 35.sp)
        Row {
            if (!game.started) {
                Slider(
                    value = game.numBlocks / 20f,
                    onValueChange = { game.numBlocks = (it * 20f).toInt().coerceAtLeast(1) },
                    modifier = Modifier.width(100.dp)
                )
            }
            Button(onClick = {
                game.started = !game.started
                if (game.started) {
                    game.start()
                }
            }) {
                Text(if (game.started) "Stop" else "Start", fontSize = 40.sp)
            }
            if (game.started) {
                Spacer(Modifier.padding(5.dp))
                Button(onClick = {
                    game.togglePause()
                }) {
                    Text(if (game.paused) "Resume" else "Pause", fontSize = 40.sp)
                }
            }
        }
        if (game.started) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .onSizeChanged {
                    with(density) {
                        game.size = it.width.toDp() to it.height.toDp()
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

