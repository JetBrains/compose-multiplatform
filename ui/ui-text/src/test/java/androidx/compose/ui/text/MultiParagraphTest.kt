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

package androidx.compose.ui.text

import com.google.common.truth.Truth.assertThat
import org.mockito.kotlin.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MultiParagraphTest {
    @Test
    fun findParagraphByIndex() {
        val paragraphNumber = 5
        val paragraphLength = 10
        var start = 0
        val paragraphInfoList = List(paragraphNumber) {
            val end = start + paragraphLength
            ParagraphInfo(mock(), start, end).also { start = end }
        }

        for (i in 0 until paragraphNumber * paragraphLength) {
            assertThat(findParagraphByIndex(paragraphInfoList, i))
                .isEqualTo(i / paragraphLength)
        }
    }

    @Test
    fun findParagraphByLineIndex() {
        val paragraphNumber = 5
        val paragraphLineCount = 10
        var startLine = 0
        val paragraphInfoList = List(paragraphNumber) {
            val endLine = startLine + paragraphLineCount
            // StartIndex and endIndex doesn't matter in this test
            ParagraphInfo(mock(), 0, 0, startLine, endLine)
                .also { startLine = endLine }
        }

        for (i in 0 until paragraphNumber * paragraphLineCount) {
            assertThat(findParagraphByLineIndex(paragraphInfoList, i))
                .isEqualTo(i / paragraphLineCount)
        }
    }

    @Test
    fun findParagraphByYPosition() {
        val paragraphNumber = 5
        val paragraphHeight = 10
        var top = 0.0f
        val paragraphInfoList = List(paragraphNumber) {
            val bottom = top + paragraphHeight
            // StartIndex and endIndex doesn't matter in this test
            ParagraphInfo(mock(), 0, 0, top = top, bottom = bottom)
                .also { top = bottom }
        }

        for (i in 0 until paragraphNumber * paragraphHeight) {
            assertThat(findParagraphByY(paragraphInfoList, i.toFloat()))
                .isEqualTo(i / paragraphHeight)
        }
    }
}