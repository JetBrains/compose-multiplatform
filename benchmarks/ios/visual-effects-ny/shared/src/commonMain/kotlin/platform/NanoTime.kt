package org.jetbrains.compose.demo.visuals.platform

expect fun nanoTime(): Long

expect fun measureTime(): Long
expect fun exit(): Unit