/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.dom

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.css.*

class StringStyleTest {
    @Test
    fun rendersStyleSheetAsRawCssText() {
        val styleSheet = object : StyleSheet(usePrefix = false) {
            val content by style {
                color(Color.red)
                property("content", "\"a < b & c\"")
            }
        }

        val html = composeHtmlToString {
            Style(styleSheet)
        }

        assertEquals(
            "<style>.content { color: red; content: \"a < b & c\";}</style>",
            html,
        )
    }

    @Test
    fun normalizesStyleHtmlInputCharacters() {
        val styleSheet = object : StyleSheet(usePrefix = false) {
            val content by style {
                property("content", "\"first\r\nsecond\rthird\u0000fourth\"")
            }
        }

        val html = composeHtmlToString {
            Style(styleSheet)
        }

        assertEquals(
            "<style>.content { content: \"first\nsecond\nthird\uFFFDfourth\";}</style>",
            html,
        )
    }

    @Test
    fun rejectsStyleContentThatCanTerminateTheElement() {
        val styleSheet = object : StyleSheet(usePrefix = false) {
            val content by style {
                property("content", "\"</StYlE><script>alert(1)</script>\"")
            }
        }

        val failure = assertFailsWith<IllegalArgumentException> {
            composeHtmlToString {
                Style(styleSheet)
            }
        }

        assertContains(failure.message.orEmpty(), "Raw text for <style>")
    }

    @Test
    fun ordinaryStylesKeepFallbacksVariablesAndEscaping() {
        assertEquals(
            "<div style=\"display: -webkit-box; display: flex; --quote: &quot;&amp;&lt;; --quote: final\"></div>",
            composeHtmlToString {
                Div(attrs = {
                    style {
                        property("display", "-webkit-box")
                        property("display", "flex")
                        variable("quote", "\"&<")
                        variable("quote", "final")
                    }
                })
            },
        )
    }

    @Test
    fun nestedStylesheetFormattingPreservesCustomDelimiters() {
        val style = StyleScopeBuilder().apply {
            property("color", "red", important = true)
            variable("accent", "blue")
        }
        val leaf = object : CSSRuleDeclaration, CSSStyledRuleDeclaration {
            override val header = ".leaf"
            override val style = style
            override fun equals(other: Any?) = this === other
            override fun hashCode() = 1
        }
        val group = object : CSSGroupingRuleDeclaration {
            override val header = "@media screen"
            override val rules = listOf(leaf)
            override fun equals(other: Any?) = this === other
            override fun hashCode() = 2
        }
        assertEquals(
            "P@media screen {|P_.leaf {|P__color: red !important;|P__--accent: blue;|P_}|P}",
            group.stringPresentation(baseIndent = "P", indent = "_", delimiter = "|"),
        )
        assertEquals(
            "@media screen { .leaf {  color: red !important;  --accent: blue; }}",
            group.stringPresentation(indent = " ", delimiter = ""),
        )
    }


    @Test
    fun emptyAssignmentsAndPriorityResetKeepTheirOrder() {
        assertEquals(
            "<div style=\"color: blue; display: flex; --accent: blue\"></div>",
            composeHtmlToString {
                Div(attrs = { style {
                    property("color", "red")
                    property("color", "")
                    property("color", "blue")
                    property("display", "block", important = true)
                    property("display", "flex")
                    variable("accent", "red")
                    variable("accent", "")
                    variable("accent", "blue")
                } })
            },
        )
        assertEquals("<div></div>", composeHtmlToString {
            Div(attrs = {
                style {
                    property("color", "red")
                    property("color", "")
                }
            })
        })
    }

}
