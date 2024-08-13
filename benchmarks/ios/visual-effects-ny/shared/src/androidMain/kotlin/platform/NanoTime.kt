package org.jetbrains.compose.demo.visuals.platform

import kotlin.system.exitProcess

actual fun nanoTime(): Long = System.nanoTime()

actual fun measureTime() = nanoTime()

actual fun exit(): Unit = exitProcess(0)