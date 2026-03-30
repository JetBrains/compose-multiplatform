/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen
import platform.UIKit.UIViewController
import kotlin.system.exitProcess
import kotlin.time.TimeSource

actual val mainTime: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()

fun setGlobalFromArgs(args : List<String>) {
    Config.setGlobalFromArgs(args.toTypedArray())
}

fun runReal() = Config.isModeEnabled(Mode.REAL) || Config.isModeEnabled(Mode.STARTUP)

fun runBenchmarks() {
    UIApplication.sharedApplication.setIdleTimerDisabled(true)
    MainScope().launch {
        runBenchmarks(graphicsContext = graphicsContext())
        println("Completed!")
        exitProcess(0)
    }
}

actual val isIosTarget: Boolean = true

@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController(): UIViewController {
    return ComposeUIViewController(configure = { parallelRendering = Config.parallelRendering }) {
        BenchmarkRunner(getBenchmarks(), UIScreen.mainScreen.maximumFramesPerSecond.toInt(), { exitProcess(0) })
    }
}
