/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.benchmark.text.empirical

import androidx.compose.foundation.benchmark.text.filterForCi
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

object AllApps {
    /**
     * For all apps, the vast majority of text is <64 characters.
     *
     * Examples of typical text
     *
     * "OK"
     * "Close"
     * "Click below to learn more"
     */
    val TextLengths: Array<Any> = arrayOf(2, 16, 32, 64).filterForCi()
    val SpanCounts: Array<Any> = arrayOf(4, 16).filterForCi()
    val TextLengthsWithSpans: List<Array<Any>> = TextLengths.cartesian(SpanCounts).filterForCi()
}

object SocialApps {
    /**
     * Social apps show lots of adjacent-text like "Profile" or "userName" mixed with some longer
     * UGC.
     */
    val TextLengths: Array<Any> = arrayOf(32).filterForCi()
    val SpanCounts: Array<Any> = arrayOf(4, 8).filterForCi()
    val TextLengthsWithSpans: List<Array<Any>> = TextLengths.cartesian(SpanCounts).filterForCi()
}

object ChatApps {
    /**
     * For chat apps, strings tend to be longer due to user generated content.
     */
    val TextLengths: Array<Any> = arrayOf(256, 512).filterForCi()
    val SpanCounts: Array<Any> = arrayOf(2).filterForCi()
    val TextLengthsWithSpans: List<Array<Any>> = TextLengths.cartesian(SpanCounts).filterForCi()
}

object ShoppingApps {
    /**
     * Shopping apps are more designed focused with short, intentional, text usage
     */
    val TextLengths: Array<Any> = arrayOf(2, 64).filterForCi()
    val SpanCounts: Array<Any> = arrayOf(16).filterForCi()
    val TextLengthsWithSpans: List<Array<Any>> = TextLengths.cartesian(SpanCounts).filterForCi()
}

/**
 * Generates the string
 *
 * "aaaa ".repeat(size)
 *
 * This is intentionally designed to incur low overhead in platform layout due to hitting layout
 * caching, isolating the runtime of Compose vs the runtime of platform layout.
 *
 * Note that platform layout cost is not 0 with these cacheable strings. See [StaticLayoutBaseline]
 * to determine the cost incurred by laying out these cached strings.
 */
fun generateCacheableStringOf(size: Int): String {
    var workingSize = size
    val builder = StringBuilder(size)
    while (workingSize > 0) {
        repeat(Integer.min(8, workingSize) - 1) {
            builder.append("a")
            workingSize--
        }
        if (workingSize > 1) {
            builder.append(" ")
        } else {
            builder.append("a")
        }
        workingSize--
    }
    return builder.toString()
}

/**
 * Append [spanCount] non-MetricsEffecting spans to [this]
 *
 * Return as [AnnotatedString].
 *
 * Note all spans are full width in this implementation.
 */
internal fun String.annotateWithSpans(spanCount: Int): AnnotatedString {
    return buildAnnotatedString {
        repeat(spanCount) {
            // this appends a [ForegroundColorSpan] which is not [MetricsAffectingSpan]
            pushStyle(SpanStyle(color = Color(it, it, it)))
        }
        append(this@annotateWithSpans)
        pop((spanCount - 1).coerceAtLeast(0))
    }
}

internal const val BenchmarkInlineContentId = "BenchmarkInlineContent.Id"

/**
 * Add inline content to a String.
 */
internal fun String.annotateWithInlineContent(): AnnotatedString {
    return buildAnnotatedString {
        appendInlineContent(BenchmarkInlineContentId)
        append(this@annotateWithInlineContent)
    }
}

/**
 * ([1,2,3] X [A, B]) -> [[1, A], [1, B], [2, A], ...
 */
private fun Array<Any>.cartesian(rhs: Array<Any>): List<Array<Any>> = flatMap { lhs ->
    rhs.map { arrayOf(lhs, it) }
}