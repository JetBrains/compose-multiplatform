package org.jetbrains.compose.kapp

import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.*
import kotlinx.browser.document
import org.jetbrains.skiko.wasm.onWasmReady

internal class AppAppScope : KAppScope {
}

class DocumentScope : FrameScope {
    // TODO: fix me
    override val density: Float
        get() = 1.0f

    override val widthPixels: Int
        get() = document.defaultView!!.innerWidth

    override val heightPixels: Int
        get() = document.defaultView!!.innerHeight
}


@Composable
actual fun KAppScope.Frame(content: @Composable FrameScope.() -> Unit) {
    val docScope = DocumentScope()
    docScope.apply {
        content()
    }
}

internal actual fun kappImpl(name: String, title: String, content: @Composable KAppScope.() -> Unit) {
    // TODO: make it multiframe.
    val scope = AppAppScope()
    scope.apply {
        val frameScope = DocumentScope()
        frameScope.apply {
            onWasmReady {
                Window(title) {
                    content()
                }
            }
        }
    }
}

internal actual fun simpleKappImpl(name: String, content: @Composable FrameScope.() -> Unit) {
    val appScope = AppAppScope()
    appScope.apply {
        onWasmReady {
            val frameScope = DocumentScope()
            frameScope.apply {
                Window(name) {
                    content()
                }
            }
        }
    }
}
