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

package androidx.compose.ui.text.android

import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
class LayoutHelperParagraphTest {

    private val WIDTH = 100
    private val TEXT_PAINT = TextPaint()

    private fun buildLayoutHelper(text: String): LayoutHelper =
        if (Build.VERSION.SDK_INT < 23) {
            @Suppress("DEPRECATION") StaticLayout(
                text, TEXT_PAINT, WIDTH,
                Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false
            )
        } else {
            StaticLayout.Builder.obtain(text, 0, text.length, TEXT_PAINT, WIDTH).build()
        }.let {
            LayoutHelper(it)
        }

    @Test
    fun testParagraphInfo() {
        val layoutHelper = buildLayoutHelper("a\nb\nc")
        assertThat(layoutHelper.paragraphCount).isEqualTo(3)

        assertThat(layoutHelper.getParagraphStart(0)).isEqualTo(0)
        assertThat(layoutHelper.getParagraphEnd(0)).isEqualTo(2)

        assertThat(layoutHelper.getParagraphStart(1)).isEqualTo(2)
        assertThat(layoutHelper.getParagraphEnd(1)).isEqualTo(4)

        assertThat(layoutHelper.getParagraphStart(2)).isEqualTo(4)
        assertThat(layoutHelper.getParagraphEnd(2)).isEqualTo(5)
    }

    @Test
    fun testParagraphInfo_singleParagraph() {
        val layoutHelper = buildLayoutHelper("Hello, World.")
        assertThat(layoutHelper.paragraphCount).isEqualTo(1)

        assertThat(layoutHelper.getParagraphStart(0)).isEqualTo(0)
        assertThat(layoutHelper.getParagraphEnd(0)).isEqualTo(layoutHelper.layout.text.length)
    }

    @Test
    fun testParagraphInfo_ignoreLastLineFeed() {
        val layoutHelper = buildLayoutHelper("Hello, World.\n")
        assertThat(layoutHelper.paragraphCount).isEqualTo(1)

        assertThat(layoutHelper.getParagraphStart(0)).isEqualTo(0)
        assertThat(layoutHelper.getParagraphEnd(0)).isEqualTo(layoutHelper.layout.text.length)
    }

    @Test
    fun testParagraphInfo_emptyText() {
        val layoutHelper = buildLayoutHelper("")
        assertThat(layoutHelper.paragraphCount).isEqualTo(1)

        assertThat(layoutHelper.getParagraphStart(0)).isEqualTo(0)
        assertThat(layoutHelper.getParagraphEnd(0)).isEqualTo(0)
    }

    @Test
    fun testParagraphInfo_lineFeedOnlyCases() {
        val layoutHelper = buildLayoutHelper("\n")
        assertThat(layoutHelper.paragraphCount).isEqualTo(1)

        assertThat(layoutHelper.getParagraphStart(0)).isEqualTo(0)
        assertThat(layoutHelper.getParagraphEnd(0)).isEqualTo(layoutHelper.layout.text.length)
    }

    @Test
    fun testParagraphInfo_lineFeedOnlyCases2() {
        val layoutHelper = buildLayoutHelper("\n\n")
        assertThat(layoutHelper.paragraphCount).isEqualTo(2)

        assertThat(layoutHelper.getParagraphStart(0)).isEqualTo(0)
        assertThat(layoutHelper.getParagraphEnd(0)).isEqualTo(1)

        assertThat(layoutHelper.getParagraphStart(1)).isEqualTo(1)
        assertThat(layoutHelper.getParagraphEnd(1)).isEqualTo(layoutHelper.layout.text.length)
    }

    @Test
    fun testParagraphForOffset() {
        val layoutHelper = buildLayoutHelper("aa\nbb\ncc")
        assertThat(layoutHelper.getParagraphForOffset(0)).isEqualTo(0)
        assertThat(layoutHelper.getParagraphForOffset(1)).isEqualTo(0)
        assertThat(layoutHelper.getParagraphForOffset(2)).isEqualTo(0)
        assertThat(layoutHelper.getParagraphForOffset(3)).isEqualTo(1)
        assertThat(layoutHelper.getParagraphForOffset(4)).isEqualTo(1)
        assertThat(layoutHelper.getParagraphForOffset(5)).isEqualTo(1)
        assertThat(layoutHelper.getParagraphForOffset(6)).isEqualTo(2)
        assertThat(layoutHelper.getParagraphForOffset(7)).isEqualTo(2)
    }

    @Test
    fun testParagraphForOffset_lineFeedOnlyCase() {
        val layoutHelper = buildLayoutHelper("\n\n")
        assertThat(layoutHelper.getParagraphForOffset(0)).isEqualTo(0)
        assertThat(layoutHelper.getParagraphForOffset(1)).isEqualTo(1)
    }

    @Test
    fun testParagarphDirection() {
        val layoutHelper = buildLayoutHelper("aa\nאא\ncc")
        assertThat(layoutHelper.isRTLParagraph(0)).isFalse()
        assertThat(layoutHelper.isRTLParagraph(1)).isTrue()
        assertThat(layoutHelper.isRTLParagraph(2)).isFalse()
    }

    @Test
    fun testParagarphDirection_case2() {
        val layoutHelper = buildLayoutHelper("אא\nbb\nאא")
        assertThat(layoutHelper.isRTLParagraph(0)).isTrue()
        assertThat(layoutHelper.isRTLParagraph(1)).isFalse()
        assertThat(layoutHelper.isRTLParagraph(2)).isTrue()
    }
}