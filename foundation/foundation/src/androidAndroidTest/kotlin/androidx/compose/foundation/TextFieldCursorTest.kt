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

package androidx.compose.foundation

import android.graphics.Bitmap
import android.os.Build
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.ui.focus.ExperimentalFocus
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focusObserver
import androidx.ui.test.assertPixels
import androidx.ui.test.assertShape
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.performClick
import androidx.ui.test.onNode
import androidx.ui.test.hasInputMethodsSupport
import androidx.ui.test.waitForIdle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@OptIn(
    ExperimentalFocus::class,
    ExperimentalFoundationApi::class
)
class TextFieldCursorTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableBlinkingCursor = false).also {
        it.clockTestRule.pauseClock()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun textFieldFocused_cursorRendered() = with(composeTestRule.density) {
        val width = 10.dp
        val height = 20.dp
        val latch = CountDownLatch(1)
        composeTestRule.setContent {
            BaseTextField(
                value = TextFieldValue(),
                onValueChange = {},
                textStyle = TextStyle(color = Color.White, background = Color.White),
                modifier = Modifier
                    .preferredSize(width, height)
                    .background(Color.White)
                    .focusObserver { if (it.isFocused) latch.countDown() },
                cursorColor = Color.Red
            )
        }
        onNode(hasInputMethodsSupport()).performClick()
        assert(latch.await(1, TimeUnit.SECONDS))

        waitForIdle()

        composeTestRule.clockTestRule.advanceClock(100)
        with(composeTestRule.density) {
            onNode(hasInputMethodsSupport())
                .captureToBitmap()
                .assertCursor(2.dp, this)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun cursorBlinkingAnimation() = with(composeTestRule.density) {
        val width = 10.dp
        val height = 20.dp
        val latch = CountDownLatch(1)
        composeTestRule.setContent {
            // The padding helps if the test is run accidentally in landscape. Landscape makes
            // the cursor to be next to the navigation bar which affects the red color to be a bit
            // different - possibly anti-aliasing.
            Box(Modifier.padding(10.dp)) {
                BaseTextField(
                    value = TextFieldValue(),
                    onValueChange = {},
                    textStyle = TextStyle(color = Color.White, background = Color.White),
                    modifier = Modifier
                        .preferredSize(width, height)
                        .background(Color.White)
                        .focusObserver { if (it.isFocused) latch.countDown() },
                    cursorColor = Color.Red
                )
            }
        }

        onNode(hasInputMethodsSupport()).performClick()
        assert(latch.await(1, TimeUnit.SECONDS))

        waitForIdle()

        // cursor visible first 500 ms
        composeTestRule.clockTestRule.advanceClock(100)
        with(composeTestRule.density) {
            onNode(hasInputMethodsSupport())
                .captureToBitmap()
                .assertCursor(2.dp, this)
        }

        // cursor invisible during next 500 ms
        composeTestRule.clockTestRule.advanceClock(700)
        onNode(hasInputMethodsSupport())
            .captureToBitmap()
            .assertShape(
                density = composeTestRule.density,
                shape = RectangleShape,
                shapeColor = Color.White,
                backgroundColor = Color.White,
                shapeOverlapPixelCount = 0.0f
            )
    }

    private fun Bitmap.assertCursor(cursorWidth: Dp, density: Density) {
        val сursorWidth = (with(density) { cursorWidth.toIntPx() })
        val width = width
        val height = height
        this.assertPixels(
            IntSize(width, height)
        ) { position ->
            if (position.x >= сursorWidth - 1 && position.x < сursorWidth + 1) {
                // skip some pixels around cursor
                null
            } else if (position.y < 5 || position.y > height - 5) {
                // skip some pixels vertically
                null
            } else if (position.x in 0..сursorWidth) {
                // cursor
                Color.Red
            } else {
                // text field background
                Color.White
            }
        }
    }
}
