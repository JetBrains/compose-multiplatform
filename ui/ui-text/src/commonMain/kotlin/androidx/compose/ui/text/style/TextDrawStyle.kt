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

package androidx.compose.ui.text.style

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.text.lerpDiscrete
import androidx.compose.ui.util.lerp

/**
 * An internal interface to represent possible ways to draw Text e.g. color, brush. This interface
 * aims to unify unspecified versions of complementary drawing styles. There are some guarantees
 * as following;
 *
 * - If [color] is not [Color.Unspecified], brush is null.
 * - If [brush] is not null, color is [Color.Unspecified].
 * - Both [color] can be [Color.Unspecified] and [brush] null, indicating that nothing is specified.
 * - [SolidColor] brushes are stored as regular [Color].
 */
internal interface TextDrawStyle {
    val color: Color

    val brush: Brush?

    val alpha: Float

    fun merge(other: TextDrawStyle): TextDrawStyle {
        // This control prevents Color or Unspecified TextDrawStyle to override an existing Brush.
        // It is a temporary measure to prevent Material Text composables to remove given Brush
        // from a TextStyle.
        // TODO(b/230787077): Just return other.takeOrElse { this } when Brush is stable.
        return when {
            other is BrushStyle && this is BrushStyle ->
                BrushStyle(other.value, other.alpha.takeOrElse { this.alpha })
            other is BrushStyle && this !is BrushStyle -> other
            other !is BrushStyle && this is BrushStyle -> this
            else -> other.takeOrElse { this }
        }
    }

    fun takeOrElse(other: () -> TextDrawStyle): TextDrawStyle {
        return if (this != Unspecified) this else other()
    }

    object Unspecified : TextDrawStyle {
        override val color: Color
            get() = Color.Unspecified

        override val brush: Brush?
            get() = null

        override val alpha: Float
            get() = Float.NaN
    }

    companion object {
        fun from(color: Color): TextDrawStyle {
            return if (color.isSpecified) ColorStyle(color) else Unspecified
        }

        fun from(brush: Brush?, alpha: Float): TextDrawStyle {
            return when (brush) {
                null -> Unspecified
                is SolidColor -> from(brush.value.modulate(alpha))
                is ShaderBrush -> BrushStyle(brush, alpha)
            }
        }
    }
}

private data class ColorStyle(
    val value: Color
) : TextDrawStyle {
    init {
        require(value.isSpecified) {
            "ColorStyle value must be specified, use TextDrawStyle.Unspecified instead."
        }
    }

    override val color: Color
        get() = value

    override val brush: Brush?
        get() = null

    override val alpha: Float
        get() = color.alpha
}

private data class BrushStyle(
    val value: ShaderBrush,
    override val alpha: Float
) : TextDrawStyle {
    override val color: Color
        get() = Color.Unspecified

    override val brush: Brush
        get() = value
}

/**
 * If both TextDrawStyles do not represent a Brush, lerp the color values. Otherwise, lerp
 * start to end discretely.
 */
internal fun lerp(start: TextDrawStyle, stop: TextDrawStyle, fraction: Float): TextDrawStyle {
    return if ((start !is BrushStyle && stop !is BrushStyle)) {
        TextDrawStyle.from(lerpColor(start.color, stop.color, fraction))
    } else if (start is BrushStyle && stop is BrushStyle) {
        TextDrawStyle.from(
            lerpDiscrete(start.brush, stop.brush, fraction),
            lerp(start.alpha, stop.alpha, fraction)
        )
    } else {
        lerpDiscrete(start, stop, fraction)
    }
}

internal fun Color.modulate(alpha: Float): Color = when {
    alpha.isNaN() || alpha >= 1f -> this
    else -> this.copy(alpha = this.alpha * alpha)
}

private fun Float.takeOrElse(block: () -> Float): Float {
    return if (this.isNaN()) block() else this
}