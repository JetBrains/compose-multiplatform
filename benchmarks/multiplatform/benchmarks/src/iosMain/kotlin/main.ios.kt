/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

fun main(args : List<String>) {
    Args.parseArgs(args.toTypedArray())
    MainScope().launch {
        runBenchmarks(graphicsContext = graphicsContext())
        println("Completed!")
    }
}

actual fun saveBenchmarksOnDisk(name: String, stats: BenchmarkStats) {
    // ignore
    // not implemented because it is difficult to transfer the file to the host system
}
