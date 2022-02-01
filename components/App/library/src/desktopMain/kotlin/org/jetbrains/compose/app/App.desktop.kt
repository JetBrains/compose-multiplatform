package org.jetbrains.compose.app

import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.*
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

class AppWindowScope : FrameWindowScope {
    val state = WindowState()
    override val window: ComposeWindow = ComposeWindow()
    fun onClose() {}
}

@Composable
actual fun ComposeAppScope.Frame(content: @Composable () -> Unit) {
    val scope by remember { mutableStateOf(AppWindowScope()) }
    scope.apply {
        Window(onCloseRequest = { scope.onClose() }, state = scope.state,
            content = @Composable { content() } )
    }
}

class AppAppScope : ComposeAppScope, androidx.compose.ui.window.ApplicationScope {
    override fun exitApplication() {
        println("Exit application")
        SwingUtilities.invokeLater {
            exitProcess(0)
        }
    }
}

internal actual fun composeApplicationImpl(name: String, title: String, content: @Composable ComposeAppScope.() -> Unit) {
    val scope = AppAppScope()
    scope.apply {
        application(exitProcessOnExit = true, content = @Composable { content() })
    }
}

internal actual fun composeAppImpl(name: String, content: @Composable () -> Unit) {
    val appScope = AppAppScope()
    val winScope = AppWindowScope()
    appScope.apply {
        winScope.apply {
            singleWindowApplication(state = winScope.state, content = @Composable { content() })
        }
    }
}