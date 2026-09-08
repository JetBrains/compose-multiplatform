/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.core.tests

import androidx.compose.web.events.SyntheticEvent
import org.jetbrains.compose.web.events.SyntheticAnimationEvent
import org.jetbrains.compose.web.events.SyntheticKeyboardEvent
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.compose.web.testutils.*

class EventTests {

    @Test
    fun explicitlyTypedCustomEventListener() = runTest {
        var observedValue = ""

        composition {
            Input(type = InputType.Text, attrs = {
                value("custom value")
                addEventListener<SyntheticEvent<HTMLInputElement>>("custom") { event ->
                    observedValue = event.target.value
                }
            })
        }

        val input = root.firstChild as HTMLInputElement
        input.dispatchEvent(Event("custom"))

        assertEquals("custom value", observedValue)
    }

    @Test
    fun keyboardLocaleCompatibilityAccessor() {
        val event = KeyboardEvent("keydown")
        event.asDynamic().locale = "nl-NL"

        assertEquals("nl-NL", SyntheticKeyboardEvent(event).locale)
    }

    @Test
    fun animationDetailsCompatibilityAccessor() {
        val event = Event("animationstart")
        event.asDynamic().animationName = "fade-in"
        event.asDynamic().elapsedTime = 1.25
        event.asDynamic().pseudoElement = "::before"

        val syntheticEvent = SyntheticAnimationEvent(event)
        assertEquals("fade-in", syntheticEvent.animationName)
        assertEquals(1.25, syntheticEvent.elapsedTime)
        assertEquals("::before", syntheticEvent.pseudoElement)
    }

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

    @Test
    fun selectionReadsValueWhenRequested() = runTest {
        var selection = ""

        composition {
            Input(
                type = InputType.Text,
                attrs = {
                    value("abcd")
                    onSelect { event ->
                        event.target.value = "wxyz"
                        selection = event.selection()
                    }
                },
            )
        }

        val input = root.firstChild as HTMLInputElement
        input.setSelectionRange(1, 3)
        input.dispatchEvent(Event("select"))

        assertEquals("xy", selection)
    }
}
