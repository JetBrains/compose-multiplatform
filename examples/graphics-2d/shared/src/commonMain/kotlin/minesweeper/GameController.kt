package minesweeper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

class GameController(
    private val options: GameSettings,
    private val onWin: (() -> Unit)? = null,
    private val onLose: (() -> Unit)? = null
) {
    /** Number of rows in current board */
    val rows: Int
        get() = options.rows

    /** Number of columns in current board */
    val columns: Int
        get() = options.columns

    /** Number of bombs in current board */
    val bombs: Int
        get() = options.mines

    /** True if current game has started, false if game is finished or until first cell is opened or flagged */
    var running by mutableStateOf(false)
        private set

    /** True if game is ended (win or lose) */
    var finished by mutableStateOf(false)
        private set

    /** Total number of flags set on cells, used for calculation of number of remaining bombs */
    var flagsSet by mutableStateOf(0)
        private set

    /** Number of remaining cells */
    var cellsToOpen by mutableStateOf(options.rows * options.columns - options.mines)
        private set

    /** Game timer, increments every second while game is running */
    var seconds by mutableStateOf(0)
        private set

    /** Global monotonic time, updated with [onTimeTick] */
    private var time = 0L

    /** The time when user starts the game by opening or flagging any cell */
    private var startTime = 0L

    /** The game board of size (rows * columns) */
    private val cells = Array(options.rows) { row ->
        Array(options.columns) { column ->
            Cell(row, column)
        }
    }

    private var isFirstOpenedCell = true

    init {
        // Put [options.mines] bombs on random positions
        for (i in 1..options.mines) {
            putBomb()
        }
    }

    /**
     * Constructor with predefined positions of mines, used for testing purposes
     */
    constructor(
        rows: Int,
        columns: Int,
        mines: Collection<Pair<Int, Int>>,
        onWin: (() -> Unit)? = null,
        onLose: (() -> Unit)? = null
    ) : this(GameSettings(rows, columns, mines.size), onWin, onLose) {
        for (row in cells) {
            for (cell in row) {
                cell.hasBomb = false
                cell.bombsNear = 0
            }
        }

        for ((row, column) in mines) {
            cellAt(row, column)?.apply {
                hasBomb = true
                neighborsOf(this).forEach {
                    it.bombsNear += 1
                }
            }
        }
    }

    /**
     * Get cell at given position, or null if any index is out of bounds
     */
    fun cellAt(row: Int, column: Int) = cells.getOrNull(row)?.getOrNull(column)

    /**
     * Open given cell:
     * - If cell is opened or flagged, or game is finished, does nothing
     * - If cell contains bomb, opens it and stops the game (lose)
     * - If cell has no bombs around int, recursively opens cells around current
     *
     * When cell opens, decrements [cellsToOpen],
     * if it becomes zero, stops the game (win). First call starts the game.
     *
     * @param cell Cell to open, **must** belong to current game board
     */
    fun openCell(cell: Cell) {
        if (finished || cell.isOpened || cell.isFlagged) return
        if (!running) {
            startGame()
        }

        cell.isOpened = true
        if (cell.hasBomb) {
            if (isFirstOpenedCell) {
                ensureNotLoseAtFirstClick(cell)
            } else {
                lose()
                return
            }
        }
        isFirstOpenedCell = false

        cellsToOpen -= 1
        if (cellsToOpen == 0) {
            win()
            return
        }

        if (cell.bombsNear == 0) {
            neighborsOf(cell).forEach {
                openCell(it)
            }
        }
    }

    /**
     * Sets or drops flag on given [cell]. Flagged cell can not be opened until flag drop
     * If game is finished, or cell is opened, does nothing. First call starts the game.
     *
     * Setting flag increments [flagsSet], dropping - decrements
     *
     * @param cell Cell to toggle flag, **must** belong to current game board
     */
    fun toggleFlag(cell: Cell) {
        if (finished || cell.isOpened) return
        if (!running) {
            startGame()
        }

        cell.isFlagged = !cell.isFlagged
        if (cell.isFlagged) {
            flagsSet += 1
        } else {
            flagsSet -= 1
        }
    }

    /**
     * Mine seeker functionality
     *
     * When called on opened cell with at least one bomb near it, and if number of flags around cell
     * is the same as number of bombs, opens all cells around given with [openCell].
     *
     * If game is finished, or cell does not meet the requirements above, does nothing
     *
     * @param cell Cell to toggle flag, **must** belong to current game board
     */
    fun openNotFlaggedNeighbors(cell: Cell) {
        if (finished || !cell.isOpened || cell.bombsNear == 0) return

        val neighbors = neighborsOf(cell)
        val flagsNear = neighbors.count() { it.isFlagged }
        if (cell.bombsNear == flagsNear) {
            neighbors.forEach { openCell(it) }
        }
    }

    /**
     * Provides current **monotonic** time to game
     * Should be called in timer loop
     *
     * @param timeMillis Current time in milliseconds
     */
    fun onTimeTick(timeMillis: Long) {
        time = timeMillis
        if (running) {
            seconds = ((time - startTime) / 1000L).toInt()
        }
    }

    private fun putBomb() {
        var cell: Cell
        do {
            // This strategy can create infinite loop, but for simplicity we can assume
            // that mine count is small enough
            val random = Random.nextInt(options.rows * options.columns)
            cell = cells[random / columns][random % columns]
        } while (cell.hasBomb)

        cell.hasBomb = true
        neighborsOf(cell).forEach {
            it.bombsNear += 1
        }
    }

    private fun flagAllBombs() {
        cells.forEach { row ->
            row.forEach { cell ->
                if (!cell.isOpened) {
                    cell.isFlagged = true
                }
            }
        }
    }

    private fun openAllBombs() {
        cells.forEach { row ->
            row.forEach { cell ->
                if (cell.hasBomb && !cell.isFlagged) {
                    cell.isOpened = true
                }
            }
        }
    }

    private fun neighborsOf(cell: Cell): List<Cell> = neighborsOf(cell.row, cell.column)

    private fun neighborsOf(row: Int, column: Int): List<Cell> {
        val result = mutableListOf<Cell>()
        cellAt(row - 1, column - 1)?.let { result.add(it) }
        cellAt(row - 1, column)?.let { result.add(it) }
        cellAt(row - 1, column + 1)?.let { result.add(it) }
        cellAt(row, column - 1)?.let { result.add(it) }
        cellAt(row, column + 1)?.let { result.add(it) }
        cellAt(row + 1, column - 1)?.let { result.add(it) }
        cellAt(row + 1, column)?.let { result.add(it) }
        cellAt(row + 1, column + 1)?.let { result.add(it) }

        return result
    }

    private fun win() {
        endGame()
        flagAllBombs()
        onWin?.invoke()
    }

    private fun lose() {
        endGame()
        openAllBombs()
        onLose?.invoke()
    }

    private fun endGame() {
        finished = true
        running = false
    }

    private fun startGame() {
        if (!finished) {
            seconds = 0
            startTime = time
            running = true
        }
    }

    private fun ensureNotLoseAtFirstClick(firstCell: Cell) {
        putBomb()
        firstCell.hasBomb = false
        neighborsOf(firstCell).forEach {
            it.bombsNear -= 1
        }
    }

    override fun toString(): String {
        return buildString {
            for (row in cells) {
                for (cell in row) {
                    if (cell.hasBomb) {
                        append('*')
                    } else if (cell.isFlagged) {
                        append('!')
                    } else if (cell.bombsNear > 0) {
                        append(cell.bombsNear)
                    } else {
                        append(' ')
                    }
                }
                append('\n')
            }
            deleteAt(length - 1)
        }
    }
}

data class GameSettings(val rows: Int, val columns: Int, val mines: Int)

class Cell(val row: Int, val column: Int) {
    var hasBomb = false
    var isOpened by mutableStateOf(false)
    var isFlagged by mutableStateOf(false)
    var bombsNear = 0
}
