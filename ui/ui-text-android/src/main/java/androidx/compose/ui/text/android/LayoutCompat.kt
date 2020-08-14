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

package androidx.compose.ui.text.android

import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.Layout.Alignment
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import androidx.annotation.IntDef
import androidx.annotation.IntRange

/**
 * LayoutCompat class which provides all supported attributes by framework, and also defines
 * default value of those attributes for Compose.
 *
 * @suppress
 */
@InternalPlatformTextApi
object LayoutCompat {
    const val ALIGN_NORMAL = 0
    const val ALIGN_OPPOSITE = 1
    const val ALIGN_CENTER = 2
    const val ALIGN_LEFT = 3
    const val ALIGN_RIGHT = 4

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ALIGN_NORMAL,
        ALIGN_CENTER,
        ALIGN_OPPOSITE,
        ALIGN_LEFT,
        ALIGN_RIGHT
    )
    internal annotation class TextLayoutAlignment

    const val JUSTIFICATION_MODE_NONE = LineBreaker.JUSTIFICATION_MODE_NONE
    const val JUSTIFICATION_MODE_INTER_WORD = LineBreaker.JUSTIFICATION_MODE_INTER_WORD

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(JUSTIFICATION_MODE_NONE, JUSTIFICATION_MODE_INTER_WORD)
    internal annotation class JustificationMode

    const val HYPHENATION_FREQUENCY_NORMAL = Layout.HYPHENATION_FREQUENCY_NORMAL
    const val HYPHENATION_FREQUENCY_FULL = Layout.HYPHENATION_FREQUENCY_FULL
    const val HYPHENATION_FREQUENCY_NONE = Layout.HYPHENATION_FREQUENCY_NONE

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        HYPHENATION_FREQUENCY_NORMAL,
        HYPHENATION_FREQUENCY_FULL,
        HYPHENATION_FREQUENCY_NONE
    )
    internal annotation class HyphenationFrequency

    const val BREAK_STRATEGY_SIMPLE = LineBreaker.BREAK_STRATEGY_SIMPLE
    const val BREAK_STRATEGY_HIGH_QUALITY = LineBreaker.BREAK_STRATEGY_HIGH_QUALITY
    const val BREAK_STRATEGY_BALANCED = LineBreaker.BREAK_STRATEGY_BALANCED

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        BREAK_STRATEGY_SIMPLE,
        BREAK_STRATEGY_HIGH_QUALITY,
        BREAK_STRATEGY_BALANCED
    )
    internal annotation class BreakStrategy

    const val TEXT_DIRECTION_LTR = 0
    const val TEXT_DIRECTION_RTL = 1
    const val TEXT_DIRECTION_FIRST_STRONG_LTR = 2
    const val TEXT_DIRECTION_FIRST_STRONG_RTL = 3
    const val TEXT_DIRECTION_ANY_RTL_LTR = 4
    const val TEXT_DIRECTION_LOCALE = 5

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        TEXT_DIRECTION_LTR,
        TEXT_DIRECTION_RTL,
        TEXT_DIRECTION_FIRST_STRONG_LTR,
        TEXT_DIRECTION_FIRST_STRONG_RTL,
        TEXT_DIRECTION_ANY_RTL_LTR,
        TEXT_DIRECTION_LOCALE
    )
    internal annotation class TextDirection

    const val DEFAULT_ALIGNMENT = ALIGN_NORMAL

    internal const val DEFAULT_TEXT_DIRECTION = TEXT_DIRECTION_FIRST_STRONG_LTR

    const val DEFAULT_LINESPACING_MULTIPLIER = 1.0f

    internal const val DEFAULT_LINESPACING_EXTRA = 0.0f

    internal const val DEFAULT_INCLUDE_PADDING = true

    internal const val DEFAULT_MAX_LINES = Integer.MAX_VALUE

    internal const val DEFAULT_BREAK_STRATEGY = BREAK_STRATEGY_SIMPLE

    internal const val DEFAULT_HYPHENATION_FREQUENCY = HYPHENATION_FREQUENCY_NONE

    const val DEFAULT_JUSTIFICATION_MODE = JUSTIFICATION_MODE_NONE

    internal const val DEFAULT_FALLBACK_LINE_SPACING = true

    internal val DEFAULT_LAYOUT_ALIGNMENT = Alignment.ALIGN_NORMAL

    internal val DEFAULT_TEXT_DIRECTION_HEURISTIC: TextDirectionHeuristic =
        TextDirectionHeuristics.FIRSTSTRONG_LTR
}

/**
 * Returns the line number at the offset
 *
 * If the automatic line break didn't happen at the given offset, this returns the 0 origin line
 * number that contains the given offset character.
 * If the automatic line break happened at the given offset, this returns the preceding 0 origin
 * line number that contains the given offset character if upstream is true. Otherwise, returns
 * the line number that contains the given offset character.
 *
 * @param offset a character offset in the text
 * @param upstream true if you want to get preceding line number for the line broken offset.
 * false if you want to get the following line number for the line broken offset. This is ignored
 * if the offset it not a line broken offset.
 * @return the line number
 *
 * @suppress
 */
@InternalPlatformTextApi
fun Layout.getLineForOffset(@IntRange(from = 0) offset: Int, upstream: Boolean): Int {
    if (offset <= 0) return 0
    if (offset >= text.length) return lineCount - 1
    val downstreamLineNo = getLineForOffset(offset)
    val lineStart = getLineStart(downstreamLineNo)
    val lineEnd = getLineEnd(downstreamLineNo)

    if (lineStart != offset && lineEnd != offset) {
        return downstreamLineNo
    }

    if (lineStart == offset) {
        return if (upstream) downstreamLineNo - 1 else downstreamLineNo
    } else { // lineEnd == offset
        return if (upstream) downstreamLineNo else downstreamLineNo + 1
    }
}