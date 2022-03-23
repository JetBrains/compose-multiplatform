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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.tokens.FilledIconButtonTokens
import androidx.compose.material3.tokens.FilledTonalIconButtonTokens
import androidx.compose.material3.tokens.IconButtonTokens
import androidx.compose.material3.tokens.OutlinedIconButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design icon button</a>.
 *
 * A "standard" icon button is a clickable icon, used to represent an action.
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * @sample androidx.compose.material3.samples.IconButtonSample
 *
 * @param onClick callback to be called when the icon button is clicked
 * @param modifier Modifier to be applied to the layout of the icon button
 * @param enabled whether or not this icon button will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this icon button. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this icon button in different states
 * @param content the content (icon) to be drawn inside the icon button. This is typically an
 * [Icon].
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Box(
        modifier =
        modifier
            .minimumTouchTargetSize()
            .size(IconButtonTokens.StateLayerSize)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = false,
                    radius = IconButtonTokens.StateLayerSize / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor =
            if (enabled) {
                IconButtonTokens.UnselectedIconColor.toColor()
            } else {
                IconButtonTokens.DisabledIconColor.toColor()
                    .copy(alpha = IconButtonTokens.DisabledIconOpacity)
            }
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design toggleable icon button</a>.
 *
 * A toggleable icon button, used to represent an action. This version of a "standard" icon
 * button is responsible for a toggling its checked state as well as everything else that a
 * clickable icon button does.
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * @sample androidx.compose.material3.samples.IconToggleButtonSample
 *
 * @param checked whether or not this icon button is toggled on or off
 * @param onCheckedChange callback to be invoked when the toggleable icon button is clicked
 * @param modifier Modifier to be applied to the layout of the icon button
 * @param enabled whether or not this icon button will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this icon button. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this icon button in different states
 * @param content the content (icon) to be drawn inside the icon button. This is typically an
 * [Icon].
 */
@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Box(
        modifier =
        modifier
            .minimumTouchTargetSize()
            .size(IconButtonTokens.StateLayerSize)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = rememberRipple(
                    bounded = false,
                    radius = IconButtonTokens.StateLayerSize / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = when {
            !enabled -> IconButtonTokens.DisabledIconColor.toColor()
                .copy(alpha = IconButtonTokens.DisabledIconOpacity)
            !checked -> IconButtonTokens.UnselectedIconColor.toColor()
            else -> IconButtonTokens.SelectedIconColor.toColor()
        }
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design filled icon button</a>.
 *
 * A "contained" filled icon button is a clickable icon, used to represent an action.
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * Use this "contained" icon button when the component requires more visual separation from the
 * background.
 *
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/filled-icon-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * Filled icon button sample:
 * @sample androidx.compose.material3.samples.FilledIconButtonSample
 *
 * @param onClick callback to be called when the icon button is clicked
 * @param modifier Modifier to be applied to the layout of the icon button
 * @param enabled whether or not this icon button will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this icon button. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this icon button in different states
 * @param shape defines the icon button's shape
 * @param colors an [IconButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.filledIconButtonColors].
 * @param content the content (icon) to be drawn inside the icon button. This is typically an
 * [Icon].
 */
@ExperimentalMaterial3Api
@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = FilledIconButtonTokens.ContainerShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    content: @Composable () -> Unit
) = Surface(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled).value,
    contentColor = colors.contentColor(enabled).value,
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(FilledIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design filled tonal icon button</a>.
 *
 * A "contained" filled tonal icon button is a clickable icon, used to represent an action.
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * Use this "contained" icon button when the component requires more visual separation from the
 * background.
 *
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/filled-tonal-icon-button.png)
 *
 * A filled tonal icon button is a medium-emphasis icon button that is an alternative middle
 * ground between the default [FilledIconButton] and [OutlinedIconButton].
 * They can be used in contexts where the lower-priority icon button requires slightly more emphasis
 * than an outline would give.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * Filled tonal icon button sample:
 * @sample androidx.compose.material3.samples.FilledTonalIconButtonSample
 *
 * @param onClick callback to be called when the icon button is clicked
 * @param modifier Modifier to be applied to the layout of the icon button
 * @param enabled whether or not this icon button will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this icon button. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this icon button in different states
 * @param shape defines the icon button's shape
 * @param colors an [IconButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.filledIconButtonColors].
 * @param content the content (icon) to be drawn inside the icon button. This is typically an
 * [Icon].
 */
@ExperimentalMaterial3Api
@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = FilledIconButtonTokens.ContainerShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    content: @Composable () -> Unit
) = Surface(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled).value,
    contentColor = colors.contentColor(enabled).value,
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(FilledTonalIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design toggleable filled icon button</a>.
 *
 * A toggleable filled icon button, used to represent an action. This version of a "contained"
 * filled icon button is responsible for a toggling its checked state as well as everything else
 * that a clickable filled icon button does.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * Use this "contained" icon button when the component requires more visual separation from the
 * background.
 *
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/filled-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * Toggleable filled icon button sample:
 * @sample androidx.compose.material3.samples.FilledIconToggleButtonSample
 *
 * @param checked whether or not this icon button is toggled on or off
 * @param onCheckedChange callback to be invoked when the toggleable icon button is clicked
 * @param modifier Modifier to be applied to the layout of the icon button
 * @param enabled whether or not this icon button will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this icon button. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this icon button in different states
 * @param shape defines the icon button's shape
 * @param colors an [IconToggleButtonColors] that will be used to resolve the colors used for
 * this icon button in different states. See [IconButtonDefaults.filledIconToggleButtonColors].
 * @param content the content (icon) to be drawn inside the icon button. This is typically an
 * [Icon].
 */
@ExperimentalMaterial3Api
@Composable
fun FilledIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = FilledIconButtonTokens.ContainerShape,
    colors: IconToggleButtonColors = IconButtonDefaults.filledIconToggleButtonColors(),
    content: @Composable () -> Unit
) = Surface(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier.semantics { role = Role.Checkbox },
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled, checked).value,
    contentColor = colors.contentColor(enabled, checked).value,
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(FilledIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design toggleable filled tonal icon button</a>.
 *
 * A toggleable filled tonal icon button, used to represent an action. This version of a "contained"
 * filled icon button is responsible for a toggling its checked state as well as everything else
 * that a clickable filled icon button does.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * Use this "contained" icon button when the component requires more visual separation from the
 * background.
 *
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/filled-tonal-icon-toggle-button.png)
 *
 * A filled tonal toggle icon button is a medium-emphasis icon button that is an alternative
 * middle ground between the default [FilledIconToggleButton] and [OutlinedIconToggleButton].
 * They can be used in contexts where the lower-priority icon button requires slightly more emphasis
 * than an outline would give.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * Toggleable filled tonal icon button sample:
 * @sample androidx.compose.material3.samples.FilledTonalIconToggleButtonSample
 *
 * @param checked whether or not this icon button is toggled on or off
 * @param onCheckedChange callback to be invoked when the toggleable icon button is clicked
 * @param modifier Modifier to be applied to the layout of the icon button
 * @param enabled whether or not this icon button will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this icon button. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this icon button in different states
 * @param shape defines the icon button's shape
 * @param colors an [IconToggleButtonColors] that will be used to resolve the colors used for
 * this icon button in different states. See [IconButtonDefaults.filledIconToggleButtonColors].
 * @param content the content (icon) to be drawn inside the icon button. This is typically an
 * [Icon].
 */
@ExperimentalMaterial3Api
@Composable
fun FilledTonalIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = FilledIconButtonTokens.ContainerShape,
    colors: IconToggleButtonColors = IconButtonDefaults.filledTonalIconToggleButtonColors(),
    content: @Composable () -> Unit
) = Surface(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier.semantics { role = Role.Checkbox },
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled, checked).value,
    contentColor = colors.contentColor(enabled, checked).value,
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(FilledTonalIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design outlined icon button</a>.
 *
 * An outlined icon button is a clickable icon, used to represent an action.
 *
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-icon-button.png)
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * Use this "contained" icon button when the component requires more visual separation from the
 * background.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * The outlined icon button has an overall minimum touch target size of 48 x 48dp, to meet
 * accessibility guidelines.
 *
 * @sample androidx.compose.material3.samples.OutlinedIconButtonSample
 *
 * @param onClick callback to be called when the icon button is clicked
 * @param modifier Modifier to be applied to the layout of the icon button
 * @param enabled whether or not this icon button will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this icon button. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this icon button in different states
 * @param shape defines the icon button's shape
 * @param border defines the icon button's border.See [IconButtonDefaults.outlinedIconButtonBorder].
 * @param colors an [IconButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.outlinedIconButtonColors].
 * @param content the content (icon) to be drawn inside the icon button. This is typically an
 * [Icon].
 */
@ExperimentalMaterial3Api
@Composable
fun OutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedIconButtonTokens.ContainerShape,
    border: BorderStroke? = IconButtonDefaults.outlinedIconButtonBorder(enabled),
    colors: IconButtonColors = IconButtonDefaults.outlinedIconButtonColors(),
    content: @Composable () -> Unit
) = Surface(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled).value,
    contentColor = colors.contentColor(enabled).value,
    border = border,
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(OutlinedIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design toggleable outlined icon button</a>.
 *
 * A toggleable outlined icon button, used to represent an action. This version of a "contained"
 * icon button is responsible for a toggling its checked state as well as everything else that a
 * clickable icon button does.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled button image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * @sample androidx.compose.material3.samples.OutlinedIconToggleButtonSample
 *
 * @param checked whether or not this icon button is toggled on or off
 * @param onCheckedChange callback to be invoked when the toggleable icon button is clicked
 * @param modifier Modifier to be applied to the layout of the icon button
 * @param enabled whether or not this icon button will handle input events and appear enabled for
 * semantics purposes
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this icon button. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this icon button in different states
 * @param shape defines the icon button's shape
 * @param border defines the icon button's border.See
 * [IconButtonDefaults.outlinedIconToggleButtonBorder]
 * @param colors an [IconToggleButtonColors] that will be used to resolve the colors used for
 * this icon button in different states. See [IconButtonDefaults.outlinedIconToggleButtonColors].
 * @param content the content (icon) to be drawn inside the icon button. This is typically an
 * [Icon].
 */
@ExperimentalMaterial3Api
@Composable
fun OutlinedIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedIconButtonTokens.ContainerShape,
    border: BorderStroke? = IconButtonDefaults.outlinedIconToggleButtonBorder(enabled, checked),
    colors: IconToggleButtonColors = IconButtonDefaults.outlinedIconToggleButtonColors(),
    content: @Composable () -> Unit
) = Surface(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier.semantics { role = Role.Checkbox },
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled, checked).value,
    contentColor = colors.contentColor(enabled, checked).value,
    border = border,
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(OutlinedIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Represents the container and content colors used in an icon button in different states.
 *
 * - See [IconButtonDefaults.filledIconButtonColors] and
 * [IconButtonDefaults.filledTonalIconButtonColors] for the default colors used in a
 * [FilledIconButton].
 * - See [IconButtonDefaults.outlinedIconButtonColors] for the default colors used in an
 * [OutlinedIconButton].
 */
@Stable
interface IconButtonColors {

    /**
     * Represents the container color for this icon button, depending on [enabled].
     *
     * @param enabled whether the icon button is enabled
     */
    @Composable
    fun containerColor(enabled: Boolean): State<Color>

    /**
     * Represents the content color for this icon button, depending on [enabled].
     *
     * @param enabled whether the icon button is enabled
     */
    @Composable
    fun contentColor(enabled: Boolean): State<Color>
}

/**
 * Represents the container and content colors used in a toggleable icon button in
 * different states.
 *
 * - See [IconButtonDefaults.filledIconToggleButtonColors] and
 * [IconButtonDefaults.filledTonalIconToggleButtonColors] for the default colors used in a
 * [FilledIconButton].
 * - See [IconButtonDefaults.outlinedIconToggleButtonColors] for the default colors used in a
 *  toggleable [OutlinedIconButton].
 */
@Stable
interface IconToggleButtonColors {

    /**
     * Represents the container color for this icon button, depending on [enabled] and [checked].
     *
     * @param enabled whether the icon button is enabled
     * @param checked whether the icon button is checked
     */
    @Composable
    fun containerColor(enabled: Boolean, checked: Boolean): State<Color>

    /**
     * Represents the content color for this icon button, depending on [enabled] and [checked].
     *
     * @param enabled whether the icon button is enabled
     * @param checked whether the icon button is checked
     */
    @Composable
    fun contentColor(enabled: Boolean, checked: Boolean): State<Color>
}

/**
 * Contains the default values used by all icon button types.
 */
object IconButtonDefaults {

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a [FilledIconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun filledIconButtonColors(
        containerColor: Color = FilledIconButtonTokens.ContainerColor.toColor(),
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = FilledIconButtonTokens.DisabledContainerColor.toColor()
            .copy(alpha = FilledIconButtonTokens.DisabledContainerOpacity),
        disabledContentColor: Color = FilledIconButtonTokens.DisabledColor.toColor()
            .copy(alpha = FilledIconButtonTokens.DisabledOpacity)
    ): IconButtonColors =
        DefaultIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * toggleable [FilledIconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun filledIconToggleButtonColors(
        containerColor: Color = FilledIconButtonTokens.UnselectedContainerColor.toColor(),
        // TODO(b/228455081): Using contentColorFor here will return OnSurfaceVariant,
        //  while the token value is Primary.
        contentColor: Color = FilledIconButtonTokens.ToggleUnselectedColor.toColor(),
        disabledContainerColor: Color = FilledIconButtonTokens.DisabledContainerColor.toColor()
            .copy(alpha = FilledIconButtonTokens.DisabledContainerOpacity),
        disabledContentColor: Color = FilledIconButtonTokens.DisabledColor.toColor()
            .copy(alpha = FilledIconButtonTokens.DisabledOpacity),
        checkedContainerColor: Color = FilledIconButtonTokens.SelectedContainerColor.toColor(),
        checkedContentColor: Color = contentColorFor(checkedContainerColor)
    ): IconToggleButtonColors =
        DefaultIconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a [FilledIconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun filledTonalIconButtonColors(
        containerColor: Color = FilledTonalIconButtonTokens.ContainerColor.toColor(),
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = FilledTonalIconButtonTokens.DisabledContainerColor.toColor()
            .copy(alpha = FilledTonalIconButtonTokens.DisabledContainerOpacity),
        disabledContentColor: Color = FilledTonalIconButtonTokens.DisabledColor.toColor()
            .copy(alpha = FilledTonalIconButtonTokens.DisabledOpacity)
    ): IconButtonColors =
        DefaultIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [FilledIconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun filledTonalIconToggleButtonColors(
        containerColor: Color = FilledTonalIconButtonTokens.UnselectedContainerColor.toColor(),
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = FilledTonalIconButtonTokens.DisabledContainerColor.toColor()
            .copy(alpha = FilledTonalIconButtonTokens.DisabledContainerOpacity),
        disabledContentColor: Color = FilledTonalIconButtonTokens.DisabledColor.toColor()
            .copy(alpha = FilledTonalIconButtonTokens.DisabledOpacity),
        checkedContainerColor: Color =
            FilledTonalIconButtonTokens.SelectedContainerColor.toColor(),
        checkedContentColor: Color = FilledTonalIconButtonTokens.ToggleSelectedColor.toColor()
    ): IconToggleButtonColors =
        DefaultIconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a
     * [OutlinedIconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun outlinedIconButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = OutlinedIconButtonTokens.UnselectedColor.toColor(),
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = OutlinedIconButtonTokens.DisabledColor.toColor()
            .copy(alpha = OutlinedIconButtonTokens.DisabledOpacity)
    ): IconButtonColors =
        DefaultIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [OutlinedIconToggleButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun outlinedIconToggleButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = OutlinedIconButtonTokens.UnselectedColor.toColor(),
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = OutlinedIconButtonTokens.DisabledColor.toColor()
            .copy(alpha = OutlinedIconButtonTokens.DisabledOpacity),
        checkedContainerColor: Color =
            OutlinedIconButtonTokens.SelectedContainerColor.toColor(),
        checkedContentColor: Color = contentColorFor(checkedContainerColor)
    ): IconToggleButtonColors =
        DefaultIconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    /**
     * Represents the [BorderStroke] for an [OutlinedIconButton], depending on its [enabled] and
     * [checked] state.
     *
     * @param enabled whether the icon button is enabled
     * @param checked whether the icon button is checked
     */
    @Composable
    fun outlinedIconToggleButtonBorder(enabled: Boolean, checked: Boolean): BorderStroke? {
        if (checked) {
            return null
        }
        return outlinedIconButtonBorder(enabled)
    }

    /**
     * Represents the [BorderStroke] for an [OutlinedIconButton], depending on its [enabled] state.
     *
     * @param enabled whether the icon button is enabled
     */
    @Composable
    fun outlinedIconButtonBorder(enabled: Boolean): BorderStroke {
        val color: Color = if (enabled) {
            OutlinedIconButtonTokens.UnselectedOutlineColor.toColor()
        } else {
            OutlinedIconButtonTokens.DisabledOutlineColor.toColor()
                .copy(alpha = OutlinedIconButtonTokens.DisabledOutlineOpacity)
        }
        return remember(color) {
            BorderStroke(OutlinedIconButtonTokens.UnselectedOutlineWidth, color)
        }
    }
}

/**
 * Default [IconButtonColors] implementation.
 */
@Immutable
private class DefaultIconButtonColors(
    private val containerColor: Color,
    private val contentColor: Color,
    private val disabledContainerColor: Color,
    private val disabledContentColor: Color,
) : IconButtonColors {
    @Composable
    override fun containerColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) containerColor else disabledContainerColor)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultIconButtonColors

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()

        return result
    }
}

/**
 * Default [IconToggleButtonColors] implementation.
 */
@Immutable
private class DefaultIconToggleButtonColors(
    private val containerColor: Color,
    private val contentColor: Color,
    private val disabledContainerColor: Color,
    private val disabledContentColor: Color,
    private val checkedContainerColor: Color,
    private val checkedContentColor: Color,
) : IconToggleButtonColors {
    @Composable
    override fun containerColor(enabled: Boolean, checked: Boolean): State<Color> {
        val target = when {
            !enabled -> disabledContainerColor
            !checked -> containerColor
            else -> checkedContainerColor
        }
        return rememberUpdatedState(target)
    }

    @Composable
    override fun contentColor(enabled: Boolean, checked: Boolean): State<Color> {
        val target = when {
            !enabled -> disabledContentColor
            !checked -> contentColor
            else -> checkedContentColor
        }
        return rememberUpdatedState(target)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultIconToggleButtonColors

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()

        return result
    }
}
