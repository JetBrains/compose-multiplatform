package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.color
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
