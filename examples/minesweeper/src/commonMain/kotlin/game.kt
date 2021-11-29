@file:OptIn(ExperimentalComposeWebWidgetsApi::class)

package com.github.veselovalex.minesweeper

import androidx.compose.runtime.*
import org.jetbrains.compose.common.core.graphics.Color
import org.jetbrains.compose.common.foundation.border
import org.jetbrains.compose.common.foundation.layout.*
import org.jetbrains.compose.common.material.Text
import org.jetbrains.compose.common.ui.*
import org.jetbrains.compose.common.ui.unit.Dp
import org.jetbrains.compose.common.ui.unit.dp
import org.jetbrains.compose.common.ui.unit.sp
import kotlin.math.max

@Composable
expect fun OpenedCell(cell: Cell)

@Composable
expect fun CellWithIcon(src: String, alt: String)

@Composable
fun Mine() {
    CellWithIcon(src="assets/mine.png", alt = "Bomb")
}

@Composable
fun Flag() {
    CellWithIcon(src="assets/flag.png", alt = "Flag")
}

class GameStyles(
    val closedCellColor: Color,
    val openedCellColor: Color,
    val borderColor: Color,
    val cellSize: Dp,
    val cellBorderWidth: Dp
) {
    fun getCellColor(cell: Cell): Color {
        return if (cell.isOpened) {
            openedCellColor
        } else {
            closedCellColor
        }
    }
}

@Composable
expect fun ClickableCell(
    onLeftMouseButtonClick: (isShiftPressed: Boolean) -> Unit,
    onRightMouseButtonClick: () -> Unit,
    content: @Composable () -> Unit
)

@Composable
fun BoardView(game: GameController, styles: GameStyles) {
    Column {
        for (row in 0 until game.rows) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (column in 0 until game.columns) {
                    val cell = game.cellAt(row, column)!!

                    ClickableCell(
                        onLeftMouseButtonClick = { shift ->
                            // It seems to be hard to implement traditional LMB + RMB click
                            // activation of mine seeker, so let it run on Shift + Click
                            if (shift) {
                                game.openNotFlaggedNeighbors(cell)
                            } else {
                                game.openCell(cell)
                            }
                         },
                        onRightMouseButtonClick = { game.toggleFlag(cell) }
                    ) {
                        Box(
                            modifier = Modifier.size(styles.cellSize, styles.cellSize)
                                .background(styles.getCellColor(cell))
                                .border(styles.cellBorderWidth, styles.borderColor)
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
}

@Composable
fun IndicatorWithIcon(iconPath: String, alt: String, value: Int) {
    Box (modifier = Modifier.background(Color(0x8e, 0x6e, 0x0e))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp, 40.dp)) {
                CellWithIcon(iconPath, alt)
            }

            Box(modifier = Modifier.size(56.dp, 36.dp)) {
                Text(
                    text = value.toString(),
                    size = 24.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
expect fun NewGameButton(text: String, onClick: () -> Unit)

@Composable
fun Game(requestWindowSize: ((width: Int, height: Int) -> Unit)? = null) = Column {
    val windowPadding = 8.dp
    val boardBorderWidth = 1.dp
    val boardPadding = 4.dp
    val extraVerticalSpace = 152.dp // This should give enough space to UI widgets
    val styles = GameStyles(
        openedCellColor = Color.White,
        closedCellColor = Color.DarkGray,
        borderColor = Color.LightGray,
        cellSize = 32.dp,
        cellBorderWidth = 1.dp
    )

    val difficulty = object {
        val EASY = GameSettings(9, 9, 10)
        val MEDIUM = GameSettings(16, 16, 40)
        val EXPERT = GameSettings(16, 30, 99)
    }

    var message by remember { mutableStateOf<String?>(null) }
    val onWin = { message = "You win!" }
    val onLose = { message = "Try again" }

    var game by remember { mutableStateOf(GameController(difficulty.EASY, onWin, onLose)) }

    fun updateWindowSize() {
        if (requestWindowSize != null) {
            val boardOffset = (windowPadding.value + boardBorderWidth.value + boardPadding.value + 1.0f) * 2.0;
            val cellsWidth = { count: Int -> count.toFloat() * (styles.cellSize.value) + 10.0f  }

            val width = boardOffset + cellsWidth(game.columns)
            val height = boardOffset + cellsWidth(game.rows) + extraVerticalSpace.value

            requestWindowSize(width.toInt(), height.toInt())
        }
    }

    fun newGame(difficulty: GameSettings) {
        game = GameController(options = difficulty, onWin, onLose)
        message = null
    }

    updateWindowSize() // TODO this calls resize twice, how can we avoid this?

    Column (
        modifier = Modifier
            .background(Color(0xed, 0x9c, 0x38))
            .padding(windowPadding)
    ) {
        // Controls
        Row {
            Column {
                val bombsLeft = max(game.bombs - game.flagsSet, 0)
                IndicatorWithIcon("assets/mine.png", "Seconds", game.seconds)
                Box(modifier = Modifier.size(2.dp)) {}
                IndicatorWithIcon("assets/mine.png", "Bombs Left", bombsLeft)
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Box {
                    Text(text = "New Game", size = 20.sp)
                }
                Row {
                    NewGameButton("Easy") { newGame(difficulty.EASY) }
                    NewGameButton("Medium") { newGame(difficulty.MEDIUM) }
                    NewGameButton("Hard") { newGame(difficulty.EXPERT) }
                }
            }
        }

        // Spacer
        Box(modifier = Modifier.size(8.dp)) {}

        // Status
        Box(modifier = Modifier.padding(4.dp).size(200.dp, 32.dp)) {
            if (message != null) {
                Text(message!!)
            }
        }

        // Cells
        Box (modifier = Modifier.border(boardBorderWidth, Color.White).padding(boardPadding)) {
            BoardView(game, styles)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                game.onTimeTick(it)
            }
        }
    }
}