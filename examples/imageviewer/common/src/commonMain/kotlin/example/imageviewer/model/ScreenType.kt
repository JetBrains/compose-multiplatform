package example.imageviewer.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

enum class ScreenType {
    Main, FullscreenImage
}

object AppState {
    private var screen: MutableState<ScreenType>
    init {
        screen = mutableStateOf(ScreenType.Main)
    }

    fun screenState() : ScreenType {
        return screen.value
    }

    fun screenState(state: ScreenType) {
        screen.value = state
    }
}