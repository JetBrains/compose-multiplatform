package org.jetbrains.compose.demo.visuals.platform

import kotlin.system.exitProcess

actual fun nanoTime(): Long = 0;//kotlin.system.getTimeNanos()

actual fun measureTime() = kotlin.system.getTimeNanos()
actual fun exit(): Unit = exitProcess(0)