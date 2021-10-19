package org.jetbrains.compose.codeeditor.keyevent

import androidx.compose.ui.input.key.KeyEvent

internal fun interface KeyEventHandler {
    fun onKeyEvent(event: KeyEvent): Boolean
}
