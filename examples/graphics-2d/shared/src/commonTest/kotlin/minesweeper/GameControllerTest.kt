package minesweeper

import kotlin.test.*

class GameControllerTest {
    @Test
    fun testToString() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        assertEquals("   \n 11\n 1*", game.toString())
        game.toggleFlag(game.cellAt(0, 1)!!)
        assertEquals(" ! \n 11\n 1*", game.toString())
    }

    @Test
    fun testCellAt() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        assertNotNull(game.cellAt(0, 0))
        assertNull(game.cellAt(-1, 1))
        assertNull(game.cellAt(1, -1))
        assertNull(game.cellAt(100, 100))
        assertTrue(game.cellAt(2, 2)!!.hasBomb)
    }

    @Test
    fun testDefaultCreated() {
        val game = GameController(GameSettings(4, 5, 7))
        assertEquals(5, game.columns)
        assertEquals(4, game.rows)
        assertEquals(7, game.bombs)
        assertEquals(13, game.cellsToOpen)
        assertEquals(0, game.flagsSet)
        assertFalse(game.finished)
        assertFalse(game.running)

        var cellsWithBombs = 0
        for (row in 0..3) {
            for (column in 0..4) {
                val cell = game.cellAt(row, column)!!
                assertFalse(cell.isOpened)
                assertFalse(cell.isFlagged)
                if (cell.hasBomb) {
                    cellsWithBombs += 1
                }
            }
        }

        assertEquals(7, cellsWithBombs)
    }

    @Test
    fun testCellOpen() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        assertEquals(8, game.cellsToOpen)
        assertFalse(game.cellAt(1, 1)!!.isOpened, "Cell is opened before game start")
        game.openCell(game.cellAt(1, 1)!!)
        assertEquals(7, game.cellsToOpen)
        assertTrue(game.cellAt(1, 1)!!.isOpened, "Cell was not opened after click")
    }

    @Test
    fun testCellFlagged() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        assertFalse(game.cellAt(1, 1)!!.isFlagged, "Cell is flagged before game start")
        assertEquals(0, game.flagsSet)
        game.toggleFlag(game.cellAt(1, 1)!!)
        assertTrue(game.cellAt(1, 1)!!.isFlagged, "Cell was not flagged after click")
        assertEquals(1, game.flagsSet)
        game.toggleFlag(game.cellAt(1, 1)!!)
        assertFalse(game.cellAt(1, 1)!!.isFlagged, "Flag was not dropped after click")
        assertEquals(0, game.flagsSet)
    }

    @Test
    fun testCellCanNotBeOpenedWhenFlagged() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        game.toggleFlag(game.cellAt(1, 1)!!)
        game.openCell(game.cellAt(1, 1)!!)
        assertFalse(game.cellAt(1, 1)!!.isOpened, "Flagged cell was opened")
        game.toggleFlag(game.cellAt(1, 1)!!)
        assertFalse(game.cellAt(1, 1)!!.isOpened, "Cell is opened while setting flag")
        game.openCell(game.cellAt(1, 1)!!)
        assertTrue(game.cellAt(1, 1)!!.isOpened, "Cell was not opened after flag dropped")
    }

    @Test
    fun testCellOpenWithCascade() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        game.openCell(game.cellAt(0, 0)!!)

        // Click on first cell must open all, except cell with a bomb
        for (row in 0..2) {
            for (column in 0..2) {
                if (row != 2 || column != 2) {
                    assertTrue(game.cellAt(row, column)!!.isOpened, "Cell at ($row, $column) was not opened in cascade")
                }
            }
        }
        assertFalse(game.cellAt(2, 2)!!.isOpened, "Cell with bomb was opened in cascade")
    }


    @Test
    fun testCellOpenWithSeeker() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        game.toggleFlag(game.cellAt(2, 2)!!)
        game.openNotFlaggedNeighbors(game.cellAt(1, 1)!!)
        assertFalse(game.cellAt(1, 1)!!.isOpened)

        game.openCell(game.cellAt(1, 1)!!)
        game.openNotFlaggedNeighbors(game.cellAt(1, 1)!!)

        for (row in 0..2) {
            for (column in 0..2) {
                if (row != 2 || column != 2) {
                    assertTrue(game.cellAt(row, column)!!.isOpened, "Cell at ($row, $column) was not opened while seek")
                }
            }
        }
        assertFalse(game.cellAt(2, 2)!!.isOpened, "Cell with bomb was opened in cascade")
        assertTrue(game.cellAt(2, 2)!!.isFlagged, "Flag was dropped while seek")
    }

    @Test
    fun testGameStartsAtFirstOpen() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        assertFalse(game.running, "Game is running before first click")
        game.openCell(game.cellAt(1, 1)!!)
        assertTrue(game.running, "Game not started after first click")
    }

    @Test
    fun testGameStartsAtFirstFlagged() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        assertFalse(game.running, "Game is running before first click")
        game.toggleFlag(game.cellAt(1, 1)!!)
        assertTrue(game.running, "Game not started after first click")
    }

    @Test
    fun testGameStopsAtBombClicked() {
        var onLoseCalledTimes = 0
        var onWinCalledTimes = 0
        val onLose = { onLoseCalledTimes += 1 }
        val onWin = { onWinCalledTimes += 1 }

        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)), onWin, onLose)
        game.openCell(game.cellAt(1, 1)!!)
        game.openCell(game.cellAt(2, 2)!!)

        assertFalse(game.running, "Game not stopped after bomb click")
        assertTrue(game.finished, "Game not stopped after bomb click")
        assertEquals(1, onLoseCalledTimes)
        assertEquals(0, onWinCalledTimes)
    }

    @Test
    fun testGameStopsAtAllCellsOpened() {
        var onLoseCalledTimes = 0
        var onWinCalledTimes = 0
        val onLose = { onLoseCalledTimes += 1 }
        val onWin = { onWinCalledTimes += 1 }

        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)), onWin, onLose)
        for (row in 0..2) {
            for (column in 0..2) {
                if (row != 2 || column != 2) {
                    game.openCell(game.cellAt(row, column)!!)
                }
            }
        }

        assertFalse(game.running, "Game not stopped after all cells are opened")
        assertTrue(game.finished, "Game not stopped after all cells are opened")
        assertEquals(0, onLoseCalledTimes)
        assertEquals(1, onWinCalledTimes)
    }

    @Test
    fun testAllBombsAreFlaggedAfterWin() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        for (row in 0..2) {
            for (column in 0..2) {
                if (row != 2 || column != 2) {
                    game.openCell(game.cellAt(row, column)!!)
                }
            }
        }

        assertTrue(game.cellAt(2, 2)!!.isFlagged, "Cell with bomb was not flagged after win")
    }

    @Test
    fun testAllBombsAreOpenedAfterLose() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2), Pair(1, 1)))
        game.openCell(game.cellAt(0, 0)!!)
        game.openCell(game.cellAt(1, 1)!!)

        assertTrue(game.cellAt(2, 2)!!.isOpened, "Cell with bomb was not opened after lose")
        assertTrue(game.cellAt(1, 1)!!.isOpened, "Cell with bomb was not opened after lose")
    }

    @Test
    fun testCanNotClickWhenGameIsFinished() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        game.openCell(game.cellAt(2, 1)!!)
        game.openCell(game.cellAt(2, 2)!!)
        game.openCell(game.cellAt(1, 1)!!)
        game.toggleFlag(game.cellAt(0, 0)!!)

        assertFalse(game.cellAt(0, 0)!!.isFlagged, "Cell was flagged after game end")
        assertFalse(game.cellAt(1, 1)!!.isOpened, "Cell was opened after game end")
    }

    @Test
    fun canNotLoseAtFirstClick() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(0, 0), Pair(1, 1)))
        game.openCell(game.cellAt(0, 0)!!)
        assertFalse(game.cellAt(0, 0)!!.hasBomb, "First cell has bomb")
        assertTrue(game.cellAt(0, 0)!!.isOpened, "First cell was not opened")
        assertEquals(2, game.bombs, "Bomb count changed after first click")

        game.openCell(game.cellAt(1, 1)!!)
        assertTrue(game.cellAt(1, 1)!!.hasBomb, "Next cell has not bomb")
    }

    @Test
    fun testTimer() {
        val game = GameController(rows = 3, columns = 3, mines = listOf(Pair(2, 2)))
        game.onTimeTick(2000L)
        assertEquals(0, game.seconds, "Timer started before game run")
        game.openCell(game.cellAt(1, 1)!!)
        game.onTimeTick(5000L)
        assertEquals(3, game.seconds, "Wrong seconds at timer after start")
        game.onTimeTick(6000L)
        game.openCell(game.cellAt(1, 1)!!)
        assertEquals(4, game.seconds, "Wrong seconds at timer after opened cell")
        game.onTimeTick(8000L)
        game.openCell(game.cellAt(2, 2)!!)
        game.onTimeTick(10000L)
        assertEquals(6, game.seconds, "Wrong seconds at timer after stop")
    }
}