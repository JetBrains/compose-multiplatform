/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import benchmarks.compose.*
import benchmarks.model.*
import java.io.StringWriter
import java.io.Writer
import kotlinx.benchmark.*
import kotlinx.browser.dom.Element
import org.openjdk.jmh.annotations.Fork
import org.jetbrains.compose.web.dom.TagElement
import org.jetbrains.compose.web.dom.Text

/** Renders the same fixtures to a String, a Writer, or String chunks. */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 5)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgsAppend = ["-Xms256m", "-Xmx256m", "-Xss16m"])
open class SsrBenchmark {
    @Param(
        "tailwind-catalog", "form-app", "data-table", "svg-dashboard", "content-article",
        "static-text", "dynamic-text", "dynamic-text-escaped", "search-results", "deep-stack",
        "raw-text-style", "raw-text-elements", "fractional-style", "style-heavy-catalog",
        "stream-pre", "stream-textarea",
    )
    @JvmField
    var workload: String = ""

    private lateinit var content: @Composable () -> Unit
    private lateinit var key: String
    private var streamedLength = 0
    private val sink: (String) -> Unit = { streamedLength += it.length }
    private val writer = object : Writer() {
        override fun write(buffer: CharArray, offset: Int, length: Int) {
            streamedLength += length
        }

        override fun flush() {}
        override fun close() {}
    }

    @Setup
    open fun setup() {
        key = "ssr-$workload"
        // Construct fixtures and composable lambdas outside measurement.
        content = when (workload) {
            "tailwind-catalog" -> catalogFixture().let { data -> { TailwindCatalogDocument(data) } }
            "form-app" -> formFixture().let { data -> { FormAppDocument(data) } }
            "data-table" -> dataTableFixture().let { data -> { DataTableDocument(data) } }
            "svg-dashboard" -> svgDashboardFixture().let { data -> { SvgDashboardDocument(data) } }
            "content-article" -> articleFixture().let { data -> { ContentArticleDocument(data) } }
            "static-text" -> { { StaticText() } }
            "dynamic-text", "dynamic-text-escaped" -> textFixture(escaped = workload == "dynamic-text-escaped")
                .let { data -> { DynamicText(data) } }
            "search-results" -> searchResultsFixture().let { data -> { SearchResults(data) } }
            "deep-stack" -> { { DeepStack() } }
            "raw-text-style" -> { { RawTextStyle() } }
            "raw-text-elements" -> { { RawTextElements() } }
            "fractional-style" -> { { FractionalStyle() } }
            "style-heavy-catalog" -> catalogFixture().let { data -> { StyleHeavyCatalog(data) } }
            "stream-pre" -> streamingContent("pre")
            "stream-textarea" -> streamingContent("textarea")
            else -> error("Unknown workload: $workload")
        }

        // Check complete output before measuring streams with non-retaining sinks.
        val expected = renderToString()
        val output = StringWriter()
        composeHtmlToStream(writer = output, key = key, content = content)
        check(output.toString() == expected) { "Writer output differs for $workload" }
        val chunks = StringBuilder()
        composeHtmlToStream(sink = { chunks.append(it) }, key = key, content = content)
        check(chunks.toString() == expected) { "Callback output differs for $workload" }
        check(streamToWriter() == expected.length)
        check(streamToCallback() == expected.length)
    }

    @Benchmark
    open fun renderToString(): String = composeHtmlToString(key = key, content = content)

    @Benchmark
    open fun streamToWriter(): Int {
        streamedLength = 0
        composeHtmlToStream(writer = writer, key = key, content = content)
        return streamedLength
    }

    @Benchmark
    open fun streamToCallback(): Int {
        streamedLength = 0
        composeHtmlToStream(sink = sink, key = key, content = content)
        return streamedLength
    }
}

private fun streamingContent(tag: String): @Composable () -> Unit {
    val text = "a&😀α".repeat(512)
    return {
        TagElement<Element>(tag, null) {
            Text("\n")
            repeat(256) { Text(text) }
        }
    }
}
