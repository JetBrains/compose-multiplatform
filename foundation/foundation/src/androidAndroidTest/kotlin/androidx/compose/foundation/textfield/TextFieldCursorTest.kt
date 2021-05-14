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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertPixelColor
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test

@LargeTest
class TextFieldCursorTest {

    @get:Rule
    val rule = createComposeRule().also {
        it.mainClock.autoAdvance = false
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorRendered() = with(rule.density) {
        val width = 10.dp
        val height = 20.dp
        var isFocused = false
        rule.setContent {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = TextStyle(color = Color.White, background = Color.White),
                modifier = Modifier
                    .size(width, height)
                    .background(Color.White)
                    .onFocusChanged { if (it.isFocused) isFocused = true },
                cursorBrush = SolidColor(Color.Red)
            )
        }

        rule.onNode(hasSetTextAction()).performClick()
        rule.mainClock.advanceTimeUntil { isFocused }

        rule.mainClock.advanceTimeBy(100)

        with(rule.density) {
            rule.onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorWithBrush() = with(rule.density) {
        val width = 10.dp
        val height = 20.dp
        var isFocused = false
        rule.setContent {
            BasicTextField(
                value = "",
                onValueChange = {},
                textStyle = TextStyle(color = Color.White, background = Color.White),
                modifier = Modifier
                    .size(width, height)
                    .background(Color.White)
                    .onFocusChanged { if (it.isFocused) isFocused = true },
                cursorBrush = Brush.verticalGradient(
                    // make a brush double color at the beginning and end so we have stable
                    // colors at the ends
                    listOf(
                        Color.Blue,
                        Color.Blue,
                        Color.Green,
                        Color.Green
                    )
                )
            )
        }

        rule.onNode(hasSetTextAction()).performClick()
        rule.mainClock.advanceTimeUntil { isFocused }

        rule.mainClock.advanceTimeBy(100)

        val bitmap = rule.onNode(hasSetTextAction())
            .captureToImage().toPixelMap()
        bitmap.assertPixelColor(Color.Blue, x = 0, y = 10)
        bitmap.assertPixelColor(Color.Green, x = 0, y = bitmap.height - 10)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorBlinkingAnimation() = with(rule.density) {
        val width = 10.dp
        val height = 20.dp
        var isFocused = false
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(10.dp)) {
                BasicTextField(
                    value = "",
                    onValueChange = {},
                    textStyle = TextStyle(color = Color.White, background = Color.White),
                    modifier = Modifier
                        .size(width, height)
                        .background(Color.White)
                        .onFocusChanged { if (it.isFocused) isFocused = true },
                    cursorBrush = SolidColor(Color.Red)
                )
            }
        }

        rule.onNode(hasSetTextAction()).performClick()
        rule.mainClock.advanceTimeUntil { isFocused }

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(100)
        with(rule.density) {
            rule.onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this)
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
        val width = 10.dp
        val height = 20.dp
        var isFocused = false
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(10.dp)) {
                BasicTextField(
                    value = "",
                    onValueChange = {},
                    textStyle = TextStyle(color = Color.White, background = Color.White),
                    modifier = Modifier
                        .size(width, height)
                        .background(Color.White)
                        .onFocusChanged { if (it.isFocused) isFocused = true },
                    cursorBrush = SolidColor(Color.Unspecified)
                )
            }
        }

        rule.onNode(hasSetTextAction()).performClick()
        rule.mainClock.advanceTimeUntil { isFocused }

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
        val width = 10.dp
        val height = 20.dp
        var isFocused = false
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(10.dp)) {
                val text = remember { mutableStateOf("test") }
                BasicTextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    textStyle = TextStyle(color = Color.White, background = Color.White),
                    modifier = Modifier
                        .size(width, height)
                        .background(Color.White)
                        .onFocusChanged { if (it.isFocused) isFocused = true },
                    cursorBrush = SolidColor(Color.Red)
                )
            }
        }

        rule.onNode(hasSetTextAction()).performClick()
        rule.mainClock.advanceTimeUntil { isFocused }

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
                .assertCursor(2.dp, this)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun selectionChanges_cursorNotBlinking() = with(rule.density) {
        rule.mainClock.autoAdvance = false
        val width = 100.dp
        val height = 20.dp
        var isFocused = false
        val textValue = mutableStateOf(TextFieldValue("test", selection = TextRange(2)))
        rule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(10.dp)) {
                BasicTextField(
                    value = textValue.value,
                    onValueChange = { textValue.value = it },
                    textStyle = TextStyle(color = Color.White, background = Color.White),
                    modifier = Modifier
                        .size(width, height)
                        .background(Color.White)
                        .onFocusChanged { if (it.isFocused) isFocused = true },
                    cursorBrush = SolidColor(Color.Red)
                )
            }
        }

        rule.onNode(hasSetTextAction()).performClick()
        rule.mainClock.advanceTimeUntil { isFocused }

        // cursor visible first 500 ms
        rule.mainClock.advanceTimeBy(500)

        // TODO(b/170298051) check here that cursor is visible when we have a way to control
        //  cursor position when sending a text

        // change text field value
        rule.runOnIdle {
            textValue.value = textValue.value.copy(selection = TextRange(0))
        }

        // cursor would have been invisible during next 500 ms if cursor blinks when selection
        // changes.
        // To prevent blinking when selection changes we restart animation when new symbol is typed.
        rule.mainClock.advanceTimeBy(400)
        with(rule.density) {
            rule.onNode(hasSetTextAction())
                .captureToImage()
                .assertCursor(2.dp, this)
        }
    }

    private fun ImageBitmap.assertCursor(cursorWidth: Dp, density: Density) {
        val cursorWidthPx = (with(density) { cursorWidth.roundToPx() })
        val width = width
        val height = height
        this.assertPixels(
            IntSize(width, height)
        ) { position ->
            if (position.x >= cursorWidthPx - 1 && position.x < cursorWidthPx + 1) {
                // skip some pixels around cursor
                null
            } else if (position.y < 5 || position.y > height - 5) {
                // skip some pixels vertically
                null
            } else if (position.x in 0..cursorWidthPx) {
                // cursor
                Color.Red
            } else {
                // text field background
                Color.White
            }
        }
    }
}
