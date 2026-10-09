/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import kotlinx.benchmark.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Threads

/** Compares renderer reuse strategies with the same HTML workload on concurrent workers. */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(value = 2, jvmArgsAppend = ["-Xms256m", "-Xmx256m"])
@Threads(4)
open class RendererPoolBenchmark {
    @Param("1", "64")
    @JvmField
    var rows: Int = 0

    // Keep the previous cache shape for a direct before/after comparison.
    private val threadLocal = ThreadLocal.withInitial { HashMap<String, HtmlRenderer>() }
    private lateinit var content: @Composable () -> Unit

    @Setup
    open fun setup() {
        content = {
            repeat(rows) {
                Div {
                    Text("row <&>😀")
                    Span { Text("tail") }
                }
            }
        }
        // Check identical output for every strategy before measuring performance.
        val expected = "<div>row &lt;&amp;&gt;😀<span>tail</span></div>".repeat(rows)
        check(expected, fresh())
        check(expected, threadLocal())
        check(expected, sharedKeyedPool())
    }

    // Allocation baseline: each call owns a new renderer.
    @Benchmark
    open fun fresh(): String = render(HtmlRenderer())

    // Each worker reuses its own renderer.
    @Benchmark
    open fun threadLocal(): String {
        val renderer = threadLocal.get().getOrPut("template") { HtmlRenderer() }
        return render(renderer)
    }

    // Workers borrow exclusive renderers from the same keyed pool.
    @Benchmark
    open fun sharedKeyedPool(): String {
        val pool = checkNotNull(getHtmlRendererPool("benchmark-template"))
        val renderer = pool.borrow()
        val result = render(renderer)
        pool.recycle(renderer)
        return result
    }

    private fun render(renderer: HtmlRenderer): String {
        // Use the same rendering path in every case, excluding public validation wrappers.
        return renderer.render(
            hydratable = true,
            requireHtmlDocumentRoot = false,
            chunkSink = null,
            chunkSize = 2048,
            prefix = "",
            content = content,
        )
    }

    private fun check(expected: String, actual: String) {
        if (expected != actual) {
            throw AssertionError("Every storage strategy must render the expected HTML")
        }
    }
}
