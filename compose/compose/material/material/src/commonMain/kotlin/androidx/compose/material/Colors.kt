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

package androidx.compose.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

/**
 * <a href="https://material.io/design/color/the-color-system.html" class="external" target="_blank">Material Design color system</a>.
 *
 * The Material Design color system can help you create a color theme that reflects your brand or
 * style.
 *
 * ![Color image](https://developer.android.com/images/reference/androidx/compose/material/color.png)
 *
 * To create a light set of colors using the baseline values, use [lightColors]
 * To create a dark set of colors using the baseline values, use [darkColors]
 *
 * @property primary The primary color is the color displayed most frequently across your app’s
 * screens and components.
 * @property primaryVariant The primary variant color is used to distinguish two elements of the
 * app using the primary color, such as the top app bar and the system bar.
 * @property secondary The secondary color provides more ways to accent and distinguish your
 * product. Secondary colors are best for:
 * - Floating action buttons
 * - Selection controls, like checkboxes and radio buttons
 * - Highlighting selected text
 * - Links and headlines
 * @property secondaryVariant The secondary variant color is used to distinguish two elements of the
 * app using the secondary color.
 * @property background The background color appears behind scrollable content.
 * @property surface The surface color is used on surfaces of components, such as cards, sheets and
 * menus.
 * @property error The error color is used to indicate error within components, such as text fields.
 * @property onPrimary Color used for text and icons displayed on top of the primary color.
 * @property onSecondary Color used for text and icons displayed on top of the secondary color.
 * @property onBackground Color used for text and icons displayed on top of the background color.
 * @property onSurface Color used for text and icons displayed on top of the surface color.
 * @property onError Color used for text and icons displayed on top of the error color.
 * @property isLight Whether this Colors is considered as a 'light' or 'dark' set of colors. This
 * affects default behavior for some components: for example, in a light theme a [TopAppBar] will
 * use [primary] by default for its background color, when in a dark theme it will use [surface].
 */
@Stable
class Colors(
    primary: Color,
    primaryVariant: Color,
    secondary: Color,
    secondaryVariant: Color,
    background: Color,
    surface: Color,
    error: Color,
    onPrimary: Color,
    onSecondary: Color,
    onBackground: Color,
    onSurface: Color,
    onError: Color,
    isLight: Boolean
) {
    var primary by mutableStateOf(primary, structuralEqualityPolicy())
        internal set
    var primaryVariant by mutableStateOf(primaryVariant, structuralEqualityPolicy())
        internal set
    var secondary by mutableStateOf(secondary, structuralEqualityPolicy())
        internal set
    var secondaryVariant by mutableStateOf(secondaryVariant, structuralEqualityPolicy())
        internal set
    var background by mutableStateOf(background, structuralEqualityPolicy())
        internal set
    var surface by mutableStateOf(surface, structuralEqualityPolicy())
        internal set
    var error by mutableStateOf(error, structuralEqualityPolicy())
        internal set
    var onPrimary by mutableStateOf(onPrimary, structuralEqualityPolicy())
        internal set
    var onSecondary by mutableStateOf(onSecondary, structuralEqualityPolicy())
        internal set
    var onBackground by mutableStateOf(onBackground, structuralEqualityPolicy())
        internal set
    var onSurface by mutableStateOf(onSurface, structuralEqualityPolicy())
        internal set
    var onError by mutableStateOf(onError, structuralEqualityPolicy())
        internal set
    var isLight by mutableStateOf(isLight, structuralEqualityPolicy())
        internal set

    /**
     * Returns a copy of this Colors, optionally overriding some of the values.
     */
    fun copy(
        primary: Color = this.primary,
        primaryVariant: Color = this.primaryVariant,
        secondary: Color = this.secondary,
        secondaryVariant: Color = this.secondaryVariant,
        background: Color = this.background,
        surface: Color = this.surface,
        error: Color = this.error,
        onPrimary: Color = this.onPrimary,
        onSecondary: Color = this.onSecondary,
        onBackground: Color = this.onBackground,
        onSurface: Color = this.onSurface,
        onError: Color = this.onError,
        isLight: Boolean = this.isLight
    ): Colors = Colors(
        primary,
        primaryVariant,
        secondary,
        secondaryVariant,
        background,
        surface,
        error,
        onPrimary,
        onSecondary,
        onBackground,
        onSurface,
        onError,
        isLight
    )

    override fun toString(): String {
        return "Colors(" +
            "primary=$primary, " +
            "primaryVariant=$primaryVariant, " +
            "secondary=$secondary, " +
            "secondaryVariant=$secondaryVariant, " +
            "background=$background, " +
            "surface=$surface, " +
            "error=$error, " +
            "onPrimary=$onPrimary, " +
            "onSecondary=$onSecondary, " +
            "onBackground=$onBackground, " +
            "onSurface=$onSurface, " +
            "onError=$onError, " +
            "isLight=$isLight" +
            ")"
    }
}

/**
 * Creates a complete color definition for the
 * [Material color specification](https://material.io/design/color/the-color-system.html#color-theme-creation)
 * using the default light theme values.
 *
 * @see darkColors
 */
fun lightColors(
    primary: Color = Color(0xFF6200EE),
    primaryVariant: Color = Color(0xFF3700B3),
    secondary: Color = Color(0xFF03DAC6),
    secondaryVariant: Color = Color(0xFF018786),
    background: Color = Color.White,
    surface: Color = Color.White,
    error: Color = Color(0xFFB00020),
    onPrimary: Color = Color.White,
    onSecondary: Color = Color.Black,
    onBackground: Color = Color.Black,
    onSurface: Color = Color.Black,
    onError: Color = Color.White
): Colors = Colors(
    primary,
    primaryVariant,
    secondary,
    secondaryVariant,
    background,
    surface,
    error,
    onPrimary,
    onSecondary,
    onBackground,
    onSurface,
    onError,
    true
)

/**
 * Creates a complete color definition for the
 * [Material color specification](https://material.io/design/color/the-color-system.html#color-theme-creation)
 * using the default dark theme values.
 *
 * Note: [secondaryVariant] is typically the same as [secondary] in dark theme since contrast
 * levels are higher, and hence there is less need for a separate secondary color.
 *
 * @see lightColors
 */
fun darkColors(
    primary: Color = Color(0xFFBB86FC),
    primaryVariant: Color = Color(0xFF3700B3),
    secondary: Color = Color(0xFF03DAC6),
    secondaryVariant: Color = secondary,
    background: Color = Color(0xFF121212),
    surface: Color = Color(0xFF121212),
    error: Color = Color(0xFFCF6679),
    onPrimary: Color = Color.Black,
    onSecondary: Color = Color.Black,
    onBackground: Color = Color.White,
    onSurface: Color = Color.White,
    onError: Color = Color.Black
): Colors = Colors(
    primary,
    primaryVariant,
    secondary,
    secondaryVariant,
    background,
    surface,
    error,
    onPrimary,
    onSecondary,
    onBackground,
    onSurface,
    onError,
    false
)

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
 * The Material color system contains pairs of colors that are typically used for the background
 * and content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [Colors], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [Colors.primary], this will return [Colors.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return
 * [Color.Unspecified].
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 * the theme's [Colors], then returns [Color.Unspecified].
 *
 * @see contentColorFor
 */
fun Colors.contentColorFor(backgroundColor: Color): Color {
    return when (backgroundColor) {
        primary -> onPrimary
        primaryVariant -> onPrimary
        secondary -> onSecondary
        secondaryVariant -> onSecondary
        background -> onBackground
        surface -> onSurface
        error -> onError
        else -> Color.Unspecified
    }
}

/**
 * The Material color system contains pairs of colors that are typically used for the background
 * and content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [Colors], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [Colors.primary], this will return [Colors.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return
 * the current value of [LocalContentColor] as a best-effort color.
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 * the theme's [Colors], then returns the current value of [LocalContentColor].
 *
 * @see Colors.contentColorFor
 */
@Composable
@ReadOnlyComposable
fun contentColorFor(backgroundColor: Color) =
    MaterialTheme.colors.contentColorFor(backgroundColor).takeOrElse { LocalContentColor.current }

/**
 * Updates the internal values of the given [Colors] with values from the [other] [Colors]. This
 * allows efficiently updating a subset of [Colors], without recomposing every composable that
 * consumes values from [LocalColors].
 *
 * Because [Colors] is very wide-reaching, and used by many expensive composables in the
 * hierarchy, providing a new value to [LocalColors] causes every composable consuming
 * [LocalColors] to recompose, which is prohibitively expensive in cases such as animating one
 * color in the theme. Instead, [Colors] is internally backed by [mutableStateOf], and this
 * function mutates the internal state of [this] to match values in [other]. This means that any
 * changes will mutate the internal state of [this], and only cause composables that are reading
 * the specific changed value to recompose.
 */
internal fun Colors.updateColorsFrom(other: Colors) {
    primary = other.primary
    primaryVariant = other.primaryVariant
    secondary = other.secondary
    secondaryVariant = other.secondaryVariant
    background = other.background
    surface = other.surface
    error = other.error
    onPrimary = other.onPrimary
    onSecondary = other.onSecondary
    onBackground = other.onBackground
    onSurface = other.onSurface
    onError = other.onError
    isLight = other.isLight
}

/**
 * CompositionLocal used to pass [Colors] down the tree.
 *
 * Setting the value here is typically done as part of [MaterialTheme], which will
 * automatically handle efficiently updating any changed colors without causing unnecessary
 * recompositions, using [Colors.updateColorsFrom].
 * To retrieve the current value of this CompositionLocal, use [MaterialTheme.colors].
 */
internal val LocalColors = staticCompositionLocalOf { lightColors() }
