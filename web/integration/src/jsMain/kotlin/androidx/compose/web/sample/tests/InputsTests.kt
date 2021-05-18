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

package androidx.compose.web.sample.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.attributes.InputType
import androidx.compose.web.attributes.checked
import androidx.compose.web.attributes.name
import androidx.compose.web.elements.Input
import androidx.compose.web.elements.TextArea

class InputsTests {
    val textAreaInputGetsPrinted by testCase {
        var state by remember { mutableStateOf("") }

        TestText(value = state)

        TextArea(
            value = state,
            attrs = {
                id("input")
                onTextInput { state = it.inputValue }
            }
        )
    }

    val textInputGetsPrinted by testCase {
        var state by remember { mutableStateOf("") }

        TestText(value = state)

        Input(
            type = InputType.Text,
            value = state,
            attrs = {
                id("input")
                onTextInput { state = it.inputValue }
            }
        )
    }

    val checkBoxChangesText by testCase {
        var checked by remember { mutableStateOf(false) }

        TestText(value = if (checked) "checked" else "not checked")

        Input(
            type = InputType.Checkbox,
            attrs = {
                id("checkbox")
                checked(checked)
                onCheckboxInput {
                    checked = !checked
                }
            }
        )
    }

    val radioButtonsChangeText by testCase {
        var text by remember { mutableStateOf("-") }

        TestText(value = text)

        Input(
            type = InputType.Radio,
            attrs = {
                id("r1")
                name("f1")

                onRadioInput {
                    text = "r1"
                }
            }
        )

        Input(
            type = InputType.Radio,
            attrs = {
                id("r2")
                name("f1")

                onRadioInput {
                    text = "r2"
                }
            }
        )
    }

    val rangeInputChangesText by testCase {
        var rangeState by remember { mutableStateOf(0) }

        TestText(rangeState.toString())

        Input(
            type = InputType.Range,
            attrs = {
                id("slider")
                attr("min", "0")
                attr("max", "100")
                attr("step", "5")

                onRangeInput {
                    val value: String = it.nativeEvent.target.asDynamic().value
                    rangeState = value.toInt()
                }
            }
        )
    }

    val timeInputChangesText by testCase {
        var timeState by remember { mutableStateOf("") }

        TestText(timeState)

        Input(
            type = InputType.Time,
            attrs = {
                id("time")
                onGenericInput {
                    val value: String = it.nativeEvent.target.asDynamic().value
                    timeState = value
                }
            }
        )
    }

    val dateInputChangesText by testCase {
        var timeState by remember { mutableStateOf("") }

        TestText(timeState)

        Input(
            type = InputType.Date,
            attrs = {
                id("date")
                onGenericInput {
                    val value: String = it.nativeEvent.target.asDynamic().value
                    timeState = value
                }
            }
        )
    }

    val dateTimeLocalInputChangesText by testCase {
        var timeState by remember { mutableStateOf("") }

        TestText(timeState)

        Input(
            type = InputType.DateTimeLocal,
            attrs = {
                id("dateTimeLocal")
                onGenericInput {
                    val value: String = it.nativeEvent.target.asDynamic().value
                    timeState = value
                }
            }
        )
    }

    val fileInputChangesText by testCase {
        var filePath by remember { mutableStateOf("") }

        TestText(filePath)

        Input(
            type = InputType.File,
            attrs = {
                id("file")
                onGenericInput {
                    val value: String = it.nativeEvent.target.asDynamic().value
                    filePath = value
                }
            }
        )
    }

    val colorInputChangesText by testCase {
        var color by remember { mutableStateOf("") }

        TestText(color)

        Input(
            type = InputType.Color,
            attrs = {
                id("color")
                onGenericInput {
                    val value: String = it.nativeEvent.target.asDynamic().value
                    color = value
                }
            }
        )
    }
}