package fallingballs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

class Game() {
    private val colors = arrayOf(
        Color.Red, Color.Blue, Color.Cyan,
        Color.Magenta, Color.Yellow, Color.Black
    )

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

    var numBlocks by mutableStateOf(5f)

    fun start() {
        clicked = 0
        started = true
        finished = false
        paused = false
        pieces.clear()
        repeat(numBlocks.toInt()) { index ->
            pieces.add(
                PieceData(
                    this,
                    index * 1.5f + 5f,
                    colors[index % colors.size]
                ).also { piece ->
                    piece.position = Random.nextDouble(0.0, 100.0).toFloat()
                })
        }
    }

    fun update(deltaTimeNanos: Long) {
        elapsed += deltaTimeNanos
        pieces.forEach { it.update(deltaTimeNanos) }
    }

    fun clicked(piece: PieceData) {
        score += piece.velocity.toInt()
        clicked++
        if (clicked == numBlocks.toInt()) {
            finished = true
        }
    }
}
