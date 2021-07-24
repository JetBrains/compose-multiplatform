package core

import androidx.compose.runtime.State
import data.GameFrame

/**
 * A generic game interface
 */
interface Game {
    val gameFrame: State<GameFrame>
    fun step()
    fun moveBirdUp()
}