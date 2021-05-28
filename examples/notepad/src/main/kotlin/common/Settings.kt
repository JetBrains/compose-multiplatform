package common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Settings {
    var isTrayEnabled by mutableStateOf(true)
        private set

    fun toggleTray() {
        isTrayEnabled = !isTrayEnabled
    }
}