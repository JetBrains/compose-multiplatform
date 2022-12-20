/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.newtext.text

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTextApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class TextUsingModifierMinMaxLinesTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun whenMaxLines_isBoundOnLineHeight() {
        val text = "H\n".repeat(1000)
        val sizes: Array<IntSize?> = Array(5) { null }
        val layouts: Array<TextLayoutResult?> = Array(5) { null }
        var unboundedTextSize: IntSize? = null

        val textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY)
        rule.setContent {
            for (i in sizes.indices) {
                Box(
                    Modifier
                        .onSizeChanged { sizes[i] = it }
                        .width(5.dp)) {
                    TextUsingModifier(
                        text,
                        style = textStyle,
                        maxLines = i + 1,
                        onTextLayout = {
                            layouts[i] = it
                        }
                    )
                }
            }

            Box(
                Modifier
                    .onSizeChanged { unboundedTextSize = it }
                    .width(5.dp)) {
                TextUsingModifier(
                    text,
                    style = textStyle,
                    maxLines = Int.MAX_VALUE
                )
            }
        }
        rule.runOnIdle {
            for (i in 1 until sizes.size) {
                assertThat(sizes[i]?.height).isGreaterThan(sizes[i - 1]?.height)
                assertThat(layouts[i]?.lineCount).isEqualTo(i + 1)
                // just ensure this is less than the unbounded size too
                assertThat(unboundedTextSize?.height).isGreaterThan(sizes[i]?.height)
            }
        }
    }

    @Test
    fun whenMinLines_setsLineHeight() {
        val text = ""
        val sizes: Array<IntSize?> = Array(5) { null }
        val layouts: Array<TextLayoutResult?> = Array(5) { null }
        var unboundedTextSize: IntSize? = null

        val textStyle = TextStyle(fontFamily = TEST_FONT_FAMILY)
        rule.setContent {
            for (i in sizes.indices) {
                Box(
                    Modifier
                        .onSizeChanged { sizes[i] = it }
                        .width(5.dp)) {
                    TextUsingModifier(
                        text,
                        style = textStyle,
                        minLines = i + 1,
                        onTextLayout = {
                            layouts[i] = it
                        }
                    )
                }
            }

            Box(
                Modifier
                    .onSizeChanged { unboundedTextSize = it }
                    .width(5.dp)) {
                TextUsingModifier(
                    text,
                    style = textStyle
                )
            }
        }
        rule.runOnIdle {
            for (i in 1 until sizes.size) {
                assertThat(sizes[i]?.height).isGreaterThan(sizes[i - 1]?.height)
                assertThat(layouts[i]?.lineCount).isEqualTo(1)
                // just ensure this is less than the unbounded size too
                assertThat(unboundedTextSize?.height).isLessThan(sizes[i]?.height)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeMinLines_throws() {
        rule.setContent {
            TextUsingModifier(text = "", minLines = -1)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeMaxLines_throws() {
        rule.setContent {
            TextUsingModifier(text = "", maxLines = -1)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun crossedMinMaxLines_throws() {
        rule.setContent {
            TextUsingModifier(text = "", minLines = 10, maxLines = 5)
        }
    }
}