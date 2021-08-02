package core

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import data.GameFrame
import data.Tube
import kotlin.js.Date

/**
 * Game logic
 */
class ComposeBirdGame : Game {

    companion object {
        const val COLUMNS = 15
        const val ROWS = 9
        const val BIRD_COLUMN = 1
        private const val TUBES_START_FROM = (COLUMNS * 0.75).toInt()
        const val TOTAL_TUBES = 10
        private const val TUBE_HORIZONTAL_DISTANCE = 3
        private const val TUBE_VERTICAL_DISTANCE = 3
        private const val TUBE_WEIGHT = 500
        private const val BIRD_WEIGHT = 300
    }

    private val tubeGapRange = TUBE_VERTICAL_DISTANCE until ROWS
    private var tubeLastSteppedAt = 0.0
    private var birdLastSteppedAt = 0.0
    private var shouldMoveBirdUp = false

    private val _gameFrame: MutableState<GameFrame> by lazy {
        mutableStateOf(
            // First frame
            GameFrame(
                birdPos = ROWS / 2,
                tubes = buildLevel(),
                isGameOver = false,
                isGameWon = false,
                score = 0
            )
        )
    }

    /**
     * To build a random level
     */
    private fun buildLevel(): List<Tube> {
        return mutableListOf<Tube>().apply {
            var tubesAdded = 0
            var tubePosition = 0
            while (tubesAdded < TOTAL_TUBES) {
                if (tubePosition > TUBES_START_FROM && tubePosition % TUBE_HORIZONTAL_DISTANCE == 0) { // To give space to each tube
                    add(
                        Tube(
                            tubePosition,
                            buildRandomTube()
                        )
                    )
                    tubesAdded++
                }
                tubePosition++
            }
        }
    }


    /**
     * To build a random vertical tube/pipe
     */
    private fun buildRandomTube(): List<Boolean> {
        // creating a full tube
        val tube = mutableListOf<Boolean>().apply {
            repeat(ROWS) {
                add(true)
            }
        }

        // Adding gaps in random middle positions to make it two tubes.
        val gap1 = tubeGapRange.random()
        repeat(TUBE_VERTICAL_DISTANCE) { index ->
            tube[gap1 - index] = false
        }

        return tube
    }

    override val gameFrame: State<GameFrame> = _gameFrame

    override fun step() {
        update {
            val now = Date().getTime()

            // Stepping tube
            val tubeDiff = now - tubeLastSteppedAt
            val newTubes = if (tubeDiff > TUBE_WEIGHT) {
                tubeLastSteppedAt = now
                tubes.map {
                    it.copy(position = it.position - 1)
                }
            } else {
                tubes
            }

            // Stepping bird position
            val birdDiff = now - birdLastSteppedAt
            val newBirdPos = when {
                shouldMoveBirdUp -> {
                    birdLastSteppedAt = now
                    shouldMoveBirdUp = false
                    birdPos - 1 // move up
                }
                birdDiff > BIRD_WEIGHT -> {
                    birdLastSteppedAt = now
                    birdPos + 1 // move down
                }
                else -> {
                    birdPos
                }
            }

            val newScore = newTubes.filter { it.position < BIRD_COLUMN }.size // All passed tube
            val newIsGameWon = newScore >= TOTAL_TUBES // If all tubes passed

            // Checking if bird gone out
            val newIsGameOver = if (newBirdPos < 0 || newBirdPos >= ROWS || isCollidedWithTube(newBirdPos, tubes)) {
                true
            } else {
                isGameOver
            }

            copy(
                isGameOver = newIsGameOver,
                tubes = newTubes,
                birdPos = newBirdPos,
                score = newScore,
                isGameWon = newIsGameWon
            )
        }
    }

    /**
     * To check if the bird collided with the tube (collision-detection)
     */
    private fun isCollidedWithTube(newBirdPos: Int, tubes: List<Tube>): Boolean {
        val birdTube = tubes.find { it.position == BIRD_COLUMN }
        return birdTube?.coordinates?.get(newBirdPos) ?: false
    }

    override fun moveBirdUp() {
        shouldMoveBirdUp = true
    }

    private inline fun update(func: GameFrame.() -> GameFrame) {
        _gameFrame.value = _gameFrame.value.func()
    }
}