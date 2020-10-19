package example.todo.common.utils

import androidx.compose.ui.input.key.ExperimentalKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent

@OptIn(ExperimentalKeyInput::class)
fun onKeyUp(key: Key, onEvent: () -> Unit): (KeyEvent) -> Boolean =
    { keyEvent ->
        if (keyEvent.key == key) {
            onEvent()
            true
        } else {
            false
        }
    }
