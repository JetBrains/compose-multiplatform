package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.desktop.LocalAppWindow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerMoveFilter
import java.awt.Cursor

actual fun Modifier.pointerMoveFilter(
    onEnter: () -> Boolean,
    onExit: () -> Boolean,
    onMove: (Offset) -> Boolean
): Modifier = this.pointerMoveFilter(onEnter = onEnter, onExit = onExit, onMove = onMove)

actual fun Modifier.cursorForHorizontalResize(): Modifier = composed {
    var isHover by remember { mutableStateOf(false) }

    if (isHover) {
        LocalAppWindow.current.window.cursor = Cursor(Cursor.E_RESIZE_CURSOR)
    } else {
        LocalAppWindow.current.window.cursor = Cursor.getDefaultCursor()
    }

    pointerMoveFilter(
        onEnter = { isHover = true; true },
        onExit = { isHover = false; true }
    )
}