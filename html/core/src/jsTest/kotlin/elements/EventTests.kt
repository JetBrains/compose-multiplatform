package org.jetbrains.compose.web.core.tests

import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.MouseEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.compose.web.testutils.*

class EventTests {

    @Test
    fun buttonClickHandled() = runTest {
        var handled = false

        composition {
            Button(
                {
                    onClick { handled = true }
                }
            ) {}
        }

        assertEquals(1, root.childElementCount)

        val btn = root.firstChild as HTMLElement
        btn.dispatchEvent(MouseEvent("click"))

        assertTrue(handled)
    }

    @Test
    fun checkboxInputHandled() = runTest {
        var handled = false

        composition {
            Input(
                type = InputType.Checkbox,
                attrs = {
                    onInput { handled = true }
                }
            )
        }

        val checkbox = root.firstChild as HTMLInputElement
        checkbox.dispatchEvent(Event("input"))

        assertTrue(handled)
    }

    @Test
    fun radioButtonInputHandled() = runTest {
        var handled = false

        composition {
            Input(
                type = InputType.Radio,
                attrs = {
                    onInput { handled = true }
                }
            )
        }

        val radio = root.firstChild as HTMLInputElement
        radio.dispatchEvent(Event("input"))
        assertEquals(false, radio.checked)

        assertTrue(handled)
    }

    @Test
    fun textAreaInputHandled() = runTest {
        var handled = false

        composition {
            TextArea(
                value = ""
            ) {
                onInput { handled = true }
            }
        }

        val radio = root.firstChild as HTMLTextAreaElement
        radio.dispatchEvent(InputEvent("input"))

        assertEquals("", radio.value)

        assertTrue(handled)
    }
}
