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

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy

/**
 * Calculates and provides the intrinsic width and height of text that contains [ParagraphStyle].
 *
 * @param annotatedString the text to be laid out
 * @param style the [TextStyle] to be applied to the whole text
 * @param placeholders a list of [Placeholder]s that specify ranges of text which will be
 * skipped during layout and replaced with [Placeholder]. It's required that the range of each
 * [Placeholder] doesn't cross paragraph boundary, otherwise [IllegalArgumentException] is thrown.
 * @param density density of the device
 * @param fontFamilyResolver [Font.ResourceLoader] to be used to load the font given in [SpanStyle]s

 * @see MultiParagraph
 * @see Placeholder
 *
 * @throws IllegalArgumentException if [ParagraphStyle.textDirection] is not set, or any
 * of the [placeholders] crosses paragraph boundary.
 */
class MultiParagraphIntrinsics(
    val annotatedString: AnnotatedString,
    style: TextStyle,
    val placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
) : ParagraphIntrinsics {

    @Suppress("DEPRECATION")
    @Deprecated("Font.ResourceLoader is deprecated, call with fontFamilyResolver",
        replaceWith = ReplaceWith("MultiParagraphIntrinsics(annotatedString, style, " +
            "placeholders, density, fontFamilyResolver)")
    )
    constructor(
        annotatedString: AnnotatedString,
        style: TextStyle,
        placeholders: List<AnnotatedString.Range<Placeholder>>,
        density: Density,
        resourceLoader: Font.ResourceLoader
    ) : this(
        annotatedString,
        style,
        placeholders,
        density,
        createFontFamilyResolver(resourceLoader)
    )

    // NOTE(text-perf-review): why are we using lazy here? Are there cases where these
    // calculations aren't executed?
    override val minIntrinsicWidth: Float by lazy(LazyThreadSafetyMode.NONE) {
        infoList.fastMaxBy {
            it.intrinsics.minIntrinsicWidth
        }?.intrinsics?.minIntrinsicWidth ?: 0f
    }

    override val maxIntrinsicWidth: Float by lazy(LazyThreadSafetyMode.NONE) {
        infoList.fastMaxBy {
            it.intrinsics.maxIntrinsicWidth
        }?.intrinsics?.maxIntrinsicWidth ?: 0f
    }

    /**
     * [ParagraphIntrinsics] for each paragraph included in the [buildAnnotatedString]. For empty string
     * there will be a single empty paragraph intrinsics info.
     */
    internal val infoList: List<ParagraphIntrinsicInfo>

    init {
        val paragraphStyle = style.toParagraphStyle()
        infoList = annotatedString
            .mapEachParagraphStyle(paragraphStyle) { annotatedString, paragraphStyleItem ->
                val currentParagraphStyle = resolveTextDirection(
                    paragraphStyleItem.item,
                    paragraphStyle
                )

                ParagraphIntrinsicInfo(
                    intrinsics = ParagraphIntrinsics(
                        text = annotatedString.text,
                        style = style.merge(currentParagraphStyle),
                        spanStyles = annotatedString.spanStyles,
                        placeholders = placeholders.getLocalPlaceholders(
                            paragraphStyleItem.start,
                            paragraphStyleItem.end
                        ),
                        density = density,
                        fontFamilyResolver = fontFamilyResolver
                    ),
                    startIndex = paragraphStyleItem.start,
                    endIndex = paragraphStyleItem.end
                )
            }
    }

    override val hasStaleResolvedFonts: Boolean
        get() = infoList.fastAny { it.intrinsics.hasStaleResolvedFonts }

    /**
     * if the [style] does `not` have [TextDirection] set, it will return a new
     * [ParagraphStyle] where [TextDirection] is set using the [defaultStyle]. Otherwise
     * returns the same [style] object.
     *
     * @param style ParagraphStyle to be checked for [TextDirection]
     * @param defaultStyle [ParagraphStyle] passed to [MultiParagraphIntrinsics] as the main style
     */
    private fun resolveTextDirection(
        style: ParagraphStyle,
        defaultStyle: ParagraphStyle
    ): ParagraphStyle {
        return style.textDirection?.let { style } ?: style.copy(
            textDirection = defaultStyle.textDirection
        )
    }
}

private fun List<AnnotatedString.Range<Placeholder>>.getLocalPlaceholders(start: Int, end: Int) =
    fastFilter { intersect(start, end, it.start, it.end) }.fastMap {
        require(start <= it.start && it.end <= end) {
            "placeholder can not overlap with paragraph."
        }
        AnnotatedString.Range(it.item, it.start - start, it.end - start)
    }

internal data class ParagraphIntrinsicInfo(
    val intrinsics: ParagraphIntrinsics,
    val startIndex: Int,
    val endIndex: Int
)