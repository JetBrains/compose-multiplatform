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

package androidx.compose.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp

/**
 * Class to specify the stroke to draw border with.
 *
 * @param width width of the border in [Dp]. Use [Dp.Hairline] for one-pixel border.
 * @param brush brush to paint the border with
 */
@Immutable
class BorderStroke(val width: Dp, val brush: Brush) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BorderStroke) return false

        if (width != other.width) return false
        if (brush != other.brush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + brush.hashCode()
        return result
    }

    override fun toString(): String {
        return "BorderStroke(width=$width, brush=$brush)"
    }

    fun copy(
        width: Dp = this.width,
        brush: Brush = this.brush
    ): BorderStroke {
        return BorderStroke(
            width = width,
            brush = brush
        )
    }
}

/**
 * Create [BorderStroke] class with width and [Color]
 *
 * @param width width of the border in [Dp]. Use [Dp.Hairline] for one-pixel border.
 * @param color color to paint the border with
 */
@Stable
fun BorderStroke(width: Dp, color: Color) = BorderStroke(width, SolidColor(color))