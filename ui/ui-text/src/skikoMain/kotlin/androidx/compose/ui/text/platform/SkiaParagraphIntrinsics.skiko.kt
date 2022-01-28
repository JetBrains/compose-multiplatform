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
package androidx.compose.ui.text.platform

import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import kotlin.math.ceil

internal actual fun ActualParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
): ParagraphIntrinsics =
    SkiaParagraphIntrinsics(
        text,
        style,
        spanStyles,
        placeholders,
        density,
        fontFamilyResolver
    )

internal class SkiaParagraphIntrinsics(
    val text: String,
    private val style: TextStyle,
    private val spanStyles: List<Range<SpanStyle>>,
    private val placeholders: List<Range<Placeholder>>,
    private val density: Density,
    private val fontFamilyResolver: FontFamily.Resolver
) : ParagraphIntrinsics {
    val textDirection = resolveTextDirection(style.textDirection)

    private var layouter: ParagraphLayouter? = newLayouter()

    fun layouter(): ParagraphLayouter {
        val layouter = this.layouter ?: newLayouter()
        this.layouter = null
        return layouter
    }

    private fun newLayouter() = ParagraphLayouter(
        text, textDirection, style, spanStyles, placeholders, density, fontFamilyResolver
    )

    override var minIntrinsicWidth = 0f
        private set
    override var maxIntrinsicWidth = 0f
        private set

    init {
        val para = layouter!!.layoutParagraph(Float.POSITIVE_INFINITY)
        minIntrinsicWidth = ceil(para.minIntrinsicWidth)
        maxIntrinsicWidth = ceil(para.maxIntrinsicWidth)
    }

    private fun resolveTextDirection(direction: TextDirection?): ResolvedTextDirection {
        return when (direction) {
            TextDirection.Ltr -> ResolvedTextDirection.Ltr
            TextDirection.Rtl -> ResolvedTextDirection.Rtl
            TextDirection.Content -> contentBasedTextDirection() ?: ResolvedTextDirection.Ltr
            TextDirection.ContentOrLtr -> contentBasedTextDirection() ?: ResolvedTextDirection.Ltr
            TextDirection.ContentOrRtl -> contentBasedTextDirection() ?: ResolvedTextDirection.Rtl
            else -> ResolvedTextDirection.Ltr
        }
    }

    private fun contentBasedTextDirection() = text.contentBasedTextDirection()
}

internal expect fun String.contentBasedTextDirection(): ResolvedTextDirection?