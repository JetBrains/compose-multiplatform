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
@file:OptIn(ExperimentalMaterialApi::class)

package androidx.compose.material.ripple

import androidx.compose.foundation.Interaction
import androidx.compose.material.AmbientContentColor
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Defines the appearance and the behavior for [RippleIndication]s.
 *
 * You can define a new theme and apply it via [AmbientRippleTheme].
 */
@ExperimentalMaterialApi
interface RippleTheme {
    /**
     * @return the default [RippleIndication] color at the call site's position in the hierarchy.
     * This color will be used when a color is not explicitly set in the [RippleIndication] itself.
     */
    @Composable
    fun defaultColor(): Color

    /**
     * @return the [RippleOpacity] used to calculate the opacity for the ripple depending on the
     * [Interaction] for a given component. This will be set as the alpha channel for
     * [defaultColor] or the color explicitly provided to the [RippleIndication].
     */
    @Composable
    fun rippleOpacity(): RippleOpacity
}

// TODO: can be a fun interface when we rebase to use Kotlin 1.4
/**
 * RippleOpacity defines the opacity of the ripple / state layer for a given [Interaction].
 */
@ExperimentalMaterialApi
interface RippleOpacity {
    /**
     * @return the opacity of the ripple for the given [interaction]. Return `0f` if this
     * particular interaction should not show a corresponding ripple / state layer.
     */
    fun opacityForInteraction(interaction: Interaction): Float
}

/**
 * Ambient used for providing [RippleTheme] down the tree.
 */
@ExperimentalMaterialApi
val AmbientRippleTheme = staticAmbientOf<RippleTheme> { DefaultRippleTheme }

private object DefaultRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor(): Color {
        val contentColor = AmbientContentColor.current
        val lightTheme = MaterialTheme.colors.isLight
        val contentLuminance = contentColor.luminance()
        // If we are on a colored surface (typically indicated by low luminance content), the
        // ripple color should be white.
        return if (!lightTheme && contentLuminance < 0.5) {
            Color.White
            // Otherwise use contentColor
        } else {
            contentColor
        }
    }

    @Composable
    override fun rippleOpacity(): RippleOpacity {
        val lightTheme = MaterialTheme.colors.isLight
        val contentLuminance = AmbientContentColor.current.luminance()
        return when {
            lightTheme -> {
                if (contentLuminance > 0.5) {
                    LightThemeHighContrastRippleOpacity
                } else {
                    LightThemeReducedContrastRippleOpacity
                }
            }
            else -> {
                DarkThemeRippleOpacity
            }
        }
    }
}

@Suppress("unused")
private sealed class DefaultRippleOpacity(
    val pressed: Float,
    val focused: Float,
    val dragged: Float,
    val hovered: Float
) : RippleOpacity {
    override fun opacityForInteraction(interaction: Interaction): Float = when (interaction) {
        Interaction.Pressed -> pressed
        Interaction.Dragged -> dragged
        else -> 0f
    }
}

/**
 * Opacity values for high luminance content in a light theme.
 *
 * This content will typically be placed on colored surfaces, so it is important that the
 * contrast here is higher to meet accessibility standards, and increase legibility.
 *
 * These levels are typically used for text / iconography in primary colored tabs /
 * bottom navigation / etc.
 */
private object LightThemeHighContrastRippleOpacity : DefaultRippleOpacity(
    pressed = 0.24f,
    focused = 0.24f,
    dragged = 0.16f,
    hovered = 0.08f
)

/**
 * Alpha levels for low luminance content in a light theme.
 *
 * This content will typically be placed on grayscale surfaces, so the contrast here can be lower
 * without sacrificing accessibility and legibility.
 *
 * These levels are typically used for body text on the main surface (white in light theme, grey
 * in dark theme) and text / iconography in surface colored tabs / bottom navigation / etc.
 */
private object LightThemeReducedContrastRippleOpacity : DefaultRippleOpacity(
    pressed = 0.12f,
    focused = 0.12f,
    dragged = 0.08f,
    hovered = 0.04f
)

/**
 * Alpha levels for all content in a dark theme.
 */
private object DarkThemeRippleOpacity : DefaultRippleOpacity(
    pressed = 0.10f,
    focused = 0.12f,
    dragged = 0.08f,
    hovered = 0.04f
)
