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
class PlatformParagraphStyleTest {

    @Test
    fun merge_returns_the_same_instance_when_other_is_null() {
        val style = PlatformParagraphStyle()
        assertThat(style.merge(other = null)).isSameInstanceAs(style)
    }

    @Test
    fun merge_returns_correct_includeFontPadding() {
        val style = PlatformParagraphStyle(includeFontPadding = false)
        val otherStyle = PlatformParagraphStyle(includeFontPadding = true)

        assertThat(style.merge(other = otherStyle).includeFontPadding).isTrue()
    }

    @Test
    fun paragraphStyle_merge_with_null_platformStyle() {
        val style = ParagraphStyle(
            platformStyle = PlatformParagraphStyle(includeFontPadding = true)
        )

        assertThat(style.merge(null).platformStyle).isEqualTo(style.platformStyle)

        val otherStyle = ParagraphStyle(
            platformStyle = PlatformParagraphStyle(includeFontPadding = false)
        )

        assertThat(otherStyle.merge(null).platformStyle).isEqualTo(otherStyle.platformStyle)
    }

    @Test
    fun paragraphStyle_merge_platformStyles() {
        val style = ParagraphStyle(
            platformStyle = PlatformParagraphStyle(includeFontPadding = true)
        )

        val otherStyle = ParagraphStyle(
            platformStyle = PlatformParagraphStyle(includeFontPadding = false)
        )

        val mergedStyle = style.merge(otherStyle)

        assertThat(mergedStyle.platformStyle).isNotNull()
        assertThat(
            mergedStyle.platformStyle?.includeFontPadding
        ).isFalse()

        val mergedStyle2 = otherStyle.merge(style)

        assertThat(mergedStyle2.platformStyle).isNotNull()
        assertThat(mergedStyle2.platformStyle?.includeFontPadding).isTrue()
    }

    @Test
    fun paragraphStyle_lerp_platformStyles_fraction_start() {
        val style = ParagraphStyle(
            platformStyle = PlatformParagraphStyle(includeFontPadding = true)
        )

        val otherStyle = ParagraphStyle(
            platformStyle = PlatformParagraphStyle(includeFontPadding = false)
        )

        val lerpedStyle = lerp(style, otherStyle, 0f)

        assertThat(lerpedStyle.platformStyle).isNotNull()
        assertThat(lerpedStyle.platformStyle?.includeFontPadding).isTrue()
    }

    @Test
    fun paragraphStyle_lerp_platformStyles_fraction_end() {
        val style = ParagraphStyle(
            platformStyle = PlatformParagraphStyle(includeFontPadding = true)
        )

        val otherStyle = ParagraphStyle(
            platformStyle = PlatformParagraphStyle(includeFontPadding = false)
        )

        val lerpedStyle = lerp(style, otherStyle, 1f)

        assertThat(lerpedStyle.platformStyle).isNotNull()
        assertThat(lerpedStyle.platformStyle?.includeFontPadding).isFalse()
    }

    @Test
    fun paragraphStyle_lerp_with_null_returns_default() {
        val defaultParagraphStyle = PlatformParagraphStyle.Default

        val style = ParagraphStyle(
            platformStyle = PlatformParagraphStyle(
                includeFontPadding = !defaultParagraphStyle.includeFontPadding
            )
        )

        val otherStyle = ParagraphStyle(platformStyle = null)

        val lerpedStyle = lerp(style, otherStyle, 1f)

        assertThat(lerpedStyle.platformStyle).isNotNull()
        assertThat(lerpedStyle.platformStyle?.includeFontPadding).isEqualTo(
            defaultParagraphStyle.includeFontPadding
        )
    }
}