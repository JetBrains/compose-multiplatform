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

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toOffset
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.floor
import org.junit.Rule
import org.junit.Test

@LargeTest
class TextFieldCursorTest {

    @get:Rule
    val rule = createComposeRule().also {
        it.mainClock.autoAdvance = false
    }

    private val boxPadding = 10.dp
    private val cursorColor = Color.Red
    private val textStyle = TextStyle(
        color = Color.White,
        background = Color.White,
        fontSize = 10.sp
    )

    private val textFieldWidth = 10.dp
    private val textFieldHeight = 20.dp
    private val textFieldBgColor = Color.White
    private var isFocused = false
    private var cursorRect = Rect.Zero

    private val bgModifier = Modifier.background(textFieldBgColor)
    private val focusModifier = Modifier.onFocusChanged { if (it.isFocused) isFocused = true }
    private val sizeModifier = Modifier.size(textFieldWidth, textFieldHeight)
    // default TextFieldModifier
    private val textFieldModifier = sizeModifier.then(bgModifier).then(focusModifier)

    // default onTextLayout to capture cursor boundaries.
    private val onTextLayout: (TextLayoutResult) -> Unit = { cursorRect = it.getCursorRect(0) }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorRendered() = with(rule.density) {
        rule.setContent {
            Box(Modifier.padding(boxPadding)) {
                BasicTextField(
                    value = "",
                    onValueChange = {},
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        with(rule.density) {
            rule.onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this, cursorRect)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorWithBrush() = with(rule.density) {
        rule.setContent {
            Box(Modifier.padding(boxPadding)) {
                BasicTextField(
                    value = "",
                    onValueChange = {},
                    textStyle = textStyle.copy(fontSize = textStyle.fontSize * 2),
                    modifier = Modifier
                        .size(textFieldWidth, textFieldHeight * 2)
                        .then(bgModifier)
                        .then(focusModifier),
                    cursorBrush = Brush.verticalGradient(
                        // make a brush double/triple color at the beginning and end so we have stable
                        // colors at the ends.
                        // Without triple bottom, the bottom color never hits to the provided color.
                        listOf(
                            Color.Blue,
                            Color.Blue,
                            Color.Green,
                            Color.Green,
                            Color.Green
                        )
                    ),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(100)

        val bitmap = rule.onNode(hasSetTextAction())
            .captureToImage().toPixelMap()

        val cursorLeft = ceil(cursorRect.left).toInt() + 1
        val cursorTop = ceil(cursorRect.top).toInt() + 1
        val cursorBottom = floor(cursorRect.bottom).toInt() - 1
        bitmap.assertPixelColor(Color.Blue, x = cursorLeft, y = cursorTop)
        bitmap.assertPixelColor(Color.Green, x = cursorLeft, y = cursorBottom)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorBlinkingAnimation() = with(rule.density) {
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) {
                BasicTextField(
                    value = "",
                    onValueChange = {},
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(100)
        with(rule.density) {
            rule.onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this, cursorRect)
        }

        // cursor invisible during next 500 ms
        rule.mainClock.advanceTimeBy(700)
        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorUnsetColor_noCursor() = with(rule.density) {
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) {
                BasicTextField(
                    value = "",
                    onValueChange = {},
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(Color.Unspecified)
                )
            }
        }

        focusAndWait()

        // no cursor when usually shown
        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )

        // no cursor when should be no cursor
        rule.mainClock.advanceTimeBy(700)
        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorNotBlinking_whileTyping() = with(rule.density) {
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) {
                val text = remember { mutableStateOf("test") }
                BasicTextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(500)
        // TODO(b/170298051) check here that cursor is visible when we have a way to control
        //  cursor position when sending a text

        // change text field value
        rule.onNode(hasSetTextAction())
            .performTextReplacement("")

        // cursor would have been invisible during next 500 ms if cursor blinks while typing.
        // To prevent blinking while typing we restart animation when new symbol is typed.
        rule.mainClock.advanceTimeBy(400)
        with(rule.density) {
            rule.onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this, cursorRect)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun selectionChanges_cursorNotBlinking() = with(rule.density) {
        rule.mainClock.autoAdvance = false
        val textValue = mutableStateOf(TextFieldValue("test", selection = TextRange(2)))
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(boxPadding)) {
                BasicTextField(
                    value = textValue.value,
                    onValueChange = { textValue.value = it },
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = SolidColor(cursorColor),
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        // hide the cursor
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.advanceTimeByFrame()

        // TODO(b/170298051) check here that cursor is visible when we have a way to control
        //  cursor position when sending a text

        rule.runOnIdle {
            textValue.value = textValue.value.copy(selection = TextRange(0))
        }

        // necessary for animation to start (shows cursor again)
        rule.mainClock.advanceTimeByFrame()

        with(rule.density) {
            rule.onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this, cursorRect)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun brushChanged_doesntResetTimer() {
        var cursorBrush by mutableStateOf(SolidColor(cursorColor))
        rule.setContent {
            Box(Modifier.padding(boxPadding)) {
                BasicTextField(
                    value = "",
                    onValueChange = {},
                    textStyle = textStyle,
                    modifier = textFieldModifier,
                    cursorBrush = cursorBrush,
                    onTextLayout = onTextLayout
                )
            }
        }

        focusAndWait()

        rule.mainClock.advanceTimeBy(800)
        cursorBrush = SolidColor(Color.Green)
        rule.mainClock.advanceTimeByFrame()

        rule.onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )
    }

    private fun focusAndWait() {
        rule.onNode(hasSetTextAction()).performClick()
        rule.mainClock.advanceTimeUntil { isFocused }
    }

    private fun ImageBitmap.assertCursor(cursorWidth: Dp, density: Density, cursorRect: Rect) {
        assertThat(cursorRect.height).isNotEqualTo(0f)
        assertThat(cursorRect).isNotEqualTo(Rect.Zero)
        val cursorWidthPx = (with(density) { cursorWidth.roundToPx() })

        // assert cursor width is greater than 2 since we will shrink the check area by 1 on each
        // side
        assertThat(cursorWidthPx).isGreaterThan(2)

        // shrink the check are by 1px for left, top, right, bottom
        val checkRect = Rect(
            ceil(cursorRect.left) + 1,
            ceil(cursorRect.top) + 1,
            floor(cursorRect.right) + cursorWidthPx - 1,
            floor(cursorRect.bottom) - 1
        )

        // skip an expanded rectangle that is 1px larger than cursor rectangle
        val skipRect = Rect(
            floor(cursorRect.left) - 1,
            floor(cursorRect.top) - 1,
            ceil(cursorRect.right) + cursorWidthPx + 1,
            ceil(cursorRect.bottom) + 1
        )

        val width = width
        val height = height
        this.assertPixels(
            IntSize(width, height)
        ) { position ->
            if (checkRect.contains(position.toOffset())) {
                // cursor
                cursorColor
            } else if (skipRect.contains(position.toOffset())) {
                // skip some pixels around cursor
                null
            } else {
                // text field background
                textFieldBgColor
            }
        }
    }
}