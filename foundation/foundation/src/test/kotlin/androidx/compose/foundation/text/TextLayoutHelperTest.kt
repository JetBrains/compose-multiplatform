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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextLayoutHelperTest {

    lateinit var resourceLoader: Font.ResourceLoader

    lateinit var referenceResult: TextLayoutResult

    @Before
    fun setUp() {
        resourceLoader = mock()

        referenceResult = TextLayoutResult(
            TextLayoutInput(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = listOf(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = Constraints.fixedWidth(100)
            ),
            multiParagraph = mock(),
            size = IntSize(50, 50)
        )
    }

    @Test
    fun testCanReuse_same() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = constraints
            )
        ).isTrue()
    }

    @Test
    fun testCanReuse_different_text() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, Android").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_style() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(fontSize = 1.5.em),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_maxLines() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_softWrap() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_overflow() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Clip,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_density() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(2.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_layoutDirection() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Rtl,
                resourceLoader = resourceLoader,
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_resourceLoader() {
        val constraints = Constraints.fixedWidth(100)
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = mock(),
                constraints = constraints
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_constraints() {
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = emptyList(),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = Constraints.fixedWidth(200)
            )
        ).isFalse()
    }

    @Test
    fun testCanReuse_different_placeholders() {
        assertThat(
            referenceResult.canReuse(
                text = AnnotatedString.Builder("Hello, World").toAnnotatedString(),
                style = TextStyle(),
                placeholders = listOf(
                    AnnotatedString.Range(
                        item = Placeholder(10.sp, 20.sp, PlaceholderVerticalAlign.AboveBaseline),
                        start = 0,
                        end = 5
                    )
                ),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                density = Density(1.0f),
                layoutDirection = LayoutDirection.Ltr,
                resourceLoader = resourceLoader,
                constraints = Constraints.fixedWidth(200)
            )
        ).isFalse()
    }
}
