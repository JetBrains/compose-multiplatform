import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import core.ComposeBirdGame
import core.Game
import data.GameFrame
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.get

fun main() {

    val game: Game = ComposeBirdGame()

    val body = document.getElementsByTagName("body")[0] as HTMLElement

    // Enabling keyboard control
    body.addEventListener("keyup", {
        when ((it as KeyboardEvent).keyCode) {
            38 -> { // Arrow up
                game.moveBirdUp()
            }
        }
    })

    renderComposable(rootElementId = "root") {

        Div(
            attrs = {
                style {
                    property("text-align", "center")
                }
            }
        ) {

            // The current frame!
            val gameFrame by game.gameFrame

            // Igniting the game loop
            LaunchedEffect(Unit) {
                while (!gameFrame.isGameOver) {
                    delay(60)
                    game.step()
                }
            }

            Header(gameFrame)

            Div(
                attrs = {
                    style {
                        marginTop(30.px)
                    }
                }
            ) {
                if (gameFrame.isGameOver || gameFrame.isGameWon) {
                    GameResult(gameFrame)
                } else {
                    // Play area
                    repeat(ComposeBirdGame.ROWS) { rowIndex ->
                        Div {
                            repeat(ComposeBirdGame.COLUMNS) { columnIndex ->
                                Input(
                                    InputType.Radio,

                                    attrs = {
                                        val tube = gameFrame.tubes.find { it.position == columnIndex }
                                        val isTube = tube?.coordinates?.get(rowIndex) ?: false
                                        val isBird =
                                            !isTube && columnIndex == ComposeBirdGame.BIRD_COLUMN && rowIndex == gameFrame.birdPos

                                        // if it's either a tube node or bird, check it
                                        checked(isTube || isBird)

                                        if (!isBird) {
                                            // if it's a bird, enable it. (to change to blue color)
                                            disabled()
                                        }


                                    }
                                )
                            }
                        }
                    }
                }
            }

        }

    }
}

@Composable
private fun Header(gameFrame: GameFrame) {
    // Game title
    H1 {
        Text(value = "🐦 Compose Bird!")
    }

    // Game score
    Text(value = "Your Score: ${gameFrame.score} || Top Score: ${ComposeBirdGame.TOTAL_TUBES}")
}

@Composable
private fun GameResult(gameFrame: GameFrame) {
    // Game Status
    H2 {
        if (gameFrame.isGameWon) {
            Text("🚀 Won the game! 🚀")
        } else {
            // core.Game over
            Text("💀 Game Over 💀")
        }
    }

    // Try Again
    Button(
        attrs = {
            onClick {
                window.location.reload()
            }
        }
    ) {
        Text("Try Again!")
    }
}