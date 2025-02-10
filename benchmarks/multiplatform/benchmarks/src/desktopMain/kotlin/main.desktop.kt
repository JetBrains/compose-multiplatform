/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Surface

fun main(args: Array<String>) {
    Args.parseArgs(args)
    runBlocking(Dispatchers.Main) { runBenchmarks(graphicsContext = graphicsContext()) }
}

object SoftwareGraphicsContext : GraphicsContext {
    override fun surface(width: Int, height: Int): Surface = Surface.makeRasterN32Premul(width, height)
    override suspend fun awaitGPUCompletion() = Unit
}