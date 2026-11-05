/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(
    kotlin.js.ExperimentalWasmJsInterop::class,
    org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi::class,
)

package benchmarks.hydration

import kotlinx.browser.dom.HTMLElement
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.web.hydrateComposable
import org.jetbrains.compose.web.internal.runtime.browserDocument
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.measureTimedValue

class HydrationBenchmark {
    @Test fun catalog() = benchmark("tailwind-catalog")
    @Test fun form() = benchmark("form-app")
    @Test fun table() = benchmark("data-table")
    @Test fun svg() = benchmark("svg-dashboard")
    @Test fun article() = benchmark("content-article")
    @Test fun hydrate1k() = benchmark("hydrate1k")

    private fun benchmark(workload: String) = MainScope().promise {
        val html = hydrationHtml.getValue(workload)
        val content = hydrationContent(workload)
        val samples = mutableListOf<Double>()
        repeat(7) { iteration ->
            val root = browserDocument.createElement("div") as HTMLElement
            root.innerHTML = html
            browserDocument.body!!.appendChild(root)
            val nodes = root.querySelectorAll("*")
            val original = List(nodes.length) { nodes.item(it)!! }
            val (composition, elapsed) = measureTimedValue {
                hydrateComposable(root, onHydrationMismatch = { throw it }) { content() }
            }
            try {
                assertTrue(original.isNotEmpty(), workload)
                val hydrated = root.querySelectorAll("*")
                assertEquals(original.size, hydrated.length, workload)
                original.forEachIndexed { index, node -> assertSame(node, hydrated.item(index), workload) }
                if (workload == "hydrate1k" && iteration == 6) {
                    assertEquals(1_000, root.querySelectorAll("tbody tr").length)
                    (root.querySelector("tbody tr:nth-child(2) td:nth-child(3) a") as HTMLElement).click()
                    withTimeout(5_000) {
                        while (root.querySelectorAll("tbody tr").length != 999) delay(1)
                    }
                    assertEquals("3", root.querySelector("tbody tr:nth-child(2) td")!!.textContent)
                }
                if (iteration >= 2) samples += elapsed.inWholeNanoseconds / 1_000_000.0
            } finally {
                composition.dispose()
                root.parentNode!!.removeChild(root)
            }
        }
        val mean = samples.average()
        val stddev = sqrt(samples.sumOf { (it - mean) * (it - mean) } / (samples.size - 1))
        println(
            "HYDRATION $workload: mean=${mean.rounded()} ms, stddev=${stddev.rounded()} ms, " +
                "range=${samples.min()}..${samples.max()} ms, n=${samples.size}; samples=$samples"
        )
    }
}

private fun Double.rounded(): Double = (this * 1_000).roundToInt() / 1_000.0
