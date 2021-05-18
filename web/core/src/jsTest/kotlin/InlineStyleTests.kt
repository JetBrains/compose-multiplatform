import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.web.css.color
import androidx.compose.web.elements.Span
import androidx.compose.web.elements.Text
import kotlin.test.Test
import kotlin.test.assertEquals

class InlineStyleTests {

    @Test
    fun conditionalStyleAppliedProperly() = runTest {

        var isRed by mutableStateOf(true)
        composition {
            Span(
                style = {
                    if (isRed) {
                        color("red")
                    } else {
                        color("green")
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
        waitChanges()

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
                style = {
                    if (isRed) {
                        color("red")
                    }
                }
            ) {
                Text("text")
            }
        }

        assertEquals(
            expected = "<span style=\"\">text</span>",
            actual = root.innerHTML
        )

        isRed = true
        waitChanges()

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
                style = {
                    if (isRed) {
                        color("red")
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
        waitChanges()

        assertEquals(
            expected = "<span style=\"\">text</span>",
            actual = root.innerHTML
        )
    }

    @Test
    fun conditionalStyleUpdatedProperly() = runTest {
        var isRed by mutableStateOf(true)
        composition {
            Span(
                style = {
                    if (isRed) {
                        color("red")
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
            waitChanges()

            val expected = if (isRed) {
                "<span style=\"color: red;\">text</span>"
            } else {
                "<span style=\"\">text</span>"
            }
            assertEquals(
                expected = expected,
                actual = root.innerHTML
            )
        }
    }
}