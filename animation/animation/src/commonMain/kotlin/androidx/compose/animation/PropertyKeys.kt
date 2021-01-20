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

@file:Suppress("DEPRECATION")

package androidx.compose.animation

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.PropKey
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Bounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Built-in property key for pixel properties.
 *
 * @param label Label for distinguishing different prop keys in Android Studio.
 */
@Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
class PxPropKey(override val label: String = "PxPropKey") : PropKey<Float, AnimationVector1D> {
    override val typeConverter = Float.VectorConverter
}

/**
 * Built-in property key for [Dp] properties.
 *
 * @param label Label for distinguishing different prop keys in Android Studio.
 */
@Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
class DpPropKey(override val label: String = "DpPropKey") : PropKey<Dp, AnimationVector1D> {
    override val typeConverter = Dp.VectorConverter
}

/**
 * Built-in property key for [Offset] properties.
 *
 * @param label Label for distinguishing different prop keys in Android Studio.
 */
@Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
class OffsetPropKey(
    override val label: String = "OffsetPropKey"
) : PropKey<Offset, AnimationVector2D> {
    override val typeConverter = Offset.VectorConverter
}

/**
 * Built-in property key for [Color] properties.
 *
 * @param label Label for distinguishing different prop keys in Android Studio.
 */
@Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
class ColorPropKey(
    colorSpace: ColorSpace = ColorSpaces.Srgb,
    override val label: String = "ColorPropKey"
) : PropKey<Color, AnimationVector4D> {
    override val typeConverter = (Color.VectorConverter)(colorSpace)
}

/**
 * Built-in property key for [Rect] properties.
 *
 * @param label Label for distinguishing different prop keys in Android Studio.
 */
@Deprecated("Please use updateTransition or rememberInfiniteTransition instead.")
class RectPropKey(
    override val label: String = "RectPropKey"
) : PropKey<Rect, AnimationVector4D> {
    override val typeConverter = Rect.VectorConverter
}

/**
 * A lambda that takes a [ColorSpace] and returns a converter that can both convert a [Color] to
 * a [AnimationVector4D], and convert a [AnimationVector4D]) back to a [Color] in the given
 * [ColorSpace].
 */
private val ColorToVector: (colorSpace: ColorSpace) -> TwoWayConverter<Color, AnimationVector4D> =
    { colorSpace ->
        TwoWayConverter(
            convertToVector = {
                val linearColor = it.convert(ColorSpaces.LinearExtendedSrgb)
                AnimationVector4D(
                    linearColor.alpha, linearColor.red, linearColor.green,
                    linearColor.blue
                )
            },
            convertFromVector = {
                Color(
                    alpha = it.v1.coerceIn(0.0f, 1.0f),
                    red = it.v2.coerceIn(0.0f, 1.0f),
                    green = it.v3.coerceIn(0.0f, 1.0f),
                    blue = it.v4.coerceIn(0.0f, 1.0f),
                    colorSpace = ColorSpaces.LinearExtendedSrgb
                ).convert(colorSpace)
            }
        )
    }

/**
 * A type converter that converts a [Rect] to a [AnimationVector4D], and vice versa.
 */
private val RectToVector: TwoWayConverter<Rect, AnimationVector4D> =
    TwoWayConverter(
        convertToVector = {
            AnimationVector4D(it.left, it.top, it.right, it.bottom)
        },
        convertFromVector = {
            Rect(it.v1, it.v2, it.v3, it.v4)
        }
    )

/**
 * A type converter that converts a [Dp] to a [AnimationVector1D], and vice versa.
 */
private val DpToVector: TwoWayConverter<Dp, AnimationVector1D> = TwoWayConverter(
    convertToVector = { AnimationVector1D(it.value) },
    convertFromVector = { Dp(it.value) }
)

/**
 * A type converter that converts a [DpOffset] to a [AnimationVector2D], and vice versa.
 */
private val DpOffsetToVector: TwoWayConverter<DpOffset, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.x.value, it.y.value) },
        convertFromVector = { DpOffset(it.v1.dp, it.v2.dp) }
    )

/**
 * A type converter that converts a [Size] to a [AnimationVector2D], and vice versa.
 */
private val SizeToVector: TwoWayConverter<Size, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.width, it.height) },
        convertFromVector = { Size(it.v1, it.v2) }
    )

/**
 * A type converter that converts a [Bounds] to a [AnimationVector4D], and vice versa.
 */
private val BoundsToVector: TwoWayConverter<Bounds, AnimationVector4D> =
    TwoWayConverter(
        convertToVector = {
            AnimationVector4D(it.left.value, it.top.value, it.right.value, it.bottom.value)
        },
        convertFromVector = { Bounds(it.v1.dp, it.v2.dp, it.v3.dp, it.v4.dp) }
    )

/**
 * A type converter that converts a [Offset] to a [AnimationVector2D], and vice versa.
 */
private val OffsetToVector: TwoWayConverter<Offset, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.x, it.y) },
        convertFromVector = { Offset(it.v1, it.v2) }
    )

/**
 * A type converter that converts a [IntOffset] to a [AnimationVector2D], and vice versa.
 */
private val IntOffsetToVector: TwoWayConverter<IntOffset, AnimationVector2D> =
    TwoWayConverter(
        convertToVector = { AnimationVector2D(it.x.toFloat(), it.y.toFloat()) },
        convertFromVector = { IntOffset(it.v1.roundToInt(), it.v2.roundToInt()) }
    )

/**
 * A type converter that converts a [IntSize] to a [AnimationVector2D], and vice versa.
 */
private val IntSizeToVector: TwoWayConverter<IntSize, AnimationVector2D> =
    TwoWayConverter(
        { AnimationVector2D(it.width.toFloat(), it.height.toFloat()) },
        { IntSize(it.v1.roundToInt(), it.v2.roundToInt()) }
    )

/**
 * A lambda that takes a [ColorSpace] and returns a converter that can both convert a [Color] to
 * a [AnimationVector4D], and convert a [AnimationVector4D]) back to a [Color] in the given
 * [ColorSpace].
 */
val Color.Companion.VectorConverter:
    (colorSpace: ColorSpace) -> TwoWayConverter<Color, AnimationVector4D>
        get() = ColorToVector

/**
 * A type converter that converts a [Rect] to a [AnimationVector4D], and vice versa.
 */
@Deprecated("Rect.VectorConverter has been moved to animation-core library")
val Rect.Companion.VectorConverter: TwoWayConverter<Rect, AnimationVector4D>
    get() = RectToVector

/**
 * A type converter that converts a [Dp] to a [AnimationVector1D], and vice versa.
 */
@Deprecated("Dp.VectorConverter has been moved to animation-core library")
val Dp.Companion.VectorConverter: TwoWayConverter<Dp, AnimationVector1D>
    get() = DpToVector

/**
 * A type converter that converts a [DpOffset] to a [AnimationVector2D], and vice versa.
 */
@Deprecated("Position.VectorConverter has been moved to animation-core library")
val DpOffset.Companion.VectorConverter: TwoWayConverter<DpOffset, AnimationVector2D>
    get() = DpOffsetToVector

/**
 * A type converter that converts a [Size] to a [AnimationVector2D], and vice versa.
 */
@Deprecated("Size.VectorConverter has been moved to animation-core library")
val Size.Companion.VectorConverter: TwoWayConverter<Size, AnimationVector2D>
    get() = SizeToVector

/**
 * A type converter that converts a [Bounds] to a [AnimationVector4D], and vice versa.
 */
@Deprecated("Bounds.VectorConverter has been moved to animation-core library")
val Bounds.Companion.VectorConverter: TwoWayConverter<Bounds, AnimationVector4D>
    get() = BoundsToVector

/**
 * A type converter that converts a [Offset] to a [AnimationVector2D], and vice versa.
 */
@Deprecated("Offset.VectorConverter has been moved to animation-core library")
val Offset.Companion.VectorConverter: TwoWayConverter<Offset, AnimationVector2D>
    get() = OffsetToVector

/**
 * A type converter that converts a [IntOffset] to a [AnimationVector2D], and vice versa.
 */
@Deprecated("IntOffset.VectorConverter has been moved to animation-core library")
val IntOffset.Companion.VectorConverter: TwoWayConverter<IntOffset, AnimationVector2D>
    get() = IntOffsetToVector

/**
 * A type converter that converts a [IntSize] to a [AnimationVector2D], and vice versa.
 */
@Deprecated("IntSize.VectorConverter has been moved to animation-core library")
val IntSize.Companion.VectorConverter: TwoWayConverter<IntSize, AnimationVector2D>
    get() = IntSizeToVector