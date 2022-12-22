package org.jetbrains.compose.demo.visuals

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SampleWithTopAppBar(sample: Screen, state: MutableState<Screen>, content: @Composable (PaddingValues) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = sample.screen) },
                    navigationIcon =
                    {
                        IconButton(onClick = {state.value = Screen.CHOOSE_SAMPLE}) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )
            },
            content = content
        )
    }
}

enum class Screen(val screen: String) {
    CHOOSE_SAMPLE("Choose a demo:"),
    WORDS("Word"),
    WAVE("Wave"),
    NY("Happy New Year!");
}

@Composable
fun MyButton(screenState: MutableState<Screen>, to: Screen) {
    Button(onClick = {
        screenState.value = to
    }) {
        Text(fontSize = 20.sp, text = to.screen)
    }
}

@Composable
fun AllSamplesView() {
    MaterialTheme {
        val screenState: MutableState<Screen> = remember { mutableStateOf(Screen.CHOOSE_SAMPLE) }
        when (screenState.value) {
            Screen.CHOOSE_SAMPLE -> {
                Column(modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally)
                {
                    Text(modifier = Modifier.padding(10.dp), fontSize = 30.sp, text = Screen.CHOOSE_SAMPLE.screen)
                    MyButton(screenState, Screen.WORDS)
                    MyButton(screenState, Screen.WAVE)
                    MyButton(screenState, Screen.NY)
                }
            }

            Screen.WORDS -> {
                SampleWithTopAppBar(Screen.WORDS, screenState) {
                    RotatingWords()
                }
            }
            Screen.WAVE -> {
                SampleWithTopAppBar(Screen.WAVE, screenState) {
                    WaveEffectGrid()
                }
            }

            Screen.NY ->  {
                SampleWithTopAppBar(Screen.NY, screenState) {
                    NYContent()
                }
            }
        }
    }
}
