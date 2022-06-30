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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.caches.LruCache
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrain
import kotlin.math.ceil

/**
 * Use cases that converge to this number;
 * - Static text is drawn on canvas for legend and labels.
 * - Text toggles between enumerated states bold, italic.
 * - Multiple texts drawn but only their colors are animated.
 *
 * If text layout is always called with different inputs, this number is a good stopping point so
 * that cache does not becomes unnecessarily large and miss penalty stays low. Of course developers
 * should be aware that in a use case like that the cache should explicitly be disabled.
 */
private val DefaultCacheSize = 8

/**
 * TextMeasurer is responsible for measuring a text in its entirety so that it's ready to be drawn.
 *
 * Text layout is a computationally expensive task. Therefore, this class holds an internal LRU
 * Cache of layout input and output pairs to optimize the repeated measure calls that use the same
 * input parameters.
 *
 * Although most input parameters have a direct influence on layout, some parameters like color,
 * brush, and shadow can be ignored during layout and set at the end. Using TextMeasurer with
 * appropriate [cacheSize] should provide significant improvements while animating
 * non-layout-affecting attributes like color.
 *
 * Moreover, if there is a need to render multiple static texts, you can provide the number of texts
 * by [cacheSize] and their layouts should be cached for repeating calls. Be careful that even a
 * slight change in input parameters like fontSize, maxLines, an additional character in text would
 * create a distinct set of input parameters. As a result, a new layout would be calculated and a
 * new set of input and output pair would be placed in LRU Cache, possibly evicting an earlier
 * result.
 *
 * [FontFamily.Resolver], [LayoutDirection], and [Density] are required parameters to construct a
 * text layout but they have no safe fallbacks outside of composition. These parameters must be
 * provided during the construction of a [TextMeasurer] to be used as default values when they
 * are skipped in [TextMeasurer.measure] call.
 *
 * @param fallbackFontFamilyResolver to be used to load fonts given in [TextStyle] and [SpanStyle]s
 * in [AnnotatedString].
 * @param fallbackLayoutDirection layout direction of the measurement environment.
 * @param fallbackDensity density of the measurement environment. Density controls the scaling
 * factor for fonts.
 * @param cacheSize Capacity of internal cache inside TextMeasurer. Value of this parameter highly
 * depends on the consumer use case. Provide a cache size that is in line with how many distinct
 * text layouts are going to be calculated by this measurer repeatedly. If you are animating font
 * attributes, or any other layout affecting input, cache can be skipped because most measure calls
 * would miss the cache.
 */
@ExperimentalTextApi
@Immutable
class TextMeasurer constructor(
    private val fallbackFontFamilyResolver: FontFamily.Resolver,
    private val fallbackDensity: Density,
    private val fallbackLayoutDirection: LayoutDirection,
    private val cacheSize: Int = DefaultCacheSize
) {
    private val textLayoutCache: TextLayoutCache? = if (cacheSize > 0) {
        TextLayoutCache(cacheSize)
    } else null

    /**
     * Creates a [TextLayoutResult] according to given parameters.
     *
     * This function supports laying out text that consists of multiple paragraphs, includes
     * placeholders, wraps around soft line breaks, and might overflow outside the specified size.
     *
     * Most parameters for text affect the final text layout. One pixel change in [size] can
     * displace a word to another line which would cause a chain reaction that completely changes
     * how text is rendered. On the other hand, some attributes only play a role when drawing the
     * created text layout. For example text layout can be created completely in black color but we
     * can apply [TextStyle.color] later in draw phase. This also means that animating text color
     * shouldn't invalidate text layout.
     *
     * Thus, [textLayoutCache] helps in the process of converting a set of text layout inputs to
     * a text layout while ignoring non-layout-affecting attributes. Iterative calls that use the
     * same input parameters should benefit from substantial performance improvements.
     *
     * @param text the text to be laid out
     * @param style the [TextStyle] to be applied to the whole text
     * @param overflow How visual overflow should be handled.
     * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in
     * the text will be positioned as if there was unlimited horizontal space. If [softWrap] is
     * false, [overflow] and TextAlign may have unexpected effects.
     * @param maxLines An optional maximum number of lines for the text to span, wrapping if
     * necessary. If the text exceeds the given number of lines, it will be truncated according to
     * [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
     * @param placeholders a list of [Placeholder]s that specify ranges of text which will be
     * skipped during layout and replaced with [Placeholder]. It's required that the range of each
     * [Placeholder] doesn't cross paragraph boundary, otherwise [IllegalArgumentException] is
     * thrown.
     * @param size how wide and tall the text is allowed to be. [IntSize.width]
     * will define the width of the text. [IntSize.height] helps defining the
     * number of lines that fit if [softWrap] is enabled and [overflow] is [TextOverflow.Ellipsis].
     * @param layoutDirection layout direction of the measurement environment. If not specified,
     * defaults to the value that was given during initialization of this [TextMeasurer].
     * @param density density of the measurement environment. If not specified, defaults to
     * the value that was given during initialization of this [TextMeasurer].
     * @param fontFamilyResolver to be used to load the font given in [SpanStyle]s. If not
     * specified, defaults to the value that was given during initialization of this [TextMeasurer].
     * @param skipCache Disables cache optimization if it is passed as true.
     */
    @Stable
    fun measure(
        text: AnnotatedString,
        style: TextStyle = TextStyle.Default,
        overflow: TextOverflow = TextOverflow.Clip,
        softWrap: Boolean = true,
        maxLines: Int = Int.MAX_VALUE,
        placeholders: List<AnnotatedString.Range<Placeholder>> = emptyList(),
        size: IntSize = IntSize(Int.MAX_VALUE, Int.MAX_VALUE),
        layoutDirection: LayoutDirection = this.fallbackLayoutDirection,
        density: Density = this.fallbackDensity,
        fontFamilyResolver: FontFamily.Resolver = this.fallbackFontFamilyResolver,
        skipCache: Boolean = false
    ): TextLayoutResult {
        val constraints = Constraints(maxWidth = size.width, maxHeight = size.height)
        val requestedTextLayoutInput = TextLayoutInput(
            text,
            style,
            placeholders,
            maxLines,
            softWrap,
            overflow,
            density,
            layoutDirection,
            fontFamilyResolver,
            constraints
        )

        val cacheResult = if (!skipCache && textLayoutCache != null) {
            textLayoutCache.get(requestedTextLayoutInput)
        } else null

        return if (cacheResult != null) {
            cacheResult.copy(
                layoutInput = requestedTextLayoutInput,
                size = constraints.constrain(
                    IntSize(
                        cacheResult.multiParagraph.width.ceilToInt(),
                        cacheResult.multiParagraph.height.ceilToInt()
                    )
                )
            )
        } else {
            layout(requestedTextLayoutInput).also {
                textLayoutCache?.put(requestedTextLayoutInput, it)
            }
        }
    }

    internal companion object {
        /**
         * Computes the visual position of the glyphs for painting the text.
         *
         * The text will layout with a width that's as close to its max intrinsic width as possible
         * while still being greater than or equal to `minWidth` and less than or equal to
         * `maxWidth`.
         */
        private fun layout(
            textLayoutInput: TextLayoutInput
        ): TextLayoutResult = with(textLayoutInput) {
            val nonNullIntrinsics = MultiParagraphIntrinsics(
                annotatedString = text,
                style = resolveDefaults(style, layoutDirection),
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                placeholders = placeholders
            )

            val minWidth = constraints.minWidth
            val widthMatters = softWrap ||
                overflow == TextOverflow.Ellipsis
            val maxWidth = if (widthMatters && constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                Constraints.Infinity
            }

            // This is a fallback behavior because native text layout doesn't support multiple
            // ellipsis in one text layout.
            // When softWrap is turned off and overflow is ellipsis, it's expected that each line
            // that exceeds maxWidth will be ellipsized.
            // For example,
            // input text:
            //     "AAAA\nAAAA"
            // maxWidth:
            //     3 * fontSize that only allow 3 characters to be displayed each line.
            // expected output:
            //     AA…
            //     AA…
            // Here we assume there won't be any '\n' character when softWrap is false. And make
            // maxLines 1 to implement the similar behavior.
            val overwriteMaxLines = !softWrap &&
                overflow == TextOverflow.Ellipsis
            val finalMaxLines = if (overwriteMaxLines) 1 else maxLines

            // if minWidth == maxWidth the width is fixed.
            //    therefore we can pass that value to our paragraph and use it
            // if minWidth != maxWidth there is a range
            //    then we should check if the max intrinsic width is in this range to decide the
            //    width to be passed to Paragraph
            //        if max intrinsic width is between minWidth and maxWidth
            //           we can use it to layout
            //        else if max intrinsic width is greater than maxWidth, we can only use maxWidth
            //        else if max intrinsic width is less than minWidth, we should use minWidth
            val width = if (minWidth == maxWidth) {
                maxWidth
            } else {
                nonNullIntrinsics.maxIntrinsicWidth.ceilToInt().coerceIn(minWidth, maxWidth)
            }

            val multiParagraph = MultiParagraph(
                intrinsics = nonNullIntrinsics,
                constraints = Constraints(maxWidth = width, maxHeight = constraints.maxHeight),
                // This is a fallback behavior for ellipsis. Native
                maxLines = finalMaxLines,
                ellipsis = overflow == TextOverflow.Ellipsis
            )

            return TextLayoutResult(
                layoutInput = textLayoutInput,
                multiParagraph = multiParagraph,
                size = constraints.constrain(
                    IntSize(
                        ceil(multiParagraph.width).toInt(),
                        ceil(multiParagraph.height).toInt()
                    )
                )
            )
        }
    }
}

/**
 * Keeps an LRU layout cache of TextLayoutInput, TextLayoutResult pairs. Any non-layout affecting
 * change in TextLayoutInput (color, brush, shadow, TextDecoration) is ignored by this cache.
 *
 * @param capacity Maximum initial size of LRU cache.
 *
 * @throws IllegalArgumentException if capacity is not a positive integer.
 */
internal class TextLayoutCache(capacity: Int = DefaultCacheSize) {
    private val lruCache = LruCache<CacheTextLayoutInput, TextLayoutResult>(capacity)

    fun get(key: TextLayoutInput): TextLayoutResult? {
        val resultFromCache = lruCache.get(CacheTextLayoutInput(key)) ?: return null

        if (resultFromCache.multiParagraph.intrinsics.hasStaleResolvedFonts) {
            // one of the resolved fonts has updated, and this MeasuredText is no longer valid for
            // measure or display
            return null
        }

        return resultFromCache
    }

    fun put(key: TextLayoutInput, value: TextLayoutResult): TextLayoutResult? {
        return lruCache.put(CacheTextLayoutInput(key), value)
    }

    fun remove(key: TextLayoutInput): TextLayoutResult? {
        return lruCache.remove(CacheTextLayoutInput(key))
    }
}

/**
 * Provides custom hashCode and equals function that are only interested in layout affecting
 * attributes in TextLayoutInput. Used as a key in [TextLayoutCache].
 */
@Immutable
internal class CacheTextLayoutInput(val textLayoutInput: TextLayoutInput) {

    override fun hashCode(): Int = with(textLayoutInput) {
        var result = text.hashCode()
        result = 31 * result + style.hashCodeLayoutAffectingAttributes()
        result = 31 * result + placeholders.hashCode()
        result = 31 * result + maxLines
        result = 31 * result + softWrap.hashCode()
        result = 31 * result + overflow.hashCode()
        result = 31 * result + density.hashCode()
        result = 31 * result + layoutDirection.hashCode()
        result = 31 * result + fontFamilyResolver.hashCode()
        result = 31 * result + constraints.maxWidth.hashCode()
        result = 31 * result + constraints.maxHeight.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CacheTextLayoutInput) return false

        with(textLayoutInput) {
            if (text != other.textLayoutInput.text) return false
            if (!style.hasSameLayoutAffectingAttributes(other.textLayoutInput.style)) return false
            if (placeholders != other.textLayoutInput.placeholders) return false
            if (maxLines != other.textLayoutInput.maxLines) return false
            if (softWrap != other.textLayoutInput.softWrap) return false
            if (overflow != other.textLayoutInput.overflow) return false
            if (density != other.textLayoutInput.density) return false
            if (layoutDirection != other.textLayoutInput.layoutDirection) return false
            if (fontFamilyResolver !== other.textLayoutInput.fontFamilyResolver) return false
            if (constraints.maxWidth != other.textLayoutInput.constraints.maxWidth) return false
            if (constraints.maxHeight != other.textLayoutInput.constraints.maxHeight) return false
        }

        return true
    }
}