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

@file:OptIn(ExperimentalTextApi::class)

package androidx.compose.ui.text

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("DEPRECATION")
@RunWith(JUnit4::class)
class PlatformTextStyleTest {

    @Test
    fun toParagraphStyle_returnsNullPlatformStyle_ifNull() {
        val style = TextStyle(platformStyle = null)
        assertThat(style.toParagraphStyle()).isEqualTo(
            ParagraphStyle(platformStyle = null)
        )
    }

    @Test
    fun toParagraphStyle_returnsCorrectPlatformTextConfig_if_nonNull() {
        val style = TextStyle(
            platformStyle = PlatformTextStyle(
                includeFontPadding = true
            )
        )
        assertThat(style.toParagraphStyle()).isEqualTo(
            ParagraphStyle(
                platformStyle = PlatformParagraphStyle(
                    includeFontPadding = true
                )
            )
        )
    }

    @Test
    fun merge_platformStyle_null_on_nonNull() {
        val style = TextStyle(
            platformStyle = PlatformTextStyle(
                includeFontPadding = true
            )
        )
        val otherStyle = TextStyle(platformStyle = null)

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.platformStyle?.paragraphStyle?.includeFontPadding).isTrue()
    }

    @Test
    fun merge_platformStyle_nonNull_on_null() {
        val style = TextStyle(platformStyle = null)
        val otherStyle = TextStyle(
            platformStyle = PlatformTextStyle(
                includeFontPadding = true
            )
        )

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.platformStyle?.paragraphStyle?.includeFontPadding).isTrue()
    }

    @Test
    fun merge_platformStyle_false_on_true() {
        val style = TextStyle(
            platformStyle = PlatformTextStyle(
                includeFontPadding = true
            )
        )
        val otherStyle = TextStyle(
            platformStyle = PlatformTextStyle(
                includeFontPadding = false
            )
        )

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.platformStyle?.paragraphStyle?.includeFontPadding).isFalse()
    }

    @Test
    fun merge_platformStyle_true_on_false_on() {
        val style = TextStyle(
            platformStyle = PlatformTextStyle(
                includeFontPadding = false
            )
        )
        val otherStyle = TextStyle(
            platformStyle = PlatformTextStyle(
                includeFontPadding = true
            )
        )

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.platformStyle?.paragraphStyle?.includeFontPadding).isTrue()
    }
}