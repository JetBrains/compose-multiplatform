package org.jetbrains.compose.html2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeHtmlToStringTest {

    @Test
    fun helloWorld() {
        val result = composeHtmlToString {
            HelloWorld()
        }

        assertEquals("<div>Hello, world!</div>", result)
    }

    @Test
    fun nested_structure_renders_in_order() {
        val html = composeHtmlToString {
            Div {
                Text("A")
                Div { Text("B") }
                Text("C")
            }
        }
        assertEquals("<div>A<div>B</div>C</div>", html)
    }

    @Test
    fun multiple_root_siblings_are_concatenated() {
        val html = composeHtmlToString {
            Div {}
            Div { Text("x") }
        }
        assertEquals("<div></div><div>x</div>", html)
    }

    @Test
    fun text_is_escaped() {
        val html = composeHtmlToString {
            Div { Text("Tom & Jerry <3 > all") }
        }
        // '&', '<', '>' must be escaped inside text nodes
        assertEquals("<div>Tom &amp; Jerry &lt;3 &gt; all</div>", html)
    }

    @Test
    fun empty_div_renders_with_closing_tag() {
        val html = composeHtmlToString { Div {} }
        assertEquals("<div></div>", html)
    }

    @Test
    fun with_effects() {
        var runDisposeEffect = false
        var runLaunchedEffect = false
        var runSideEffect = false

        val html = composeHtmlToString {
            var showText by remember { mutableStateOf(false) }

            Div {
                DisposableEffect(Unit) {
                    runDisposeEffect = true
                    onDispose {}
                }

                LaunchedEffect(Unit) {
                    runLaunchedEffect = true
                    showText = true
                }

                SideEffect { runSideEffect = true }

                if (showText) Text("Hello, world!")
            }
        }

        assertTrue(runDisposeEffect)
        assertTrue(runLaunchedEffect)
        assertTrue(runSideEffect)
        assertEquals("<div></div>", html)
    }
}

@Composable
fun HelloWorld() {
    Div {
        Text("Hello, world!")
    }
}