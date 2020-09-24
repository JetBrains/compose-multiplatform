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
class LayoutCompatTest {

    private val LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
        "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim " +
        "veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo " +
        "consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum " +
        "dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, " +
        "sunt in culpa qui officia deserunt mollit anim id est laborum."

    private val TEXT_PAINT = TextPaint().apply {
        textSize = 48f
    }

    private val MAX_INTRINSIC_WIDTH =
        Math.ceil(Layout.getDesiredWidth(LOREM_IPSUM, TEXT_PAINT).toDouble()).toInt()

    // Make about 10 lines of layout.

    private fun getTestLayout(): Layout = if (Build.VERSION.SDK_INT < 23) {
        @Suppress("DEPRECATION") StaticLayout(
            LOREM_IPSUM, TEXT_PAINT, MAX_INTRINSIC_WIDTH / 10,
            Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false
        )
    } else {
        StaticLayout.Builder.obtain(
            LOREM_IPSUM, 0, LOREM_IPSUM.length, TEXT_PAINT, MAX_INTRINSIC_WIDTH / 10
        ).build()
    }.also {
        require(it.lineCount >= 3) {
            "In these test cases, the test layout must have more than three lines"
        }
    }

    @Test
    fun testLayoutLineForOffset_upstream_first_line() {
        val layout = getTestLayout()
        val lineEnd = layout.getLineEnd(0)

        assertThat(layout.getLineForOffset(0, true /* upstream */))
            .isEqualTo(0)
        assertThat(layout.getLineForOffset(lineEnd, true /* upstream */))
            .isEqualTo(0)
    }

    @Test
    fun testLayoutLineForOffset_upstream_last_line() {
        val layout = getTestLayout()
        val lastLineNo = layout.lineCount - 1
        val lineStart = layout.getLineStart(lastLineNo)

        assertThat(layout.getLineForOffset(lineStart, true /* upstream */))
            .isEqualTo(lastLineNo - 1)
        assertThat(layout.getLineForOffset(layout.text.length, true /* upstream */))
            .isEqualTo(lastLineNo)
    }

    @Test
    fun testLayoutLineForOffset_upstream_middles() {
        val layout = getTestLayout()
        for (i in 1 until layout.lineCount - 1) {
            val lineStart = layout.getLineStart(i)
            val lineEnd = layout.getLineEnd(i)

            assertThat(layout.getLineForOffset(lineStart, true /* upstream */))
                .isEqualTo(i - 1)
            assertThat(layout.getLineForOffset(lineEnd, true /* upstream */))
                .isEqualTo(i)
        }
    }

    @Test
    fun testLayoutLineForOffset_downstream_first_line() {
        val layout = getTestLayout()
        val lineEnd = layout.getLineEnd(0)

        assertThat(layout.getLineForOffset(0, false /* upstream */))
            .isEqualTo(0)
        assertThat(layout.getLineForOffset(lineEnd, false /* upstream */))
            .isEqualTo(1)
    }

    @Test
    fun testLayoutLineForOffset_downstream_last_line() {
        val layout = getTestLayout()
        val lastLineNo = layout.lineCount - 1
        val lineStart = layout.getLineStart(lastLineNo)

        assertThat(layout.getLineForOffset(lineStart, false /* upstream */))
            .isEqualTo(lastLineNo)
        assertThat(layout.getLineForOffset(layout.text.length, false /* upstream */))
            .isEqualTo(lastLineNo)
    }

    @Test
    fun testLayoutLineForOffset_downstream_middles() {
        val layout = getTestLayout()
        for (i in 1 until layout.lineCount - 1) {
            val lineStart = layout.getLineStart(i)
            val lineEnd = layout.getLineEnd(i)

            assertThat(layout.getLineForOffset(lineStart, false /* upstream */))
                .isEqualTo(i)
            assertThat(layout.getLineForOffset(lineEnd, false /* upstream */))
                .isEqualTo(i + 1)
        }
    }
}