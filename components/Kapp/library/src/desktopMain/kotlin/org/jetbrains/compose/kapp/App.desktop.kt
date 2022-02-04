package org.jetbrains.compose.kapp

import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.*
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

class AppWindowScope : FrameScope, FrameWindowScope {
    val state = WindowState()
    override val window: ComposeWindow = ComposeWindow()
    fun onClose() {}

    // TODO: fix me
    override val density: Float
        get() = 1.0f

    override val widthPixels: Int
        get() = window.width

    override val heightPixels: Int
        get() = window.height
}

@Composable
actual fun KAppScope.Frame(content: @Composable FrameScope.() -> Unit) {
    val scope by remember { mutableStateOf(AppWindowScope()) }
    scope.apply {
        Window(onCloseRequest = { scope.onClose() }, state = scope.state,
            content = @Composable { content() } )
    }
}

internal class AppAppScope : KAppScope, ApplicationScope {
    override fun exitApplication() {
        println("Exit application")
        SwingUtilities.invokeLater {
            exitProcess(0)
        }
    }
}

internal actual fun kappImpl(name: String, title: String, content: @Composable KAppScope.() -> Unit) {
    val scope = AppAppScope()
    scope.apply {
        application(content = @Composable { content() })
    }
}

internal actual fun simpleKappImpl(name: String, content: @Composable FrameScope.() -> Unit) {
    val appScope = AppAppScope()
    val winScope = AppWindowScope()
    appScope.apply {
        winScope.apply {
            singleWindowApplication(state = winScope.state, content = @Composable { content() })
        }
    }
}
