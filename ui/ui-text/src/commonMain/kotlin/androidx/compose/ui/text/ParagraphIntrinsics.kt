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
import androidx.compose.ui.text.platform.ActualParagraphIntrinsics
import androidx.compose.ui.unit.Density

/**
 * Calculates and presents the intrinsic width and height of text.
 */
interface ParagraphIntrinsics {
    /**
     * The width for text if all soft wrap opportunities were taken.
     */
    val minIntrinsicWidth: Float

    /**
     * Returns the smallest width beyond which increasing the width never
     * decreases the height.
     */
    val maxIntrinsicWidth: Float
}

/**
 *  Factory method to create a [ParagraphIntrinsics].
 *
 *  If the [style] does not contain any [androidx.compose.ui.text.style.TextDirection],
 * [androidx.compose.ui.text.style.TextDirection.Content] is used as the default value.
 *
 * @see ParagraphIntrinsics
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Font.ResourceLoader is deprecated, instead use FontFamily.Resolver",
    ReplaceWith("ParagraphIntrinsics(text, style, spanStyles, placeholders, density, " +
        "fontFamilyResolver")
)
fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    density: Density,
    resourceLoader: Font.ResourceLoader
): ParagraphIntrinsics = ActualParagraphIntrinsics(
    text = text,
    style = style,
    spanStyles = spanStyles,
    placeholders = placeholders,
    density = density,
    fontFamilyResolver = createFontFamilyResolver(resourceLoader)
)

fun ParagraphIntrinsics(
    text: String,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
    placeholders: List<AnnotatedString.Range<Placeholder>> = listOf(),
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
): ParagraphIntrinsics = ActualParagraphIntrinsics(
    text = text,
    style = style,
    spanStyles = spanStyles,
    placeholders = placeholders,
    density = density,
    fontFamilyResolver = fontFamilyResolver
)