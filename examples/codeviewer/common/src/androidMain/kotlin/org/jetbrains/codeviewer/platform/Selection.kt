package org.jetbrains.codeviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.selection.Selection
import androidx.compose.ui.selection.SelectionContainer

@Composable
actual fun SelectionContainer(children: @Composable () -> Unit) {
    val selection = remember { mutableStateOf<Selection?>(null) }
    SelectionContainer(
        selection = selection.value,
        onSelectionChange = { selection.value = it },
        children = children
    )
}

@Composable
actual fun WithoutSelection(children: @Composable () -> Unit) {
    SelectionContainer(
        selection = null,
        onSelectionChange = {},
        children = children
    )
}