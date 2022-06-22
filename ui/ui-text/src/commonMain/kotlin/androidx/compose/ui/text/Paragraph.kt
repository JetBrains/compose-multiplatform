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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.ActualParagraph
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.math.ceil
import kotlin.jvm.JvmDefaultWithCompatibility

internal const val DefaultMaxLines = Int.MAX_VALUE

/**
 * A paragraph of text that is laid out.
 *
 * Paragraphs can be displayed on a [Canvas] using the [paint] method.
 */
@JvmDefaultWithCompatibility
interface Paragraph {
    /**
     * The amount of horizontal space this paragraph occupies.
     */
    val width: Float

    /**
     * The amount of vertical space this paragraph occupies.
     */
    val height: Float

    /**
     * The width for text if all soft wrap opportunities were taken.
     */
    val minIntrinsicWidth: Float

    /**
     * Returns the smallest width beyond which increasing the width never
     * decreases the height.
     */
    val maxIntrinsicWidth: Float

    /**
     * The distance from the top of the paragraph to the alphabetic
     * baseline of the first line, in logical pixels.
     */
    val firstBaseline: Float

    /**
     * The distance from the top of the paragraph to the alphabetic
     * baseline of the last line, in logical pixels.
     */
    val lastBaseline: Float

    /**
     * True if there is more vertical content, but the text was truncated, either
     * because we reached `maxLines` lines of text or because the `maxLines` was
     * null, `ellipsis` was not null, and one of the lines exceeded the width
     * constraint.
     *
     * See the discussion of the `maxLines` and `ellipsis` arguments at [ParagraphStyle].
     */
    val didExceedMaxLines: Boolean

    /**
     * The total number of lines in the text.
     */
    val lineCount: Int

    /**
     * The bounding boxes reserved for the input placeholders in this Paragraphs. Their locations
     * are relative to this Paragraph's coordinate. The order of this list corresponds to that of
     * input placeholders.
     * Notice that [Rect] in [placeholderRects] is nullable. When [Rect] is null, it indicates
     * that the corresponding [Placeholder] is ellipsized.
     */
    val placeholderRects: List<Rect?>

    /** Returns path that enclose the given text range. */
    fun getPathForRange(start: Int, end: Int): Path

    /** Returns rectangle of the cursor area. */
    fun getCursorRect(offset: Int): Rect

    /** Returns the left x Coordinate of the given line. */
    fun getLineLeft(lineIndex: Int): Float

    /** Returns the right x Coordinate of the given line. */
    fun getLineRight(lineIndex: Int): Float

    /** Returns the bottom y coordinate of the given line. */
    fun getLineTop(lineIndex: Int): Float

    /** Returns the bottom y coordinate of the given line. */
    fun getLineBottom(lineIndex: Int): Float

    /** Returns the height of the given line. */
    fun getLineHeight(lineIndex: Int): Float

    /** Returns the width of the given line. */
    fun getLineWidth(lineIndex: Int): Float

    /** Returns the start offset of the given line, inclusive. */
    fun getLineStart(lineIndex: Int): Int

    /**
     * Returns the end offset of the given line
     *
     * Characters being ellipsized are treated as invisible characters. So that if visibleEnd is
     * false, it will return line end including the ellipsized characters and vice verse.
     *
     * @param lineIndex the line number
     * @param visibleEnd if true, the returned line end will not count trailing whitespaces or
     * linefeed characters. Otherwise, this function will return the logical line end. By default
     * it's false.
     * @return an exclusive end offset of the line.
     */
    fun getLineEnd(lineIndex: Int, visibleEnd: Boolean = false): Int

    /**
     * Returns true if ellipsis happens on the given line, otherwise returns false
     *
     * @param lineIndex a 0 based line index
     * @return true if ellipsis happens on the given line, otherwise false
     */
    fun isLineEllipsized(lineIndex: Int): Boolean

    /**
     * Returns the line number on which the specified text offset appears.
     * If you ask for a position before 0, you get 0; if you ask for a position
     * beyond the end of the text, you get the last line.
     */
    fun getLineForOffset(offset: Int): Int

    /**
     * Compute the horizontal position where a newly inserted character at [offset] would be.
     *
     * If the inserted character at [offset] is within a LTR/RTL run, the returned position will be
     * the left(right) edge of the character.
     * ```
     * For example:
     *     Paragraph's direction is LTR.
     *     Text in logic order:               L0 L1 L2 R3 R4 R5
     *     Text in visual order:              L0 L1 L2 R5 R4 R3
     *         position of the offset(2):          |
     *         position of the offset(4):                   |
     *```
     * However, when the [offset] is at the BiDi transition offset, there will be two possible
     * visual positions, which depends on the direction of the inserted character.
     * ```
     * For example:
     *     Paragraph's direction is LTR.
     *     Text in logic order:               L0 L1 L2 R3 R4 R5
     *     Text in visual order:              L0 L1 L2 R5 R4 R3
     *         position of the offset(3):             |           (The inserted character is LTR)
     *                                                         |  (The inserted character is RTL)
     *```
     * In this case, [usePrimaryDirection] will be used to resolve the ambiguity. If true, the
     * inserted character's direction is assumed to be the same as Paragraph's direction.
     * Otherwise, the inserted character's direction is assumed to be the opposite of the
     * Paragraph's direction.
     * ```
     * For example:
     *     Paragraph's direction is LTR.
     *     Text in logic order:               L0 L1 L2 R3 R4 R5
     *     Text in visual order:              L0 L1 L2 R5 R4 R3
     *         position of the offset(3):             |           (usePrimaryDirection is true)
     *                                                         |  (usePrimaryDirection is false)
     *```
     * This method is useful to compute cursor position.
     *
     * @param offset the offset of the character, in the range of [0, length].
     * @param usePrimaryDirection whether the paragraph direction is respected when [offset]
     * points to a BiDi transition point.
     * @return a float number representing the horizontal position in the unit of pixel.
     */
    fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float

    /**
     * Get the text direction of the paragraph containing the given offset.
     */
    fun getParagraphDirection(offset: Int): ResolvedTextDirection

    /**
     * Get the text direction of the character at the given offset.
     */
    fun getBidiRunDirection(offset: Int): ResolvedTextDirection

    /**
     * Returns line number closest to the given graphical vertical position.
     * If you ask for a vertical position before 0, you get 0; if you ask for a vertical position
     * beyond the last line, you get the last line.
     */
    fun getLineForVerticalPosition(vertical: Float): Int

    /** Returns the character offset closest to the given graphical position. */
    fun getOffsetForPosition(position: Offset): Int

    /**
     * Returns the bounding box as Rect of the character for given character offset. Rect
     * includes the top, bottom, left and right of a character.
     */
    fun getBoundingBox(offset: Int): Rect

    /**
     * Returns the TextRange of the word at the given character offset. Characters not
     * part of a word, such as spaces, symbols, and punctuation, have word breaks
     * on both sides. In such cases, this method will return TextRange(offset, offset).
     * Word boundaries are defined more precisely in Unicode Standard Annex #29
     * http://www.unicode.org/reports/tr29/#Word_Boundaries
     */
    fun getWordBoundary(offset: Int): TextRange

    /**
     * Paint the paragraph to canvas, and also overrides some paint settings.
     */
    fun paint(
        canvas: Canvas,
        color: Color = Color.Unspecified,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null
    )

    /**
     * Paint the paragraph to canvas, and also overrides some paint settings.
     *
     * If not overridden, this function @throws [UnsupportedOperationException].
     */
    @ExperimentalTextApi
    fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float = Float.NaN,
        shadow: Shadow? = null,
        textDecoration: TextDecoration? = null
    ) {
        throw UnsupportedOperationException(
            "Using brush for painting the paragraph is a separate functionality that " +
                "is not supported on this platform")
    }
}

/**
 * Lays out a given [text] with the given constraints. A paragraph is a text that has a single
 * [ParagraphStyle].
 *
 * If the [style] does not contain any [androidx.compose.ui.text.style.TextDirection],
 * [androidx.compose.ui.text.style.TextDirection.Content] is used as the default value.
 *
 * @param text the text to be laid out
 * @param style the [TextStyle] to be applied to the whole text
 * @param spanStyles [SpanStyle]s to be applied to parts of text
 * @param placeholders a list of placeholder metrics which tells [Paragraph] where should
 * be left blank to leave space for inline elements.
 * @param maxLines the maximum number of lines that the text can have
 * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
 * @param width how wide the text is allowed to be
 * @param density density of the device
 * @param resourceLoader [Font.ResourceLoader] to be used to load the font given in [SpanStyle]s
 *
 * @throws IllegalArgumentException if [ParagraphStyle.textDirection] is not set
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Font.ResourceLoader is deprecated, instead pass FontFamily.Resolver",
    replaceWith = ReplaceWith(
        "Paragraph(text, style, spanStyles, placeholders, maxLines, " +
            "ellipsis, width, density, fontFamilyResolver)"
    ),
)
fun Paragraph(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    maxLines: Int = DefaultMaxLines,
    ellipsis: Boolean = false,
    width: Float,
    density: Density,
    resourceLoader: Font.ResourceLoader
): Paragraph = ActualParagraph(
    text,
    style,
    spanStyles,
    placeholders,
    maxLines,
    ellipsis,
    width,
    density,
    resourceLoader
)

/**
 * Lays out a given [text] with the given constraints. A paragraph is a text that has a single
 * [ParagraphStyle].
 *
 * If the [style] does not contain any [androidx.compose.ui.text.style.TextDirection],
 * [androidx.compose.ui.text.style.TextDirection.Content] is used as the default value.
 *
 * @param text the text to be laid out
 * @param style the [TextStyle] to be applied to the whole text
 * @param width how wide the text is allowed to be
 * @param density density of the device
 * @param fontFamilyResolver [FontFamily.Resolver] to be used to load the font given in [SpanStyle]s
 * @param spanStyles [SpanStyle]s to be applied to parts of text
 * @param placeholders a list of placeholder metrics which tells [Paragraph] where should
 * be left blank to leave space for inline elements.
 * @param maxLines the maximum number of lines that the text can have
 * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
 *
 * @throws IllegalArgumentException if [ParagraphStyle.textDirection] is not set
 */
@Deprecated(
    "Paragraph that takes maximum allowed width is deprecated, pass constraints instead.",
    ReplaceWith(
        "Paragraph(text, style, Constraints(maxWidth = ceil(width).toInt()), density, " +
            "fontFamilyResolver, spanStyles, placeholders, maxLines, ellipsis)",
        "kotlin.math.ceil",
        "androidx.compose.ui.unit.Constraints"
    )
)
fun Paragraph(
    text: String,
    style: TextStyle,
    width: Float,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    maxLines: Int = DefaultMaxLines,
    ellipsis: Boolean = false
): Paragraph = ActualParagraph(
    text,
    style,
    spanStyles,
    placeholders,
    maxLines,
    ellipsis,
    Constraints(maxWidth = width.ceilToInt()),
    density,
    fontFamilyResolver
)

/**
 * Lays out a given [text] with the given constraints. A paragraph is a text that has a single
 * [ParagraphStyle].
 *
 * If the [style] does not contain any [androidx.compose.ui.text.style.TextDirection],
 * [androidx.compose.ui.text.style.TextDirection.Content] is used as the default value.
 *
 * @param text the text to be laid out
 * @param style the [TextStyle] to be applied to the whole text
 * @param constraints how wide and tall the text is allowed to be. [Constraints.maxWidth]
 * will define the width of the Paragraph. [Constraints.maxHeight] helps defining the number of
 * lines that fit with ellipsis is true. Minimum components of the [Constraints] object are no-op.
 * @param density density of the device
 * @param fontFamilyResolver [FontFamily.Resolver] to be used to load the font given in [SpanStyle]s
 * @param spanStyles [SpanStyle]s to be applied to parts of text
 * @param placeholders a list of placeholder metrics which tells [Paragraph] where should
 * be left blank to leave space for inline elements.
 * @param maxLines the maximum number of lines that the text can have
 * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
 *
 * @throws IllegalArgumentException if [ParagraphStyle.textDirection] is not set
 */
fun Paragraph(
    text: String,
    style: TextStyle,
    constraints: Constraints,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    maxLines: Int = DefaultMaxLines,
    ellipsis: Boolean = false
): Paragraph = ActualParagraph(
    text,
    style,
    spanStyles,
    placeholders,
    maxLines,
    ellipsis,
    constraints,
    density,
    fontFamilyResolver
)

/**
 * Lays out the text in [ParagraphIntrinsics] with the given constraints. A paragraph is a text
 * that has a single [ParagraphStyle].
 *
 * @param paragraphIntrinsics [ParagraphIntrinsics] instance
 * @param maxLines the maximum number of lines that the text can have
 * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
 * @param width how wide the text is allowed to be
 */
@Deprecated(
    "Paragraph that takes maximum allowed width is deprecated, pass constraints instead.",
    ReplaceWith(
        "Paragraph(paragraphIntrinsics, Constraints(maxWidth = ceil(width).toInt()), maxLines, " +
            "ellipsis)",
        "kotlin.math.ceil",
        "androidx.compose.ui.unit.Constraints"
    )
)
fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    maxLines: Int = DefaultMaxLines,
    ellipsis: Boolean = false,
    width: Float
): Paragraph = ActualParagraph(
    paragraphIntrinsics,
    maxLines,
    ellipsis,
    Constraints(maxWidth = width.ceilToInt())
)

/**
 * Lays out the text in [ParagraphIntrinsics] with the given constraints. A paragraph is a text
 * that has a single [ParagraphStyle].
 *
 * @param paragraphIntrinsics [ParagraphIntrinsics] instance
 * @param constraints how wide and tall the text is allowed to be. [Constraints.maxWidth]
 * will define the width of the Paragraph. [Constraints.maxHeight] helps defining the number of
 * lines that fit with ellipsis is true. Minimum components of the [Constraints] object are no-op.
 * @param maxLines the maximum number of lines that the text can have
 * @param ellipsis whether to ellipsize text, applied only when [maxLines] is set
 */
fun Paragraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    constraints: Constraints,
    maxLines: Int = DefaultMaxLines,
    ellipsis: Boolean = false
): Paragraph = ActualParagraph(
    paragraphIntrinsics,
    maxLines,
    ellipsis,
    constraints
)

internal fun Float.ceilToInt(): Int = ceil(this).toInt()