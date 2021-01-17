package example.todo.common.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key

fun onKeyUp(key: Key, onEvent: () -> Unit): (KeyEvent) -> Boolean =
    { keyEvent ->
        if (keyEvent.key == key) {
            onEvent()
            true
        } else {
            false
        }
    }
