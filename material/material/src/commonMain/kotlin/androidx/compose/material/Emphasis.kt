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

package androidx.compose.material

import androidx.compose.runtime.Ambient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.util.annotation.FloatRange

/**
 * Emphasis allows certain parts of a component to be accentuated, or shown with lower contrast
 * to reflect importance / state inside a component. For example, inside a disabled button, text
 * should have an emphasis level of [EmphasisLevels.disabled], to show that the button is
 * currently not active / able to be interacted with.
 *
 * Emphasis works by adjusting the color provided by [AmbientContentColor], so that emphasis levels
 * cascade through a subtree without requiring components to be aware of their context.
 *
 * The default implementations convey emphasis by changing the alpha / opacity of [color], to
 * increase / reduce contrast for a particular element.
 *
 * To set emphasis for a particular subtree, see [ProvideEmphasis].
 *
 * To define the emphasis levels in your application, see [EmphasisLevels] and [AmbientEmphasisLevels]
 * - note that this should not typically be customized, as the default values are optimized for
 * accessibility and contrast on different surfaces.
 *
 * For more information on emphasis and ensuring legibility for content, see
 * [Text legibility](https://material.io/design/color/text-legibility.html)
 */
@Deprecated(
    message = "Emphasis has been simplified and replaced with ContentAlpha"
)
@Immutable
interface Emphasis {
    /**
     * Applies emphasis to the given [color].
     */
    fun applyEmphasis(color: Color): Color
}

/**
 * EmphasisLevels represents the different levels of [Emphasis] that can be applied to a component.
 *
 * By default, the [Emphasis] implementation for each level varies depending on the color being
 * emphasized (typically [AmbientContentColor]). This ensures that the [Emphasis] has the correct
 * contrast for the background they are on, as [Colors.primary] surfaces typically require
 * higher contrast for the content color than [Colors.surface] surfaces to ensure they are
 * accessible.
 *
 * This typically should not be customized as the default implementation is optimized for
 * correct accessibility and contrast on different surfaces.
 *
 * See [AmbientEmphasisLevels] to retrieve the current [EmphasisLevels]
 */
@Deprecated(
    message = "Emphasis has been simplified and replaced with ContentAlpha"
)
interface EmphasisLevels {
    /**
     * Emphasis used to express high emphasis, such as for selected text fields.
     */
    @Composable
    val high: Emphasis
    /**
     * Emphasis used to express medium emphasis, such as for placeholder text in a text field.
     */
    @Composable
    val medium: Emphasis
    /**
     * Emphasis used to express disabled state, such as for a disabled button.
     */
    @Composable
    val disabled: Emphasis
}

/**
 * Applies [emphasis] to [content], by modifying the value of [AmbientContentColor].
 *
 * See [AmbientEmphasisLevels] to retrieve the levels of emphasis provided in the theme,
 * so they can be applied with this function.
 */
@Deprecated(
    message = "Emphasis has been simplified and replaced with ContentAlpha",
    replaceWith = ReplaceWith(
        "Providers(AmbientContentAlpha provides ContentAlpha.high, children = content)",
        "androidx.compose.runtime.Providers",
        "androidx.compose.material.ContentAlpha"
    )
)
@Composable
fun ProvideEmphasis(emphasis: Emphasis, content: @Composable () -> Unit) {
    val emphasizedColor = emphasis.applyEmphasis(AmbientContentColor.current)
    Providers(AmbientContentColor provides emphasizedColor, content = content)
}

@Deprecated(
    message = "Emphasis has been simplified and replaced with ContentAlpha"
)
/**
 * Ambient containing the current [EmphasisLevels] in this hierarchy.
 */
val AmbientEmphasisLevels: Ambient<EmphasisLevels> = staticAmbientOf { DefaultEmphasisLevels }

private object DefaultEmphasisLevels : EmphasisLevels {

    /**
     * This default implementation uses separate alpha levels depending on the luminance of the
     * incoming color, and whether the theme is light or dark. This is to ensure correct contrast
     * and accessibility on all surfaces.
     *
     * See [HighContrastAlphaLevels] and [ReducedContrastAlphaLevels] for what the levels are
     * used for, and under what circumstances.
     */
    private class AlphaEmphasis(
        private val lightTheme: Boolean,
        @FloatRange(from = 0.0, to = 1.0) private val highContrastAlpha: Float,
        @FloatRange(from = 0.0, to = 1.0) private val reducedContrastAlpha: Float
    ) : Emphasis {
        override fun applyEmphasis(color: Color): Color {
            if (color.alpha != 1f) return color
            val alpha = if (lightTheme) {
                if (color.luminance() > 0.5) highContrastAlpha else reducedContrastAlpha
            } else {
                if (color.luminance() < 0.5) highContrastAlpha else reducedContrastAlpha
            }
            return color.copy(alpha = alpha)
        }
    }

    @Composable
    override val high: Emphasis
        get() = AlphaEmphasis(
            lightTheme = MaterialTheme.colors.isLight,
            highContrastAlpha = HighContrastAlphaLevels.high,
            reducedContrastAlpha = ReducedContrastAlphaLevels.high
        )

    @Composable
    override val medium: Emphasis
        get() = AlphaEmphasis(
            lightTheme = MaterialTheme.colors.isLight,
            highContrastAlpha = HighContrastAlphaLevels.medium,
            reducedContrastAlpha = ReducedContrastAlphaLevels.medium
        )

    @Composable
    override val disabled: Emphasis
        get() = AlphaEmphasis(
            lightTheme = MaterialTheme.colors.isLight,
            highContrastAlpha = HighContrastAlphaLevels.disabled,
            reducedContrastAlpha = ReducedContrastAlphaLevels.disabled
        )
}

/**
 * Alpha levels for high luminance content in light theme, or low luminance content in dark theme.
 *
 * This content will typically be placed on colored surfaces, so it is important that the
 * contrast here is higher to meet accessibility standards, and increase legibility.
 *
 * These levels are typically used for text / iconography in primary colored tabs /
 * bottom navigation / etc.
 */
private object HighContrastAlphaLevels {
    const val high: Float = 1.00f
    const val medium: Float = 0.74f
    const val disabled: Float = 0.38f
}

/**
 * Alpha levels for low luminance content in light theme, or high luminance content in dark theme.
 *
 * This content will typically be placed on grayscale surfaces, so the contrast here can be lower
 * without sacrificing accessibility and legibility.
 *
 * These levels are typically used for body text on the main surface (white in light theme, grey
 * in dark theme) and text / iconography in surface colored tabs / bottom navigation / etc.
 */
private object ReducedContrastAlphaLevels {
    const val high: Float = 0.87f
    const val medium: Float = 0.60f
    const val disabled: Float = 0.38f
}
