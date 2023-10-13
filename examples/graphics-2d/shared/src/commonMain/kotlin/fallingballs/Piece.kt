package fallingballs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Piece(index: Int, piece: PieceData) {
    val boxSize = 40.dp
    Box(
        Modifier
            .offset(boxSize * index * 5 / 3, piece.position.dp)
            .shadow(30.dp)
            .clip(CircleShape)
    ) {
        Box(
            Modifier
                .size(boxSize, boxSize)
                .background(if (piece.clicked) Color.Gray else piece.color)
                .clickable(onClick = { piece.click() })
        )
    }
}

data class PieceData(val game: Game, val velocity: Float, val color: Color) {
    var clicked by mutableStateOf(false)
    var position by mutableStateOf(0f)

    fun update(dt: Long) {
        if (clicked) return
        val delta = (dt / 1E8 * velocity).toFloat()
        position = if (position < game.height.value) position + delta else 0f
    }

    fun click() {
        if (!clicked && !game.paused) {
            clicked = true
            game.clicked(this)
        }
    }
}
