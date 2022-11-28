package org.jetbrains.codeviewer.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

actual fun Modifier.cursorForHorizontalResize(): Modifier =
    this.pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
