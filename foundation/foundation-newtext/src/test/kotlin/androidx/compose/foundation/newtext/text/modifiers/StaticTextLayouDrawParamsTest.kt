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

package androidx.compose.foundation.newtext.text.modifiers

import androidx.compose.foundation.newtext.text.copypasta.selection.SelectionRegistrarImpl
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class StaticTextLayouDrawParamsTest {
    private val density = Density(density = 1f)
    private val fontFamilyResolver = mock<FontFamily.Resolver>()

    @Test
    fun textDiffers_flipsLayoutAndSemantics() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(text = AnnotatedString("other"))

        val diff = subject.diff(other)

        assertThat(diff.value).isEqualTo(1.toShort())
        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isTrue()
        assertThat(diff.hasSemanticsDiffs).isTrue()
        assertThat(diff.hasCallbackDiffs).isFalse()
    }

    @Test
    fun styleDiffers_flipsLayout() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(style = TextStyle(fontFeatureSettings = "other"))

        val diff = subject.diff(other)

        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isTrue()
        assertThat(diff.hasSemanticsDiffs).isFalse()
        assertThat(diff.hasCallbackDiffs).isFalse()
    }

    @Test
    fun ffrDiffers_flipsLayout() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(fontFamilyResolver = mock<FontFamily.Resolver>())

        val diff = subject.diff(other)

        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isTrue()
        assertThat(diff.hasSemanticsDiffs).isFalse()
        assertThat(diff.hasCallbackDiffs).isFalse()
    }

    @Test
    fun overflow_flipsLayout() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(overflow = TextOverflow.Ellipsis)

        val diff = subject.diff(other)

        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isTrue()
        assertThat(diff.hasSemanticsDiffs).isFalse()
        assertThat(diff.hasCallbackDiffs).isFalse()
    }

    @Test
    fun softWrap_flipsLayout() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(softWrap = false)

        val diff = subject.diff(other)

        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isTrue()
        assertThat(diff.hasSemanticsDiffs).isFalse()
        assertThat(diff.hasCallbackDiffs).isFalse()
    }

    @Test
    fun maxLines_flipsLayout() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(maxLines = 10)

        val diff = subject.diff(other)

        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isTrue()
        assertThat(diff.hasSemanticsDiffs).isFalse()
        assertThat(diff.hasCallbackDiffs).isFalse()
    }

    @Test
    fun minLines_flipsLayout() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(minLines = 10)

        val diff = subject.diff(other)

        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isTrue()
        assertThat(diff.hasSemanticsDiffs).isFalse()
        assertThat(diff.hasCallbackDiffs).isFalse()
    }

    @Test
    fun placeholders_flipsLayout() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(placeholders = emptyList())

        val diff = subject.diff(other)

        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isTrue()
        assertThat(diff.hasSemanticsDiffs).isFalse()
        assertThat(diff.hasCallbackDiffs).isFalse()
    }

    @Test
    fun onPlaceholderLayout_flipsCallbacks() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(onPlaceholderLayout = { println("Do it") })

        val diff = subject.diff(other)

        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isFalse()
        assertThat(diff.hasSemanticsDiffs).isFalse()
        assertThat(diff.hasCallbackDiffs).isTrue()
    }

    @Test
    fun onTextLayout_flipsCallbacks() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(onTextLayout = { println("Did a layout") })

        val diff = subject.diff(other)

        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isFalse()
        assertThat(diff.hasSemanticsDiffs).isFalse()
        assertThat(diff.hasCallbackDiffs).isTrue()
    }

    @Test
    fun onSelectionController_flipsAnyAndCallbacks() {
        val subject = StaticTextLayoutDrawParams(
            AnnotatedString("text"),
            TextStyle.Default,
            fontFamilyResolver
        )
        val other = subject.copy(
            selectionController = StaticTextSelectionModifierController(
                SelectionRegistrarImpl(),
                Color.Black
            )
        )

        val diff = subject.diff(other)

        assertThat(diff.anyDiffs).isTrue()
        assertThat(diff.hasLayoutDiffs).isFalse()
        assertThat(diff.hasSemanticsDiffs).isFalse()
        assertThat(diff.hasCallbackDiffs).isTrue()
    }
}
