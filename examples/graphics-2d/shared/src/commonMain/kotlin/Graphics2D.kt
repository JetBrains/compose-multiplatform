import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import bouncingBalls.BouncingBallsApp
import fallingballs.FallingBalls
import minesweeper.MineSweeper

private val TOP_APP_BAR_HEIGHT = 100.dp

@Composable
fun Graphics2D(requestWindowSize: ((width: Dp, height: Dp) -> Unit)) {
    val exampleState: MutableState<Example?> = remember { mutableStateOf(null) }
    val example = exampleState.value

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (example != null) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.clickable {
                                exampleState.value = null
                            }
                        )
                    }
                },
                title = {
                    Text(example?.name ?: "Choose example")
                }
            )
        }
    ) {

        if (example == null) {
            LazyColumn {
                items(examples) {
                    Button(onClick = {
                        exampleState.value = it
                    }) {
                        Text(it.name)
                    }
                }
            }
        } else {
            example.content(
                requestWindowSize = { w, h ->
                    requestWindowSize(w, h + TOP_APP_BAR_HEIGHT)
                }
            )
        }

    }
}

private class Example(
    val name: String,
    val content: @Composable (requestWindowSize: ((width: Dp, height: Dp) -> Unit)) -> Unit
)

private val examples: List<Example> = listOf(
    Example("FallingBalls") {
        FallingBalls()
    },
    Example("BouncingBalls") {
        BouncingBallsApp()
    },
    Example("MineSweeper") {
        MineSweeper(it)
    },
)
