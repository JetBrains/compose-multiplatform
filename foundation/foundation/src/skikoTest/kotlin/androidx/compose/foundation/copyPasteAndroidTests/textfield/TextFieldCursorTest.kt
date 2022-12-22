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

package androidx.compose.foundation.copyPasteAndroidTests.textfield

import androidx.compose.foundation.assertPixelColor
import androidx.compose.foundation.assertPixels
import androidx.compose.foundation.assertShape
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.background
import androidx.compose.foundation.isNotEqualTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runSkikoComposeUiTest
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TextFieldCursorTest {


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

    @BeforeTest
    fun reset() {
        isFocused = false
        cursorRect = Rect.Zero
    }

    @Test
    fun textFieldFocused_cursorRendered() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
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

        mainClock.advanceTimeBy(100)
        waitForIdle()

        with(density) {
            onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this, cursorRect)
        }
    }

    @Test
    fun textFieldFocused_cursorWithBrush() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
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

        mainClock.advanceTimeBy(100)
        waitForIdle()

        val bitmap = onNode(hasSetTextAction())
            .captureToImage().toPixelMap()

        val cursorLeft = ceil(cursorRect.left).toInt() // + 1
        val cursorTop = ceil(cursorRect.top).toInt()  + 1
        val cursorBottom = floor(cursorRect.bottom).toInt() - 1
        bitmap.assertPixelColor(Color.Blue, x = cursorLeft, y = cursorTop)
        bitmap.assertPixelColor(Color.Green, x = cursorLeft, y = cursorBottom)
    }

    @Test
    fun cursorBlinkingAnimation() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
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
        mainClock.advanceTimeBy(100)

        with(density) {
            onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this, cursorRect)
        }

        // cursor invisible during next 500 ms
        mainClock.advanceTimeBy(500)
        waitForIdle()

        onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Test
    fun cursorUnsetColor_noCursor() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
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
        onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )

        // no cursor when should be no cursor
        mainClock.advanceTimeBy(700)
        waitForIdle()
        onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )
    }

    @Test
    fun cursorNotBlinking_whileTyping() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
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
        mainClock.advanceTimeBy(500)

        // TODO(b/170298051) check here that cursor is visible when we have a way to control
        //  cursor position when sending a text

        // change text field value
        runOnIdle {
            onNode(hasSetTextAction())
                .performTextReplacement("")
        }


        // cursor would have been invisible during next 500 ms if cursor blinks while typing.
        // To prevent blinking while typing we restart animation when new symbol is typed.
        mainClock.advanceTimeBy(400)
        waitForIdle()

        with(density) {
            onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this, cursorRect)
        }
    }

    @Test
    fun selectionChanges_cursorNotBlinking() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        val textValue = mutableStateOf(TextFieldValue("test", selection = TextRange(2)))
        setContent {
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
        mainClock.advanceTimeBy(500)
        mainClock.advanceTimeByFrame()
        waitForIdle()

        // TODO(b/170298051) check here that cursor is visible when we have a way to control
        //  cursor position when sending a text

        textValue.value = textValue.value.copy(selection = TextRange(0))

        // necessary for animation to start (shows cursor again)
        mainClock.advanceTimeByFrame()
        waitForIdle()

        with(density) {
            onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this, cursorRect)
        }
    }

    @Test
    fun brushChanged_doesntResetTimer() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        var cursorBrush by mutableStateOf(SolidColor(cursorColor))
        setContent {
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

        mainClock.advanceTimeBy(600)
        cursorBrush = SolidColor(Color.Green)
        mainClock.advanceTimeByFrame()

        onNode(hasSetTextAction())
            .captureToImage()
            .assertShape(
                density = density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )
    }

    private fun SkikoComposeUiTest.focusAndWait() {
        onNode(hasSetTextAction()).performClick()
        mainClock.advanceTimeUntil { isFocused }
        waitForIdle() // 1st to initialize the cursor animation
        waitForIdle() // 2nd to send the first frame (to let animation.callWithFrameNanos run)
    }

    private fun ImageBitmap.assertCursor(cursorWidth: Dp, density: Density, cursorRect: Rect) {
        assertThat(cursorRect.height).isNotEqualTo(0f)
        assertThat(cursorRect).isNotEqualTo(Rect.Zero)
        val cursorWidthPx = (with(density) { cursorWidth.roundToPx() })

        // assert cursor width is greater than 2 since we will shrink the check area by 1 on each
        // side
        // assertThat(cursorWidthPx).isGreaterThan(2) // it's not commented on android though

        val checkRect = Rect(
            ceil(cursorRect.left),
            ceil(cursorRect.top),
            floor(cursorRect.right) + cursorWidthPx - 1,
            floor(cursorRect.bottom)
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