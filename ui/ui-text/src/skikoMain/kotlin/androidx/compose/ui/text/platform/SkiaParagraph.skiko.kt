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

import org.jetbrains.skia.Font as SkFont
import org.jetbrains.skia.FontStyle as SkFontStyle
import org.jetbrains.skia.paragraph.Alignment as SkAlignment
import org.jetbrains.skia.paragraph.DecorationLineStyle as SkDecorationLineStyle
import org.jetbrains.skia.paragraph.DecorationStyle as SkDecorationStyle
import org.jetbrains.skia.paragraph.Direction as SkDirection
import org.jetbrains.skia.paragraph.Paragraph as SkParagraph
import org.jetbrains.skia.paragraph.Shadow as SkShadow
import org.jetbrains.skia.paragraph.TextStyle as SkTextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SkiaParagraph
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ceilToInt
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontFamilyResolverImpl
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.SkiaFontLoader
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Paint
import org.jetbrains.skia.paragraph.BaselineMode
import org.jetbrains.skia.paragraph.ParagraphStyle
import org.jetbrains.skia.paragraph.PlaceholderAlignment
import org.jetbrains.skia.paragraph.PlaceholderStyle
import org.jetbrains.skia.paragraph.StrutStyle
import org.jetbrains.skia.paragraph.TextBox

private val DefaultFontSize = 16.sp

@Suppress("DEPRECATION")
@Deprecated(
    "Font.ResourceLoader is deprecated, instead pass FontFamily.Resolver",
    replaceWith = ReplaceWith("ActualParagraph(text, style, spanStyles, placeholders, " +
        "maxLines, ellipsis, width, density, fontFamilyResolver)"),
)
internal actual fun ActualParagraph(
    text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    maxLines: Int,
    ellipsis: Boolean,
    width: Float,
    density: Density,
    @Suppress("DEPRECATION") resourceLoader: Font.ResourceLoader
): Paragraph = SkiaParagraph(
    SkiaParagraphIntrinsics(
        text,
        style,
        spanStyles,
        placeholders,
        density,
        createFontFamilyResolver(resourceLoader)
    ),
    maxLines,
    ellipsis,
    Constraints(maxWidth = width.ceilToInt())
)

internal actual fun ActualParagraph(
    text: String,
    style: TextStyle,
    spanStyles: List<Range<SpanStyle>>,
    placeholders: List<Range<Placeholder>>,
    maxLines: Int,
    ellipsis: Boolean,
    constraints: Constraints,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
): Paragraph = SkiaParagraph(
    SkiaParagraphIntrinsics(
        text,
        style,
        spanStyles,
        placeholders,
        density,
        fontFamilyResolver
    ),
    maxLines,
    ellipsis,
    constraints
)

@Suppress("UNUSED_PARAMETER")
internal actual fun ActualParagraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    maxLines: Int,
    ellipsis: Boolean,
    constraints: Constraints
): Paragraph = SkiaParagraph(
    paragraphIntrinsics as SkiaParagraphIntrinsics,
    maxLines,
    ellipsis,
    constraints
)

private fun fontSizeInHierarchy(density: Density, base: Float, other: TextUnit): Float {
    return when {
        other.isUnspecified -> base
        other.isEm -> base * other.value
        other.isSp -> with(density) { other.toPx() }
        else -> error("Unexpected size in fontSizeInHierarchy")
    }
}

// Computed ComputedStyles always have font/letter size in pixels for particular `density`.
// It's important because density could be changed in runtime and it should force
// SkTextStyle to be recalculated. Or we can have different densities in different windows.
internal data class ComputedStyle(
    var color: Color,
    var fontSize: Float,
    var fontWeight: FontWeight?,
    var fontStyle: FontStyle?,
    var fontSynthesis: FontSynthesis?,
    var fontFamily: FontFamily?,
    var fontFeatureSettings: String?,
    var letterSpacing: Float?,
    var baselineShift: BaselineShift?,
    var textGeometricTransform: TextGeometricTransform?,
    var localeList: LocaleList?,
    var background: Color = Color.Unspecified,
    var textDecoration: TextDecoration?,
    var shadow: Shadow?
) {

    constructor(density: Density, spanStyle: SpanStyle) : this(
        color = spanStyle.color,
        fontSize = with(density) { spanStyle.fontSize.toPx() },
        fontWeight = spanStyle.fontWeight,
        fontStyle = spanStyle.fontStyle,
        fontSynthesis = spanStyle.fontSynthesis,
        fontFamily = spanStyle.fontFamily,
        fontFeatureSettings = spanStyle.fontFeatureSettings,
        letterSpacing = if (spanStyle.letterSpacing.isUnspecified) {
            null
        } else {
            with(density) {
                spanStyle.letterSpacing.toPx()
            }
        },
        baselineShift = spanStyle.baselineShift,
        textGeometricTransform = spanStyle.textGeometricTransform,
        localeList = spanStyle.localeList,
        background = spanStyle.background,
        textDecoration = spanStyle.textDecoration,
        shadow = spanStyle.shadow
    )

    @OptIn(ExperimentalTextApi::class)
    fun toSkTextStyle(fontFamilyResolver: FontFamily.Resolver): SkTextStyle {
        val res = SkTextStyle()
        if (color != Color.Unspecified) {
            res.color = color.toArgb()
        }
        fontStyle?.let {
            res.fontStyle = it.toSkFontStyle()
        }
        textDecoration?.let {
            res.decorationStyle = it.toSkDecorationStyle(this.color)
        }
        if (background != Color.Unspecified) {
            res.background = Paint().also {
                it.color = background.toArgb()
            }
        }
        fontWeight?.let {
            res.fontStyle = res.fontStyle.withWeight(it.weight)
        }
        shadow?.let {
            res.addShadow(it.toSkShadow())
        }

        letterSpacing?.let {
            res.letterSpacing = it
        }

        res.fontSize = fontSize
        fontFamily?.let {
            @Suppress("UNCHECKED_CAST")
            val resolved = fontFamilyResolver.resolve(
                it,
                fontWeight ?: FontWeight.Normal,
                fontStyle ?: FontStyle.Normal,
                fontSynthesis ?: FontSynthesis.None
            ).value as FontLoadResult
            res.fontFamilies = resolved.aliases.toTypedArray()
        }
        return res
    }

    fun merge(density: Density, other: SpanStyle) {
        val fontSize = fontSizeInHierarchy(density, fontSize, other.fontSize)
        if (other.color.isSpecified) {
            color = other.color
        }
        other.fontFamily?.let { fontFamily = it }
        this.fontSize = fontSize
        other.fontWeight?.let { fontWeight = it }
        other.fontStyle?.let { fontStyle = it }
        other.fontSynthesis?.let { fontSynthesis = it }
        other.fontFeatureSettings?.let { fontFeatureSettings = it }
        if (!other.letterSpacing.isUnspecified) {
            when {
                other.letterSpacing.isEm ->
                    letterSpacing = fontSize * other.letterSpacing.value
                other.letterSpacing.isSp ->
                    letterSpacing = with(density) {
                        other.letterSpacing.toPx()
                    }
                else -> throw UnsupportedOperationException()
            }
        }
        other.baselineShift?.let { baselineShift = it }
        other.textGeometricTransform?.let { textGeometricTransform = it }
        other.localeList?.let { localeList = it }
        if (other.background.isSpecified) {
            background = other.background
        }
        other.textDecoration?.let { textDecoration = it }
        other.shadow?.let { shadow = it }
    }
}

internal expect class WeakHashMap<K, V> : MutableMap<K, V>

// Building of SkTextStyle is a relatively expensive operation. We enable simple caching by
// mapping SpanStyle to SkTextStyle. To increase the efficiency of this mapping we are making
// most of the computations before converting Compose paragraph styles to Skia paragraph
private val skTextStylesCache = WeakHashMap<ComputedStyle, SkTextStyle>()

internal class ParagraphBuilder(
    val fontFamilyResolver: FontFamily.Resolver,
    val text: String,
    var textStyle: TextStyle,
    var ellipsis: String = "",
    var maxLines: Int = Int.MAX_VALUE,
    val spanStyles: List<Range<SpanStyle>>,
    val placeholders: List<Range<Placeholder>>,
    val density: Density,
    val textDirection: ResolvedTextDirection
) {
    private lateinit var initialStyle: SpanStyle
    private lateinit var defaultStyle: ComputedStyle
    private lateinit var ops: List<Op>

    /**
     * SkParagraph styles model doesn't match Compose's one.
     * SkParagraph has only a stack-based push/pop styles interface that works great with Span
     * trees.
     * But in Compose we have a list of SpanStyles attached to arbitrary ranges, possibly
     * overlapped, where a position in the list denotes style's priority
     * We map Compose styles to SkParagraph styles by projecting every range start/end to single
     * positions line and maintaining a list of active styles while building a paragraph. This list
     * of active styles is being compiled into single SkParagraph's style for every chunk of text
     */
    @OptIn(ExperimentalTextApi::class)
    fun build(): SkParagraph {
        initialStyle = textStyle.toSpanStyle().withDefaultFontSize()
        defaultStyle = ComputedStyle(density, initialStyle)
        ops = makeOps(
            spanStyles,
            placeholders
        )

        var pos = 0
        val ps = textStyleToParagraphStyle(textStyle, defaultStyle)

        if (maxLines != Int.MAX_VALUE) {
            ps.maxLinesCount = maxLines
            ps.ellipsis = ellipsis
        }

        // this downcast is always safe because of sealed types and we control construction
        @OptIn(ExperimentalTextApi::class)
        val platformFontLoader = (fontFamilyResolver as FontFamilyResolverImpl).platformFontLoader
        val fontCollection = when (platformFontLoader) {
            is SkiaFontLoader -> platformFontLoader.fontCollection
            else -> throw IllegalStateException("Unsupported font loader $platformFontLoader")
        }

        val pb = ParagraphBuilder(ps, fontCollection)

        var addText = true

        for (op in ops) {
            if (addText && pos < op.position) {
                pb.addText(text.subSequence(pos, op.position).toString())
            }

            when (op) {
                is Op.StyleAdd -> {
                    // FontLoader may have changed, so ensure that Font resolution is still valid
                    fontFamilyResolver.resolve(
                        op.style.fontFamily,
                        op.style.fontWeight ?: FontWeight.Normal,
                        op.style.fontStyle ?: FontStyle.Normal,
                        op.style.fontSynthesis ?: FontSynthesis.All
                    )
                    pb.pushStyle(makeSkTextStyle(op.style))
                }
                is Op.PutPlaceholder -> {
                    val placeholderStyle =
                        PlaceholderStyle(
                            op.width,
                            op.height,
                            op.cut.placeholder.placeholderVerticalAlign
                                .toSkPlaceholderAlignment(),
                            // TODO: figure out how exactly we have to work with BaselineMode & offset
                            BaselineMode.ALPHABETIC,
                            0f
                        )
                    pb.addPlaceholder(placeholderStyle)
                    addText = false
                }
                is Op.EndPlaceholder -> {
                    addText = true
                }
            }

            pos = op.position
        }

        if (addText && pos < text.length) {
            pb.addText(text.subSequence(pos, text.length).toString())
        }

        return pb.build()
    }

    private sealed class Op {
        abstract val position: Int

        data class StyleAdd(
            override val position: Int,
            val style: ComputedStyle
        ) : Op()

        data class PutPlaceholder(
            val cut: Cut.PutPlaceholder,
            var width: Float,
            var height: Float
        ) : Op() {
            override val position: Int by cut::position
        }

        data class EndPlaceholder(
            val cut: Cut.EndPlaceholder
        ) : Op() {
            override val position: Int by cut::position
        }
    }

    private sealed class Cut {
        abstract val position: Int

        data class StyleAdd(
            override val position: Int,
            val style: SpanStyle
        ) : Cut()

        data class StyleRemove(
            override val position: Int,
            val style: SpanStyle
        ) : Cut()

        data class PutPlaceholder(
            override val position: Int,
            val placeholder: Placeholder,
        ) : Cut()

        data class EndPlaceholder(override val position: Int) : Cut()
    }

    private fun makeOps(
        spans: List<Range<SpanStyle>>,
        placeholders: List<Range<Placeholder>>
    ): List<Op> {
        val cuts = mutableListOf<Cut>()
        for (span in spans) {
            cuts.add(Cut.StyleAdd(span.start, span.item))
            cuts.add(Cut.StyleRemove(span.end, span.item))
        }

        for (placeholder in placeholders) {
            cuts.add(Cut.PutPlaceholder(placeholder.start, placeholder.item))
            cuts.add(Cut.EndPlaceholder(placeholder.end))
        }

        val ops = mutableListOf<Op>(Op.StyleAdd(0, defaultStyle))
        cuts.sortBy { it.position }
        val activeStyles = mutableListOf(initialStyle)
        for (cut in cuts) {
            when {
                cut is Cut.StyleAdd -> {
                    activeStyles.add(cut.style)
                    val prev = previousStyleAddAtTheSamePosition(cut.position, ops)
                    if (prev == null) {
                        ops.add(
                            Op.StyleAdd(
                                cut.position,
                                mergeStyles(activeStyles).also { it.merge(density, cut.style) }
                            )
                        )
                    } else {
                        prev.style.merge(density, cut.style)
                    }
                }
                cut is Cut.StyleRemove -> {
                    activeStyles.remove(cut.style)
                    ops.add(Op.StyleAdd(cut.position, mergeStyles(activeStyles)))
                }
                cut is Cut.PutPlaceholder -> {
                    val currentStyle = mergeStyles(activeStyles)
                    val op = Op.PutPlaceholder(
                        cut = cut,
                        width = fontSizeInHierarchy(
                            density,
                            currentStyle.fontSize,
                            cut.placeholder.width
                        ),
                        height = fontSizeInHierarchy(
                            density,
                            currentStyle.fontSize,
                            cut.placeholder.height
                        ),
                    )
                    ops.add(op)
                }
                cut is Cut.EndPlaceholder ->
                    ops.add(Op.EndPlaceholder(cut))
            }
        }
        return ops
    }

    private fun mergeStyles(activeStyles: List<SpanStyle>): ComputedStyle {
        // there is always at least one active style
        val style = ComputedStyle(density, activeStyles[0])
        for (i in 1 until activeStyles.size) {
            style.merge(density, activeStyles[i])
        }
        return style
    }

    private fun previousStyleAddAtTheSamePosition(position: Int, ops: List<Op>): Op.StyleAdd? {
        for (prevOp in ops.asReversed()) {
            if (prevOp.position < position) return null
            if (prevOp is Op.StyleAdd) return prevOp
        }
        return null
    }

    private fun textStyleToParagraphStyle(
        style: TextStyle,
        computedStyle: ComputedStyle
    ): ParagraphStyle {
        val pStyle = ParagraphStyle()
        pStyle.textStyle = makeSkTextStyle(computedStyle)
        style.textAlign?.let {
            pStyle.alignment = it.toSkAlignment()
        }

        if (style.lineHeight.isSpecified) {
            val strutStyle = StrutStyle()

            strutStyle.isEnabled = true
            strutStyle.isHeightOverridden = true
            val fontSize = with(density) {
                style.fontSize.orDefaultFontSize().toPx()
            }
            val lineHeight = when {
                style.lineHeight.isSp -> with(density) {
                    style.lineHeight.toPx()
                }
                style.lineHeight.isEm -> fontSize * style.lineHeight.value
                else -> error("Unexpected size in textStyleToParagraphStyle")
            }
            strutStyle.height = lineHeight / fontSize
            pStyle.strutStyle = strutStyle
        }
        pStyle.direction = textDirection.toSkDirection()
        return pStyle
    }

    private fun makeSkTextStyle(style: ComputedStyle): SkTextStyle {
        return skTextStylesCache.getOrPut(style) {
            style.toSkTextStyle(fontFamilyResolver)
        }
    }

    @OptIn(ExperimentalTextApi::class)
    internal val defaultFont by lazy {
        val loadResult = textStyle.fontFamily?.let {
            @Suppress("UNCHECKED_CAST")
            fontFamilyResolver.resolve(
                it,
                textStyle.fontWeight ?: FontWeight.Normal,
                textStyle.fontStyle ?: FontStyle.Normal,
                textStyle.fontSynthesis ?: FontSynthesis.All
            ).value as FontLoadResult
        }
        SkFont(loadResult?.typeface, defaultStyle.fontSize)
    }

    internal val defaultHeight by lazy {
        defaultFont.metrics.height
    }
}

private fun TextUnit.orDefaultFontSize() = when {
    isUnspecified -> DefaultFontSize
    isEm -> DefaultFontSize * value
    else -> this
}

private fun SpanStyle.withDefaultFontSize(): SpanStyle {
    val fontSize = this.fontSize.orDefaultFontSize()
    val letterSpacing = when {
        this.letterSpacing.isEm -> fontSize * this.letterSpacing.value
        else -> this.letterSpacing
    }
    return this.copy(
        fontSize = fontSize,
        letterSpacing = letterSpacing
    )
}

fun FontStyle.toSkFontStyle(): SkFontStyle {
    return when (this) {
        FontStyle.Italic -> org.jetbrains.skia.FontStyle.ITALIC
        else -> org.jetbrains.skia.FontStyle.NORMAL
    }
}

fun TextDecoration.toSkDecorationStyle(color: Color): SkDecorationStyle {
    val underline = contains(TextDecoration.Underline)
    val overline = false
    val lineThrough = contains(TextDecoration.LineThrough)
    val gaps = false
    val lineStyle = SkDecorationLineStyle.SOLID
    val thicknessMultiplier = 1f
    return SkDecorationStyle(
        underline,
        overline,
        lineThrough,
        gaps,
        color.toArgb(),
        lineStyle,
        thicknessMultiplier
    )
}

fun PlaceholderVerticalAlign.toSkPlaceholderAlignment(): PlaceholderAlignment {
    return when (this) {
        PlaceholderVerticalAlign.AboveBaseline -> PlaceholderAlignment.ABOVE_BASELINE
        PlaceholderVerticalAlign.TextTop -> PlaceholderAlignment.TOP
        PlaceholderVerticalAlign.TextBottom -> PlaceholderAlignment.BOTTOM
        PlaceholderVerticalAlign.TextCenter -> PlaceholderAlignment.MIDDLE

        // TODO: figure out how we have to handle it properly
        PlaceholderVerticalAlign.Top -> PlaceholderAlignment.TOP
        PlaceholderVerticalAlign.Bottom -> PlaceholderAlignment.BOTTOM
        PlaceholderVerticalAlign.Center -> PlaceholderAlignment.MIDDLE
        else -> error("Invalid PlaceholderVerticalAlign.")
    }
}

internal fun Shadow.toSkShadow(): SkShadow {
    return SkShadow(color.toArgb(), offset.x, offset.y, blurRadius.toDouble())
}

internal fun TextAlign.toSkAlignment(): SkAlignment {
    return when (this) {
        TextAlign.Left -> SkAlignment.LEFT
        TextAlign.Right -> SkAlignment.RIGHT
        TextAlign.Center -> SkAlignment.CENTER
        TextAlign.Justify -> SkAlignment.JUSTIFY
        TextAlign.Start -> SkAlignment.START
        TextAlign.End -> SkAlignment.END
        else -> error("Invalid TextAlign")
    }
}

internal fun ResolvedTextDirection.toSkDirection(): SkDirection {
    return when (this) {
        ResolvedTextDirection.Ltr -> SkDirection.LTR
        ResolvedTextDirection.Rtl -> SkDirection.RTL
    }
}

internal fun TextBox.cursorHorizontalPosition(opposite: Boolean = false): Float {
    return when (direction) {
        SkDirection.LTR -> if (opposite) rect.left else rect.right
        SkDirection.RTL -> if (opposite) rect.right else rect.left
    }
}
