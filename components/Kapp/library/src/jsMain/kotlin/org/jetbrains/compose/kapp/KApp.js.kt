package org.jetbrains.compose.kapp

import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.*
import org.jetbrains.skiko.wasm.onWasmReady

@Composable
actual fun KAppScope.Frame(content: @Composable () -> Unit) {
    content()
}


internal class AppAppScope : KAppScope {

}

internal actual fun kappImpl(name: String, title: String, content: @Composable KAppScope.() -> Unit) {
    // TODO: make it multiframe.
    val scope = AppAppScope()
    scope.apply {
        onWasmReady {
            Window(title) {
                content()
            }
        }
    }
}

internal actual fun simpleKappImpl(name: String, content: @Composable () -> Unit) {
    val appScope = AppAppScope()
    appScope.apply {
        onWasmReady {
            Window(name) {
                content()
            }
        }
    }
}
