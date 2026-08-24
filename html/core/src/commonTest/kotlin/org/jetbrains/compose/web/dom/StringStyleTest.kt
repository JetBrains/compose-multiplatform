package org.jetbrains.compose.web.dom

import org.jetbrains.compose.web.composeHtmlToString
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.color
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
