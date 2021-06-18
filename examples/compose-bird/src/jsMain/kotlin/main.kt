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
import org.jetbrains.compose.web.attributes.checked
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.*
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

    body.onclick = {
        game.moveBirdUp()
    }

    renderComposable(rootElementId = "root") {

        Div(
            attrs = {
                style {
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.Center)
                }
                onClick {
                    game.moveBirdUp()
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

            Div {

                // Title
                GameTitle()
                Score(gameFrame)
                Br()

                if (gameFrame.isGameOver || gameFrame.isGameWon) {
                    Div(
                        attrs = {
                            style {
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                justifyContent(JustifyContent.Center)
                            }
                        }
                    ) {
                        GameStatus(gameFrame)
                        TryAgain()
                    }


                } else {
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

                                        if (isTube || isBird) {
                                            // if it's either a tube node or bird, check it
                                            checked(true)
                                        } else {
                                            // otherwise, uncheck
                                            checked(false)
                                        }

                                        if (isBird) {
                                            // if it's a bird, enable it. (to change to blue color)
                                            disabled(false)
                                        } else {
                                            // if it's not a bird, disable it. (to change to grey color)
                                            disabled(true)
                                        }

                                        style {
                                            width(25.px)
                                            height(25.px)
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
private fun TryAgain() {
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

@Composable
private fun GameStatus(gameFrame: GameFrame) {
    H2(
        attrs = {
            style {
                alignSelf(AlignSelf.Center)
            }
        }
    ) {
        if (gameFrame.isGameWon) {
            Text("🚀 Won the game! 🚀")
        } else {
            // core.Game over
            Text("💀 Game Over 💀")
        }
    }
}

@Composable
private fun Score(gameFrame: GameFrame) {
    Div(
        attrs = {
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.Center)
            }
        }
    ) {
        Text("Your Score: ${gameFrame.score} || Top Score: ${ComposeBirdGame.TOTAL_TUBES}")
    }
}

@Composable
private fun GameTitle() {
    H1(
        attrs = {
            style {
                display(DisplayStyle.Flex)
                justifyContent(JustifyContent.Center)
            }
        }
    ) {
        Text("🐦 Compose Bird!")
    }
}