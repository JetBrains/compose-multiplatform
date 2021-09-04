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

class UncontrolledInputsTests {

    val textInputDefaultValueRemainsTheSameButValueCanBeChanged by testCase {
        var inputValue by remember { mutableStateOf("") }

        Input(type = InputType.Text) {

            id("textInput")
            defaultValue("defaultInputValue")

            attr("data-input-value", inputValue)

            onInput {
                inputValue = it.value
            }
        }
    }

    val textAreaDefaultValueRemainsTheSameButValueCanBeChanged by testCase {
        var inputValue by remember { mutableStateOf("") }

        TextArea {

            id("textArea")
            defaultValue("defaultTextAreaValue")

            attr("data-text-area-value", inputValue)

            onInput {
                inputValue = it.value
            }
        }
    }

    val checkBoxDefaultCheckedRemainsTheSameButCheckedCanBeChanged by testCase {
        var checkedValue by remember { mutableStateOf(true) }

        Input(type = InputType.Checkbox) {
            id("checkbox")
            defaultChecked()
            value("checkbox-value")

            attr("data-checkbox", checkedValue.toString())

            onInput {
                checkedValue = it.value
            }
        }
    }

    val radioDefaultCheckedRemainsTheSameButCheckedCanBeChanged by testCase {
        var checkedValue by remember { mutableStateOf("") }

        Input(type = InputType.Radio) {
            id("radio1")
            defaultChecked()
            value("radio-value1")
            name("radiogroup")

            attr("data-radio", checkedValue)

            onInput {
                checkedValue = "radio-value1"
            }
        }

        Input(type = InputType.Radio) {
            id("radio2")
            value("radio-value2")
            name("radiogroup")

            attr("data-radio", checkedValue)

            onInput {
                checkedValue = "radio-value2"
            }
        }
    }

    val numberDefaultValueRemainsTheSameButValueCanBeChanged by testCase {
        var typedValue by remember { mutableStateOf("None") }

        TestText(value = "Value = $typedValue")

        Input(type = InputType.Number) {
            id("numberInput")
            defaultValue(11)
            onInput {
                typedValue = it.value.toString()
            }
        }
    }

    val rangeDefaultValueRemainsTheSameButValueCanBeChanged by testCase {
        var typedValue by remember { mutableStateOf("None") }

        TestText(value = "Value = $typedValue")

        Input(type = InputType.Range) {
            id("rangeInput")
            defaultValue(7)
            onInput {
                typedValue = it.value.toString()
            }
        }
    }

    val emailDefaultValueRemainsTheSameButValueCanBeChanged by testCase {
        var typedValue by remember { mutableStateOf("None") }
        TestText(value = "Value = $typedValue")

        Input(type = InputType.Email) {
            id("emailInput")
            defaultValue("a@a.abc")
            onInput {
                typedValue = it.value
            }
        }
    }

    val passwordDefaultValueRemainsTheSameButValueCanBeChanged by testCase {
        var typedValue by remember { mutableStateOf("None") }
        TestText(value = "Value = $typedValue")

        Input(type = InputType.Password) {
            id("passwordInput")
            defaultValue("1111")
            onInput {
                typedValue = it.value
            }
        }
    }

    val searchDefaultValueRemainsTheSameButValueCanBeChanged by testCase {
        var typedValue by remember { mutableStateOf("None") }
        TestText(value = "Value = $typedValue")

        Input(type = InputType.Search) {
            id("searchInput")
            defaultValue("kotlin")
            onInput {
                typedValue = it.value
            }
        }
    }

    val telDefaultValueRemainsTheSameButValueCanBeChanged by testCase {
        var typedValue by remember { mutableStateOf("None") }
        TestText(value = typedValue)

        Input(type = InputType.Tel) {
            id("telInput")
            defaultValue("123123")
            onInput {
                typedValue = it.value
            }
        }
    }

    val urlDefaultValueRemainsTheSameButValueCanBeChanged by testCase {
        var typedValue by remember { mutableStateOf("None") }
        TestText(value = typedValue)

        Input(type = InputType.Url) {
            id("urlInput")
            defaultValue("www.site.com")
            onInput {
                typedValue = it.value
            }
        }
    }

}
