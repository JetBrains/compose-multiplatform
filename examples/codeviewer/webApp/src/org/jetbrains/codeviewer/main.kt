package org.jetbrains.codeviewer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.jetbrains.codeviewer.ui.MainView

@OptIn(ExperimentalComposeUiApi::class)
fun main() = ComposeViewport {
    MainView()
}
