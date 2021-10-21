/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.web.sample.tests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.sample.tests.TestText
import org.jetbrains.compose.web.sample.tests.testCase

class ControlledInputsCursorsPositionTests {

    val textInputTypingIntoMiddle by testCase {
        var onInputText by remember { mutableStateOf("None") }
        var textValue by remember { mutableStateOf("") }

        P { TestText(onInputText) }

        Div {
            TextInput(value = textValue, attrs = {
                id("textInput")
                onInput {
                    onInputText = it.value
                    textValue = it.value
                }
            })
        }
    }

    val textAreaTypingIntoMiddle by testCase {
        var onInputText by remember { mutableStateOf("None") }
        var textValue by remember { mutableStateOf("") }

        P { TestText(onInputText) }

        Div {
            TextArea(value = textValue, attrs = {
                id("textArea")
                onInput {
                    onInputText = it.value
                    textValue = it.value
                }
            })
        }
    }
}
