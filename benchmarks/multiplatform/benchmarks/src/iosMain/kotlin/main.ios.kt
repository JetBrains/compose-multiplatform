/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import platform.UIKit.UIViewController
import kotlin.system.exitProcess

fun setGlobalFromArgs(args : List<String>) {
    Config.setGlobalFromArgs(args.toTypedArray())
}

fun runReal() = Config.isModeEnabled(Mode.REAL)

fun runBenchmarks() {
    MainScope().launch {
        runBenchmarks(graphicsContext = graphicsContext())
        println("Completed!")
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController(): UIViewController {
    return ComposeUIViewController(configure = { parallelRendering = Config.parallelRendering }) {
        BenchmarkRunner(getBenchmarks(), { exitProcess(0) })
    }
}
