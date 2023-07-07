/*
 * Copyright 2023 The Android Open Source Project
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

import kotlin.jvm.JvmInline
import org.jetbrains.skia.BreakIterator

internal actual fun String.findPrecedingBreak(index: Int): Int {
    val it = BreakIterator.makeCharacterInstance()
    it.setText(this)
    return it.preceding(index)
}

internal actual fun String.findFollowingBreak(index: Int): Int {
    val it = BreakIterator.makeCharacterInstance()
    it.setText(this)
    return it.following(index)
}

/**
 * See https://www.unicode.org/reports/tr9/
 */
@JvmInline
internal value class StrongDirectionType private constructor(val value: Int) {
    companion object {
        val None = StrongDirectionType(0)
        val Ltr = StrongDirectionType(1)
        val Rtl = StrongDirectionType(2)
    }
}

// TODO Remove once it's available in common stdlib https://youtrack.jetbrains.com/issue/KT-23251
internal typealias CodePoint = Int

/**
 * Converts a surrogate pair to a unicode code point.
 */
private fun Char.Companion.toCodePoint(high: Char, low: Char): CodePoint =
    (((high - MIN_HIGH_SURROGATE) shl 10) or (low - MIN_LOW_SURROGATE)) + 0x10000

/**
 * The minimum value of a supplementary code point, `\u0x10000`.
 */
private const val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x10000

/**
 * The maximum value of a Unicode code point.
 */
private const val MAX_CODE_POINT = 0X10FFFF

internal fun CodePoint.charCount(): Int = if (this >= MIN_SUPPLEMENTARY_CODE_POINT) 2 else 1

/**
 * Checks if the codepoint specified is a supplementary codepoint or not.
 */
internal fun CodePoint.isSupplementaryCodePoint(): Boolean =
    this in MIN_SUPPLEMENTARY_CODE_POINT..MAX_CODE_POINT

internal expect fun CodePoint.strongDirectionType(): StrongDirectionType
internal expect fun CodePoint.isNeutralDirection(): Boolean

/**
 * Determine direction based on the first strong directional character.
 * Only considers the characters outside isolate pairs.
 */
internal fun String.firstStrongDirectionType(): StrongDirectionType {
    for (codePoint in codePointsOutsideDirectionalIsolate) {
        return when (val strongDirectionType = codePoint.strongDirectionType()) {
            StrongDirectionType.None -> continue
            else -> strongDirectionType
        }
    }
    return StrongDirectionType.None
}

/**
 * U+2066 LEFT-TO-RIGHT ISOLATE (LRI)
 * U+2067 RIGHT-TO-LEFT ISOLATE (RLI)
 * U+2068 FIRST STRONG ISOLATE (FSI)
 */
private val PUSH_DIRECTIONAL_ISOLATE_RANGE: IntRange = 0x2066..0x2068

/**
 * U+2069 POP DIRECTIONAL ISOLATE (PDI)
 */
private const val POP_DIRECTIONAL_ISOLATE_CODE_POINT: Int = 0x2069

private val String.codePointsOutsideDirectionalIsolate get() = sequence {
    var openIsolateCount = 0
    for (codePoint in codePoints) {
        if (codePoint in PUSH_DIRECTIONAL_ISOLATE_RANGE) {
            openIsolateCount++
        } else if (codePoint == POP_DIRECTIONAL_ISOLATE_CODE_POINT) {
            if (openIsolateCount > 0) {
                openIsolateCount--
            }
        } else if (openIsolateCount == 0) {
            yield(codePoint)
        }
    }
}

internal val String.codePoints get() = sequence {
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        yield(codePoint)
        index += codePoint.charCount()
    }
}

/**
 * Returns the character (Unicode code point) at the specified index.
 */
internal fun String.codePointAt(index: Int): CodePoint {
    val high = this[index]
    if (high.isHighSurrogate() && index + 1 < this.length) {
        val low = this[index + 1]
        if (low.isLowSurrogate()) {
            return Char.toCodePoint(high, low)
        }
    }
    return high.code
}

/**
 * Returns the character (Unicode code point) before the specified index.
 */
internal fun String.codePointBefore(index: Int): CodePoint {
    val low = this[index]
    if (low.isLowSurrogate() && index - 1 >= 0) {
        val high = this[index - 1]
        if (high.isHighSurrogate()) {
            return Char.toCodePoint(high, low)
        }
    }
    return low.code
}
