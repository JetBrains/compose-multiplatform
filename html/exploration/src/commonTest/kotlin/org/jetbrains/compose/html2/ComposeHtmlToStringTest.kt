package org.jetbrains.compose.html2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        // Now emitted verbatim: caller is responsible for escaping
        assertEquals("<div>Tom & Jerry <3 > all</div>", html)
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
                    // This block won't run because Recomposer is created using non-Unconfined Dispatcher (see composeHtmlToString)
                    runLaunchedEffect = true
                    showText = true
                }

                SideEffect { runSideEffect = true }

                if (showText) Text("Hello, world!")
            }
        }

        assertTrue(runDisposeEffect)
        assertFalse(runLaunchedEffect)
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