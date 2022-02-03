package org.jetbrains.compose.kapp

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window

import platform.AppKit.NSApp
import platform.AppKit.NSApplication

@Composable
actual fun KAppScope.Frame(content: @Composable () -> Unit) {
    content()
}

internal class AppAppScope : KAppScope {}

internal actual fun kappImpl(name: String, title: String, content: @Composable KAppScope.() -> Unit) {
    // TODO: make it multiframe.
    val scope = AppAppScope()
    scope.apply {
        TODO()
    }
}

internal actual fun simpleKappImpl(name: String, content: @Composable () -> Unit) {
    NSApplication.sharedApplication()
    Window(name) {
        content()
    }
    NSApp?.run()
}

