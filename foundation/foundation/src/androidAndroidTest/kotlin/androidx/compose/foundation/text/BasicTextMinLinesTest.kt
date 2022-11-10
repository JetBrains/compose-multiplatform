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

package androidx.compose.foundation.text

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.properties.Delegates
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class BasicTextMinLinesTest(private val useAnnotatedString: Boolean) {
    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "useAnnotatedString={0}")
        fun parameters() = arrayOf(true, false)
    }

    private val density = Density(1f)
    private val fontSize = 20

    @Test
    fun defaultMinLines_withEmptyText() {
        displayText("", 1) { height ->
            assertThat(height).isEqualTo(fontSize)
        }
    }

    @Test
    fun minLines_greater_thanEmptyText() {
        displayText("", 5) { height ->
            assertThat(height).isEqualTo(fontSize * 5)
        }
    }

    @Test
    fun minLines_smaller_thanTextLines() {
        displayText("Line1\nLine2", 1) { height ->
            assertThat(height).isEqualTo(fontSize * 2)
        }
    }

    @Test
    fun minLines_greater_thanTextLines() {
        displayText("Line1\nLine2", 5) { height ->
            assertThat(height).isEqualTo(fontSize * 5)
        }
    }

    private fun displayText(text: String, minLines: Int, verify: (Int) -> Unit) {
        var height by Delegates.notNull<Int>()
        val modifier = Modifier.fillMaxWidth().onSizeChanged { height = it.height }
        val style = TextStyle(
            fontSize = fontSize.sp,
            fontFamily = TEST_FONT_FAMILY,
            lineHeight = fontSize.sp
        )

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                if (useAnnotatedString) {
                    BasicText(
                        text = AnnotatedString(text),
                        modifier = modifier,
                        style = style,
                        minLines = minLines
                    )
                } else {
                    BasicText(
                        text = text,
                        modifier = modifier,
                        style = style,
                        minLines = minLines
                    )
                }
            }
        }

        rule.runOnIdle { verify(height) }
    }
}