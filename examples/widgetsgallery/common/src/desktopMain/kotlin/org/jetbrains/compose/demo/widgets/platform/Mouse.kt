package org.jetbrains.compose.demo.widgets.platform

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.window.FrameWindowScope
import java.awt.Cursor

val AppFrame = staticCompositionLocalOf<FrameWindowScope> { error("Undefined repository") }

actual fun Modifier.pointerMoveFilter(
    onEnter: () -> Boolean,
    onExit: () -> Boolean,
    onMove: (Offset) -> Boolean
): Modifier = this.pointerMoveFilter(onEnter = onEnter, onExit = onExit, onMove = onMove)

actual fun Modifier.cursorForHorizontalResize(): Modifier = composed {
    var isHover by remember { mutableStateOf(false) }

    val window = AppFrame.current.window
    window.cursor = if (isHover) {
        Cursor(Cursor.E_RESIZE_CURSOR)
    } else {
        Cursor.getDefaultCursor()
    }

    pointerMoveFilter(
        onEnter = { isHover = true; true },
        onExit = { isHover = false; true }
    )
}