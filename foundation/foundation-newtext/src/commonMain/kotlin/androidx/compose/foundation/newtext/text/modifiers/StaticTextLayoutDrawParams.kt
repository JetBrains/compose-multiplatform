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
import kotlin.experimental.and
import kotlin.experimental.or

// TODO(seanmcq): break this into (text, style) and (rest...) objects to avoid high-invalidation cost
// TODO(seanmcq): Explore this holding non-AnnotatedString (future perf opt)
internal data class StaticTextLayoutDrawParams constructor(
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

internal fun StaticTextLayoutDrawParams.diff(
    other: StaticTextLayoutDrawParams
): StaticTextLayoutDrawParamsDiff {
    return packCompares(
        text = text != other.text,
        style = style != other.style,
        placeholders = placeholders != other.placeholders,
        layoutParams = !(minLines == other.minLines && maxLines == other.maxLines &&
            softWrap == other.softWrap &&
            fontFamilyResolver == other.fontFamilyResolver &&
            overflow == other.overflow),
        onTextLayout = onTextLayout != other.onTextLayout,
        onPlaceholderLayout = onPlaceholderLayout != other.onPlaceholderLayout,
        selectionController = selectionController != other.selectionController
    )
}

@JvmInline
internal value class StaticTextLayoutDrawParamsDiff(internal val value: Short)

internal const val TextFlag: Short = 0b0000000000000001
private const val StyleFlag: Short = 0b0000000000000010
private const val PlaceholderFlag: Short = 0b0000000000000100
private const val LayoutFlag: Short = 0b0000000000001000
private const val CallbackFlag: Short = 0b0000000000010000
private const val SelectionFlag: Short = 0b0000000000100000

private val AllLayoutAffectingFlags =
    TextFlag or StyleFlag or PlaceholderFlag or LayoutFlag

private val AllCallbackFlags =
    CallbackFlag or SelectionFlag

internal val StaticTextLayoutDrawParamsDiff.anyDiffs
    get() = value != 0.toShort()

internal val StaticTextLayoutDrawParamsDiff.hasLayoutDiffs: Boolean
    get() = (value and AllLayoutAffectingFlags).toInt() != 0

internal val StaticTextLayoutDrawParamsDiff.hasSemanticsDiffs: Boolean
    get() = (value and TextFlag).toInt() != 0

internal val StaticTextLayoutDrawParamsDiff.hasCallbackDiffs: Boolean
    get() = (value and AllCallbackFlags).toInt() != 0

private fun packCompares(
    text: Boolean,
    style: Boolean,
    placeholders: Boolean,
    layoutParams: Boolean,
    onTextLayout: Boolean,
    onPlaceholderLayout: Boolean,
    selectionController: Boolean
): StaticTextLayoutDrawParamsDiff {
    return StaticTextLayoutDrawParamsDiff(
        text.adds(TextFlag) or
        style.adds(StyleFlag) or
        placeholders.adds(PlaceholderFlag) or
        layoutParams.adds(LayoutFlag) or
        (onTextLayout || onPlaceholderLayout).adds(CallbackFlag) or
        selectionController.adds(SelectionFlag)
    )
}

private fun Boolean.adds(result: Short): Short {
    return if (this) result else 0
}
