/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.newtext.text.modifiers

import androidx.compose.foundation.newtext.text.DefaultMinLines
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow

// TODO(seanmcq): break this into (text, style) and (rest...) objects to avoid high-invalidation cost
// TODO(seanmcq): Explore this holding non-AnnotatedString (future perf opt)
internal data class TextInlineContentLayoutDrawParams constructor(
    val text: AnnotatedString,
    val style: TextStyle,
    val fontFamilyResolver: FontFamily.Resolver,
    val onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    val overflow: TextOverflow = TextOverflow.Clip,
    val softWrap: Boolean = true,
    val maxLines: Int = Int.MAX_VALUE,
    val minLines: Int = DefaultMinLines,
    val placeholders: List<AnnotatedString.Range<Placeholder>>? = null,
    val onPlaceholderLayout: ((List<Rect?>) -> Unit)? = null,
    val selectionController: StaticTextSelectionModifierController? = null
) {
    init {
        validateMinMaxLines(minLines, maxLines)
    }
}

internal fun TextInlineContentLayoutDrawParams.equalForCallbacks(
    newParams: TextInlineContentLayoutDrawParams
): Boolean {
    return onTextLayout == newParams.onTextLayout &&
        onPlaceholderLayout == newParams.onPlaceholderLayout
}
internal fun TextInlineContentLayoutDrawParams.equalForLayout(
    newParams: TextInlineContentLayoutDrawParams
): Boolean {
    if (this === newParams) return true

    if (text != newParams.text) return false
    if (!style.hasSameLayoutAffectingAttributes(newParams.style)) return false
    if (maxLines != newParams.maxLines) return false
    if (minLines != newParams.minLines) return false
    if (softWrap != newParams.softWrap) return false
    if (fontFamilyResolver != newParams.fontFamilyResolver) return false
    if (overflow != newParams.overflow) return false
    if (placeholders != newParams.placeholders) return false

    return true
}