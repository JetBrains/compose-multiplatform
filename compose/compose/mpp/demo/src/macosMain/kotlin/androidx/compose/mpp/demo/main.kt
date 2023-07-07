package androidx.compose.mpp.demo

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import platform.AppKit.NSApp
import platform.AppKit.NSApplication

fun main() {
    NSApplication.sharedApplication()
    Window("Compose/Native sample") {
        val app = remember { App() }
        app.Content()
    }
    NSApp?.run()
}
