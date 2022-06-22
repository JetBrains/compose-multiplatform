/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Metadata for overscroll effects for android platform.
 *
 * @param glowColor color for the glow effect, if the platform effect is a glow effect, otherwise
 * ignored.
 * @param drawPadding the amount of padding to apply from scrollable container bounds to
 * effect before drawing it
 */
@ExperimentalFoundationApi
@Stable
class OverscrollConfiguration(
    val glowColor: Color = Color(0xff666666), // taken from EdgeEffect.java defaults
    val drawPadding: PaddingValues = PaddingValues()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OverscrollConfiguration

        if (glowColor != other.glowColor) return false
        if (drawPadding != other.drawPadding) return false

        return true
    }

    override fun hashCode(): Int {
        var result = glowColor.hashCode()
        result = 31 * result + drawPadding.hashCode()
        return result
    }

    override fun toString(): String {
        return "OverscrollConfiguration(glowColor=$glowColor, drawPadding=$drawPadding)"
    }
}

/**
 * Composition local to provide configuration for scrolling containers down the
 * hierarchy. `null` means there will be no overscroll at all.
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@ExperimentalFoundationApi
@get:ExperimentalFoundationApi
val LocalOverscrollConfiguration = compositionLocalOf<OverscrollConfiguration?> {
    OverscrollConfiguration()
}