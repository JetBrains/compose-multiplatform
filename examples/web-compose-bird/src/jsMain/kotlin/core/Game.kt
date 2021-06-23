package core

import androidx.compose.runtime.State
import data.GameFrame

interface Game {
    val gameFrame: State<GameFrame>
    fun step()
    fun moveBirdUp()
}