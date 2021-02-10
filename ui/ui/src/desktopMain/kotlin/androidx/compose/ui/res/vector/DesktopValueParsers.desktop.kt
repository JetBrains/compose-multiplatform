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

package androidx.compose.ui.res.vector

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val ALPHA_MASK = 0xFF000000.toInt()

// parseColorValue is copied from Android:
// https://cs.android.com/android-studio/platform/tools/base/+/05fadd8cb2aaafb77da02048c7a240b2147ff293:sdk-common/src/main/java/com/android/ide/common/vectordrawable/VdUtil.kt;l=58
/**
 * Parses a color value in #AARRGGBB format.
 *
 * @param color the color value string
 * @return the integer color value
 */
internal fun parseColorValue(color: String): Int {
    require(color.startsWith("#")) { "Invalid color value $color" }

    return when (color.length) {
        7 -> {
            // #RRGGBB
            Integer.parseUnsignedInt(color.substring(1), 16) or ALPHA_MASK
        }
        9 -> {
            // #AARRGGBB
            Integer.parseUnsignedInt(color.substring(1), 16)
        }
        4 -> {
            // #RGB
            val v = Integer.parseUnsignedInt(color.substring(1), 16)
            var k = (v shr 8 and 0xF) * 0x110000
            k = k or (v shr 4 and 0xF) * 0x1100
            k = k or (v and 0xF) * 0x11
            k or ALPHA_MASK
        }
        5 -> {
            // #ARGB
            val v = Integer.parseUnsignedInt(color.substring(1), 16)
            var k = (v shr 12 and 0xF) * 0x11000000
            k = k or (v shr 8 and 0xF) * 0x110000
            k = k or (v shr 4 and 0xF) * 0x1100
            k = k or (v and 0xF) * 0x11
            k or ALPHA_MASK
        }
        else -> ALPHA_MASK
    }
}

internal fun parseFillType(fillType: String): PathFillType = when (fillType) {
    "nonZero" -> PathFillType.NonZero
    "evenOdd" -> PathFillType.EvenOdd
    else -> throw UnsupportedOperationException("unknown fillType: $fillType")
}

internal fun parseStrokeCap(strokeCap: String): StrokeCap = when (strokeCap) {
    "butt" -> StrokeCap.Butt
    "round" -> StrokeCap.Round
    "square" -> StrokeCap.Square
    else -> throw UnsupportedOperationException("unknown strokeCap: $strokeCap")
}

internal fun parseStrokeJoin(strokeJoin: String): StrokeJoin = when (strokeJoin) {
    "miter" -> StrokeJoin.Miter
    "round" -> StrokeJoin.Round
    "bevel" -> StrokeJoin.Bevel
    else -> throw UnsupportedOperationException("unknown strokeJoin: $strokeJoin")
}

internal fun parseTileMode(tileMode: String): TileMode = when (tileMode) {
    "clamp" -> TileMode.Clamp
    "repeated" -> TileMode.Repeated
    "mirror" -> TileMode.Mirror
    else -> throw throw UnsupportedOperationException("unknown tileMode: $tileMode")
}

internal fun String?.parseDp(density: Density): Dp = with(density) {
    return when {
        this@parseDp == null -> 0f.dp
        endsWith("dp") -> removeSuffix("dp").toFloat().dp
        endsWith("px") -> removeSuffix("px").toFloat().toDp()
        else -> throw UnsupportedOperationException("value should ends with dp or px")
    }
}