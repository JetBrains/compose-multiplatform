/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import kotlinx.coroutines.runBlocking
import platform.AppKit.NSApp
import platform.AppKit.NSApplication
import kotlin.system.exitProcess

fun main(args : Array<String>) {
    Config.setGlobalFromArgs(args)
    if (Config.isModeEnabled(Mode.REAL)) {
        NSApplication.sharedApplication()
        val frameRate = 120 // TODO: get from device
        Window(
            "Benchmarks",
            DpSize(
                width = 1920.dp, height = 1080.dp
            )
        ) {
            BenchmarkRunner(getBenchmarks(), frameRate, { exitProcess(0) })
        }
        NSApp?.run()
    } else {
        runBlocking { runBenchmarks(graphicsContext = graphicsContext()) }
    }
}