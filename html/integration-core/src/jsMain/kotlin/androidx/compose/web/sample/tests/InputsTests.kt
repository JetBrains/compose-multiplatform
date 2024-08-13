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
            attrs = {
                id("input")
                onInput { state = it.value }
            }
        )
    }

    val textInputGetsPrinted by testCase {
        var state by remember { mutableStateOf("Initial-") }

        TestText(value = state)

        Input(
            type = InputType.Text,
            attrs = {
                defaultValue(state)
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
                checked(checked)
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
                onChange { state = it.value!!.toString() }
            })
        }
    }

    val changeEventInTextAreaUpdatesText by testCase {
        var state by remember { mutableStateOf("None") }

        P { TestText(state) }

        Div {
            TextArea(attrs = {
                defaultValue(state)
                id("textArea")
                onChange { state = it.value }
            })
        }
    }

    val beforeInputEventUpdatesText by testCase {
        var inputState by remember { mutableStateOf("") }
        var state by remember { mutableStateOf("None") }

        P { TestText(state) }
        P { TestText(inputState, id = "txt2") }


        Div {
            TextInput(value = inputState, attrs = {
                id("textInput")
                onBeforeInput {
                    state = it.data ?: ""
                }
                onInput {
                    inputState = it.value
                }
            })
        }
    }

    val beforeInputEventInTextAreaUpdatesText by testCase {
        var inputState by remember { mutableStateOf("") }
        var state by remember { mutableStateOf("None") }

        P { TestText(state) }
        P { TestText(inputState, id = "txt2") }


        Div {
            TextArea(attrs = {
                id("textArea")
                onBeforeInput {
                    state = it.data ?: ""
                }
                onInput {
                    inputState = it.value
                }
            })
        }
    }

    val stopOnInputImmediatePropagationWorks by testCase {
        var state by remember { mutableStateOf("None") }

        var shouldStopImmediatePropagation by remember { mutableStateOf(false) }

        P { TestText(state) }

        Div {
            Input(type = InputType.Radio, attrs = {
                id("radioBtn")
                onInput {
                    shouldStopImmediatePropagation = true
                    state = "None"
                }
            })

            Input(type = InputType.Checkbox, attrs = {
                id("checkbox")
                onInput {
                    if (shouldStopImmediatePropagation) it.stopImmediatePropagation()
                    state = "onInput1"
                }
                onInput { state = "onInput2" }
            })
        }
    }

    val preventDefaultWorks by testCase {
        var state by remember { mutableStateOf("None") }
        var state2 by remember { mutableStateOf("None") }

        P { TestText(state) }
        P { TestText(state2, id = "txt2") }

        Input(
            type = InputType.Checkbox,
            attrs = {
                id("checkbox")
                onClick {
                    state = "Clicked but check should be prevented"
                    it.nativeEvent.preventDefault()
                }
                onInput {
                    state2 = "This text should never be displayed as onClick calls preventDefault()"
                }
            }
        )
    }

    val stopPropagationWorks by testCase {
        var state by remember { mutableStateOf("None") }
        var state2 by remember { mutableStateOf("None") }

        var shouldStopPropagation by remember { mutableStateOf(false) }

        P { TestText(state) }
        P { TestText(state2, id = "txt2") }

        Div {
            Input(type = InputType.Radio, attrs = {
                id("radioBtn")
                onInput {
                    shouldStopPropagation = true
                    state = "None"
                    state2 = "None"
                }
            })

            Div(attrs = {
                addEventListener(EventsListenerScope.INPUT) {
                    state2 = "div caught an input"
                }
            }) {
                Input(type = InputType.Checkbox, attrs = {
                    id("checkbox")
                    onInput {
                        if (shouldStopPropagation) it.stopPropagation()
                        state = "childInput"
                    }
                })
            }
        }
    }
}
