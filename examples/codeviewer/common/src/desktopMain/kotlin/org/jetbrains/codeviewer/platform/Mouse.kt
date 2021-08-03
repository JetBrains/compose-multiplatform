package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.awtCursor
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.input.pointer.pointerMoveFilter
import java.awt.Cursor

actual fun Modifier.pointerMoveFilter(
    onEnter: () -> Boolean,
    onExit: () -> Boolean,
    onMove: (Offset) -> Boolean
): Modifier = this.pointerMoveFilter(onEnter = onEnter, onExit = onExit, onMove = onMove)

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.cursorForHorizontalResize(): Modifier = composed {
    var isHover by remember { mutableStateOf(false) }

    pointerMoveFilter(
        onEnter = { isHover = true; true },
        onExit = { isHover = false; true }
    ).pointerIcon(
        awtCursor(
            if (isHover) {
                Cursor(Cursor.E_RESIZE_CURSOR)
            } else {
                Cursor.getDefaultCursor()
            }
        )
    )
}