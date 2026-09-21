/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlin.test.Test
import kotlin.test.assertEquals

class RawTextScannerTest {
    @Test
    fun scannerMatchesPreviousValidatorAtEveryDelimiterAndCaseBoundary() {
        val delimiters = listOf(
            "\t", "\n", "\u000C", "\r", " ", "/", ">", "", "x", "\u0000", "\u00A0", "-",
        )
        for (tag in listOf("script", "style", "iframe", "xmp", "noembed", "noframes", "noscript")) {
            for (closing in listOf(false, true)) {
                val prefix = if (closing) "</" else "<"
                val regex = Regex("$prefix$tag(?=[\\t\\n\\u000C\\r />])", RegexOption.IGNORE_CASE)
                for (spelling in listOf(tag, tag.uppercase(), tag.replace('s', '\u017F'), tag.replace('i', '\u0130'))) {
                    for (delimiter in delimiters) {
                        for (before in listOf("", "<", "<scriptx ", "<!--", "🦊 text ")) {
                            val text = "$before$prefix$spelling${delimiter}tail"
                            for (start in listOf(0, 1, text.length)) {
                                assertEquals(
                                    regex.find(text, start)?.range?.first ?: -1,
                                    text.rawTextTagIndex(tag, closing, start),
                                    "$text @ $start",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
