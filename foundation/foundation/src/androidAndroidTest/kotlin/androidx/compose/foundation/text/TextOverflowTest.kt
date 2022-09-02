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

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TextOverflowTest {
    @get:Rule
    val rule = createComposeRule()

    private val density = Density(1f)
    private val fontFamilyResolver =
        createFontFamilyResolver(InstrumentationRegistry.getInstrumentation().context)
    private val fontFamily = TEST_FONT_FAMILY

    private val BoxTag = "wrapping box"

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun paint_singleParagraph_withVisibleOverflow() {
        val text = "Hello\nHello\nHello\nHello"

        val lineHeight = 20
        val boxHeight = 100
        val boxWidth = 200

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalFontFamilyResolver provides fontFamilyResolver
            ) {
                Box(Modifier.testTag(BoxTag).size(boxWidth.dp, boxHeight.dp)) {
                    BasicText(
                        text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = (2 * lineHeight).dp)
                            .padding(bottom = lineHeight.dp),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontFamily = fontFamily,
                            color = Color.Red,
                            lineHeight = 20.sp
                        ),
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }

        val boxBitmap = rule.onNodeWithTag(BoxTag).captureToImage().asAndroidBitmap()
        val croppedBoxBitmap = Bitmap.createBitmap(
            boxBitmap,
            0,
            2 * lineHeight,
            boxWidth,
            boxHeight - 2 * lineHeight
        )
        croppedBoxBitmap.asImageBitmap().assertContainsColor(Color.Red)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun paint_multiParagraph_withVisibleOverflow() {
        val text = buildAnnotatedString {
            append("Hello\nHello")
            withStyle(ParagraphStyle(textAlign = TextAlign.Center)) {
                append("Hello\nHello")
            }
        }
        val lineHeight = 20
        val boxHeight = 100
        val boxWidth = 200

        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides density,
                LocalFontFamilyResolver provides fontFamilyResolver
            ) {
                Box(Modifier.testTag(BoxTag).size(boxWidth.dp, boxHeight.dp)) {
                    BasicText(
                        text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = (2 * lineHeight).dp)
                            .padding(bottom = lineHeight.dp),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontFamily = fontFamily,
                            color = Color.Red,
                            lineHeight = 20.sp
                        ),
                        overflow = TextOverflow.Visible
                    )
                }
            }
        }

        val boxBitmap = rule.onNodeWithTag(BoxTag).captureToImage().asAndroidBitmap()
        val croppedBoxBitmap = Bitmap.createBitmap(
            boxBitmap,
            0,
            2 * lineHeight,
            boxWidth,
            boxHeight - 2 * lineHeight
        )
        croppedBoxBitmap.asImageBitmap().assertContainsColor(Color.Red)
    }
}