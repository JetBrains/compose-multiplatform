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