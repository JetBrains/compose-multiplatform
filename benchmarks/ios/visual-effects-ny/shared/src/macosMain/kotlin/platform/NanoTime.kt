package org.jetbrains.compose.demo.visuals.platform

actual fun nanoTime(): Long = kotlin.system.getTimeNanos()