/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose

import com.jetbrains.compose.benchmark.PerformanceInfoDialog
import java.util.concurrent.ConcurrentHashMap

val performanceDialog = PerformanceInfoDialog()
private val runtime = Runtime.getRuntime()
private val measurements: MutableMap<String, Measure> = ConcurrentHashMap()

class Measure(val averageMemory: Float = 0f, val count: Int = 0)

fun doMeasure(key: String) {
    runtime.gc()
    val memory = runtime.totalMemory() - runtime.freeMemory()
    val previous = measurements.getOrDefault(key, Measure())
    val current =
        Measure(
            averageMemory = ((previous.averageMemory * previous.count) + memory) / (previous.count + 1),
            count = previous.count + 1
        )
    measurements[key] = current

    performanceDialog.setText(
        buildString {
            measurements.entries.sortedBy { it.value.averageMemory }.forEach { (key, measure)->
                appendLine("---------")
                append("$key:")
                append(String.format("%.2f MB", measure.averageMemory / 1e6f))
                appendLine("")
            }
        }
    )
}
