package org.jetbrains.compose.web.core.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.keywords.auto
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.web.testutils.*

class InlineStyleTests {

    @Test
    fun conditionalStyleAppliedProperly() = runTest {

        var isRed by mutableStateOf(true)
        composition {
            Span(
                {
                    style {
                        if (isRed) {
                            color(Color.red)
                        } else {
                            color(Color.green)
                        }
                    }
                }
            ) {
                Text("text")
            }
        }

        assertEquals(
            expected = "<span style=\"color: red;\">text</span>",
            actual = root.innerHTML
        )

        isRed = false
        waitForChanges()

        assertEquals(
            expected = "<span style=\"color: green;\">text</span>",
            actual = root.innerHTML
        )
    }

    @Test
    fun conditionalStyleAddedWhenTrue() = runTest {
        var isRed by mutableStateOf(false)
        composition {
            Span(
                {
                    style {
                        if (isRed) {
                            color(Color.red)
                        }
                    }
                }
            ) {
                Text("text")
            }
        }

        assertEquals(
            expected = "<span>text</span>",
            actual = root.innerHTML
        )

        isRed = true
        waitForChanges()

        assertEquals(
            expected = "<span style=\"color: red;\">text</span>",
            actual = root.innerHTML
        )
    }

    @Test
    fun conditionalStyleGetsRemovedWhenFalse() = runTest {
        var isRed by mutableStateOf(true)
        composition {
            Span(
                {
                    style {
                        if (isRed) {
                            color(Color.red)
                        }
                    }
                }
            ) {
                Text("text")
            }
        }

        assertEquals(
            expected = "<span style=\"color: red;\">text</span>",
            actual = root.innerHTML
        )

        isRed = false
        waitForChanges()

        assertEquals(
            expected = "<span>text</span>",
            actual = root.innerHTML
        )
    }

    @Test
    fun conditionalStyleUpdatedProperly() = runTest {
        var isRed by mutableStateOf(true)
        composition {
            Span(
                {
                    style {
                        if (isRed) {
                            color(Color.red)
                        }
                    }
                }
            ) {
                Text("text")
            }
        }

        assertEquals(
            expected = "<span style=\"color: red;\">text</span>",
            actual = root.innerHTML
        )

        repeat(4) {
            isRed = !isRed
            waitForChanges()

            val expected = if (isRed) {
                "<span style=\"color: red;\">text</span>"
            } else {
                "<span>text</span>"
            }
            assertEquals(
                expected = expected,
                actual = root.innerHTML
            )
        }
    }

    @Test
    fun sequentialStyleAccumulation() = runTest {
        val k by mutableStateOf(40)
        composition {
            Span({
                style {
                    opacity(k / 100f)
                }

                id("container")

                style {
                    padding(k.px)
                }
            }) {}
        }

        with(nextChild()) {
            val attrsMap = getAttributeNames().associateWith { getAttribute(it) }
            assertEquals(2, attrsMap.size)
            assertEquals("container", attrsMap["id"])
            assertEquals("opacity: 0.4; padding: 40px;", attrsMap["style"])
        }
    }

    @Test
    fun heightAuto() = runTest {
        composition {
            Span({
                style {
                    height(auto)
                }
                id("container")
            })
        }

        with(nextChild()) {
            val attrsMap = getAttributeNames().associateWith { getAttribute(it) }
            assertEquals(2, attrsMap.size)
            assertEquals("container", attrsMap["id"])
            assertEquals("height: auto;", attrsMap["style"])
        }
    }
}
