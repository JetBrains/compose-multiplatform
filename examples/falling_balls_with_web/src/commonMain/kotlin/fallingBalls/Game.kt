package org.jetbrains.compose.demo.falling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.common.ui.unit.IntSize
import org.jetbrains.compose.common.core.graphics.Color
import kotlin.random.Random

private fun Color.Companion.random() =
    Color((0..255).random(), (0..255).random(), (0..255).random())

abstract class Game {
    internal var previousTime: Long = Long.MAX_VALUE
    private var startTime = 0L

    var size by mutableStateOf(IntSize(0, 0))

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

    abstract fun now(): Long

    fun togglePause() {
        paused = !paused
        previousTime = Long.MAX_VALUE
    }

    fun start() {
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
        val dt = (nanos - previousTime).coerceAtLeast(0)
        previousTime = nanos
        elapsed = nanos - startTime
        pieces.forEach { it.update(dt) }
    }
}
