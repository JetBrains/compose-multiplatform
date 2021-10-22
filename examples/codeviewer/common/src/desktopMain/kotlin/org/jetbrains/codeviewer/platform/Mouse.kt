package org.jetbrains.codeviewer.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerMoveFilter
import java.awt.Cursor

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.pointerMoveFilter(
    onEnter: () -> Boolean,
    onExit: () -> Boolean,
    onMove: (Offset) -> Boolean
): Modifier = this.pointerMoveFilter(onEnter = onEnter, onExit = onExit, onMove = onMove)

@OptIn(ExperimentalComposeUiApi::class)
internal class AwtCursor(val cursor: Cursor) : PointerIcon {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AwtCursor

        if (cursor != other.cursor) return false

        return true
    }

    override fun hashCode(): Int {
        return cursor.hashCode()
    }

    override fun toString(): String {
        return "AwtCursor(cursor=$cursor)"
    }
}

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.cursorForHorizontalResize(): Modifier =
    Modifier.pointerHoverIcon(AwtCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
