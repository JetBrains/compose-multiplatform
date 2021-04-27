/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ui.tooling.preview.rpc

import java.io.PrintStream

abstract class PreviewLogger {
    inline operator fun invoke(s: () -> String) {
        if (isEnabled) {
            log(s())
        }
    }

    inline fun error(msg: () -> String) {
        invoke { "error: ${msg()}" }
    }

    abstract val isEnabled: Boolean
    abstract fun log(s: String)
}

internal class PrintStreamLogger(
    private val prefix: String,
    private val printStream: PrintStream = System.out
) : PreviewLogger() {
    override val isEnabled: Boolean = true

    override fun log(s: String)  {
        printStream.print(prefix)
        printStream.print(":")
        printStream.println(s)
    }
}