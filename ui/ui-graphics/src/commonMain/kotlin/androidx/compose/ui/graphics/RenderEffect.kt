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

package androidx.compose.ui.graphics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset

/**
 * Intermediate rendering step used to render drawing commands with a corresponding
 * visual effect. A [RenderEffect] can be configured on a [GraphicsLayerScope]
 * and will be applied when drawn.
 */
@Immutable
expect sealed class RenderEffect() {

    /**
     * Capability query to determine if the particular platform supports the [RenderEffect]. Not
     * all platforms support all render effects.
     *
     * Note RenderEffect is only supported on Android 12 and above.
     * Attempts to use RenderEffect on older Android versions will be ignored.
     */
    open fun isSupported(): Boolean
}

/**
 * Create a [BlurEffect] that implicitly blurs the contents of the [GraphicsLayerScope] it is
 * configured on
 */
@Stable
fun BlurEffect(radiusX: Float, radiusY: Float, edgeTreatment: TileMode = TileMode.Clamp) =
    BlurEffect(null, radiusX, radiusY, edgeTreatment)

/**
 * [RenderEffect] that will blur the contents of an optional input [RenderEffect]. If no
 * input [RenderEffect] is provided, the drawing commands on the [GraphicsLayerScope] this
 * [RenderEffect] is configured on will be blurred.
 * @param radiusX Blur radius in the horizontal direction
 * @param radiusY Blur radius in the vertical direction
 * @param edgeTreatment Strategy used to render pixels outside of bounds of the original input
 */
@Immutable
expect class BlurEffect(
    renderEffect: RenderEffect?,
    radiusX: Float,
    radiusY: Float = radiusX,
    edgeTreatment: TileMode = TileMode.Clamp
) : RenderEffect

/**
 * Create an [OffsetEffect] that implicitly offsets the contents of the [GraphicsLayerScope] it is
 * configured on
 */
@Stable
fun OffsetEffect(offsetX: Float, offsetY: Float) = OffsetEffect(null, Offset(offsetX, offsetY))

/**
 * [RenderEffect] used to translate either the given [RenderEffect] or the content of
 * the [GraphicsLayerScope] it is configured on.
 */
@Immutable
expect class OffsetEffect(
    renderEffect: RenderEffect?,
    offset: Offset
) : RenderEffect