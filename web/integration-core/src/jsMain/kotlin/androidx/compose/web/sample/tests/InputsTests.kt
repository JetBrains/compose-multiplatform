package org.jetbrains.compose.web.sample.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.dom.*

class InputsTests {
    val textAreaInputGetsPrinted by testCase {
        var state by remember { mutableStateOf("") }

        TestText(value = state)

        TextArea(
            value = state,
            attrs = {
                id("input")
                onInput { state = it.value }
            }
        )
    }

    val textInputGetsPrinted by testCase {
        var state by remember { mutableStateOf("") }

        TestText(value = state)

        Input(
            type = InputType.Text,
            attrs = {
                value(state)
                id("input")
                onInput { state = it.value }
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
                if (checked) {
                    checked()
                }
                onInput { checked = !checked }
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

                onInput { text = "r1" }
            }
        )

        Input(
            type = InputType.Radio,
            attrs = {
                id("r2")
                name("f1")

                onInput { text = "r2" }
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

                onInput {
                    rangeState = it.value?.toInt() ?: 0
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
                onInput {
                    timeState = it.value
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
                onInput {
                    timeState = it.value
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
                onInput {
                    timeState = it.value
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
                onInput {
                    filePath = it.value
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
                onInput {
                    color = it.value
                }
            }
        )
    }

    val invalidInputUpdatesText by testCase {
        var state by remember { mutableStateOf("None") }

        P { TestText(state) }

        Form(attrs = {
            action("#")
        }) {
            Input(type = InputType.Number, attrs = {
                id("numberInput")
                min("1")
                max("5")

                onInvalid {
                    state = "INVALID VALUE ENTERED"
                }

                onInput { state = "SOMETHING ENTERED" }
            })

            Input(type = InputType.Submit, attrs = {
                value("submit")
                id("submitBtn")
            })
        }
    }


    val changeEventUpdatesText by testCase {
        var state by remember { mutableStateOf("None") }

        P { TestText(state) }

        Div {
            Input(type = InputType.Number, attrs = {
                id("numberInput")
                onChange { state = "INPUT HAS CHANGED" }
            })
        }
    }
}
