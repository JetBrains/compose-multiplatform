/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.sample.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.sample.tests.TestText
import org.jetbrains.compose.web.sample.tests.testCase

class ControlledInputsTests {

    val textInputHardcodedValueShouldNotChange by testCase {
        var onInputText by remember { mutableStateOf("None") }

        P { TestText(onInputText) }

        Div {
            TextInput(value = "hardcoded", attrs = {
                id("textInput")
                onInput {
                    onInputText = it.value
                }
            })
        }
    }

    val textInputMutableValueShouldGetOverridden by testCase {
        var onInputText by remember { mutableStateOf("InitialValue") }

        P { TestText(onInputText) }

        Div {
            TextInput(value = onInputText, attrs = {
                id("textInput")
                onInput {
                    onInputText = "OVERRIDDEN VALUE"
                }
            })
        }
    }

    val textInputMutableValueShouldChange by testCase {
        var onInputText by remember { mutableStateOf("InitialValue") }

        P { TestText(onInputText) }

        Div {
            TextInput(value = onInputText, attrs = {
                id("textInput")
                onInput {
                    onInputText = it.value
                }
            })
        }
    }

    val textAreaHardcodedValueShouldNotChange by testCase {
        var onInputText by remember { mutableStateOf("None") }

        P { TestText(onInputText) }

        Div {
            TextArea(value = "hardcoded", attrs = {
                id("textArea")
                onInput {
                    onInputText = it.value
                }
            })
        }
    }

    val textAreaMutableValueShouldGetOverridden by testCase {
        var onInputText by remember { mutableStateOf("InitialValue") }

        P { TestText(onInputText) }

        Div {
            TextArea(value = onInputText, attrs = {
                id("textArea")
                onInput {
                    onInputText = "OVERRIDDEN VALUE"
                }
            })
        }
    }

    val textAreaMutableValueShouldChange by testCase {
        var onInputText by remember { mutableStateOf("InitialValue") }

        P { TestText(onInputText) }

        Div {
            TextArea(value = onInputText, attrs = {
                id("textArea")
                onInput {
                    onInputText = it.value
                }
            })
        }
    }

    val checkBoxHardcodedNeverChanges by testCase {
        var checkClicked by remember { mutableStateOf(false) }

        P { TestText(checkClicked.toString()) }

        Div {
            CheckboxInput(checked = false) {
                id("checkbox")
                onInput {
                    checkClicked = it.value
                }
            }
        }
    }

    val checkBoxMutableValueChanges by testCase {
        var checked by remember { mutableStateOf(false) }

        P { TestText(checked.toString()) }

        Div {
            CheckboxInput(checked = checked) {
                id("checkbox")
                onInput {
                    checked = it.value
                }
            }
        }
    }

    val checkBoxDefaultCheckedChangesDoesntAffectState by testCase {
        var checked by remember { mutableStateOf(true) }

        P { TestText(checked.toString()) }

        Div {
            Input(type = InputType.Checkbox) {
                id("checkboxMirror")
                if (checked) defaultChecked()
            }

            Input(type = InputType.Checkbox) {
                id("checkboxMain")
                checked(checked)
                onInput { checked = it.value }
            }
        }
    }

    val radioHardcodedNeverChanges by testCase {
        Div {
            RadioInput(checked = true) {
                id("radio1")
                name("group1")
            }
            RadioInput(checked = false) {
                id("radio2")
                name("group1")
            }
        }
    }
}
