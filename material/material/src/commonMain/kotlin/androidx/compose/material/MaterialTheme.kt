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

import androidx.compose.foundation.AmbientIndication
import androidx.compose.foundation.Indication
import androidx.compose.material.ripple.AmbientRippleTheme
import androidx.compose.material.ripple.ExperimentalRippleApi
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposableContract
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.remember
import androidx.compose.ui.selection.AmbientTextSelectionColors

/**
 * A MaterialTheme defines the styling principles from the Material design specification.
 *
 * Material components such as [Button] and [Checkbox] use values provided here when retrieving
 * default values.
 *
 * It defines colors as specified in the [Material Color theme creation spec](https://material.io/design/color/the-color-system.html#color-theme-creation),
 * typography defined in the [Material Type Scale spec](https://material.io/design/typography/the-type-system.html#type-scale),
 * and shapes defined in the [Shape scheme](https://material.io/design/shape/applying-shape-to-ui.html#shape-scheme).
 *
 * All values may be set by providing this component with the [colors][Colors],
 * [typography][Typography], and [shapes][Shapes] attributes. Use this to configure the overall
 * theme of elements within this MaterialTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent MaterialTheme. This allows using a MaterialTheme at the top
 * of your application, and then separate MaterialTheme(s) for different screens / parts of your
 * UI, overriding only the parts of the theme definition that need to change.
 *
 * @sample androidx.compose.material.samples.MaterialThemeSample
 *
 * @param colors A complete definition of the Material Color theme for this hierarchy
 * @param typography A set of text styles to be used as this hierarchy's typography system
 * @param shapes A set of shapes to be used by the components in this hierarchy
 */
@OptIn(ExperimentalRippleApi::class)
@Composable
fun MaterialTheme(
    colors: Colors = MaterialTheme.colors,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = MaterialTheme.shapes,
    content: @Composable () -> Unit
) {
    val rememberedColors = remember {
        // TODO: b/162450508 remove the unnecessary .copy() here when it isn't needed to ensure that
        // we don't skip the updateColorsFrom call
        colors.copy()
    }.apply { updateColorsFrom(colors) }
    val indicationFactory: @Composable () -> Indication = remember {
        @Composable { rememberRipple() }
    }
    val selectionColors = rememberTextSelectionColors(rememberedColors)
    Providers(
        AmbientColors provides rememberedColors,
        AmbientContentAlpha provides ContentAlpha.high,
        AmbientIndication provides indicationFactory,
        AmbientRippleTheme provides MaterialRippleTheme,
        AmbientShapes provides shapes,
        AmbientTextSelectionColors provides selectionColors,
        AmbientTypography provides typography
    ) {
        ProvideTextStyle(value = typography.body1, content = content)
    }
}

/**
 * Contains functions to access the current theme values provided at the call site's position in
 * the hierarchy.
 */
object MaterialTheme {
    /**
     * Retrieves the current [Colors] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material.samples.ThemeColorSample
     */
    val colors: Colors
        @Composable
        @ComposableContract(readonly = true)
        get() = AmbientColors.current

    /**
     * Retrieves the current [Typography] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material.samples.ThemeTextStyleSample
     */
    val typography: Typography
        @Composable
        @ComposableContract(readonly = true)
        get() = AmbientTypography.current

    /**
     * Retrieves the current [Shapes] at the call site's position in the hierarchy.
     */
    val shapes: Shapes
        @Composable
        @ComposableContract(readonly = true)
        get() = AmbientShapes.current
}

@OptIn(ExperimentalRippleApi::class)
@Immutable
private object MaterialRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = RippleTheme.defaultRippleColor(
        contentColor = AmbientContentColor.current,
        lightTheme = MaterialTheme.colors.isLight
    )

    @Composable
    override fun rippleAlpha() = RippleTheme.defaultRippleAlpha(
        contentColor = AmbientContentColor.current,
        lightTheme = MaterialTheme.colors.isLight
    )
}
