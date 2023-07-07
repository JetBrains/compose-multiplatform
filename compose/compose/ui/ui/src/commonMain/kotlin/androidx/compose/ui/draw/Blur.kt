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

package androidx.compose.ui.draw

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Determines the strategy used to render pixels in the blurred result that may extend beyond
 * the bounds of the original input.
 *
 * [BlurredEdgeTreatment] will clip the blur result to the boundaries of the
 * original content and optionally specified [shape].
 *
 * Sampling of pixels outside of content bounds will have the same value as the pixels at the
 * closest edge.
 * This is recommended for blurring content that does not contain transparent pixels
 * and ensuring the blurred result does not extend beyond the original bounds (ex. blurring
 * an image)
 *
 * @see TileMode.Clamp
 *
 * Alternatively using [BlurredEdgeTreatment.Unbounded] will not clip the blur result to the
 * boundaries of the original content. Sampling of pixels outside of the content bounds
 * will sample transparent black instead.
 * This is recommended for blurring content that is intended to render outside of the
 * original bounds and may contain transparent pixels in the original bounds (ex. blurring
 * an arbitrary shape or text)
 *
 * @see TileMode.Decal
*/
@Immutable
@kotlin.jvm.JvmInline
value class BlurredEdgeTreatment(val shape: Shape?) {

    companion object {

        /**
         * Bounded [BlurredEdgeTreatment] that clips content bounds to a rectangular shape
         */
        val Rectangle = BlurredEdgeTreatment(RectangleShape)

        /**
         * Do not clip the blur result to the boundaries of the original content.
         * Sampling of pixels outside of the content bounds will sample transparent black instead.
         * This is recommended for blurring content that is intended to render outside of the
         * original bounds and may contain transparent pixels in the original bounds (ex. blurring
         * an arbitrary shape or text)
         *
         * @see TileMode.Decal
         */
        val Unbounded = BlurredEdgeTreatment(null)
    }
}

/**
 * Draw content blurred with the specified radii. Note this effect is only supported on Android 12
 * and above. Attempts to use this Modifier on older Android versions will be ignored.
 *
 * Usage of this API renders the corresponding composable into a separate graphics layer.
 * Because the blurred content renders a larger area by the blur radius, this layer is explicitly
 * clipped to the content bounds. It is recommended introduce additional space around the drawn
 * content by the specified blur radius to remain within the content bounds.
 *
 * @see graphicsLayer
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.BlurSample
 * @sample androidx.compose.ui.samples.ImageBlurSample
 *
 * @param radiusX Radius of the blur along the x axis
 * @param radiusY Radius of the blur along the y axis
 * @param edgeTreatment Strategy used to render pixels outside of bounds of the original input
 */
@Stable
fun Modifier.blur(
    radiusX: Dp,
    radiusY: Dp,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Rectangle,
): Modifier {
    val clip: Boolean
    val tileMode: TileMode
    if (edgeTreatment.shape != null) {
        clip = true
        tileMode = TileMode.Clamp
    } else {
        clip = false
        tileMode = TileMode.Decal
    }
    return if ((radiusX > 0.dp && radiusY > 0.dp) || clip) {
        graphicsLayer {
            val horizontalBlurPixels = radiusX.toPx()
            val verticalBlurPixels = radiusY.toPx()
            this.renderEffect =
                // Only non-zero blur radii are valid BlurEffect parameters
                if (horizontalBlurPixels > 0f && verticalBlurPixels > 0f) {
                    BlurEffect(horizontalBlurPixels, verticalBlurPixels, tileMode)
                } else {
                    null
                }
            this.shape = edgeTreatment.shape ?: RectangleShape
            this.clip = clip
        }
    } else {
        this
    }
}

/**
 * Draw content blurred with the specified radii. Note this effect is only supported on Android 12
 * and above. Attempts to use this Modifier on older Android versions will be ignored.
 *
 * Usage of this API renders the corresponding composable into a separate graphics layer.
 * Because the blurred content renders a larger area by the blur radius, this layer is explicitly
 * clipped to the content bounds. It is recommended introduce additional space around the drawn
 * content by the specified blur radius to remain within the content bounds.
 *
 * @see graphicsLayer
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.BlurSample
 * @sample androidx.compose.ui.samples.ImageBlurSample
 *
 * @param radius Radius of the blur along both the x and y axis
 * @param edgeTreatment Strategy used to render pixels outside of bounds of the original input
 */
@Stable
fun Modifier.blur(
    radius: Dp,
    edgeTreatment: BlurredEdgeTreatment = BlurredEdgeTreatment.Rectangle
) = blur(radius, radius, edgeTreatment)