/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@OptIn(InternalFoundationTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class TextDelegateIntegrationTest {

    private val fontFamily = TEST_FONT_FAMILY
    private val density = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val resourceLoader = TestFontResourceLoader(context)

    @Test
    fun minIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )

            textDelegate.layoutIntrinsics(LayoutDirection.Ltr)

            assertThat(textDelegate.minIntrinsicWidth)
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

    @Test
    fun maxIntrinsicWidth_getter() {
        with(density) {
            val fontSize = 20.sp
            val text = "Hello"
            val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
            val annotatedString = AnnotatedString(text, spanStyle)
            val textDelegate = TextDelegate(
                text = annotatedString,
                style = TextStyle.Default,
                density = this,
                resourceLoader = resourceLoader
            )

            textDelegate.layoutIntrinsics(LayoutDirection.Ltr)

            assertThat(textDelegate.maxIntrinsicWidth)
                .isEqualTo((fontSize.toPx() * text.length).toIntPx())
        }
    }

//    @Test
//    fun multiParagraphIntrinsics_isReused() {
//        val textDelegate = TextDelegate(
//            text = AnnotatedString(text = "abc"),
//            style = TextStyle.Default,
//            density = density,
//            resourceLoader = resourceLoader
//        )
//
//        // create the intrinsics object
//        textDelegate.layoutIntrinsics(LayoutDirection.Ltr)
//        val multiParagraphIntrinsics = textDelegate.paragraphIntrinsics
//
//        // layout should create the MultiParagraph. The final MultiParagraph is expected to use
//        // the previously calculated intrinsics
//        val layoutResult = textDelegate.layout(Constraints(), LayoutDirection.Ltr)
//        val layoutIntrinsics = layoutResult.multiParagraph.intrinsics
//
//        // primary assertions to make sure that the objects are not null
//        assertThat(layoutIntrinsics.infoList.get(0)).isNotNull()
//        assertThat(multiParagraphIntrinsics?.infoList?.get(0)).isNotNull()
//
//        // the intrinsics passed to multi paragraph should be the same instance
//        assertThat(layoutIntrinsics).isSameInstanceAs(multiParagraphIntrinsics)
//        // the ParagraphIntrinsic in the MultiParagraphIntrinsic should be the same instance
//        assertThat(layoutIntrinsics.infoList.get(0))
//            .isSameInstanceAs(multiParagraphIntrinsics?.infoList?.get(0))
//    }

    @Test
    fun TextLayoutInput_reLayout_withDifferentHeight() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "Hello World!"),
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        val resultFirstLayout = textDelegate.layout(constraintsFirstLayout, LayoutDirection.Ltr)
        assertThat(resultFirstLayout.layoutInput.constraints).isEqualTo(constraintsFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        val resultSecondLayout = textDelegate.layout(
            constraintsSecondLayout,
            LayoutDirection.Ltr,
            resultFirstLayout
        )
        assertThat(resultSecondLayout.layoutInput.constraints).isEqualTo(constraintsSecondLayout)
    }

    @Test
    fun TextLayoutResult_reLayout_withDifferentHeight() {
        val textDelegate = TextDelegate(
            text = AnnotatedString(text = "Hello World!"),
            style = TextStyle.Default,
            density = density,
            resourceLoader = resourceLoader
        )
        val width = 200
        val heightFirstLayout = 100
        val heightSecondLayout = 200

        val constraintsFirstLayout = Constraints.fixed(width, heightFirstLayout)
        val resultFirstLayout = textDelegate.layout(constraintsFirstLayout, LayoutDirection.Ltr)
        assertThat(resultFirstLayout.size.height).isEqualTo(heightFirstLayout)

        val constraintsSecondLayout = Constraints.fixed(width, heightSecondLayout)
        val resultSecondLayout = textDelegate.layout(
            constraintsSecondLayout,
            LayoutDirection.Ltr,
            resultFirstLayout
        )
        assertThat(resultSecondLayout.size.height).isEqualTo(heightSecondLayout)
    }

    @Test
    fun TextLayoutResult_layout_withEllipsis_withoutSoftWrap() {
        val fontSize = 20f
        val text = AnnotatedString(text = "Hello World! Hello World! Hello World! Hello World!")
        val textDelegate = TextDelegate(
            text = text,
            style = TextStyle(fontSize = fontSize.sp),
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            density = density,
            resourceLoader = resourceLoader
        )
        textDelegate.layoutIntrinsics(LayoutDirection.Ltr)
        // Makes width smaller than needed.
        val width = textDelegate.maxIntrinsicWidth / 2
        val constraints = Constraints(maxWidth = width)
        val layoutResult = textDelegate.layout(constraints, LayoutDirection.Ltr)

        assertThat(layoutResult.lineCount).isEqualTo(1)
        assertThat(layoutResult.isLineEllipsized(0)).isTrue()
    }
}

private fun TextLayoutResult.toBitmap() = Bitmap.createBitmap(
    size.width,
    size.height,
    Bitmap.Config.ARGB_8888
)
