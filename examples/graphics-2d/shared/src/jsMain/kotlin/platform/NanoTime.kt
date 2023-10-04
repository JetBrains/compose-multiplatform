package org.jetbrains.compose.demo.visuals.platform

actual fun nanoTime(): Long = kotlinx.browser.window.performance.now().toLong()
