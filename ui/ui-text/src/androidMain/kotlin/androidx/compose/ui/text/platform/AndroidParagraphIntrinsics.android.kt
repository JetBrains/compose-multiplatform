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

package androidx.compose.ui.text.platform

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.State
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.android.LayoutCompat
import androidx.compose.ui.text.android.LayoutIntrinsics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.AndroidLocale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.platform.extensions.applySpanStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastAny
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import java.util.Locale

@OptIn(InternalPlatformTextApi::class)
internal class AndroidParagraphIntrinsics constructor(
    val text: String,
    val style: TextStyle,
    val spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    val placeholders: List<AnnotatedString.Range<Placeholder>>,
    val fontFamilyResolver: FontFamily.Resolver,
    val density: Density
) : ParagraphIntrinsics {

    internal val textPaint = AndroidTextPaint(Paint.ANTI_ALIAS_FLAG, density.density)

    internal val charSequence: CharSequence

    internal val layoutIntrinsics: LayoutIntrinsics

    override val maxIntrinsicWidth: Float
        get() = layoutIntrinsics.maxIntrinsicWidth

    override val minIntrinsicWidth: Float
        get() = layoutIntrinsics.minIntrinsicWidth

    private val resolvedTypefaces: MutableList<TypefaceDirtyTracker> = mutableListOf()

    override val hasStaleResolvedFonts: Boolean
        get() = resolvedTypefaces.fastAny { it.isStaleResolvedFont }

    internal val textDirectionHeuristic = resolveTextDirectionHeuristics(
        style.textDirection,
        style.localeList
    )

    init {
        val resolveTypeface: (FontFamily?, FontWeight, FontStyle, FontSynthesis) -> Typeface = {
                fontFamily, fontWeight, fontStyle, fontSynthesis ->
            val result = fontFamilyResolver.resolve(
                fontFamily,
                fontWeight,
                fontStyle,
                fontSynthesis
            )
            val holder = TypefaceDirtyTracker(result)
            resolvedTypefaces.add(holder)
            holder.typeface
        }

        val notAppliedStyle = textPaint.applySpanStyle(
            style = style.toSpanStyle(),
            resolveTypeface = resolveTypeface,
            density = density,
        )

        charSequence = createCharSequence(
            text = text,
            contextFontSize = textPaint.textSize,
            contextTextStyle = style,
            // NOTE(text-perf-review): this is sabotaging the optimization that
            // createCharSequence makes where it just uses `text` if there are no spanStyles!
            spanStyles = listOf(
                AnnotatedString.Range(
                    item = notAppliedStyle,
                    start = 0,
                    end = text.length
                )
            ) + spanStyles,
            placeholders = placeholders,
            density = density,
            resolveTypeface = resolveTypeface,
        )

        layoutIntrinsics = LayoutIntrinsics(charSequence, textPaint, textDirectionHeuristic)
    }
}

/**
 * For a given [TextDirection] return [TextLayout] constants for text direction
 * heuristics.
 */
@OptIn(InternalPlatformTextApi::class)
internal fun resolveTextDirectionHeuristics(
    textDirection: TextDirection? = null,
    localeList: LocaleList? = null
): Int {
    return when (textDirection ?: TextDirection.Content) {
        TextDirection.ContentOrLtr -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_LTR
        TextDirection.ContentOrRtl -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_RTL
        TextDirection.Ltr -> LayoutCompat.TEXT_DIRECTION_LTR
        TextDirection.Rtl -> LayoutCompat.TEXT_DIRECTION_RTL
        TextDirection.Content -> {
            val currentLocale = localeList?.let {
                (it[0].platformLocale as AndroidLocale).javaLocale
            } ?: Locale.getDefault()
            when (TextUtilsCompat.getLayoutDirectionFromLocale(currentLocale)) {
                ViewCompat.LAYOUT_DIRECTION_LTR -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_LTR
                ViewCompat.LAYOUT_DIRECTION_RTL -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_RTL
                else -> LayoutCompat.TEXT_DIRECTION_FIRST_STRONG_LTR
            }
        }
        else -> error("Invalid TextDirection.")
    }
}

@OptIn(InternalPlatformTextApi::class)
internal actual fun ActualParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
): ParagraphIntrinsics = AndroidParagraphIntrinsics(
    text = text,
    style = style,
    placeholders = placeholders,
    fontFamilyResolver = fontFamilyResolver,
    spanStyles = spanStyles,
    density = density
)

private class TypefaceDirtyTracker(val resolveResult: State<Any>) {
    val initial = resolveResult.value
    val typeface: Typeface
        get() = initial as Typeface

    val isStaleResolvedFont: Boolean
        get() = resolveResult.value !== initial
}