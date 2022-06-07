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

import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.font.getAndroidTypefaceStyle
import androidx.compose.ui.text.platform.extensions.setBackground
import androidx.compose.ui.text.platform.extensions.setColor
import androidx.compose.ui.text.platform.extensions.setFontSize
import androidx.compose.ui.text.platform.extensions.setLocaleList
import androidx.compose.ui.text.platform.extensions.toSpan
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach

/**
 * Convert an AnnotatedString into SpannableString for Android text to speech support.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalTextApi // used in ui:ui
fun AnnotatedString.toAccessibilitySpannableString(
    density: Density,
    @Suppress("DEPRECATION") resourceLoader: Font.ResourceLoader
): SpannableString {
    @Suppress("DEPRECATION")
    return toAccessibilitySpannableString(density, createFontFamilyResolver(resourceLoader))
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@InternalTextApi // used in ui:ui
fun AnnotatedString.toAccessibilitySpannableString(
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
): SpannableString {
    val spannableString = SpannableString(text)
    spanStyles.fastForEach { (style, start, end) ->
        // b/232238615 looking up fonts inside of accessibility does not honor overwritten
        // FontFamilyResolver. This is not safe until Font.ResourceLoader is fully removed.
        val noFontStyle = style.copy(fontFamily = null)
        spannableString.setSpanStyle(noFontStyle, start, end, density, fontFamilyResolver)
    }

    getTtsAnnotations(0, length).fastForEach { (ttsAnnotation, start, end) ->
        spannableString.setSpan(
            ttsAnnotation.toSpan(),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    return spannableString
}

/** Apply the serializable styles to SpannableString. */
@OptIn(ExperimentalTextApi::class)
private fun SpannableString.setSpanStyle(
    spanStyle: SpanStyle,
    start: Int,
    end: Int,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
) {
    setColor(spanStyle.color, start, end)

    setFontSize(spanStyle.fontSize, density, start, end)

    if (spanStyle.fontWeight != null || spanStyle.fontStyle != null) {
        // If current typeface is bold, StyleSpan won't change it to normal. The same applies to
        // font style, so use normal as default works here.
        // This is also a bug in framework span. But we can't find a good solution so far.
        val fontWeight = spanStyle.fontWeight ?: FontWeight.Normal
        val fontStyle = spanStyle.fontStyle ?: FontStyle.Normal
        setSpan(
            StyleSpan(getAndroidTypefaceStyle(fontWeight, fontStyle)),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    // TypefaceSpan accepts Typeface as parameter only after P. And only font family string can be
    // pass to other thread.
    // Here we try to create TypefaceSpan with font family string if possible.
    if (spanStyle.fontFamily != null) {
        if (spanStyle.fontFamily is GenericFontFamily) {
            setSpan(
                TypefaceSpan(spanStyle.fontFamily.name),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // TODO(b/214587005): Check for async here and uncache
                val typeface = fontFamilyResolver.resolve(
                    fontFamily = spanStyle.fontFamily,
                    fontSynthesis = spanStyle.fontSynthesis ?: FontSynthesis.All
                ).value as Typeface
                setSpan(
                    Api28Impl.createTypefaceSpan(typeface),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    if (spanStyle.textDecoration != null) {
        // This doesn't match how we rendering the styles. When TextDecoration.None is set, it
        // should remove the underline and lineThrough effect on the given range. Here we didn't
        // remove any previously applied spans yet.
        if (TextDecoration.Underline in spanStyle.textDecoration) {
            setSpan(
                UnderlineSpan(),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        if (TextDecoration.LineThrough in spanStyle.textDecoration) {
            setSpan(
                StrikethroughSpan(),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    if (spanStyle.textGeometricTransform != null) {
        setSpan(
            ScaleXSpan(spanStyle.textGeometricTransform.scaleX),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    setLocaleList(spanStyle.localeList, start, end)

    setBackground(spanStyle.background, start, end)
}

@RequiresApi(28)
private object Api28Impl {
    @DoNotInline
    fun createTypefaceSpan(typeface: Typeface): TypefaceSpan = TypefaceSpan(typeface)
}