/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package elements

import androidx.compose.web.attributes.InputType
import androidx.compose.web.elements.Button
import androidx.compose.web.elements.Input
import androidx.compose.web.elements.TextArea
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.MouseEvent
import runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventTests {

    @Test
    fun buttonClickHandled() = runTest {
        var handeled = false

        composition {
            Button(
                attrs = {
                    onClick { handeled = true }
                }
            ) {}
        }

        assertEquals(1, root.childElementCount)

        val btn = root.firstChild as HTMLElement
        btn.dispatchEvent(MouseEvent("click"))

        assertTrue(handeled)
    }

    @Test
    fun checkboxInputHandled() = runTest {
        var handeled = false

        composition {
            Input(
                type = InputType.Checkbox,
                attrs = {
                    onCheckboxInput { handeled = true }
                }
            )
        }

        val checkbox = root.firstChild as HTMLInputElement
        checkbox.dispatchEvent(Event("input"))

        assertTrue(handeled)
    }

    @Test
    fun radioButtonInputHandled() = runTest {
        var handeled = false

        composition {
            Input(
                type = InputType.Radio,
                attrs = {
                    onRadioInput { handeled = true }
                }
            )
        }

        val radio = root.firstChild as HTMLInputElement
        radio.dispatchEvent(Event("input"))
        assertEquals(false, radio.checked)

        assertTrue(handeled)
    }

    @Test
    fun textAreaInputHandled() = runTest {
        var handeled = false

        composition {
            TextArea(
                attrs = {
                    onTextInput { handeled = true }
                },
                value = ""
            )
        }

        val radio = root.firstChild as HTMLTextAreaElement
        radio.dispatchEvent(InputEvent("input"))

        assertEquals("", radio.value)

        assertTrue(handeled)
    }
}