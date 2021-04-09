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

import android.text.SpannableString
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.platform.extensions.setLineHeight
import androidx.compose.ui.text.platform.extensions.setPlaceholders
import androidx.compose.ui.text.platform.extensions.setSpanStyles
import androidx.compose.ui.text.platform.extensions.setTextIndent
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.isUnspecified

@OptIn(InternalPlatformTextApi::class)
internal fun createCharSequence(
    text: String,
    contextFontSize: Float,
    contextTextStyle: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    typefaceAdapter: TypefaceAdapter
): CharSequence {
    if (spanStyles.isEmpty() &&
        placeholders.isEmpty() &&
        contextTextStyle.textIndent == TextIndent.None &&
        contextTextStyle.lineHeight.isUnspecified
    ) {
        return text
    }

    val spannableString = SpannableString(text)

    spannableString.setLineHeight(contextTextStyle.lineHeight, contextFontSize, density)

    spannableString.setTextIndent(contextTextStyle.textIndent, contextFontSize, density)

    spannableString.setSpanStyles(contextTextStyle, spanStyles, density, typefaceAdapter)

    spannableString.setPlaceholders(placeholders, density)

    return spannableString
}
