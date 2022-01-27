package org.jetbrains.compose.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.singleWindowApplication
import javax.swing.JComponent

actual fun appImpl(name: String, title: String, body: @Composable () -> Unit) {
    singleWindowApplication {
        body()
    }
}

actual typealias EmbedderContext = JComponent
actual fun embed(context: EmbedderContext,  body: @Composable () -> Unit) {
    TODO("implement me")
}