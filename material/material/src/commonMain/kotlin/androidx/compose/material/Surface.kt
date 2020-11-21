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

package androidx.compose.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material surface is the central metaphor in material design. Each surface exists at a given
 * elevation, which influences how that piece of surface visually relates to other surfaces and
 * how that surface casts shadows.
 *
 * The [Surface] is responsible for:
 *
 * 1) Clipping: Surface clips its children to the shape specified by [shape]
 *
 * 2) Elevation: Surface elevates its children on the Z axis by [elevation] pixels,
 * and draws the appropriate shadow.
 *
 * 3) Borders: If [shape] has a border, then it will also be drawn.
 *
 * 4) Background: Surface fills the shape specified by [shape] with the [color]. If [color] is
 * [Colors.surface], the [ElevationOverlay] from [AmbientElevationOverlay] will be used to apply
 * an overlay - by default this will only occur in dark theme. The color of the overlay depends
 * on the [elevation] of this Surface, and the [AmbientAbsoluteElevation] set by any parent
 * surfaces. This ensures that a Surface never appears to have a lower elevation overlay than its
 * ancestors, by summing the elevation of all previous Surfaces.
 *
 * 5) Content color: Surface uses [contentColor] to specify a preferred color for the content of
 * this surface - this is used by the [Text] and [Icon] components as a default color.
 *
 * If no [contentColor] is set, this surface will try and match its background color to a color
 * defined in the theme [Colors], and return the corresponding `onFoo` color. For example,
 * if the [color] of this surface is [Colors.surface], [contentColor] will be set to
 * [Colors.onSurface]. If [color] is not part of the theme palette, [contentColor] will keep
 * the same value set above this Surface.
 *
 * @sample androidx.compose.material.samples.SurfaceSample
 *
 * To modify these default style values used by text, use [ProvideTextStyle] or explicitly
 * pass a new [TextStyle] to your text.
 *
 * To manually retrieve the content color inside a surface, use [AmbientContentColor].
 *
 * @param modifier Modifier to be applied to the layout corresponding to the surface
 * @param shape Defines the surface's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param color The background color. Use [Color.Transparent] to have no color.
 * @param contentColor The preferred content color provided by this Surface to its children.
 * Defaults to either the matching `onFoo` color for [color], or if [color] is not a color from
 * the theme, this will keep the same value set above this Surface.
 * @param border Optional border to draw on top of the surface
 * @param elevation The size of the shadow below the surface. Note that It will not affect z index
 * of the Surface. If you want to change the drawing order you can use `Modifier.zIndex`.
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val elevationPx = with(AmbientDensity.current) { elevation.toPx() }
    val elevationOverlay = AmbientElevationOverlay.current
    val absoluteElevation = AmbientAbsoluteElevation.current + elevation
    val backgroundColor = if (color == MaterialTheme.colors.surface && elevationOverlay != null) {
        elevationOverlay.apply(color, absoluteElevation)
    } else {
        color
    }
    SurfaceLayout(
        modifier.graphicsLayer(shadowElevation = elevationPx, shape = shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(
                color = backgroundColor,
                shape = shape
            )
            .clip(shape)
    ) {
        Providers(
            AmbientContentColor provides contentColor,
            AmbientAbsoluteElevation provides absoluteElevation,
            content = content
        )
    }
}

/**
 * primarySurface represents the background color of components that are [Colors.primary]
 * in light theme, and [Colors.surface] in dark theme, such as [androidx.compose.material.TabRow]
 * and [androidx.compose.material.TopAppBar]. This is to reduce brightness of large surfaces in dark
 * theme, aiding contrast and readability. See
 * [Dark Theme](https://material.io/design/color/dark-theme.html#custom-application).
 *
 * @return [Colors.primary] if in light theme, else [Colors.surface]
 */
val Colors.primarySurface: Color get() = if (isLight) primary else surface

/**
 * A simple layout which just reserves a space for a [Surface].
 * It positions the only child in the left top corner.
 */
// TODO("Andrey: Should be replaced with some basic layout implementation when we have it")
@Composable
private fun SurfaceLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        if (measurables.size > 1) {
            throw IllegalStateException("Surface can have only one direct measurable child!")
        }
        val measurable = measurables.firstOrNull()
        if (measurable == null) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }
}
