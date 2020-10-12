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

import androidx.compose.runtime.Ambient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableContract
import androidx.compose.runtime.ProvidableAmbient
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln

/**
 * [Ambient] containing the [ElevationOverlay] used by [Surface] components. Provide `null` to
 * turn off [ElevationOverlay]s for the children within this [Ambient].
 *
 * @see ElevationOverlay
 */
val AmbientElevationOverlay: ProvidableAmbient<ElevationOverlay?> = staticAmbientOf {
    DefaultElevationOverlay
}

// TODO: make this a fun interface
/**
 * An ElevationOverlay is an overlay applied to the background color of [Surface] components,
 * used to emphasize elevation in dark theme, where shadows are not as visible. An
 * ElevationOverlay does not replace the shadows drawn by a [Surface], but is used as an
 * additional representation of elevation.
 *
 * The default ElevationOverlay only applies in dark theme (![Colors.isLight]), in accordance with
 * the Material specification for
 * [Dark Theme](https://material.io/design/color/dark-theme.html#properties).
 *
 * See [AmbientElevationOverlay] to provide your own [ElevationOverlay]. You can provide `null`
 * to have no ElevationOverlay applied.
 */
interface ElevationOverlay {
    /**
     * Returns the new background [Color] to use, representing the original background [color]
     * with an overlay corresponding to [elevation] applied. Typically this should only be
     * applied to [Colors.surface].
     */
    @Composable
    fun apply(color: Color, elevation: Dp): Color
}

/**
 * The default [ElevationOverlay] implementation.
 */
private object DefaultElevationOverlay : ElevationOverlay {
    @ComposableContract(readonly = true)
    @Composable
    override fun apply(color: Color, elevation: Dp): Color {
        val colors = MaterialTheme.colors
        return if (elevation > 0.dp && !colors.isLight) {
            val foregroundColor = calculateForegroundColor(color, elevation)
            foregroundColor.compositeOver(color)
        } else {
            color
        }
    }
}

/**
 * @return the alpha-modified foreground color to overlay on top of the surface color to produce
 * the resultant color. This color is the [contentColorFor] the [backgroundColor], with alpha
 * applied depending on the value of [elevation].
 */
@ComposableContract(readonly = true)
@Composable
private fun calculateForegroundColor(backgroundColor: Color, elevation: Dp): Color {
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    val baseForegroundColor = contentColorFor(backgroundColor)
    return baseForegroundColor.copy(alpha = alpha)
}
