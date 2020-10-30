package androidx.ui.examples.jetissues.view.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.DesktopSelectionContainer
import androidx.compose.ui.selection.Selection

@Composable
actual fun SelectionContainer(children: @Composable () -> Unit) {
    val selection = remember { mutableStateOf<Selection?>(null) }
    DesktopSelectionContainer(
        selection = selection.value,
        onSelectionChange = { selection.value = it },
        children = children
    )
}