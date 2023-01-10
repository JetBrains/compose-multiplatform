@file:Suppress("FunctionName")

import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.orEmpty
import org.jetbrains.compose.resources.rememberImageBitmap
import org.jetbrains.compose.resources.resource
import kotlin.math.max

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun loadImage(res: String): Painter = BitmapPainter(resource(res).rememberImageBitmap().orEmpty())

expect fun hasRightClick(): Boolean

object Difficulty {
    val EASY = GameSettings(9, 9, 10)
    val MEDIUM = GameSettings(16, 16, 40)
    val EXPERT = GameSettings(16, 30, 99)
}

object GameStyles {
    val closedCellColor = Color.DarkGray
    val openedCellColor = Color.White
    val borderColor = Color.LightGray
    val cellSize = 32.dp
    val cellBorderWidth = 1.dp

    val windowPadding = 16.dp
    val boardBorderWidth = 1.dp
    val boardPadding = 4.dp
    val extraVerticalSpace = 158.dp // This should give enough space to UI widgets
}

@Composable
internal fun Game(requestWindowSize: ((width: Dp, height: Dp) -> Unit)? = null) = MainLayout {
    var message by remember { mutableStateOf<String?>(null) }

    val onWin = { message = "You win!" }
    val onLose = { message = "Try again" }
    var game by remember {
        mutableStateOf(GameController(Difficulty.EASY, onWin, onLose))
    }

    fun updateWindowSize() = with(GameStyles) {
        if (requestWindowSize != null) {
            val boardOffset = (windowPadding + boardPadding) * 2;

            val width = boardOffset + game.columns * cellSize
            val height = boardOffset + game.rows * cellSize + extraVerticalSpace

            requestWindowSize(width, height)
        }
    }

    fun newGame(difficulty: GameSettings) {
        game = GameController(options = difficulty, onWin, onLose)
        message = null
    }

    updateWindowSize()

    Column(
        modifier = Modifier
            .background(Color(0xed, 0x9c, 0x38))
            .padding(GameStyles.windowPadding)
    ) {
        // Controls
        Row {
            Column {
                val bombsLeft = max(game.bombs - game.flagsSet, 0)
                IndicatorWithIcon("assets/clock.png", "Seconds", game.seconds)
                Box(modifier = Modifier.size(2.dp)) {}
                IndicatorWithIcon("assets/mine.png", "Bombs Left", bombsLeft)
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Box {
                    Text(text = "New Game", fontSize = 20.sp)
                }
                Row {
                    NewGameButton("Easy") { newGame(Difficulty.EASY) }
                    NewGameButton("Medium") { newGame(Difficulty.MEDIUM) }
                    NewGameButton("Hard") { newGame(Difficulty.EXPERT) }
                }
            }
        }

        // Spacer
        Box(modifier = Modifier.size(8.dp)) {}

        // Status
        Box(modifier = Modifier.padding(4.dp).size(200.dp, 32.dp)) {
            Text(message ?: "")
        }

        // Cells
        Box(modifier = Modifier
            .border(GameStyles.boardBorderWidth, Color.White)
            .padding(GameStyles.boardPadding)
        ) {
            BoardView(game)
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

@Composable
private fun MainLayout(block:@Composable ColumnScope.() -> Unit) {
    Column { block() }
}