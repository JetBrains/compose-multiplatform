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

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.foundation.text.TEST_FONT
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.heightInLines
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.ValueElement
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.properties.Delegates
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class HeightInLinesModifierTest {
    private val fontSize = 20
    private val defaultTextStyle = TextStyle(fontSize = 20.sp, fontFamily = TEST_FONT_FAMILY)

    private val longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
        "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam," +
        " quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. " +
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu " +
        "fugiat nulla pariatur."

    private val context = InstrumentationRegistry.getInstrumentation().context

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
    fun minLines_shortInputText() {
        setTextFieldWithMinMaxLines(
            TextFieldValue("abc"),
            minLines = 5
        ) { height, textLayoutResult ->
            assertThat(textLayoutResult.lineCount).isEqualTo(1)
            assertThat(height).isGreaterThan(fontSize * 5)
        }
    }

    @Test
    fun maxLines_shortInputText() {
        setTextFieldWithMinMaxLines(
            TextFieldValue("abc"),
            maxLines = 5
        ) { height, textLayoutResult ->
            assertThat(textLayoutResult.lineCount).isEqualTo(1)
            assertThat(height).isGreaterThan(fontSize)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun minLines_invalidValue() {
        rule.setContent {
            CoreTextField(
                value = TextFieldValue(),
                onValueChange = {},
                modifier = Modifier.heightInLines(textStyle = TextStyle.Default, minLines = 0)
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun maxLines_invalidValue() {
        rule.setContent {
            CoreTextField(
                value = TextFieldValue(),
                onValueChange = {},
                modifier = Modifier.heightInLines(textStyle = TextStyle.Default, maxLines = 0)
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun minLines_greaterThan_maxLines_invalidValue() {
        rule.setContent {
            CoreTextField(
                value = TextFieldValue(),
                onValueChange = {},
                modifier = Modifier.heightInLines(
                    textStyle = TextStyle.Default,
                    minLines = 2,
                    maxLines = 1
                )
            )
        }
    }

    @Test
    fun minLines_longInputText() {
       setTextFieldWithMinMaxLines(
            TextFieldValue(longText),
            minLines = 2
        ) { height, textLayoutResult ->
            assertThat(textLayoutResult.lineCount).isGreaterThan(2)
            assertThat(height).isGreaterThan(fontSize * 2)
        }
    }

    @Test
    fun maxLines_longInputText() {
        var subjectLayout: TextLayoutResult? = null
        var subjectHeight: Int? = null
        var twoLineHeight: Int? = null

        rule.setContent {
            HeightObservingText(
                onHeightChanged = { subjectHeight = it },
                onTextLayoutResult = { subjectLayout = it },
                textFieldValue = TextFieldValue(longText),
                maxLines = 2
            )
            HeightObservingText(
                onHeightChanged = { twoLineHeight = it },
                onTextLayoutResult = {},
                textFieldValue = TextFieldValue("1\n2"),
                maxLines = 2
            )
        }

        rule.runOnIdle {
            assertThat(subjectLayout).isNotNull()
            // should be in the 20s, but use this to create invariant for the next assertion
            assertThat(subjectLayout!!.lineCount).isGreaterThan(2)
            assertThat(subjectHeight!!).isEqualTo(twoLineHeight)
        }
    }

    @OptIn(ExperimentalTextApi::class, ExperimentalCoroutinesApi::class)
    @Test
    fun asyncFontLoad_changesLineHeight() {
        val testDispatcher = UnconfinedTestDispatcher()
        val resolver = createFontFamilyResolver(context, testDispatcher)

        val typefaceDeferred = CompletableDeferred<Typeface>()
        val asyncLoader = object : AndroidFont.TypefaceLoader {
            override fun loadBlocking(context: Context, font: AndroidFont): Typeface =
                TODO("Not yet implemented")

            override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface {
                return typefaceDeferred.await()
            }
        }
        val fontFamily = FontFamily(
            object : AndroidFont(FontLoadingStrategy.Async, asyncLoader) {
                override val weight: FontWeight = FontWeight.Normal
                override val style: FontStyle = FontStyle.Normal
            },
            TEST_FONT
        )

        val heights = mutableListOf<Int>()

        rule.setContent {
            CompositionLocalProvider(
                LocalFontFamilyResolver provides resolver,
                LocalDensity provides Density(1.0f, 1f)
            ) {
                HeightObservingText(
                    onHeightChanged = {
                        heights.add(it)
                    },
                    textFieldValue = TextFieldValue(longText),
                    maxLines = 10,
                    textStyle = defaultTextStyle.copy(
                        fontFamily = fontFamily,
                        fontSize = 80.sp
                    )
                )
            }
        }

        val before = heights.toList()
        typefaceDeferred.complete(Typeface.create("cursive", Typeface.BOLD_ITALIC))

        rule.runOnIdle {
            assertThat(heights.size).isGreaterThan(before.size)
            assertThat(heights.distinct().size).isGreaterThan(before.distinct().size)
        }
    }

    @Test
    fun testInspectableValue() {
        val modifier = Modifier.heightInLines(
            textStyle = TextStyle.Default,
            minLines = 5,
            maxLines = 10
        ) as InspectableValue
        assertThat(modifier.nameFallback).isEqualTo("heightInLines")
        assertThat(modifier.inspectableElements.asIterable()).containsExactly(
            ValueElement("minLines", 5),
            ValueElement("maxLines", 10),
            ValueElement("textStyle", TextStyle.Default)
        )
    }

    private fun setTextFieldWithMinMaxLines(
        textFieldValue: TextFieldValue,
        minLines: Int = 1,
        maxLines: Int = Int.MAX_VALUE,
        verify: (Int, TextLayoutResult) -> Unit
    ) {
        var height by Delegates.notNull<Int>()
        lateinit var textLayoutResult: TextLayoutResult
        rule.setContent {
            HeightObservingText(
                onHeightChanged = { height = it },
                textFieldValue = textFieldValue,
                onTextLayoutResult = { textLayoutResult = it },
                minLines = minLines,
                maxLines = maxLines
            )
        }

        rule.runOnIdle {
            verify(height, textLayoutResult)
        }
    }

    @Composable
    private fun HeightObservingText(
        onHeightChanged: (Int) -> Unit,
        textFieldValue: TextFieldValue,
        onTextLayoutResult: (TextLayoutResult) -> Unit = {},
        minLines: Int = 1,
        maxLines: Int = Int.MAX_VALUE,
        textStyle: TextStyle = defaultTextStyle
    ) {
        CoreTextField(
            value = textFieldValue,
            onValueChange = {},
            textStyle = textStyle,
            onTextLayout = onTextLayoutResult,
            modifier = Modifier
                .onSizeChanged {
                    onHeightChanged(it.height)
                }
                .requiredWidth(100.dp)
                // we test modifier here so propagating min and max lines here instead of passing
                // them to the CoreTextField directly
                .heightInLines(
                    textStyle = textStyle,
                    minLines = minLines,
                    maxLines = maxLines
                )
        )
    }
}