package org.jetbrains.compose.demo.visuals.platform

import kotlin.system.exitProcess

actual fun nanoTime(): Long = 0//System.nanoTime()

actual fun measureTime() = System.nanoTime()
actual fun exit(): Unit = exitProcess(0)