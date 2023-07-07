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

@RunWith(JUnit4::class)
class PlatformSpanStyleTest {
    @Test
    fun equals_return_true_if_same_instance() {
        val platformStyle = PlatformSpanStyle()
        assertThat(platformStyle).isEqualTo(platformStyle)
    }

    @Test
    fun equals_return_true_if_equal_empty_instance() {
        val platformStyle = PlatformSpanStyle()
        val otherPlatformStyle = PlatformSpanStyle()
        assertThat(platformStyle).isEqualTo(otherPlatformStyle)
    }

    @Test
    fun equals_return_false_if_different_class() {
        val platformStyle = PlatformSpanStyle()
        val other = PlatformParagraphStyle()
        assertThat(platformStyle).isNotEqualTo(other)
    }

    @Test
    fun merge_returns_the_same_instance_when_other_is_null() {
        val style = PlatformSpanStyle()
        assertThat(style.merge(other = null)).isSameInstanceAs(style)
    }

    @Test
    fun merge_returns_the_same_instance_when_other_is_not_null() {
        val style = PlatformSpanStyle()
        val otherStyle = PlatformSpanStyle()

        assertThat(style.merge(other = otherStyle)).isSameInstanceAs(style)
    }

    @Test
    fun lerp_returns_the_same_instance() {
        val style = PlatformSpanStyle()
        val otherStyle = PlatformSpanStyle()

        assertThat(lerp(start = style, stop = otherStyle, 0.5f)).isSameInstanceAs(style)
    }

    @Test
    fun equals_return_true_for_equal_config() {
        val style = PlatformSpanStyle()
        val otherStyle = PlatformSpanStyle()
        assertThat(style).isEqualTo(otherStyle)
    }

    @Test
    fun copy_copiesPlatformStyle() {
        val platformStyle = PlatformSpanStyle()
        val style = SpanStyle(platformStyle = platformStyle)

        assertThat(style.copy().platformStyle).isSameInstanceAs(platformStyle)
    }

    @Test
    fun copy_withPlatformStyle_returnsInstanceWithPassedStyle() {
        val style = SpanStyle(platformStyle = null)

        val platformStyle = PlatformSpanStyle()
        assertThat(
            style.copy(platformStyle = platformStyle).platformStyle
        ).isSameInstanceAs(platformStyle)
    }

    @Test
    fun equals_return_true_if_platformStyle_is_same() {
        val platformStyle = PlatformSpanStyle()
        val style = SpanStyle(platformStyle = platformStyle)
        val otherStyle = SpanStyle(platformStyle = platformStyle)

        assertThat(style).isEqualTo(otherStyle)
    }
}