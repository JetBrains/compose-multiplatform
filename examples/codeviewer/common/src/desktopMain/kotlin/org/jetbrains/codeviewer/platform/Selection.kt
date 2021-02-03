package org.jetbrains.codeviewer.platform

import androidx.compose.foundation.text.selection.DesktopSelectionContainer
import androidx.compose.runtime.Composable

@Composable
actual fun SelectionContainer(children: @Composable () -> Unit) {
    DesktopSelectionContainer(content = children)
}
