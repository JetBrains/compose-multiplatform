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

package androidx.compose.ui.draw

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DefaultShadowColor
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates a [graphicsLayer] that draws a shadow. The [elevation] defines the visual
 * depth of the physical object. The physical object has a shape specified by [shape].
 *
 * If the passed [shape] is concave the shadow will not be drawn on Android versions less than 10.
 *
 * Note that [elevation] is only affecting the shadow size and doesn't change the drawing order.
 * Use a [androidx.compose.ui.zIndex] modifier if you want to draw the elements with larger
 * [elevation] after all the elements with a smaller one.
 *
 * Usage of this API renders this composable into a separate graphics layer
 * @see graphicsLayer
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.ShadowSample
 *
 * @param elevation The elevation for the shadow in pixels
 * @param shape Defines a shape of the physical object
 * @param clip When active, the content drawing clips to the shape.
 */
@Deprecated(
    "Replace with shadow which accepts ambientColor and spotColor parameters",
    ReplaceWith(
        "Modifier.shadow(elevation, shape, clip, DefaultShadowColor, DefaultShadowColor)",
        "androidx.compose.ui.draw"
    ),
    DeprecationLevel.HIDDEN
)
@Stable
fun Modifier.shadow(
    elevation: Dp,
    shape: Shape = RectangleShape,
    clip: Boolean = elevation > 0.dp
) = shadow(
    elevation,
    shape,
    clip,
    DefaultShadowColor,
    DefaultShadowColor,
)

/**
 * Creates a [graphicsLayer] that draws a shadow. The [elevation] defines the visual
 * depth of the physical object. The physical object has a shape specified by [shape].
 *
 * If the passed [shape] is concave the shadow will not be drawn on Android versions less than 10.
 *
 * Note that [elevation] is only affecting the shadow size and doesn't change the drawing order.
 * Use a [androidx.compose.ui.zIndex] modifier if you want to draw the elements with larger
 * [elevation] after all the elements with a smaller one.
 *
 * Usage of this API renders this composable into a separate graphics layer
 * @see graphicsLayer
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.ShadowSample
 *
 * @param elevation The elevation for the shadow in pixels
 * @param shape Defines a shape of the physical object
 * @param clip When active, the content drawing clips to the shape.
 */
@Stable
fun Modifier.shadow(
    elevation: Dp,
    shape: Shape = RectangleShape,
    clip: Boolean = elevation > 0.dp,
    ambientColor: Color = DefaultShadowColor,
    spotColor: Color = DefaultShadowColor,
) = if (elevation > 0.dp || clip) {
    inspectable(
        inspectorInfo = debugInspectorInfo {
            name = "shadow"
            properties["elevation"] = elevation
            properties["shape"] = shape
            properties["clip"] = clip
            properties["ambientColor"] = ambientColor
            properties["spotColor"] = spotColor
        }
    ) {
        graphicsLayer {
            this.shadowElevation = elevation.toPx()
            this.shape = shape
            this.clip = clip
            this.ambientShadowColor = ambientColor
            this.spotShadowColor = spotColor
        }
    }
} else {
    this
}
