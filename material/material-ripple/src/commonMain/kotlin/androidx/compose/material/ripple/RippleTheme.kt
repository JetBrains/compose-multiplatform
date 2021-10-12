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

package androidx.compose.material.ripple

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.material.ripple.RippleTheme.Companion.defaultRippleAlpha
import androidx.compose.material.ripple.RippleTheme.Companion.defaultRippleColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Defines the appearance for Ripples. You can define a new theme and apply it using
 * [LocalRippleTheme]. See [defaultRippleColor] and [defaultRippleAlpha] for default values
 * that can be used when creating your own [RippleTheme].
 *
 * @see rememberRipple
 */
public interface RippleTheme {
    /**
     * @return the default ripple color at the call site's position in the hierarchy.
     * This color will be used when a color is not explicitly set in the ripple itself.
     * @see defaultRippleColor
     */
    @Composable
    public fun defaultColor(): Color

    /**
     * @return the [RippleAlpha] used to calculate the alpha for the ripple depending on the
     * [Interaction] for a given component. This will be set as the alpha channel for
     * [defaultColor] or the color explicitly provided to the ripple.
     * @see defaultRippleAlpha
     */
    @Composable
    public fun rippleAlpha(): RippleAlpha

    public companion object {
        /**
         * Represents the default color that will be used for a ripple if a color has not been
         * explicitly set on the ripple instance.
         *
         * @param contentColor the color of content (text or iconography) in the component that
         * contains the ripple.
         * @param lightTheme whether the theme is light or not
         */
        public fun defaultRippleColor(
            contentColor: Color,
            lightTheme: Boolean
        ): Color {
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

        /**
         * Represents the default [RippleAlpha] that will be used for a ripple to indicate different
         * states.
         *
         * @param contentColor the color of content (text or iconography) in the component that
         * contains the ripple.
         * @param lightTheme whether the theme is light or not
         */
        public fun defaultRippleAlpha(contentColor: Color, lightTheme: Boolean): RippleAlpha {
            return when {
                lightTheme -> {
                    if (contentColor.luminance() > 0.5) {
                        LightThemeHighContrastRippleAlpha
                    } else {
                        LightThemeLowContrastRippleAlpha
                    }
                }
                else -> {
                    DarkThemeRippleAlpha
                }
            }
        }
    }
}

/**
 * RippleAlpha defines the alpha of the ripple / state layer for different [Interaction]s.
 *
 * @property draggedAlpha the alpha used when the ripple is dragged
 * @property focusedAlpha the alpha used when the ripple is focused
 * @property hoveredAlpha the alpha used when the ripple is hovered
 * @property pressedAlpha the alpha used when the ripple is pressed
 */
@Immutable
public class RippleAlpha(
    public val draggedAlpha: Float,
    public val focusedAlpha: Float,
    public val hoveredAlpha: Float,
    public val pressedAlpha: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RippleAlpha) return false

        if (draggedAlpha != other.draggedAlpha) return false
        if (focusedAlpha != other.focusedAlpha) return false
        if (hoveredAlpha != other.hoveredAlpha) return false
        if (pressedAlpha != other.pressedAlpha) return false

        return true
    }

    override fun hashCode(): Int {
        var result = draggedAlpha.hashCode()
        result = 31 * result + focusedAlpha.hashCode()
        result = 31 * result + hoveredAlpha.hashCode()
        result = 31 * result + pressedAlpha.hashCode()
        return result
    }

    override fun toString(): String {
        return "RippleAlpha(draggedAlpha=$draggedAlpha, focusedAlpha=$focusedAlpha, " +
            "hoveredAlpha=$hoveredAlpha, pressedAlpha=$pressedAlpha)"
    }
}

/**
 * CompositionLocal used for providing [RippleTheme] down the tree.
 *
 * See [RippleTheme.defaultRippleColor] and [RippleTheme.defaultRippleAlpha] functions for the
 * default implementations for color and alpha.
 */
public val LocalRippleTheme: ProvidableCompositionLocal<RippleTheme> =
    staticCompositionLocalOf { DebugRippleTheme }

/**
 * Alpha values for high luminance content in a light theme.
 *
 * This content will typically be placed on colored surfaces, so it is important that the
 * contrast here is higher to meet accessibility standards, and increase legibility.
 *
 * These levels are typically used for text / iconography in primary colored tabs /
 * bottom navigation / etc.
 */
private val LightThemeHighContrastRippleAlpha = RippleAlpha(
    pressedAlpha = 0.24f,
    focusedAlpha = 0.24f,
    draggedAlpha = 0.16f,
    hoveredAlpha = 0.08f
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
private val LightThemeLowContrastRippleAlpha = RippleAlpha(
    pressedAlpha = 0.12f,
    focusedAlpha = 0.12f,
    draggedAlpha = 0.08f,
    hoveredAlpha = 0.04f
)

/**
 * Alpha levels for all content in a dark theme.
 */
private val DarkThemeRippleAlpha = RippleAlpha(
    pressedAlpha = 0.10f,
    focusedAlpha = 0.12f,
    draggedAlpha = 0.08f,
    hoveredAlpha = 0.04f
)

/**
 * Simple debug indication that will assume black content color and light theme. You should
 * instead provide your own theme with meaningful values - this exists as an alternative to
 * crashing if no theme is provided.
 */
@Immutable
private object DebugRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = defaultRippleColor(Color.Black, lightTheme = true)

    @Composable
    override fun rippleAlpha(): RippleAlpha = defaultRippleAlpha(
        Color.Black,
        lightTheme = true
    )
}
