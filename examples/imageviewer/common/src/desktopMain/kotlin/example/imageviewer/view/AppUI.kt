package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import example.imageviewer.model.AppState
import example.imageviewer.model.ScreenType
import example.imageviewer.model.ContentState
import example.imageviewer.style.Gray

private val message: MutableState<String> = mutableStateOf("")
private val state: MutableState<Boolean> = mutableStateOf(false)

@Composable
fun BuildAppUI(content: ContentState) {

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Gray
    ) {
        when (AppState.screenState()) {
            ScreenType.Main -> {
                setMainScreen(content)
            }
            ScreenType.FullscreenImage -> {
                setImageFullScreen(content)
            }
        }
    }

    Toast(message.value, state)
}

fun showPopUpMessage(text: String) {
    message.value = text
    state.value = true
}