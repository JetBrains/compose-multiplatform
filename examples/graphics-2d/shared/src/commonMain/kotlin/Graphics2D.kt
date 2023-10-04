import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bouncingballs.BouncingBallsApp
import fallingballs.FallingBalls
import minesweeper.MineSweeper
import visualeffects.NYContent
import visualeffects.RotatingWords
import visualeffects.WaveEffectGrid

private val TOP_APP_BAR_HEIGHT = 100.dp
private val EMPTY_WINDOW_RESIZER: (width: Dp, height: Dp) -> Unit = { w, h ->  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Graphics2D(requestWindowSize: ((width: Dp, height: Dp) -> Unit) = EMPTY_WINDOW_RESIZER) {
    val exampleState: MutableState<Example?> = remember { mutableStateOf(null) }
    val example = exampleState.value

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
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
            Box(Modifier.padding(it)) {
                if (example == null) {
                    LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                        items(examples) {
                            Button(onClick = {
                                exampleState.value = it
                            }) {
                                Text(it.name)
                            }
                        }
                    }
                } else {
                    example.content { w, h ->
                        requestWindowSize(w, h + TOP_APP_BAR_HEIGHT)
                    }
                }
            }

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
    Example("RotatingWords") {
        RotatingWords()
    },
    Example("WaveEffectGrid") {
        WaveEffectGrid()
    },
    Example("Happy New Year!") {
        NYContent()
    },
)
