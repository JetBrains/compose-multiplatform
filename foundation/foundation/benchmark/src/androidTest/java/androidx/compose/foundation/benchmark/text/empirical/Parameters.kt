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
    val TextLengths: Array<Any> = arrayOf(2, 16, 32, 64)
    val SpanCounts: Array<Any> = arrayOf(4, 16)
    val TextLengthsWithSpans: List<Array<Any>> = TextLengths.cartesian(SpanCounts)
}

object SocialApps {
    /**
     * Social apps show lots of adjacent-text like "Profile" or "userName" mixed with some longer
     * UGC.
     */
    val TextLengths: Array<Any> = arrayOf(32)
    val SpanCounts: Array<Any> = arrayOf(4, 8)
    val TextLengthsWithSpans: List<Array<Any>> = TextLengths.cartesian(SpanCounts)
}

object ChatApps {
    /**
     * For chat apps, strings tend to be longer due to user generated content.
     */
    val TextLengths: Array<Any> = arrayOf(256, 512)
    val SpanCounts: Array<Any> = arrayOf(2)
    val TextLengthsWithSpans: List<Array<Any>> = TextLengths.cartesian(SpanCounts)
}

object ShoppingApps {
    /**
     * Shopping apps are more designed focused with short, intentional, text usage
     */
    val TextLengths: Array<Any> = arrayOf(2, 64)
    val SpanCounts: Array<Any> = arrayOf(16)
    val TextLengthsWithSpans: List<Array<Any>> = TextLengths.cartesian(SpanCounts)
}

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