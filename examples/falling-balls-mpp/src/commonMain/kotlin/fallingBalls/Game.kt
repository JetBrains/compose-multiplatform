/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private fun Color.Companion.random() =
    Color((0..255).random(), (0..255).random(), (0..255).random())

class Game(width: Int, height: Int) {
    private var previousTime: Long = Long.MAX_VALUE
    private var startTime = 0L

    var size by mutableStateOf(IntSize(width, height))

    var width: Int
        get() = size.width
        set(newWidth: Int) {
            size = IntSize(newWidth, height)
        }

    var height: Int
        get() = size.height
        set(newHeight) {
            size = IntSize(width, newHeight)
        }

    var pieces = mutableStateListOf<PieceData>()
        private set

    var elapsed by mutableStateOf(0L)
    var score by mutableStateOf(0)
    var clicked by mutableStateOf(0)

    var started by mutableStateOf(false)
    var paused by mutableStateOf(false)
    var finished by mutableStateOf(false)

    var numBlocks by mutableStateOf(5)

    fun isInBoundaries(pieceData: PieceData) = pieceData.position < size.height

    fun togglePause() {
        paused = !paused
        previousTime = Long.MAX_VALUE
    }

    fun start() {
        println("Game.start")
        previousTime = now()
        startTime = previousTime
        clicked = 0
        started = true
        finished = false
        paused = false
        pieces.clear()
        repeat(numBlocks) { index ->
            pieces.add(
                PieceData(this, index * 1.5f + 5f, Color.random()).also { piece ->
                    piece.position = Random.nextDouble(0.0, 100.0).toFloat()
                }
            )
        }
    }

    fun update(nanos: Long) {
        println("Game.update: $nanos")
        val dt = (nanos - previousTime).coerceAtLeast(0)
        previousTime = nanos
        elapsed = nanos - startTime
        pieces.forEach { it.update(dt) }
    }
}
