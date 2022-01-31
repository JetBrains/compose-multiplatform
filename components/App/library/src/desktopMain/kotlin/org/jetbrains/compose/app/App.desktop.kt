package org.jetbrains.compose.app

import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.*
import androidx.compose.ui.window.ApplicationScope
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

class AppWindowScope : FrameWindowScope {
    val state = WindowState()
    override val window: ComposeWindow = ComposeWindow()

    fun onClose() {}
}

@Composable
actual fun AppScope.Frame(content: @Composable () -> Unit) {
    val scope by remember { mutableStateOf(AppWindowScope()) }
    scope.apply {
        Window(onCloseRequest = { scope.onClose() }, state = scope.state,
            content = @Composable { content() } )
    }
}

class AppAppScope : AppScope, ApplicationScope {
    override fun exitApplication() {
        println("Exit application")
        SwingUtilities.invokeLater {
            exitProcess(0)
        }
    }
}

internal actual fun appImpl(name: String, title: String, content: @Composable AppScope.() -> Unit) {
    val scope = AppAppScope()
    scope.apply {
        application(content = @Composable { content() })
    }
}

actual fun embed(context: Any,  body: @Composable () -> Unit) {
    when (context) {
        is JComponent -> embed(context, body)
        else -> throw UnsupportedOperationException("cannot embed to $context")
    }
}

fun embed(context: JComponent, body: @Composable () -> Unit) {
    TODO("implement me")
}