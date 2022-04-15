/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package com.jetbrains.compose

import com.jetbrains.compose.benchmark.PerformanceInfoDialog
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap

val performanceDialog = PerformanceInfoDialog()
private val runtime = Runtime.getRuntime()
private val measurements: MutableMap<String, Measure> = ConcurrentHashMap()

class Measure(val averageMemory: Double = 0.0, val systemLoad: Double = 0.0, val count: Int = 0)

fun doMeasure(key: String) {
    runtime.gc()
    val memory = runtime.totalMemory() - runtime.freeMemory()
    val systemLoad = ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
    val previous = measurements.getOrDefault(key, Measure())
    val current =
        Measure(
            averageMemory = ((previous.averageMemory * previous.count) + memory) / (previous.count + 1),
            systemLoad = ((previous.systemLoad * previous.count) + systemLoad) / (previous.count + 1),
            count = previous.count + 1
        )
    measurements[key] = current

    performanceDialog.setText(
        buildString {
            measurements.entries.sortedBy { it.key }.forEach { (key, measure) ->
                appendLine("---------")
                appendLine("$key:")
                appendLine("memory: " + String.format("%.1f MB", measure.averageMemory / 1e6f))
                appendLine("system load: " + String.format("%.3f", measure.systemLoad) + " % ")
                appendLine("")
            }
        }
    )
}
