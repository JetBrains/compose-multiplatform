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

package androidx.compose.foundation.text

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.text.input.ImeAction.Companion.Go
import androidx.compose.ui.text.input.ImeAction.Companion.Search
import androidx.compose.ui.text.input.ImeAction.Companion.Send
import androidx.compose.ui.text.input.ImeAction.Companion.Previous
import androidx.compose.ui.text.input.ImeAction.Companion.Next
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@ExperimentalComposeUiApi
@LargeTest
@RunWith(Parameterized::class)
class DefaultKeyboardActionsTest(param: Param) {
    @get:Rule
    val rule = createComposeRule()

    // We need to wrap the inline class parameter in another class because Java can't instantiate
    // the inline class.
    class Param(val imeAction: ImeAction) {
        override fun toString() = imeAction.toString()
    }

    private val imeAction = param.imeAction

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "ImeAction = {0}")
        fun initParameters() = listOf(
            // OS never shows a Default or None ImeAction.
            Param(Go), Param(Search), Param(Send), Param(Previous), Param(Next), Param(Done)
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun noActionSpecified_triggersDefaultCallback() {
        // Arrange.
        val initialTextField = "text field test tag"
        val (value1, value2, value3) = List(3) { TextFieldValue("Placeholder Text") }
        val (textField1, textField2, textField3) = FocusRequester.createRefs()
        var (focusState1, focusState2, focusState3) = List(3) { false }

        rule.setContent {
            Column {
                CoreTextField(
                    value = value1,
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(textField1)
                        .onFocusChanged { focusState1 = it.isFocused }
                )
                CoreTextField(
                    value = value2,
                    onValueChange = {},
                    modifier = Modifier
                        .testTag(initialTextField)
                        .focusRequester(textField2)
                        .focusProperties { previous = textField1; next = textField3 }
                        .onFocusChanged { focusState2 = it.isFocused },
                    imeOptions = ImeOptions(imeAction = imeAction)
                )
                CoreTextField(
                    value = value3,
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(textField3)
                        .onFocusChanged { focusState3 = it.isFocused }
                )
            }
        }

        // Act.
        rule.onNodeWithTag(initialTextField).performImeAction()

        // Assert.
        when (imeAction) {
            Next -> {
                // Focus Moves to the next item.
                assertThat(focusState1).isFalse()
                assertThat(focusState2).isFalse()
                assertThat(focusState3).isTrue()
            }
            Previous -> {
                // Focus Moves to the previous item.
                assertThat(focusState1).isTrue()
                assertThat(focusState2).isFalse()
                assertThat(focusState3).isFalse()
            }
            else -> {
                // No change to focus state.
                assertThat(focusState1).isFalse()
                assertThat(focusState2).isTrue()
                assertThat(focusState3).isFalse()
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun callingPerformDefaultAction_triggersDefaultImplementation() {
        // Arrange.
        val initialTextField = "text field test tag"
        val (value1, value2, value3) = List(3) { TextFieldValue("Placeholder Text") }
        val (textField1, textField2, textField3) = FocusRequester.createRefs()
        var (focusState1, focusState2, focusState3) = List(3) { false }

        rule.setContent {
            Column {
                CoreTextField(
                    value = value1,
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(textField1)
                        .onFocusChanged { focusState1 = it.isFocused }
                )
                CoreTextField(
                    value = value2,
                    onValueChange = {},
                    modifier = Modifier
                        .testTag(initialTextField)
                        .focusRequester(textField2)
                        .focusProperties { previous = textField1; next = textField3 }
                        .onFocusChanged { focusState2 = it.isFocused },
                    imeOptions = ImeOptions(imeAction = imeAction),
                    keyboardActions = KeyboardActions(
                        onDone = { defaultKeyboardAction(Done) },
                        onGo = { defaultKeyboardAction(Go) },
                        onNext = { defaultKeyboardAction(Next) },
                        onPrevious = { defaultKeyboardAction(Previous) },
                        onSearch = { defaultKeyboardAction(Search) },
                        onSend = { defaultKeyboardAction(Send) },
                    )
                )
                CoreTextField(
                    value = value3,
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(textField3)
                        .onFocusChanged { focusState3 = it.isFocused }
                )
            }
        }

        // Act.
        rule.onNodeWithTag(initialTextField).performImeAction()

        // Assert.
        when (imeAction) {
            Next -> {
                // Focus Moves to the next item.
                assertThat(focusState1).isFalse()
                assertThat(focusState2).isFalse()
                assertThat(focusState3).isTrue()
            }
            Previous -> {
                // Focus Moves to the previous item.
                assertThat(focusState1).isTrue()
                assertThat(focusState2).isFalse()
                assertThat(focusState3).isFalse()
            }
            else -> {
                // No change to focus state.
                assertThat(focusState1).isFalse()
                assertThat(focusState2).isTrue()
                assertThat(focusState3).isFalse()
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun triggeringDifferentDefaultKeyboardAction_thanTheActionClickedOnTheSoftwareKeyboard() {
        // Arrange.
        val initialTextField = "text field test tag"
        val (value1, value2) = List(2) { TextFieldValue("Placeholder Text") }
        val (textField1, textField2) = FocusRequester.createRefs()
        var (focusState1, focusState2) = arrayOf(false, false)

        rule.setContent {
            Column {
                CoreTextField(
                    value = value1,
                    onValueChange = {},
                    modifier = Modifier
                        .testTag(initialTextField)
                        .focusRequester(textField1)
                        .focusProperties { next = textField2 }
                        .onFocusChanged { focusState1 = it.isFocused },
                    imeOptions = ImeOptions(imeAction = imeAction),
                    keyboardActions = KeyboardActions(
                        onDone = { defaultKeyboardAction(Next) },
                        onGo = { defaultKeyboardAction(Next) },
                        onNext = { defaultKeyboardAction(Next) },
                        onPrevious = { defaultKeyboardAction(Next) },
                        onSearch = { defaultKeyboardAction(Next) },
                        onSend = { defaultKeyboardAction(Next) },
                    )
                )
                CoreTextField(
                    value = value2,
                    onValueChange = {},
                    modifier = Modifier
                        .focusRequester(textField2)
                        .onFocusChanged { focusState2 = it.isFocused }
                )
            }
        }

        // Act.
        rule.onNodeWithTag(initialTextField).performImeAction()

        // Assert.
        // Focus Moves to the next item.
        assertThat(focusState1).isFalse()
        assertThat(focusState2).isTrue()
    }
}
