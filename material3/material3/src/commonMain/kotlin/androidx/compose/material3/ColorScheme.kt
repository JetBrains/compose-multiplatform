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

package androidx.compose.material3

import androidx.compose.material3.tokens.ColorDarkTokens
import androidx.compose.material3.tokens.ColorLightTokens
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln

/**
 * A color scheme holds all the named color parameters for a [MaterialTheme].
 *
 * Color schemes are designed to be harmonious, ensure accessible text, and distinguish UI
 * elements and surfaces from one another. There are two built-in baseline schemes,
 * [lightColorScheme] and a [darkColorScheme], that can be used as-is or customized.
 *
 * The Material color system and custom schemes provide default values for color as a starting point
 * for customization.
 *
 * To learn more about colors, see [Material Design colors](https://m3.material.io/styles/color/overview).
 *
 * @property primary The primary color is the color displayed most frequently across your app’s
 * screens and components.
 * @property onPrimary Color used for text and icons displayed on top of the primary color.
 * @property primaryContainer The preferred tonal color of containers.
 * @property onPrimaryContainer The color (and state variants) that should be used for content on
 * top of [primaryContainer].
 * @property inversePrimary Color to be used as a "primary" color in places where the inverse color
 * scheme is needed, such as the button on a SnackBar.
 * @property secondary The secondary color provides more ways to accent and distinguish your
 * product. Secondary colors are best for:
 * - Floating action buttons
 * - Selection controls, like checkboxes and radio buttons
 * - Highlighting selected text
 * - Links and headlines
 * @property onSecondary Color used for text and icons displayed on top of the secondary color.
 * @property secondaryContainer A tonal color to be used in containers.
 * @property onSecondaryContainer The color (and state variants) that should be used for content on
 * top of [secondaryContainer].
 * @property tertiary The tertiary color that can be used to balance primary and secondary
 * colors, or bring heightened attention to an element such as an input field.
 * @property onTertiary Color used for text and icons displayed on top of the tertiary color.
 * @property tertiaryContainer A tonal color to be used in containers.
 * @property onTertiaryContainer The color (and state variants) that should be used for content on
 * top of [tertiaryContainer].
 * @property background The background color that appears behind scrollable content.
 * @property onBackground Color used for text and icons displayed on top of the background color.
 * @property surface The surface color that affect surfaces of components, such as cards, sheets,
 * and menus.
 * @property onSurface Color used for text and icons displayed on top of the surface color.
 * @property surfaceVariant Another option for a color with similar uses of [surface].
 * @property onSurfaceVariant The color (and state variants) that can be used for content on top of
 * [surface].
 * @property surfaceTint This color will be used by components that apply tonal elevation and is
 * applied on top of [surface]. The higher the elevation the more this color is used.
 * @property inverseSurface A color that contrasts sharply with [surface]. Useful for surfaces that
 * sit on top of other surfaces with [surface] color.
 * @property inverseOnSurface A color that contrasts well with [inverseSurface]. Useful for content
 * that sits on top of containers that are [inverseSurface].
 * @property error The error color is used to indicate errors in components, such as invalid text in
 * a text field.
 * @property onError Color used for text and icons displayed on top of the error color.
 * @property errorContainer The preferred tonal color of error containers.
 * @property onErrorContainer The color (and state variants) that should be used for content on
 * top of [errorContainer].
 * @property outline Subtle color used for boundaries. Outline color role adds contrast for
 * accessibility purposes.
 */
@Stable
class ColorScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    inversePrimary: Color,
    secondary: Color,
    onSecondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    onTertiary: Color,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
    background: Color,
    onBackground: Color,
    surface: Color,
    onSurface: Color,
    surfaceVariant: Color,
    onSurfaceVariant: Color,
    surfaceTint: Color,
    inverseSurface: Color,
    inverseOnSurface: Color,
    error: Color,
    onError: Color,
    errorContainer: Color,
    onErrorContainer: Color,
    outline: Color,
) {
    var primary by mutableStateOf(primary, structuralEqualityPolicy())
        internal set
    var onPrimary by mutableStateOf(onPrimary, structuralEqualityPolicy())
        internal set
    var primaryContainer by mutableStateOf(primaryContainer, structuralEqualityPolicy())
        internal set
    var onPrimaryContainer by mutableStateOf(onPrimaryContainer, structuralEqualityPolicy())
        internal set
    var inversePrimary by mutableStateOf(inversePrimary, structuralEqualityPolicy())
        internal set
    var secondary by mutableStateOf(secondary, structuralEqualityPolicy())
        internal set
    var onSecondary by mutableStateOf(onSecondary, structuralEqualityPolicy())
        internal set
    var secondaryContainer by mutableStateOf(secondaryContainer, structuralEqualityPolicy())
        internal set
    var onSecondaryContainer by mutableStateOf(onSecondaryContainer, structuralEqualityPolicy())
        internal set
    var tertiary by mutableStateOf(tertiary, structuralEqualityPolicy())
        internal set
    var onTertiary by mutableStateOf(onTertiary, structuralEqualityPolicy())
        internal set
    var tertiaryContainer by mutableStateOf(tertiaryContainer, structuralEqualityPolicy())
        internal set
    var onTertiaryContainer by mutableStateOf(onTertiaryContainer, structuralEqualityPolicy())
        internal set
    var background by mutableStateOf(background, structuralEqualityPolicy())
        internal set
    var onBackground by mutableStateOf(onBackground, structuralEqualityPolicy())
        internal set
    var surface by mutableStateOf(surface, structuralEqualityPolicy())
        internal set
    var onSurface by mutableStateOf(onSurface, structuralEqualityPolicy())
        internal set
    var surfaceVariant by mutableStateOf(surfaceVariant, structuralEqualityPolicy())
        internal set
    var onSurfaceVariant by mutableStateOf(onSurfaceVariant, structuralEqualityPolicy())
        internal set
    var surfaceTint by mutableStateOf(surfaceTint, structuralEqualityPolicy())
        internal set
    var inverseSurface by mutableStateOf(inverseSurface, structuralEqualityPolicy())
        internal set
    var inverseOnSurface by mutableStateOf(inverseOnSurface, structuralEqualityPolicy())
        internal set
    var error by mutableStateOf(error, structuralEqualityPolicy())
        internal set
    var onError by mutableStateOf(onError, structuralEqualityPolicy())
        internal set
    var errorContainer by mutableStateOf(errorContainer, structuralEqualityPolicy())
        internal set
    var onErrorContainer by mutableStateOf(onErrorContainer, structuralEqualityPolicy())
        internal set
    var outline by mutableStateOf(outline, structuralEqualityPolicy())
        internal set

    /** Returns a copy of this ColorScheme, optionally overriding some of the values. */
    fun copy(
        primary: Color = this.primary,
        onPrimary: Color = this.onPrimary,
        primaryContainer: Color = this.primaryContainer,
        onPrimaryContainer: Color = this.onPrimaryContainer,
        inversePrimary: Color = this.inversePrimary,
        secondary: Color = this.secondary,
        onSecondary: Color = this.onSecondary,
        secondaryContainer: Color = this.secondaryContainer,
        onSecondaryContainer: Color = this.onSecondaryContainer,
        tertiary: Color = this.tertiary,
        onTertiary: Color = this.onTertiary,
        tertiaryContainer: Color = this.tertiaryContainer,
        onTertiaryContainer: Color = this.onTertiaryContainer,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
        surface: Color = this.surface,
        onSurface: Color = this.onSurface,
        surfaceVariant: Color = this.surfaceVariant,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        surfaceTint: Color = this.surfaceTint,
        inverseSurface: Color = this.inverseSurface,
        inverseOnSurface: Color = this.inverseOnSurface,
        error: Color = this.error,
        onError: Color = this.onError,
        errorContainer: Color = this.errorContainer,
        onErrorContainer: Color = this.onErrorContainer,
        outline: Color = this.outline,
    ): ColorScheme =
        ColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
        )

    override fun toString(): String {
        return "ColorScheme(" +
            "primary=$primary" +
            "onPrimary=$onPrimary" +
            "primaryContainer=$primaryContainer" +
            "onPrimaryContainer=$onPrimaryContainer" +
            "inversePrimary=$inversePrimary" +
            "secondary=$secondary" +
            "onSecondary=$onSecondary" +
            "secondaryContainer=$secondaryContainer" +
            "onSecondaryContainer=$onSecondaryContainer" +
            "tertiary=$tertiary" +
            "onTertiary=$onTertiary" +
            "tertiaryContainer=$tertiaryContainer" +
            "onTertiaryContainer=$onTertiaryContainer" +
            "background=$background" +
            "onBackground=$onBackground" +
            "surface=$surface" +
            "onSurface=$onSurface" +
            "surfaceVariant=$surfaceVariant" +
            "onSurfaceVariant=$onSurfaceVariant" +
            "surfaceTint=$surfaceTint" +
            "inverseSurface=$inverseSurface" +
            "inverseOnSurface=$inverseOnSurface" +
            "error=$error" +
            "onError=$onError" +
            "errorContainer=$errorContainer" +
            "onErrorContainer=$onErrorContainer" +
            "outline=$outline" +
            ")"
    }
}

/**
 * Returns a light Material color scheme.
 */
fun lightColorScheme(
    primary: Color = ColorLightTokens.Primary,
    onPrimary: Color = ColorLightTokens.OnPrimary,
    primaryContainer: Color = ColorLightTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorLightTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorLightTokens.InversePrimary,
    secondary: Color = ColorLightTokens.Secondary,
    onSecondary: Color = ColorLightTokens.OnSecondary,
    secondaryContainer: Color = ColorLightTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorLightTokens.OnSecondaryContainer,
    tertiary: Color = ColorLightTokens.Tertiary,
    onTertiary: Color = ColorLightTokens.OnTertiary,
    tertiaryContainer: Color = ColorLightTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorLightTokens.OnTertiaryContainer,
    background: Color = ColorLightTokens.Background,
    onBackground: Color = ColorLightTokens.OnBackground,
    surface: Color = ColorLightTokens.Surface,
    onSurface: Color = ColorLightTokens.OnSurface,
    surfaceVariant: Color = ColorLightTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorLightTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorLightTokens.InverseSurface,
    inverseOnSurface: Color = ColorLightTokens.InverseOnSurface,
    error: Color = ColorLightTokens.Error,
    onError: Color = ColorLightTokens.OnError,
    errorContainer: Color = ColorLightTokens.ErrorContainer,
    onErrorContainer: Color = ColorLightTokens.OnErrorContainer,
    outline: Color = ColorLightTokens.Outline,
): ColorScheme =
    ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
    )

/**
 * Returns a dark Material color scheme.
 */
fun darkColorScheme(
    primary: Color = ColorDarkTokens.Primary,
    onPrimary: Color = ColorDarkTokens.OnPrimary,
    primaryContainer: Color = ColorDarkTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorDarkTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorDarkTokens.InversePrimary,
    secondary: Color = ColorDarkTokens.Secondary,
    onSecondary: Color = ColorDarkTokens.OnSecondary,
    secondaryContainer: Color = ColorDarkTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorDarkTokens.OnSecondaryContainer,
    tertiary: Color = ColorDarkTokens.Tertiary,
    onTertiary: Color = ColorDarkTokens.OnTertiary,
    tertiaryContainer: Color = ColorDarkTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorDarkTokens.OnTertiaryContainer,
    background: Color = ColorDarkTokens.Background,
    onBackground: Color = ColorDarkTokens.OnBackground,
    surface: Color = ColorDarkTokens.Surface,
    onSurface: Color = ColorDarkTokens.OnSurface,
    surfaceVariant: Color = ColorDarkTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorDarkTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorDarkTokens.InverseSurface,
    inverseOnSurface: Color = ColorDarkTokens.InverseOnSurface,
    error: Color = ColorDarkTokens.Error,
    onError: Color = ColorDarkTokens.OnError,
    errorContainer: Color = ColorDarkTokens.ErrorContainer,
    onErrorContainer: Color = ColorDarkTokens.OnErrorContainer,
    outline: Color = ColorDarkTokens.Outline,
): ColorScheme =
    ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
    )

/**
 * The Material color system contains pairs of colors that are typically used for the background and
 * content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [ColorScheme], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [ColorScheme.primary], this will return [ColorScheme.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return
 * [Color.Unspecified].
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 * the theme's [ColorScheme], then returns [Color.Unspecified].
 *
 * @see contentColorFor
 */
fun ColorScheme.contentColorFor(backgroundColor: Color): Color =
    when (backgroundColor) {
        primary -> onPrimary
        secondary -> onSecondary
        tertiary -> onTertiary
        background -> onBackground
        error -> onError
        surface -> onSurface
        surfaceVariant -> onSurfaceVariant
        primaryContainer -> onPrimaryContainer
        secondaryContainer -> onSecondaryContainer
        tertiaryContainer -> onTertiaryContainer
        errorContainer -> onErrorContainer
        inverseSurface -> inverseOnSurface
        else -> Color.Unspecified
    }

/**
 * The Material color system contains pairs of colors that are typically used for the background and
 * content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [ColorScheme], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [ColorScheme.primary], this will return [ColorScheme.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return the current
 * value of [LocalContentColor] as a best-effort color.
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 * the theme's [ColorScheme], then returns the current value of [LocalContentColor].
 *
 * @see ColorScheme.contentColorFor
 */
@Composable
@ReadOnlyComposable
fun contentColorFor(backgroundColor: Color) =
    MaterialTheme.colorScheme.contentColorFor(backgroundColor).takeOrElse {
        LocalContentColor.current
    }

/**
 * Returns the new background [Color] to use, representing the original background [color] with an
 * overlay corresponding to [elevation] applied. The overlay will only be applied to
 * [ColorScheme.surface].
 */
internal fun ColorScheme.applyTonalElevation(backgroundColor: Color, elevation: Dp): Color {
    return if (backgroundColor == surface) {
        surfaceColorAtElevation(elevation)
    } else {
        backgroundColor
    }
}

/**
 * Computes the surface tonal color at different elevation levels e.g. surface1 through surface5.
 *
 * @param elevation Elevation value used to compute alpha of the color overlay layer.
 *
 * @return the [ColorScheme.surface] color with an alpha of the [ColorScheme.surfaceTint] color
 * overlaid on top of it.

 */
fun ColorScheme.surfaceColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return surfaceTint.copy(alpha = alpha).compositeOver(surface)
}

/**
 * Updates the internal values of a given [ColorScheme] with values from the [other]
 * [ColorScheme].
 * This allows efficiently updating a subset of [ColorScheme], without recomposing every
 * composable that consumes values from [LocalColorScheme].
 *
 * Because [ColorScheme] is very wide-reaching, and used by many expensive composables in the
 * hierarchy, providing a new value to [LocalColorScheme] causes every composable consuming
 * [LocalColorScheme] to recompose, which is prohibitively expensive in cases such as animating one
 * color in the theme. Instead, [ColorScheme] is internally backed by [mutableStateOf], and this
 * function mutates the internal state of [this] to match values in [other]. This means that any
 * changes will mutate the internal state of [this], and only cause composables that are reading the
 * specific changed value to recompose.
 */
internal fun ColorScheme.updateColorSchemeFrom(other: ColorScheme) {
    primary = other.primary
    onPrimary = other.onPrimary
    primaryContainer = other.primaryContainer
    onPrimaryContainer = other.onPrimaryContainer
    inversePrimary = other.inversePrimary
    secondary = other.secondary
    onSecondary = other.onSecondary
    secondaryContainer = other.secondaryContainer
    onSecondaryContainer = other.onSecondaryContainer
    tertiary = other.tertiary
    onTertiary = other.onTertiary
    tertiaryContainer = other.tertiaryContainer
    onTertiaryContainer = other.onTertiaryContainer
    background = other.background
    onBackground = other.onBackground
    surface = other.surface
    onSurface = other.onSurface
    surfaceVariant = other.surfaceVariant
    onSurfaceVariant = other.onSurfaceVariant
    surfaceTint = other.surfaceTint
    inverseSurface = other.inverseSurface
    inverseOnSurface = other.inverseOnSurface
    error = other.error
    onError = other.onError
    errorContainer = other.errorContainer
    onErrorContainer = other.onErrorContainer
    outline = other.outline
}

/**
 * Helper function for component color tokens. Here is an example on how to use component color
 * tokens:
 * ``MaterialTheme.colorScheme.fromToken(ExtendedFabBranded.BrandedContainerColor)``
 */
internal fun ColorScheme.fromToken(value: ColorSchemeKeyTokens): Color {
    return when (value) {
        ColorSchemeKeyTokens.Background -> background
        ColorSchemeKeyTokens.Error -> error
        ColorSchemeKeyTokens.ErrorContainer -> errorContainer
        ColorSchemeKeyTokens.InverseOnSurface -> inverseOnSurface
        ColorSchemeKeyTokens.InversePrimary -> inversePrimary
        ColorSchemeKeyTokens.InverseSurface -> inverseSurface
        ColorSchemeKeyTokens.OnBackground -> onBackground
        ColorSchemeKeyTokens.OnError -> onError
        ColorSchemeKeyTokens.OnErrorContainer -> onErrorContainer
        ColorSchemeKeyTokens.OnPrimary -> onPrimary
        ColorSchemeKeyTokens.OnPrimaryContainer -> onPrimaryContainer
        ColorSchemeKeyTokens.OnSecondary -> onSecondary
        ColorSchemeKeyTokens.OnSecondaryContainer -> onSecondaryContainer
        ColorSchemeKeyTokens.OnSurface -> onSurface
        ColorSchemeKeyTokens.OnSurfaceVariant -> onSurfaceVariant
        ColorSchemeKeyTokens.SurfaceTint -> surfaceTint
        ColorSchemeKeyTokens.OnTertiary -> onTertiary
        ColorSchemeKeyTokens.OnTertiaryContainer -> onTertiaryContainer
        ColorSchemeKeyTokens.Outline -> outline
        ColorSchemeKeyTokens.Primary -> primary
        ColorSchemeKeyTokens.PrimaryContainer -> primaryContainer
        ColorSchemeKeyTokens.Secondary -> secondary
        ColorSchemeKeyTokens.SecondaryContainer -> secondaryContainer
        ColorSchemeKeyTokens.Surface -> surface
        ColorSchemeKeyTokens.SurfaceVariant -> surfaceVariant
        ColorSchemeKeyTokens.Tertiary -> tertiary
        ColorSchemeKeyTokens.TertiaryContainer -> tertiaryContainer
    }
}

/**
 * CompositionLocal used to pass [ColorScheme] down the tree.
 *
 * Setting the value here is typically done as part of [MaterialTheme], which will automatically
 * handle efficiently updating any changed colors without causing unnecessary recompositions, using
 * [ColorScheme.updateColorSchemeFrom]. To retrieve the current value of this CompositionLocal, use
 * [MaterialTheme.colorScheme].
 */
internal val LocalColorScheme = staticCompositionLocalOf { lightColorScheme() }

/**
 * A low level of alpha used to represent disabled components, such as text in a disabled Button.
 */
internal const val DisabledAlpha = 0.38f

/** Converts a color token key to the local color scheme provided by the theme */
@Composable
internal fun ColorSchemeKeyTokens.toColor(): Color {
    return MaterialTheme.colorScheme.fromToken(this)
}
