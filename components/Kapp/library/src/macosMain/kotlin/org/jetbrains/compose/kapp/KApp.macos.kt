package org.jetbrains.compose.kapp

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window

import platform.AppKit.NSApp
import platform.AppKit.NSApplication

class QuartzScope : FrameScope {
    // TODO: fix me
    override val density: Float
        get() = 1.0f

    override val widthPixels: Int
        get() = 800

    override val heightPixels: Int
        get() = 600
}

@Composable
actual fun KAppScope.Frame(content: @Composable FrameScope.() -> Unit) {
    val scope = QuartzScope()
    scope.apply {
        content()
    }
}

internal class AppAppScope : KAppScope {}

internal actual fun kappImpl(name: String, title: String, content: @Composable KAppScope.() -> Unit) {
    // TODO: make it multiframe.
    val scope = AppAppScope()
    scope.apply {
        TODO()
    }
}

internal actual fun simpleKappImpl(name: String, content: @Composable FrameScope.() -> Unit) {
    NSApplication.sharedApplication()
    val scope = QuartzScope()
    scope.apply {
        Window(name) {
            content()
        }
    }
    NSApp?.run()
}

