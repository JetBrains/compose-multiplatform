/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.compose.desktop.ui.tooling.preview.process

import java.awt.*
import java.io.File
import java.net.URLClassLoader
import kotlin.concurrent.thread

class PreviewHost {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            mainImpl(args)
        }

        private fun mainImpl(args: Array<String>) {
            val fqName = args[0]
            val urls = Array(args.size - 1) { File(args[it + 1]).toURI().toURL() }
            val classLoader = URLClassLoader(urls)
            val previewRunner = classLoader.loadClass(
                "androidx.compose.desktop.ui.tooling.preview.runtime.PreviewRunner"
            )
            val runPreview = previewRunner.getMethod("main", Array<String>::class.java)
            val locationAndSize = previewRunner.getMethod("" +
                    "locationAndSize", Int::class.java, Int::class.java, Int::class.java, Int::class.java
            )
            val isPanelVisible = previewRunner.getMethod("isPanelVisible", Boolean::class.java)
            val isIdeInFocus = previewRunner.getMethod("isIdeInFocus", Boolean::class.java)

            val composeThread = thread {
                runPreview.invoke(previewRunner, arrayOf(fqName))
            }

            val reader = System.`in`.bufferedReader()
            val readerThread = thread {
                for (line in reader.lines()) {
                    val words = line.split(" ")
                    when (words.firstOrNull()) {
                        "VIEW_PORT" -> {
                            val x = words[1].toInt()
                            val y = words[2].toInt()
                            val w = words[3].toInt()
                            val h = words[4].toInt()
                            locationAndSize.invoke(previewRunner, x, y, w, h)
                        }
                        "PANEL_SHOWN" -> {
                            when (words.getOrNull(1)) {
                                "true" -> isPanelVisible.invoke(previewRunner, true)
                                "false" -> isPanelVisible.invoke(previewRunner, false)
                            }
                        }
                        "IDE_FOCUS" -> {
                            when (words.getOrNull(1)) {
                                "true" -> isIdeInFocus.invoke(previewRunner, true)
                                "false" -> isIdeInFocus.invoke(previewRunner, false)
                            }
                        }
                    }
                }
            }

            readerThread.join()
            System.exit(0)
        }
    }
}