package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.web.elements.Div
import androidx.compose.web.renderComposable
import kotlinx.browser.document
import kotlinx.dom.clear
import kotlin.test.Test
import kotlin.test.assertEquals

class DomSideEffectTests {

    @Test
    fun canCreateElementsInDomSideEffect() {
        val root = "div".asHtmlElement()

        renderComposable(
            root = root
        ) {
            Div {
                DomSideEffect {
                    it.appendChild(
                        document.createElement("p").also {
                            it.appendChild(document.createTextNode("Hello World!"))
                        }
                    )
                }
            }
        }
        assertEquals(
            expected = "<div><div><p>Hello World!</p></div></div>",
            actual = root.outerHTML
        )
    }

    @Test
    fun canUpdateElementsCreatedInDomSideEffect() = runTest {
        var i: Int by mutableStateOf(0)
        val disposeCalls = mutableListOf<Int>()

        @Composable
        fun CustomDiv(value: Int) {
            Div {
                DomSideEffect(value) {
                    it.appendChild(
                        it.appendChild(document.createTextNode("Value = $value"))
                    )
                    onDispose {
                        disposeCalls.add(value)
                        it.clear()
                    }
                }
            }
        }

        composition { CustomDiv(i) }

        assertEquals(
            expected = "<div><div>Value = 0</div></div>",
            actual = root.outerHTML
        )

        i = 1

        waitChanges()
        assertEquals(
            expected = 1,
            actual = disposeCalls.size,
            message = "Only one onDispose call expected"
        )
        assertEquals(
            expected = 0,
            actual = disposeCalls[0],
            message = "onDispose should be called with a previous value"
        )
        assertEquals(
            expected = "<div><div>Value = 1</div></div>",
            actual = root.outerHTML
        )
    }

    @Test
    fun onDisposeIsCalledWhenComposableRemovedFromComposition() = runTest {
        var showDiv: Boolean by mutableStateOf(true)
        var onDisposeCalledTimes = 0

        composition {
            if (showDiv) {
                Div {
                    DomSideEffect {
                        it.appendChild(document.createTextNode("Goedemorgen!"))
                        onDispose { onDisposeCalledTimes++ }
                    }
                }
            }
        }

        assertEquals(
            expected = "<div><div>Goedemorgen!</div></div>",
            actual = root.outerHTML
        )

        showDiv = false

        waitChanges()
        assertEquals(1, onDisposeCalledTimes)
        assertEquals(expected = "<div></div>", actual = root.outerHTML)
    }
}