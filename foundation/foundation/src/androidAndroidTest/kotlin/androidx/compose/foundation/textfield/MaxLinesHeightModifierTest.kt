/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.foundation.text.maxLinesHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class MaxLinesHeightModifierTest {

    private val longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
        "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
        " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
        "fugiat nulla pariatur."

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun maxLinesHeight_shortInputText() {
        val (textLayoutResult, height) = setTextFieldWithMaxLines(TextFieldValue("abc"), 5)

        rule.runOnIdle {
            assertThat(textLayoutResult).isNotNull()
            assertThat(textLayoutResult!!.lineCount).isEqualTo(1)
            assertThat(textLayoutResult.size.height).isEqualTo(height)
        }
    }

    @Test
    fun maxLinesHeight_notApplied_infiniteMaxLines() {
        val (textLayoutResult, height) =
            setTextFieldWithMaxLines(TextFieldValue(longText), Int.MAX_VALUE)

        rule.runOnIdle {
            assertThat(textLayoutResult).isNotNull()
            assertThat(textLayoutResult!!.size.height).isEqualTo(height)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun maxLinesHeight_invalidValue() {
        rule.setContent {
            CoreTextField(
                value = TextFieldValue(),
                onValueChange = {},
                modifier = Modifier.maxLinesHeight(0, TextStyle.Default)
            )
        }
    }

    @Test
    fun maxLinesHeight_longInputText() {
        val (textLayoutResult, _) = setTextFieldWithMaxLines(TextFieldValue(longText), 2)

        rule.runOnIdle {
            assertThat(textLayoutResult).isNotNull()
            assertThat(textLayoutResult!!.lineCount).isGreaterThan(2)
        }
    }

    @Test
    fun testInspectableValue() {
        val modifier = Modifier.maxLinesHeight(10, TextStyle.Default) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("maxLinesHeight")
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("maxLines", 10),
            ValueElement("textStyle", TextStyle.Default)
        )
    }

    private fun setTextFieldWithMaxLines(
        textFieldValue: TextFieldValue,
        maxLines: Int
    ): Pair<TextLayoutResult?, Int?> {
        var textLayoutResult: TextLayoutResult? = null
        var height: Int? = null
        val positionedLatch = CountDownLatch(1)

        rule.setContent {
            Box(
                Modifier.onGloballyPositioned {
                    height = it.size.height
                    positionedLatch.countDown()
                }
            ) {
                CoreTextField(
                    value = textFieldValue,
                    onValueChange = {},
                    textStyle = TextStyle.Default,
                    modifier = Modifier
                        .requiredWidth(100.dp)
                        .maxLinesHeight(maxLines, TextStyle.Default),
                    onTextLayout = { textLayoutResult = it }
                )
            }
        }
        assertThat(positionedLatch.await(1, TimeUnit.SECONDS)).isTrue()

        return Pair(textLayoutResult, height)
    }
}