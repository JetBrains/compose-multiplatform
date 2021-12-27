/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import kotlin.random.Random

interface Time {
    fun now(): Long
}

class Game(val time: Time) {
    private var previousTimeNanos: Long = Long.MAX_VALUE
    private val colors = arrayOf(
        Color.Red, Color.Blue, Color.Cyan,
        Color.Magenta, Color.Yellow, Color.Black
    )
    private var startTime = 0L

    var width by mutableStateOf(0.dp)
    var height by mutableStateOf(0.dp)

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
        previousTimeNanos = time.now()
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
        previousTimeNanos = time.now()
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