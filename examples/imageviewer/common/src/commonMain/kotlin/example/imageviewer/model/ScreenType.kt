package example.imageviewer.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

enum class ScreenType {
    MainScreen, FullscreenImage
}

object AppState {
    private var screen: MutableState<ScreenType>
    init {
        screen = mutableStateOf(ScreenType.MainScreen)
    }

    fun screenState() : ScreenType {
        return screen.value
    }

    fun screenState(state: ScreenType) {
        screen.value = state
    }
}