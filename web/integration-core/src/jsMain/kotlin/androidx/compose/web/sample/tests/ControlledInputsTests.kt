/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.sample.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.TextInput
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
}
