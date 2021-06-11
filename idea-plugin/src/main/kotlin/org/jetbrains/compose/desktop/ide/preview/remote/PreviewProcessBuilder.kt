/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.ide.preview.remote

import com.intellij.openapi.util.SystemInfo
import java.io.File

internal data class PreviewProcessBuilder(
    private val javaExecutable: File,
    private val serverCP: String,
    private val target: String,
    private val cp: String,
    private val debugPort: Int? = null
) {
    fun start(): PreviewClient {
        val args = arrayListOf(
            javaExecutable.absolutePath,
            "-cp", serverCP + File.pathSeparator + cp,
            "-ea"
        )
        if (SystemInfo.isMac) {
            args.add("-Dapple.awt.UIElement=true")
        }
        if (debugPort != null) {
            args.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:$debugPort")
        }
        args.add("org.jetbrains.compose.desktop.ui.tooling.preview.process.PreviewHost")
        args.add(target)
        return PreviewClient(args.toTypedArray())
    }
}
