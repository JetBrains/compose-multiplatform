package minesweeper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun BoardView(game: GameController) = with(GameStyles) {
    Column {
        for (row in 0 until game.rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (column in 0 until game.columns) {
                    val cell = game.cellAt(row, column)!!
                    Box(
                        modifier = Modifier.size(cellSize, cellSize)
                            .background(color = getCellColor(cell))
                            .border(width = cellBorderWidth, color = borderColor)
                            .gameInteraction(
                                open = { game.openCell(cell) },
                                flag = { game.toggleFlag(cell) },
                                seek = { game.openNotFlaggedNeighbors(cell) }
                            )
                    ) {
                        if (cell.isOpened) {
                            if (cell.hasBomb) {
                                Mine()
                            } else if (cell.bombsNear > 0) {
                                OpenedCell(cell)
                            }
                        } else if (cell.isFlagged) {
                            Flag()
                        }
                    }
                }
            }
        }
    }
}

private fun GameStyles.getCellColor(cell: Cell): Color =
    if (cell.isOpened) openedCellColor else closedCellColor
