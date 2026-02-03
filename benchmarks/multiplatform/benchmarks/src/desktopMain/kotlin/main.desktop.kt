/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    Config.setGlobalFromArgs(args)

    if (Config.runServer) {
        // Start the benchmark server to receive results from browsers
        BenchmarksSaveServer.start()
    } else if (Config.isModeEnabled(Mode.REAL)) {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                alwaysOnTop = true,
                state = rememberWindowState(
                    width = 1920.dp, height = 1080.dp
                )
            ) {
                BenchmarkRunner(getBenchmarks(), { System.exit(0) })
            }
        }
    } else {
        runBlocking(Dispatchers.Main) { runBenchmarks(graphicsContext = graphicsContext()) }
    }
}
