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

    val checkBoxMutableValueChangesThroughOnChange by testCase {
        var checked by remember { mutableStateOf(false) }

        P { TestText(checked.toString()) }

        Div {
            CheckboxInput(checked = checked) {
                id("checkbox")
                onChange { event ->
                    checked = event.value
                }
            }
        }
    }

    val checkBoxMutableValueChangesForEveryOnChange by testCase {
        var checked by remember { mutableStateOf(true) }

        P { TestText(checked.toString()) }

        Div {
            CheckboxInput(checked = checked) {
                id("checkbox")
                onChange { event ->
                    checked = event.value
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

    val radioMutableCheckedChanges by testCase {
        var checked by remember { mutableStateOf(0) }

        TestText("Checked - $checked")

        Div {
            RadioInput(checked = checked == 1) {
                id("radio1")
                name("group1")
                onInput { checked = 1 }
            }
            RadioInput(checked = checked == 2) {
                id("radio2")
                name("group1")
                onInput { checked = 2 }
            }
        }
    }

    val radioMutableCheckedChangesThroughOnChange by testCase {
        var checked by remember { mutableStateOf(0) }

        TestText("Checked - $checked")

        Div {
            RadioInput(checked = checked == 1) {
                id("radio1")
                name("group1")
                onChange { event ->
                    if (event.value) checked = 1
                }
            }
            RadioInput(checked = checked == 2) {
                id("radio2")
                name("group1")
                onChange { event ->
                    if (event.value) checked = 2
                }
            }
        }
    }

    val numberHardcodedNeverChanges by testCase {
        var typedValue by remember { mutableStateOf("None") }
        TestText(value = typedValue)

        NumberInput(value = 5, min = 0, max = 100) {
            id("numberInput")
            onInput {
                typedValue = it.value.toString()
            }
        }
    }

    val numberMutableChanges by testCase {
        var value by remember { mutableStateOf(5) }
        TestText(value = value.toString())

        NumberInput(value = value, min = 0, max = 100) {
            id("numberInput")
            onInput {
                value = it.value!!.toInt()
            }
        }
    }

    val rangeHardcodedNeverChanges by testCase {
        var typedValue by remember { mutableStateOf("None") }

        TestText(value = typedValue)

        RangeInput(value = 21) {
            id("rangeInput")
            onInput {
                typedValue = it.value.toString()
            }
        }
    }

    val rangeMutableChanges by testCase {
        var value by remember { mutableStateOf(10) }

        TestText(value = value.toString())

        RangeInput(value = value) {
            id("rangeInput")
            onInput {
                value = it.value!!.toInt()
            }
        }
    }

    val emailHardcodedNeverChanges by testCase {
        var typedValue by remember { mutableStateOf("None") }
        TestText(value = typedValue)

        EmailInput(value = "a@a.abc") {
            id("emailInput")
            onInput {
                typedValue = it.value
            }
        }
    }

    val emailMutableChanges by testCase {
        var value by remember { mutableStateOf("") }
        TestText(value = value)

        EmailInput(value = value) {
            id("emailInput")
            onInput {
                value = it.value
            }
        }
    }

    val passwordHardcodedNeverChanges by testCase {
        var typeValue by remember { mutableStateOf("None") }
        TestText(value = typeValue)

        PasswordInput(value = "123456") {
            id("passwordInput")
            onInput {
                typeValue = it.value
            }
        }
    }

    val passwordMutableChanges by testCase {
        var value by remember { mutableStateOf("") }
        TestText(value = value)

        EmailInput(value = value) {
            id("passwordInput")
            onInput {
                value = it.value
            }
        }
    }

    val searchHardcodedNeverChanges by testCase {
        var typeValue by remember { mutableStateOf("None") }
        TestText(value = typeValue)

        SearchInput(value = "hardcoded") {
            id("searchInput")
            onInput {
                typeValue = it.value
            }
        }
    }

    val searchMutableChanges by testCase {
        var typeValue by remember { mutableStateOf("") }
        TestText(value = typeValue)

        SearchInput(value = typeValue) {
            id("searchInput")
            onInput {
                typeValue = it.value
            }
        }
    }

    val telHardcodedNeverChanges by testCase {
        var typedValue by remember { mutableStateOf("None") }
        TestText(value = typedValue)

        TelInput(value = "123456") {
            id("telInput")
            onInput {
                typedValue = it.value
            }
        }
    }

    val telMutableChanges by testCase {
        var value by remember { mutableStateOf("") }
        TestText(value = value)

        TelInput(value = value) {
            id("telInput")
            onInput {
                value = it.value
            }
        }
    }

    val urlHardcodedNeverChanges by testCase {
        var typedValue by remember { mutableStateOf("None") }
        TestText(value = typedValue)

        UrlInput(value = "www.site.com") {
            id("urlInput")
            onInput {
                typedValue = it.value
            }
        }
    }

    val urlMutableChanges by testCase {
        var value by remember { mutableStateOf("") }
        TestText(value = value)

        UrlInput(value = value) {
            id("urlInput")
            onInput {
                value = it.value
            }
        }
    }

    val hardcodedDateInputNeverChanges by testCase {
        var inputValue by remember { mutableStateOf("None") }

        TestText(inputValue)

        DateInput(value = "") {
            id("dateInput")
            onInput {
                inputValue = "onInput Caught"
            }
        }
    }

    val mutableDateInputChanges by testCase {
        var inputValue by remember { mutableStateOf("") }

        TestText(inputValue)

        DateInput(value = inputValue) {
            id("dateInput")
            onInput {
                inputValue = it.value
            }
        }
    }

    val hardcodedTimeNeverChanges by testCase {
        var typedValue by remember { mutableStateOf("None") }

        TestText(typedValue)

        TimeInput(value = "14:00") {
            id("time")
            onInput {
                typedValue = "onInput Caught"
            }
        }
    }

    val mutableTimeChanges by testCase {
        var value by remember { mutableStateOf("") }

        TestText(value)

        TimeInput(value = value) {
            id("time")
            onInput {
                value = it.value
            }
        }
    }
}
