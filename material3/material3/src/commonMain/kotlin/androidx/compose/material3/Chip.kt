/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.tokens.AssistChipTokens
import androidx.compose.material3.tokens.FilterChipTokens
import androidx.compose.material3.tokens.InputChipTokens
import androidx.compose.material3.tokens.SuggestionChipTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * <a href="https://m3.material.io/components/chips/overview" class="external" target="_blank">Material Design assist chip</a>.
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Assist chips represent smart or automated actions that can span multiple apps, such as opening a
 * calendar event from the home screen. Assist chips function as though the user asked an assistant
 * to complete the action. They should appear dynamically and contextually in a UI.
 *
 * ![Assist chip image](https://developer.android.com/images/reference/androidx/compose/material3/assist-chip.png)
 *
 * This assist chip is applied with a flat style. If you want an elevated style, use the
 * [ElevatedAssistChip].
 *
 * Example of a flat AssistChip:
 * @sample androidx.compose.material3.samples.AssistChipSample
 *
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param trailingIcon optional icon at the end of the chip
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states.
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 * This controls the size of the shadow below the chip. Additionally, when the container color is
 * [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 * [AssistChipDefaults.assistChipElevation].
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 * shadow (when using [elevation])
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 * See [AssistChipDefaults.assistChipBorder].
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 * different states. See [AssistChipDefaults.assistChipColors].
 */
@ExperimentalMaterial3Api
@Composable
fun AssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ChipElevation? = AssistChipDefaults.assistChipElevation(),
    shape: Shape = AssistChipTokens.ContainerShape.toShape(),
    border: ChipBorder? = AssistChipDefaults.assistChipBorder(),
    colors: ChipColors = AssistChipDefaults.assistChipColors()
) = Chip(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    label = label,
    labelTextStyle = MaterialTheme.typography.fromToken(AssistChipTokens.LabelTextFont),
    labelColor = colors.labelColor(enabled).value,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    elevation = elevation,
    colors = colors,
    minHeight = AssistChipDefaults.Height,
    paddingValues = AssistChipPadding,
    shape = shape,
    border = border?.borderStroke(enabled)?.value,
    interactionSource = interactionSource
)

/**
 * <a href="https://m3.material.io/components/chips/overview" class="external" target="_blank">Material Design elevated assist chip</a>.
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Assist chips represent smart or automated actions that can span multiple apps, such as opening a
 * calendar event from the home screen. Assist chips function as though the user asked an assistant
 * to complete the action. They should appear dynamically and contextually in a UI.
 *
 * ![Assist chip image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-assist-chip.png)
 *
 * This assist chip is applied with an elevated style. If you want a flat style, use the
 * [AssistChip].
 *
 * Example of an elevated AssistChip with a trailing icon:
 * @sample androidx.compose.material3.samples.ElevatedAssistChipSample
 *
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param trailingIcon optional icon at the end of the chip
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states.
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 * This controls the size of the shadow below the chip. Additionally, when the container color is
 * [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 * [AssistChipDefaults.elevatedAssistChipElevation].
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 * shadow (when using [elevation])
 * @param border the border to draw around the container of this chip
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 * different states. See [AssistChipDefaults.elevatedAssistChipColors].
 */
@ExperimentalMaterial3Api
@Composable
fun ElevatedAssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ChipElevation? = AssistChipDefaults.elevatedAssistChipElevation(),
    shape: Shape = AssistChipTokens.ContainerShape.toShape(),
    border: ChipBorder? = null,
    colors: ChipColors = AssistChipDefaults.elevatedAssistChipColors()
) = Chip(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    label = label,
    labelTextStyle = MaterialTheme.typography.fromToken(AssistChipTokens.LabelTextFont),
    labelColor = colors.labelColor(enabled).value,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    elevation = elevation,
    colors = colors,
    minHeight = AssistChipDefaults.Height,
    paddingValues = AssistChipPadding,
    shape = shape,
    border = border?.borderStroke(enabled)?.value,
    interactionSource = interactionSource
)

/**
 * <a href="https://m3.material.io/components/chips/overview" class="external" target="_blank">Material Design filter chip</a>.
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Filter chips use tags or descriptive words to filter content. They can be a good alternative to
 * toggle buttons or checkboxes.
 *
 * ![Filter chip image](https://developer.android.com/images/reference/androidx/compose/material3/filter-chip.png)
 *
 * This filter chip is applied with a flat style. If you want an elevated style, use the
 * [ElevatedFilterChip].
 *
 * Tapping on a filter chip selects it, and in case a [selectedIcon] is provided (e.g. a checkmark),
 * it's appended to the starting edge of the chip's label, drawn instead of any given [leadingIcon].
 *
 * Example of a flat FilterChip with a trailing icon:
 * @sample androidx.compose.material3.samples.FilterChipSample
 *
 * Example of a FilterChip with both a leading icon and a selected icon:
 * @sample androidx.compose.material3.samples.FilterChipWithLeadingIconSample
 *
 * @param selected whether this chip is selected or not
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param selectedIcon optional icon at the start of the chip, preceding the [label] text, which is
 * displayed when the chip is selected, instead of any given [leadingIcon]
 * @param trailingIcon optional icon at the end of the chip
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states.
 * @param elevation [SelectableChipElevation] used to resolve the elevation for this chip in
 * different states. This controls the size of the shadow below the chip. Additionally, when the
 * container color is [ColorScheme.surface], this controls the amount of primary color applied as an
 * overlay. See [FilterChipDefaults.filterChipElevation].
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 * shadow (when using [elevation])
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 * See [FilterChipDefaults.filterChipBorder].
 * @param colors [SelectableChipColors] that will be used to resolve the colors used for this chip
 * in different states. See [FilterChipDefaults.filterChipColors].
 */
@ExperimentalMaterial3Api
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    selectedIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    shape: Shape = FilterChipTokens.ContainerShape.toShape(),
    border: SelectableChipBorder? = FilterChipDefaults.filterChipBorder(),
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors()
) = SelectableChip(
    selected = selected,
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    label = label,
    labelTextStyle = MaterialTheme.typography.fromToken(FilterChipTokens.LabelTextFont),
    leadingIcon = if (selected) selectedIcon else leadingIcon,
    avatar = null,
    trailingIcon = trailingIcon,
    elevation = elevation,
    colors = colors,
    minHeight = FilterChipDefaults.Height,
    paddingValues = FilterChipPadding,
    shape = shape,
    border = border?.borderStroke(enabled, selected)?.value,
    interactionSource = interactionSource
)

/**
 * <a href="https://m3.material.io/components/chips/overview" class="external" target="_blank">Material Design elevated filter chip</a>.
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Filter chips use tags or descriptive words to filter content. They can be a good alternative to
 * toggle buttons or checkboxes.
 *
 * ![Filter chip image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-filter-chip.png)
 *
 * This filter chip is applied with an elevated style. If you want a flat style, use the
 * [FilterChip].
 *
 * Tapping on a filter chip selects it, and in case a [selectedIcon] is provided (e.g. a checkmark),
 * it's appended to the starting edge of the chip's label, drawn instead of any given [leadingIcon].
 *
 * Example of an elevated FilterChip with a trailing icon:
 * @sample androidx.compose.material3.samples.ElevatedFilterChipSample
 *
 * @param selected whether this chip is selected or not
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param selectedIcon optional icon at the start of the chip, preceding the [label] text, which is
 * displayed when the chip is selected, instead of any given [leadingIcon]
 * @param trailingIcon optional icon at the end of the chip
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states.
 * @param elevation [SelectableChipElevation] used to resolve the elevation for this chip in
 * different states. This controls the size of the shadow below the chip. Additionally, when the
 * container color is [ColorScheme.surface], this controls the amount of primary color applied as an
 * overlay. See [FilterChipDefaults.filterChipElevation].
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 * shadow (when using [elevation])
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 * See [FilterChipDefaults.filterChipBorder].
 * @param colors [SelectableChipColors] that will be used to resolve the colors used for this chip
 * in different states. See [FilterChipDefaults.elevatedFilterChipColors].
 */
@ExperimentalMaterial3Api
@Composable
fun ElevatedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    selectedIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: SelectableChipElevation? = FilterChipDefaults.elevatedFilterChipElevation(),
    shape: Shape = FilterChipTokens.ContainerShape.toShape(),
    border: SelectableChipBorder? = null,
    colors: SelectableChipColors = FilterChipDefaults.elevatedFilterChipColors()
) = SelectableChip(
    selected = selected,
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    label = label,
    labelTextStyle = MaterialTheme.typography.fromToken(FilterChipTokens.LabelTextFont),
    leadingIcon = if (selected) selectedIcon else leadingIcon,
    avatar = null,
    trailingIcon = trailingIcon,
    elevation = elevation,
    colors = colors,
    minHeight = FilterChipDefaults.Height,
    paddingValues = FilterChipPadding,
    shape = shape,
    border = border?.borderStroke(enabled, selected)?.value,
    interactionSource = interactionSource
)

/**
 * <a href="https://m3.material.io/components/chips/overview" class="external" target="_blank">Material Design input chip</a>.
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Input chips represent discrete pieces of information entered by a user.
 *
 * ![Input chip image](https://developer.android.com/images/reference/androidx/compose/material3/input-chip.png)
 *
 * An Input Chip can have a leading icon or an avatar at its start. In case both are provided, the
 * avatar will take precedence and will be displayed.
 *
 * Example of an InputChip with a trailing icon:
 * @sample androidx.compose.material3.samples.InputChipSample
 *
 * Example of an InputChip with an avatar and a trailing icon:
 * @sample androidx.compose.material3.samples.InputChipWithAvatarSample
 *
 * Input chips should appear in a set and can be horizontally scrollable:
 * @sample androidx.compose.material3.samples.ChipGroupSingleLineSample
 *
 * Alternatively, use Accompanist's [Flow Layouts](https://google.github.io/accompanist/flowlayout/)
 * to wrap chips to a new line.
 *
 * @param selected whether this chip is selected or not
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param avatar optional avatar at the start of the chip, preceding the [label] text
 * @param trailingIcon optional icon at the end of the chip
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states.
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 * This controls the size of the shadow below the chip. Additionally, when the container color is
 * [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 * [InputChipDefaults.inputChipElevation].
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 * shadow (when using [elevation])
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 * See [InputChipDefaults.inputChipBorder].
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 * different states. See [InputChipDefaults.inputChipColors].
 */
@ExperimentalMaterial3Api
@Composable
fun InputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    avatar: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: SelectableChipElevation? = InputChipDefaults.inputChipElevation(),
    shape: Shape = InputChipTokens.ContainerShape.toShape(),
    border: SelectableChipBorder? = InputChipDefaults.inputChipBorder(),
    colors: SelectableChipColors = InputChipDefaults.inputChipColors()
) {
    // If given, place the avatar in an InputChipTokens.AvatarShape shape before passing it into the
    // Chip function.
    var shapedAvatar: @Composable (() -> Unit)? = null
    if (avatar != null) {
        val avatarOpacity = if (enabled) 1f else InputChipTokens.DisabledAvatarOpacity
        val avatarShape = InputChipTokens.AvatarShape.toShape()
        shapedAvatar = @Composable {
            Box(
                modifier = Modifier.graphicsLayer {
                    this.alpha = avatarOpacity
                    this.shape = avatarShape
                    this.clip = true
                },
                contentAlignment = Alignment.Center
            ) {
                avatar()
            }
        }
    }
    SelectableChip(
        selected = selected,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = MaterialTheme.typography.fromToken(InputChipTokens.LabelTextFont),
        leadingIcon = leadingIcon,
        avatar = shapedAvatar,
        trailingIcon = trailingIcon,
        elevation = elevation,
        colors = colors,
        minHeight = InputChipDefaults.Height,
        paddingValues = inputChipPadding(
            hasAvatar = shapedAvatar != null,
            hasLeadingIcon = leadingIcon != null,
            hasTrailingIcon = trailingIcon != null
        ),
        shape = shape,
        border = border?.borderStroke(enabled, selected)?.value,
        interactionSource = interactionSource
    )
}

/**
 * <a href="https://m3.material.io/components/chips/overview" class="external" target="_blank">Material Design suggestion chip</a>.
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Suggestion chips help narrow a user's intent by presenting dynamically generated suggestions,
 * such as possible responses or search filters.
 *
 * ![Suggestion chip image](https://developer.android.com/images/reference/androidx/compose/material3/suggestion-chip.png)
 *
 * This suggestion chip is applied with a flat style. If you want an elevated style, use the
 * [ElevatedSuggestionChip].
 *
 * Example of a flat SuggestionChip with a trailing icon:
 * @sample androidx.compose.material3.samples.SuggestionChipSample
 *
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param icon optional icon at the start of the chip, preceding the [label] text
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states.
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 * This controls the size of the shadow below the chip. Additionally, when the container color is
 * [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 * [SuggestionChipDefaults.suggestionChipElevation].
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 * shadow (when using [elevation])
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 * See [SuggestionChipDefaults.suggestionChipBorder].
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 * different states. See [SuggestionChipDefaults.suggestionChipColors].
 */
@ExperimentalMaterial3Api
@Composable
fun SuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ChipElevation? = SuggestionChipDefaults.suggestionChipElevation(),
    shape: Shape = SuggestionChipTokens.ContainerShape.toShape(),
    border: ChipBorder? = SuggestionChipDefaults.suggestionChipBorder(),
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors()
) = Chip(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    label = label,
    labelTextStyle = MaterialTheme.typography.fromToken(SuggestionChipTokens.LabelTextFont),
    labelColor = colors.labelColor(enabled).value,
    leadingIcon = icon,
    trailingIcon = null,
    elevation = elevation,
    colors = colors,
    minHeight = SuggestionChipDefaults.Height,
    paddingValues = SuggestionChipPadding,
    shape = shape,
    border = border?.borderStroke(enabled)?.value,
    interactionSource = interactionSource
)

/**
 * <a href="https://m3.material.io/components/chips/overview" class="external" target="_blank">Material Design elevated suggestion chip</a>.
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Suggestion chips help narrow a user's intent by presenting dynamically generated suggestions,
 * such as possible responses or search filters.
 *
 * ![Suggestion chip image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-suggestion-chip.png)
 *
 * This suggestion chip is applied with an elevated style. If you want a flat style, use the
 * [SuggestionChip].
 *
 * Example of an elevated SuggestionChip with a trailing icon:
 * @sample androidx.compose.material3.samples.ElevatedSuggestionChipSample
 *
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param icon optional icon at the start of the chip, preceding the [label] text
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states.
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 * This controls the size of the shadow below the chip. Additionally, when the container color is
 * [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 * [Surface] and [SuggestionChipDefaults.elevatedSuggestionChipElevation].
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 * shadow (when using [elevation])
 * @param border the border to draw around the container of this chip
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 * different states. See [SuggestionChipDefaults.elevatedSuggestionChipColors].
 */
@ExperimentalMaterial3Api
@Composable
fun ElevatedSuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ChipElevation? = SuggestionChipDefaults.elevatedSuggestionChipElevation(),
    shape: Shape = SuggestionChipTokens.ContainerShape.toShape(),
    border: ChipBorder? = null,
    colors: ChipColors = SuggestionChipDefaults.elevatedSuggestionChipColors()
) = Chip(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    label = label,
    labelTextStyle = MaterialTheme.typography.fromToken(SuggestionChipTokens.LabelTextFont),
    labelColor = colors.labelColor(enabled).value,
    leadingIcon = icon,
    trailingIcon = null,
    elevation = elevation,
    colors = colors,
    minHeight = SuggestionChipDefaults.Height,
    paddingValues = SuggestionChipPadding,
    shape = shape,
    border = border?.borderStroke(enabled)?.value,
    interactionSource = interactionSource
)

/**
 * Represents the elevation for a chip in different states.
 */
@Stable
@ExperimentalMaterial3Api
interface ChipElevation {
    /**
     * Represents the tonal elevation used in a chip, depending on its [enabled] state and
     * [interactionSource]. This should typically be the same value as the [shadowElevation].
     *
     * Tonal elevation is used to apply a color shift to the surface to give the it higher emphasis.
     * When surface's color is [ColorScheme.surface], a higher elevation will result in a darker
     * color in light theme and lighter color in dark theme.
     *
     * See [shadowElevation] which controls the elevation of the shadow drawn around the chip.
     *
     * @param enabled whether the chip is enabled
     * @param interactionSource the [InteractionSource] for this chip
     */
    @Composable
    fun tonalElevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp>

    /**
     * Represents the shadow elevation used in a chip, depending on its [enabled] state and
     * [interactionSource]. This should typically be the same value as the [tonalElevation].
     *
     * Shadow elevation is used to apply a shadow around the chip to give it higher emphasis.
     *
     * See [tonalElevation] which controls the elevation with a color shift to the surface.
     *
     * @param enabled whether the chip is enabled
     * @param interactionSource the [InteractionSource] for this chip
     */
    @Composable
    fun shadowElevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp>
}

/**
 * Represents the elevation used in a selectable chip in different states.
 */
@Stable
@ExperimentalMaterial3Api
interface SelectableChipElevation {
    /**
     * Represents the tonal elevation used in a chip, depending on [enabled], [selected], and
     * [interactionSource]. This should typically be the same value as the [shadowElevation].
     *
     * Tonal elevation is used to apply a color shift to the surface to give the it higher emphasis.
     * When surface's color is [ColorScheme.surface], a higher elevation will result in a darker
     * color in light theme and lighter color in dark theme.
     *
     * See [shadowElevation] which controls the elevation of the shadow drawn around the Chip.
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     * @param interactionSource the [InteractionSource] for this chip
     */
    @Composable
    fun tonalElevation(
        enabled: Boolean,
        selected: Boolean,
        interactionSource: InteractionSource
    ): State<Dp>

    /**
     * Represents the shadow elevation used in a chip, depending on [enabled], [selected], and
     * [interactionSource]. This should typically be the same value as the [tonalElevation].
     *
     * Shadow elevation is used to apply a shadow around the surface to give it higher emphasis.
     *
     * See [tonalElevation] which controls the elevation with a color shift to the surface.
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     * @param interactionSource the [InteractionSource] for this chip
     */
    @Composable
    fun shadowElevation(
        enabled: Boolean,
        selected: Boolean,
        interactionSource: InteractionSource
    ): State<Dp>
}

/**
 * Represents the container and content colors used in a clickable chip in different states.
 *
 * See [AssistChipDefaults], [InputChipDefaults], and [SuggestionChipDefaults] for the default
 * colors used in the various Chip configurations.
 */
@Stable
@ExperimentalMaterial3Api
interface ChipColors {
    /**
     * Represents the container color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    fun containerColor(enabled: Boolean): State<Color>

    /**
     * Represents the label color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    fun labelColor(enabled: Boolean): State<Color>

    /**
     * Represents the leading icon's content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    fun leadingIconContentColor(enabled: Boolean): State<Color>

    /**
     * Represents the trailing icon's content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    fun trailingIconContentColor(enabled: Boolean): State<Color>
}

/**
 * Represents the container and content colors used in a selectable chip in different states.
 *
 * See [FilterChipDefaults.filterChipColors] and [FilterChipDefaults.elevatedFilterChipColors] for
 * the default colors used in [FilterChip].
 */
@Stable
@ExperimentalMaterial3Api
interface SelectableChipColors {
    /**
     * Represents the container color for this chip, depending on [enabled] and [selected].
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     */
    @Composable
    fun containerColor(enabled: Boolean, selected: Boolean): State<Color>

    /**
     * Represents the label color for this chip, depending on [enabled] and [selected].
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     */
    @Composable
    fun labelColor(enabled: Boolean, selected: Boolean): State<Color>

    /**
     * Represents the leading icon color for this chip, depending on [enabled] and [selected].
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     */
    @Composable
    fun leadingIconContentColor(enabled: Boolean, selected: Boolean): State<Color>

    /**
     * Represents the trailing icon color for this chip, depending on [enabled] and [selected].
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     */
    @Composable
    fun trailingIconContentColor(enabled: Boolean, selected: Boolean): State<Color>
}

/**
 * Represents the border stroke used in a chip in different states.
 */
@Stable
@ExperimentalMaterial3Api
interface ChipBorder {

    /**
     * Represents the [BorderStroke] for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    fun borderStroke(enabled: Boolean): State<BorderStroke?>
}

/**
 * Represents the border stroke used used in a selectable chip in different states.
 */
@Stable
@ExperimentalMaterial3Api
interface SelectableChipBorder {
    /**
     * Represents the [BorderStroke] stroke used for this chip, depending on [enabled] and
     * [selected].
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     */
    @Composable
    fun borderStroke(enabled: Boolean, selected: Boolean): State<BorderStroke?>
}

/**
 * Contains the baseline values used by [AssistChip].
 */
@ExperimentalMaterial3Api
object AssistChipDefaults {
    /**
     * The height applied for an assist chip.
     * Note that you can override it by applying Modifier.height directly on a chip.
     */
    val Height = AssistChipTokens.ContainerHeight

    /**
     * The size of an assist chip icon.
     */
    val IconSize = AssistChipTokens.IconSize

    /**
     * Creates a [ChipColors] that represents the default container , label, and icon colors used in
     * a flat [AssistChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param leadingIconContentColor the color of this chip's start icon when enabled
     * @param trailingIconContentColor the color of this chip's end icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledLeadingIconContentColor the color of this chip's start icon when not enabled
     * @param disabledTrailingIconContentColor the color of this chip's end icon when not enabled
     */
    @Composable
    fun assistChipColors(
        containerColor: Color = Color.Transparent,
        labelColor: Color = AssistChipTokens.LabelTextColor.toColor(),
        leadingIconContentColor: Color = AssistChipTokens.IconColor.toColor(),
        trailingIconContentColor: Color = leadingIconContentColor,
        disabledContainerColor: Color = Color.Transparent,
        disabledLabelColor: Color = AssistChipTokens.DisabledLabelTextColor.toColor()
            .copy(alpha = AssistChipTokens.DisabledLabelTextOpacity),
        disabledLeadingIconContentColor: Color =
            AssistChipTokens.DisabledIconColor.toColor()
                .copy(alpha = AssistChipTokens.DisabledIconOpacity),
        disabledTrailingIconContentColor: Color = disabledLeadingIconContentColor,
    ): ChipColors = DefaultChipColors(
        containerColor = containerColor,
        labelColor = labelColor,
        leadingIconContentColor = leadingIconContentColor,
        trailingIconContentColor = trailingIconContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledLabelColor = disabledLabelColor,
        disabledLeadingIconContentColor = disabledLeadingIconContentColor,
        disabledTrailingIconContentColor = disabledTrailingIconContentColor
    )

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for a flat [AssistChip].
     *
     * @param defaultElevation the elevation used when the [AssistChip] is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed.
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun assistChipElevation(
        defaultElevation: Dp = AssistChipTokens.FlatContainerElevation,
        pressedElevation: Dp = defaultElevation,
        focusedElevation: Dp = defaultElevation,
        hoveredElevation: Dp = defaultElevation,
        draggedElevation: Dp = AssistChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = defaultElevation
    ): ChipElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation,
            disabledElevation
        ) {
            DefaultChipElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation
            )
        }
    }

    /**
     * Creates a [ChipBorder] that represents the default border used in a flat [AssistChip].
     *
     * @param borderColor the border color of this chip when enabled
     * @param disabledBorderColor the border color of this chip when not enabled
     * @param borderWidth the border stroke width of this chip
     */
    @Composable
    fun assistChipBorder(
        borderColor: Color = AssistChipTokens.FlatOutlineColor.toColor(),
        disabledBorderColor: Color = AssistChipTokens.FlatDisabledOutlineColor.toColor()
            .copy(alpha = AssistChipTokens.FlatDisabledOutlineOpacity),
        borderWidth: Dp = AssistChipTokens.FlatOutlineWidth,
    ): ChipBorder {
        return remember(
            borderColor,
            disabledBorderColor,
            borderWidth
        ) {
            DefaultChipBorder(
                borderColor = borderColor,
                disabledBorderColor = disabledBorderColor,
                borderWidth = borderWidth
            )
        }
    }

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * an elevated [AssistChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param leadingIconContentColor the color of this chip's start icon when enabled
     * @param trailingIconContentColor the color of this chip's end icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledLeadingIconContentColor the color of this chip's start icon when not enabled
     * @param disabledTrailingIconContentColor the color of this chip's end icon when not enabled
     */
    @Composable
    fun elevatedAssistChipColors(
        containerColor: Color = AssistChipTokens.ElevatedContainerColor.toColor(),
        labelColor: Color = AssistChipTokens.LabelTextColor.toColor(),
        leadingIconContentColor: Color = AssistChipTokens.IconColor.toColor(),
        trailingIconContentColor: Color = leadingIconContentColor,
        disabledContainerColor: Color = AssistChipTokens.ElevatedDisabledContainerColor.toColor()
            .copy(alpha = AssistChipTokens.ElevatedDisabledContainerOpacity),
        disabledLabelColor: Color = AssistChipTokens.DisabledLabelTextColor.toColor()
            .copy(alpha = AssistChipTokens.DisabledLabelTextOpacity),
        disabledLeadingIconContentColor: Color =
            AssistChipTokens.DisabledIconColor.toColor()
                .copy(alpha = AssistChipTokens.DisabledIconOpacity),
        disabledTrailingIconContentColor: Color = disabledLeadingIconContentColor,
    ): ChipColors = DefaultChipColors(
        containerColor = containerColor,
        labelColor = labelColor,
        leadingIconContentColor = leadingIconContentColor,
        trailingIconContentColor = trailingIconContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledLabelColor = disabledLabelColor,
        disabledLeadingIconContentColor = disabledLeadingIconContentColor,
        disabledTrailingIconContentColor = disabledTrailingIconContentColor
    )

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for an elevated [AssistChip].
     *
     * @param defaultElevation the elevation used when the [AssistChip] is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed.
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun elevatedAssistChipElevation(
        defaultElevation: Dp = AssistChipTokens.ElevatedContainerElevation,
        pressedElevation: Dp = AssistChipTokens.ElevatedPressedContainerElevation,
        focusedElevation: Dp = AssistChipTokens.ElevatedFocusContainerElevation,
        hoveredElevation: Dp = AssistChipTokens.ElevatedHoverContainerElevation,
        draggedElevation: Dp = AssistChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = AssistChipTokens.ElevatedDisabledContainerElevation
    ): ChipElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation,
            disabledElevation
        ) {
            DefaultChipElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation
            )
        }
    }
}

/**
 * Contains the baseline values used by [FilterChip].
 */
@ExperimentalMaterial3Api
object FilterChipDefaults {

    /**
     * The height applied for a filter chip.
     * Note that you can override it by applying Modifier.height directly on a chip.
     */
    val Height = FilterChipTokens.ContainerHeight

    /**
     * The size of an filter chip leading icon.
     */
    val IconSize = FilterChipTokens.IconSize

    /**
     * Creates a [SelectableChipColors] that represents the default container and content colors
     * used in a flat [FilterChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconColor the color of this chip's start and end icons when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledLeadingIconColor the color of this chip's start icon when not enabled
     * @param disabledTrailingIconColor the color of this chip's end icon when not enabled
     * @param selectedContainerColor the container color of this chip when selected
     * @param disabledSelectedContainerColor the container color of this chip when not enabled and
     * selected
     * @param selectedLabelColor the label color of this chip when selected
     * @param selectedLeadingIconColor the color of this chip's start icon when selected
     * @param selectedTrailingIconColor the color of this chip's end icon when selected
     */
    @Composable
    fun filterChipColors(
        containerColor: Color = Color.Transparent,
        labelColor: Color = FilterChipTokens.UnselectedLabelTextColor.toColor(),
        iconColor: Color = FilterChipTokens.UnselectedIconColor.toColor(),
        disabledContainerColor: Color = Color.Transparent,
        disabledLabelColor: Color = FilterChipTokens.DisabledLabelTextColor.toColor()
            .copy(alpha = FilterChipTokens.DisabledLabelTextOpacity),
        disabledLeadingIconColor: Color = FilterChipTokens.DisabledIconColor.toColor()
            .copy(alpha = FilterChipTokens.DisabledIconOpacity),
        disabledTrailingIconColor: Color = disabledLeadingIconColor,
        selectedContainerColor: Color = FilterChipTokens.FlatSelectedContainerColor.toColor(),
        disabledSelectedContainerColor: Color =
            FilterChipTokens.FlatDisabledSelectedContainerColor.toColor()
                .copy(alpha = FilterChipTokens.FlatDisabledSelectedContainerOpacity),
        selectedLabelColor: Color = FilterChipTokens.SelectedLabelTextColor.toColor(),
        selectedLeadingIconColor: Color = FilterChipTokens.SelectedIconColor.toColor(),
        selectedTrailingIconColor: Color = selectedLeadingIconColor
    ): SelectableChipColors = DefaultSelectableChipColors(
        containerColor = containerColor,
        labelColor = labelColor,
        leadingIconColor = iconColor,
        trailingIconColor = iconColor,
        disabledContainerColor = disabledContainerColor,
        disabledLabelColor = disabledLabelColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        selectedContainerColor = selectedContainerColor,
        disabledSelectedContainerColor = disabledSelectedContainerColor,
        selectedLabelColor = selectedLabelColor,
        selectedLeadingIconColor = selectedLeadingIconColor,
        selectedTrailingIconColor = selectedTrailingIconColor
    )

    /**
     * Creates a [SelectableChipElevation] that will animate between the provided values according
     * to the Material specification for a flat [FilterChip].
     *
     * @param defaultElevation the elevation used when the [FilterChip] is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun filterChipElevation(
        defaultElevation: Dp = FilterChipTokens.FlatContainerElevation,
        pressedElevation: Dp = FilterChipTokens.FlatSelectedPressedContainerElevation,
        focusedElevation: Dp = FilterChipTokens.FlatSelectedFocusContainerElevation,
        hoveredElevation: Dp = FilterChipTokens.FlatSelectedHoverContainerElevation,
        draggedElevation: Dp = FilterChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = defaultElevation
    ): SelectableChipElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation,
            disabledElevation
        ) {
            DefaultSelectableChipElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation
            )
        }
    }

    /**
     * Creates a [SelectableChipBorder] that represents the default border used in a flat
     * [FilterChip].
     *
     * @param borderColor the border color of this chip when enabled and not selected
     * @param selectedBorderColor the border color of this chip when enabled and selected
     * @param disabledBorderColor the border color of this chip when not enabled and not
     * selected
     * @param disabledSelectedBorderColor the border color of this chip when not enabled
     * but selected
     * @param borderWidth the border stroke width of this chip when not selected
     * @param selectedBorderWidth the border stroke width of this chip when selected
     */
    @Composable
    fun filterChipBorder(
        borderColor: Color = FilterChipTokens.FlatUnselectedOutlineColor.toColor(),
        selectedBorderColor: Color = Color.Transparent,
        disabledBorderColor: Color = FilterChipTokens.FlatDisabledUnselectedOutlineColor.toColor()
            .copy(alpha = FilterChipTokens.FlatDisabledUnselectedOutlineOpacity),
        disabledSelectedBorderColor: Color = Color.Transparent,
        borderWidth: Dp = FilterChipTokens.FlatUnselectedOutlineWidth,
        selectedBorderWidth: Dp = FilterChipTokens.FlatSelectedOutlineWidth,
    ): SelectableChipBorder {
        return remember(
            borderColor,
            selectedBorderColor,
            disabledBorderColor,
            disabledSelectedBorderColor,
            borderWidth,
            selectedBorderWidth
        ) {
            DefaultSelectableChipBorder(
                borderColor = borderColor,
                selectedBorderColor = selectedBorderColor,
                disabledBorderColor = disabledBorderColor,
                disabledSelectedBorderColor = disabledSelectedBorderColor,
                borderWidth = borderWidth,
                selectedBorderWidth = selectedBorderWidth
            )
        }
    }

    /**
     * Creates a [SelectableChipColors] that represents the default container and content colors
     * used in an elevated [FilterChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconColor the color of this chip's start and end icons when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledLeadingIconColor the color of this chip's start icon when not enabled
     * @param disabledTrailingIconColor the color of this chip's end icon when not enabled
     * @param selectedContainerColor the container color of this chip when selected
     * @param disabledSelectedContainerColor the container color of this chip when not enabled and
     * selected
     * @param selectedLabelColor the label color of this chip when selected
     * @param selectedLeadingIconColor the color of this chip's start icon when selected
     * @param selectedTrailingIconColor the color of this chip's end icon when selected
     */
    @Composable
    fun elevatedFilterChipColors(
        containerColor: Color = FilterChipTokens.ElevatedUnselectedContainerColor.toColor(),
        labelColor: Color = FilterChipTokens.UnselectedLabelTextColor.toColor(),
        iconColor: Color = FilterChipTokens.UnselectedIconColor.toColor(),
        disabledContainerColor: Color = FilterChipTokens.ElevatedDisabledContainerColor.toColor()
            .copy(alpha = FilterChipTokens.ElevatedDisabledContainerOpacity),
        disabledLabelColor: Color = FilterChipTokens.DisabledLabelTextColor.toColor()
            .copy(alpha = FilterChipTokens.DisabledLabelTextOpacity),
        disabledLeadingIconColor: Color = FilterChipTokens.DisabledIconColor.toColor()
            .copy(alpha = FilterChipTokens.DisabledIconOpacity),
        disabledTrailingIconColor: Color = disabledLeadingIconColor,
        selectedContainerColor: Color = FilterChipTokens.ElevatedSelectedContainerColor.toColor(),
        disabledSelectedContainerColor: Color = disabledContainerColor,
        selectedLabelColor: Color = FilterChipTokens.SelectedLabelTextColor.toColor(),
        selectedLeadingIconColor: Color = FilterChipTokens.SelectedIconColor.toColor(),
        selectedTrailingIconColor: Color = selectedLeadingIconColor
    ): SelectableChipColors = DefaultSelectableChipColors(
        containerColor = containerColor,
        labelColor = labelColor,
        leadingIconColor = iconColor,
        trailingIconColor = iconColor,
        disabledContainerColor = disabledContainerColor,
        disabledLabelColor = disabledLabelColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        selectedContainerColor = selectedContainerColor,
        disabledSelectedContainerColor = disabledSelectedContainerColor,
        selectedLabelColor = selectedLabelColor,
        selectedLeadingIconColor = selectedLeadingIconColor,
        selectedTrailingIconColor = selectedTrailingIconColor
    )

    /**
     * Creates a [SelectableChipElevation] that will animate between the provided values according
     * to the Material specification for an elevated [FilterChip].
     *
     * @param defaultElevation the elevation used when the chip is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun elevatedFilterChipElevation(
        defaultElevation: Dp = FilterChipTokens.ElevatedContainerElevation,
        pressedElevation: Dp = FilterChipTokens.ElevatedPressedContainerElevation,
        focusedElevation: Dp = FilterChipTokens.ElevatedFocusContainerElevation,
        hoveredElevation: Dp = FilterChipTokens.ElevatedHoverContainerElevation,
        draggedElevation: Dp = FilterChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = FilterChipTokens.ElevatedDisabledContainerElevation
    ): SelectableChipElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation,
            disabledElevation
        ) {
            DefaultSelectableChipElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation
            )
        }
    }
}

/**
 * Contains the baseline values used by an [InputChip].
 */
@ExperimentalMaterial3Api
object InputChipDefaults {
    /**
     * The height applied for a input chip.
     * Note that you can override it by applying Modifier.height directly on a chip.
     */
    val Height = InputChipTokens.ContainerHeight

    /**
     * The size of an input chip icon.
     */
    val IconSize = InputChipTokens.LeadingIconSize

    /**
     * The size of an input chip avatar.
     */
    val AvatarSize = InputChipTokens.AvatarSize

    /**
     * Creates a [SelectableChipColors] that represents the default container, label, and icon
     * colors used in an [InputChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param leadingIconColor the color of this chip's start icon when enabled
     * @param trailingIconColor the color of this chip's start end icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledLeadingIconColor the color of this chip's start icon when not enabled
     * @param disabledTrailingIconColor the color of this chip's end icon when not enabled
     * @param selectedContainerColor the container color of this chip when selected
     * @param disabledSelectedContainerColor the container color of this chip when not enabled and
     * selected
     * @param selectedLabelColor the label color of this chip when selected
     * @param selectedLeadingIconColor the color of this chip's start icon when selected
     * @param selectedTrailingIconColor the color of this chip's end icon when selected
     */
    @Composable
    fun inputChipColors(
        containerColor: Color = Color.Transparent,
        labelColor: Color = InputChipTokens.UnselectedLabelTextColor.toColor(),
        leadingIconColor: Color = InputChipTokens.UnselectedLeadingIconColor.toColor(),
        trailingIconColor: Color = InputChipTokens.UnselectedTrailingIconColor.toColor(),
        disabledContainerColor: Color = Color.Transparent,
        disabledLabelColor: Color = InputChipTokens.DisabledLabelTextColor.toColor()
            .copy(alpha = InputChipTokens.DisabledLabelTextOpacity),
        disabledLeadingIconColor: Color = InputChipTokens.DisabledLeadingIconColor.toColor()
            .copy(alpha = InputChipTokens.DisabledLeadingIconOpacity),
        disabledTrailingIconColor: Color = InputChipTokens.DisabledTrailingIconColor.toColor()
            .copy(alpha = InputChipTokens.DisabledTrailingIconOpacity),
        selectedContainerColor: Color = InputChipTokens.SelectedContainerColor.toColor(),
        disabledSelectedContainerColor: Color =
            InputChipTokens.DisabledSelectedContainerColor.toColor()
                .copy(alpha = InputChipTokens.DisabledSelectedContainerOpacity),
        selectedLabelColor: Color = InputChipTokens.SelectedLabelTextColor.toColor(),
        selectedLeadingIconColor: Color = InputChipTokens.SelectedLeadingIconColor.toColor(),
        selectedTrailingIconColor: Color = InputChipTokens.SelectedTrailingIconColor.toColor()
    ): SelectableChipColors = DefaultSelectableChipColors(
        containerColor = containerColor,
        labelColor = labelColor,
        leadingIconColor = leadingIconColor,
        trailingIconColor = trailingIconColor,
        disabledContainerColor = disabledContainerColor,
        disabledLabelColor = disabledLabelColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        selectedContainerColor = selectedContainerColor,
        disabledSelectedContainerColor = disabledSelectedContainerColor,
        selectedLabelColor = selectedLabelColor,
        selectedLeadingIconColor = selectedLeadingIconColor,
        selectedTrailingIconColor = selectedTrailingIconColor
    )

    /**
     * Creates a [SelectableChipElevation] that will animate between the provided values according
     * to the Material specification for an [InputChip].
     *
     * @param defaultElevation the elevation used when the [FilterChip] is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun inputChipElevation(
        defaultElevation: Dp = InputChipTokens.ContainerElevation,
        pressedElevation: Dp = defaultElevation,
        focusedElevation: Dp = defaultElevation,
        hoveredElevation: Dp = defaultElevation,
        draggedElevation: Dp = InputChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = defaultElevation
    ): SelectableChipElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation,
            disabledElevation
        ) {
            DefaultSelectableChipElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation
            )
        }
    }

    /**
     * Creates a [SelectableChipBorder] that represents the default border used in an [InputChip].
     *
     * @param borderColor the border color of this chip when enabled and not selected
     * @param selectedBorderColor the border color of this chip when enabled and selected
     * @param disabledBorderColor the border color of this chip when not enabled and not
     * selected
     * @param disabledSelectedBorderColor the border color of this chip when not enabled
     * but selected
     * @param borderWidth the border stroke width of this chip when not selected
     * @param selectedBorderWidth the border stroke width of this chip when selected
     */
    @Composable
    fun inputChipBorder(
        borderColor: Color = InputChipTokens.UnselectedOutlineColor.toColor(),
        selectedBorderColor: Color = Color.Transparent,
        disabledBorderColor: Color = InputChipTokens.DisabledUnselectedOutlineColor.toColor()
            .copy(alpha = InputChipTokens.DisabledUnselectedOutlineOpacity),
        disabledSelectedBorderColor: Color = Color.Transparent,
        borderWidth: Dp = InputChipTokens.UnselectedOutlineWidth,
        selectedBorderWidth: Dp = InputChipTokens.SelectedOutlineWidth,
    ): SelectableChipBorder {
        return remember(
            borderColor,
            selectedBorderColor,
            disabledBorderColor,
            disabledSelectedBorderColor,
            borderWidth,
            selectedBorderWidth
        ) {
            DefaultSelectableChipBorder(
                borderColor = borderColor,
                selectedBorderColor = selectedBorderColor,
                disabledBorderColor = disabledBorderColor,
                disabledSelectedBorderColor = disabledSelectedBorderColor,
                borderWidth = borderWidth,
                selectedBorderWidth = selectedBorderWidth
            )
        }
    }
}

/**
 * Contains the baseline values used by [SuggestionChip].
 */
@ExperimentalMaterial3Api
object SuggestionChipDefaults {
    /**
     * The height applied for a suggestion chip.
     * Note that you can override it by applying Modifier.height directly on a chip.
     */
    val Height = SuggestionChipTokens.ContainerHeight

    /**
     * The size of an suggestion chip icon.
     */
    // TODO(b/229778210): Read from the tokens when available.
    val IconSize = 18.dp

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * a flat [SuggestionChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconContentColor the color of this chip's icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledIconContentColor the color of this chip's icon when not enabled
     */
    @Composable
    fun suggestionChipColors(
        containerColor: Color = Color.Transparent,
        labelColor: Color = SuggestionChipTokens.LabelTextColor.toColor(),
        // TODO(b/229778210): Read from the tokens when available
        //  (i.e. SuggestionChipTokens.IconColor.toColor()).
        iconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor: Color = Color.Transparent,
        disabledLabelColor: Color = SuggestionChipTokens.DisabledLabelTextColor.toColor()
            .copy(alpha = SuggestionChipTokens.DisabledLabelTextOpacity),
        // TODO(b/229778210): Read from the tokens when available.
        disabledIconContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ): ChipColors = DefaultChipColors(
        containerColor = containerColor,
        labelColor = labelColor,
        leadingIconContentColor = iconContentColor,
        trailingIconContentColor = Color.Unspecified,
        disabledContainerColor = disabledContainerColor,
        disabledLabelColor = disabledLabelColor,
        disabledLeadingIconContentColor = disabledIconContentColor,
        disabledTrailingIconContentColor = Color.Unspecified
    )

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for a flat [SuggestionChip].
     *
     * @param defaultElevation the elevation used when the chip is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun suggestionChipElevation(
        defaultElevation: Dp = SuggestionChipTokens.FlatContainerElevation,
        pressedElevation: Dp = defaultElevation,
        focusedElevation: Dp = defaultElevation,
        hoveredElevation: Dp = defaultElevation,
        draggedElevation: Dp = SuggestionChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = defaultElevation
    ): ChipElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation,
            disabledElevation
        ) {
            DefaultChipElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation
            )
        }
    }

    /**
     * Creates a [ChipBorder] that represents the default border used in a flat [SuggestionChip].
     *
     * @param borderColor the border color of this chip when enabled
     * @param disabledBorderColor the border color of this chip when not enabled
     * @param borderWidth the border stroke width of this chip
     */
    @Composable
    fun suggestionChipBorder(
        borderColor: Color = SuggestionChipTokens.FlatOutlineColor.toColor(),
        disabledBorderColor: Color = SuggestionChipTokens.FlatDisabledOutlineColor.toColor()
            .copy(alpha = SuggestionChipTokens.FlatDisabledOutlineOpacity),
        borderWidth: Dp = SuggestionChipTokens.FlatOutlineWidth,
    ): ChipBorder {
        return remember(
            borderColor, disabledBorderColor, borderWidth
        ) {
            DefaultChipBorder(
                borderColor = borderColor,
                disabledBorderColor = disabledBorderColor,
                borderWidth = borderWidth
            )
        }
    }

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * an elevated [SuggestionChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconContentColor the color of this chip's icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledIconContentColor the color of this chip's icon when not enabled
     */
    @Composable
    fun elevatedSuggestionChipColors(
        containerColor: Color = SuggestionChipTokens.ElevatedContainerColor.toColor(),
        labelColor: Color = SuggestionChipTokens.LabelTextColor.toColor(),
        // TODO(b/229778210) Read from the tokens when available
        //  (i.e. SuggestionChipTokens.IconColor.toColor()).
        iconContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor: Color =
            SuggestionChipTokens.ElevatedDisabledContainerColor.toColor()
                .copy(alpha = SuggestionChipTokens.ElevatedDisabledContainerOpacity),
        disabledLabelColor: Color = SuggestionChipTokens.DisabledLabelTextColor.toColor()
            .copy(alpha = SuggestionChipTokens.DisabledLabelTextOpacity),
        // TODO(b/229778210): Read from the tokens when available.
        disabledIconContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    ): ChipColors = DefaultChipColors(
        containerColor = containerColor,
        labelColor = labelColor,
        leadingIconContentColor = iconContentColor,
        trailingIconContentColor = Color.Unspecified,
        disabledContainerColor = disabledContainerColor,
        disabledLabelColor = disabledLabelColor,
        disabledLeadingIconContentColor = disabledIconContentColor,
        disabledTrailingIconContentColor = Color.Unspecified
    )

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for an elevated [SuggestionChip].
     *
     * @param defaultElevation the elevation used when the chip is has no other
     * [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun elevatedSuggestionChipElevation(
        defaultElevation: Dp = SuggestionChipTokens.ElevatedContainerElevation,
        pressedElevation: Dp = SuggestionChipTokens.ElevatedPressedContainerElevation,
        focusedElevation: Dp = SuggestionChipTokens.ElevatedFocusContainerElevation,
        hoveredElevation: Dp = SuggestionChipTokens.ElevatedHoverContainerElevation,
        draggedElevation: Dp = SuggestionChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = SuggestionChipTokens.ElevatedDisabledContainerElevation
    ): ChipElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation,
            disabledElevation
        ) {
            DefaultChipElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
                disabledElevation = disabledElevation
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun Chip(
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    labelColor: Color,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    elevation: ChipElevation?,
    colors: ChipColors,
    minHeight: Dp,
    paddingValues: PaddingValues,
    shape: Shape,
    border: BorderStroke?,
    interactionSource: MutableInteractionSource,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled).value,
        tonalElevation = elevation?.tonalElevation(enabled, interactionSource)?.value ?: 0.dp,
        shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp,
        border = border,
        interactionSource = interactionSource,
    ) {
        ChipContent(
            label = label,
            labelTextStyle = labelTextStyle,
            labelColor = labelColor,
            leadingIcon = leadingIcon,
            avatar = null,
            trailingIcon = trailingIcon,
            leadingIconColor = colors.leadingIconContentColor(enabled).value,
            trailingIconColor = colors.trailingIconContentColor(enabled).value,
            minHeight = minHeight,
            paddingValues = paddingValues
        )
    }
}

@ExperimentalMaterial3Api
@Composable
private fun SelectableChip(
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    leadingIcon: @Composable (() -> Unit)?,
    avatar: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    elevation: SelectableChipElevation?,
    colors: SelectableChipColors,
    minHeight: Dp,
    paddingValues: PaddingValues,
    shape: Shape,
    border: BorderStroke?,
    interactionSource: MutableInteractionSource
) {
    // TODO(b/229794614): Animate transition between unselected and selected.
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled, selected).value,
        tonalElevation = elevation?.tonalElevation(enabled, selected, interactionSource)?.value
            ?: 0.dp,
        shadowElevation = elevation?.shadowElevation(enabled, selected, interactionSource)?.value
            ?: 0.dp,
        border = border,
        interactionSource = interactionSource,
    ) {
        ChipContent(
            label = label,
            labelTextStyle = labelTextStyle,
            leadingIcon = leadingIcon,
            avatar = avatar,
            labelColor = colors.labelColor(enabled, selected).value,
            trailingIcon = trailingIcon,
            leadingIconColor = colors.leadingIconContentColor(enabled, selected).value,
            trailingIconColor = colors.trailingIconContentColor(enabled, selected).value,
            minHeight = minHeight,
            paddingValues = paddingValues
        )
    }
}

@Composable
private fun ChipContent(
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    labelColor: Color,
    leadingIcon: @Composable (() -> Unit)?,
    avatar: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    leadingIconColor: Color,
    trailingIconColor: Color,
    minHeight: Dp,
    paddingValues: PaddingValues
) {
    CompositionLocalProvider(
        LocalContentColor provides labelColor,
        LocalTextStyle provides labelTextStyle
    ) {
        Row(
            Modifier
                .defaultMinSize(minHeight = minHeight)
                .padding(paddingValues),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatar != null) {
                avatar()
            } else if (leadingIcon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides leadingIconColor, content = leadingIcon
                )
            }
            Spacer(Modifier.width(HorizontalElementsPadding))
            label()
            Spacer(Modifier.width(HorizontalElementsPadding))
            if (trailingIcon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides trailingIconColor, content = trailingIcon
                )
            }
        }
    }
}

/** Default [ChipElevation] implementation. */
@ExperimentalMaterial3Api
@Immutable
private class DefaultChipElevation(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val draggedElevation: Dp,
    private val disabledElevation: Dp
) : ChipElevation {
    @Composable
    override fun tonalElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    override fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                    is DragInteraction.Start -> {
                        interactions.add(interaction)
                    }
                    is DragInteraction.Stop -> {
                        interactions.remove(interaction.start)
                    }
                    is DragInteraction.Cancel -> {
                        interactions.remove(interaction.start)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target = if (!enabled) {
            disabledElevation
        } else {
            when (interaction) {
                is PressInteraction.Press -> pressedElevation
                is HoverInteraction.Enter -> hoveredElevation
                is FocusInteraction.Focus -> focusedElevation
                is DragInteraction.Start -> draggedElevation
                else -> defaultElevation
            }
        }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        if (!enabled) {
            // No transition when moving to a disabled state
            LaunchedEffect(target) { animatable.snapTo(target) }
        } else {
            LaunchedEffect(target) {
                val lastInteraction = when (animatable.targetValue) {
                    pressedElevation -> PressInteraction.Press(Offset.Zero)
                    hoveredElevation -> HoverInteraction.Enter()
                    focusedElevation -> FocusInteraction.Focus()
                    draggedElevation -> DragInteraction.Start()
                    else -> null
                }
                animatable.animateElevation(
                    from = lastInteraction, to = interaction, target = target
                )
            }
        }

        return animatable.asState()
    }
}

/**
 * Default [SelectableChipElevation] implementation.
 *
 * Note that this default implementation does not take into consideration the `selectable` state
 * passed into its [tonalElevation] and [shadowElevation]. If you wish to apply that state, use a
 * different [SelectableChipElevation].
 */
@ExperimentalMaterial3Api
@Immutable
private class DefaultSelectableChipElevation(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val draggedElevation: Dp,
    private val disabledElevation: Dp
) : SelectableChipElevation {
    @Composable
    override fun tonalElevation(
        enabled: Boolean,
        selected: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    override fun shadowElevation(
        enabled: Boolean,
        selected: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                    is DragInteraction.Start -> {
                        interactions.add(interaction)
                    }
                    is DragInteraction.Stop -> {
                        interactions.remove(interaction.start)
                    }
                    is DragInteraction.Cancel -> {
                        interactions.remove(interaction.start)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target = if (!enabled) {
            disabledElevation
        } else {
            when (interaction) {
                is PressInteraction.Press -> pressedElevation
                is HoverInteraction.Enter -> hoveredElevation
                is FocusInteraction.Focus -> focusedElevation
                is DragInteraction.Start -> draggedElevation
                else -> defaultElevation
            }
        }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        if (!enabled) {
            // No transition when moving to a disabled state
            LaunchedEffect(target) { animatable.snapTo(target) }
        } else {
            LaunchedEffect(target) {
                val lastInteraction = when (animatable.targetValue) {
                    pressedElevation -> PressInteraction.Press(Offset.Zero)
                    hoveredElevation -> HoverInteraction.Enter()
                    focusedElevation -> FocusInteraction.Focus()
                    draggedElevation -> DragInteraction.Start()
                    else -> null
                }
                animatable.animateElevation(
                    from = lastInteraction, to = interaction, target = target
                )
            }
        }

        return animatable.asState()
    }
}

/**
 * Default [ChipColors] implementation.
 */
@ExperimentalMaterial3Api
@Immutable
private class DefaultChipColors(
    private val containerColor: Color,
    private val labelColor: Color,
    private val leadingIconContentColor: Color,
    private val trailingIconContentColor: Color,
    private val disabledContainerColor: Color,
    private val disabledLabelColor: Color,
    private val disabledLeadingIconContentColor: Color,
    private val disabledTrailingIconContentColor: Color
    // TODO(b/113855296): Support other states: hover, focus, drag
) : ChipColors {
    @Composable
    override fun containerColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) containerColor else disabledContainerColor)
    }

    @Composable
    override fun labelColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) labelColor else disabledLabelColor)
    }

    @Composable
    override fun leadingIconContentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) leadingIconContentColor else disabledLeadingIconContentColor
        )
    }

    @Composable
    override fun trailingIconContentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) trailingIconContentColor else disabledTrailingIconContentColor
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultChipColors

        if (containerColor != other.containerColor) return false
        if (labelColor != other.labelColor) return false
        if (leadingIconContentColor != other.leadingIconContentColor) return false
        if (trailingIconContentColor != other.trailingIconContentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledLabelColor != other.disabledLabelColor) return false
        if (disabledLeadingIconContentColor != other.disabledLeadingIconContentColor) return false
        if (disabledTrailingIconContentColor != other.disabledTrailingIconContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + labelColor.hashCode()
        result = 31 * result + leadingIconContentColor.hashCode()
        result = 31 * result + trailingIconContentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledLabelColor.hashCode()
        result = 31 * result + disabledLeadingIconContentColor.hashCode()
        result = 31 * result + disabledTrailingIconContentColor.hashCode()

        return result
    }
}

/**
 * Default [SelectableChipColors] implementation.
 */
@ExperimentalMaterial3Api
@Immutable
private class DefaultSelectableChipColors(
    private val containerColor: Color,
    private val labelColor: Color,
    private val leadingIconColor: Color,
    private val trailingIconColor: Color,
    private val disabledContainerColor: Color,
    private val disabledLabelColor: Color,
    private val disabledLeadingIconColor: Color,
    private val disabledTrailingIconColor: Color,
    private val selectedContainerColor: Color,
    private val disabledSelectedContainerColor: Color,
    private val selectedLabelColor: Color,
    private val selectedLeadingIconColor: Color,
    private val selectedTrailingIconColor: Color
    // TODO(b/113855296): Support other states: hover, focus, drag
) : SelectableChipColors {
    @Composable
    override fun containerColor(enabled: Boolean, selected: Boolean): State<Color> {
        val target = when {
            !enabled -> if (selected) disabledSelectedContainerColor else disabledContainerColor
            !selected -> containerColor
            else -> selectedContainerColor
        }
        return rememberUpdatedState(target)
    }

    @Composable
    override fun labelColor(enabled: Boolean, selected: Boolean): State<Color> {
        val target = when {
            !enabled -> disabledLabelColor
            !selected -> labelColor
            else -> selectedLabelColor
        }
        return rememberUpdatedState(target)
    }

    @Composable
    override fun leadingIconContentColor(enabled: Boolean, selected: Boolean): State<Color> {
        val target = when {
            !enabled -> disabledLeadingIconColor
            !selected -> leadingIconColor
            else -> selectedLeadingIconColor
        }
        return rememberUpdatedState(target)
    }

    @Composable
    override fun trailingIconContentColor(enabled: Boolean, selected: Boolean): State<Color> {
        val target = when {
            !enabled -> disabledTrailingIconColor
            !selected -> trailingIconColor
            else -> selectedTrailingIconColor
        }
        return rememberUpdatedState(target)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultSelectableChipColors

        if (containerColor != other.containerColor) return false
        if (labelColor != other.labelColor) return false
        if (leadingIconColor != other.leadingIconColor) return false
        if (trailingIconColor != other.trailingIconColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledLabelColor != other.disabledLabelColor) return false
        if (disabledLeadingIconColor != other.disabledLeadingIconColor) return false
        if (disabledTrailingIconColor != other.disabledTrailingIconColor) return false
        if (selectedContainerColor != other.selectedContainerColor) return false
        if (disabledSelectedContainerColor != other.disabledSelectedContainerColor) return false
        if (selectedLabelColor != other.selectedLabelColor) return false
        if (selectedLeadingIconColor != other.selectedLeadingIconColor) return false
        if (selectedTrailingIconColor != other.selectedTrailingIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + labelColor.hashCode()
        result = 31 * result + leadingIconColor.hashCode()
        result = 31 * result + trailingIconColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledLabelColor.hashCode()
        result = 31 * result + disabledLeadingIconColor.hashCode()
        result = 31 * result + disabledTrailingIconColor.hashCode()
        result = 31 * result + selectedContainerColor.hashCode()
        result = 31 * result + disabledSelectedContainerColor.hashCode()
        result = 31 * result + selectedLabelColor.hashCode()
        result = 31 * result + selectedLeadingIconColor.hashCode()
        result = 31 * result + selectedTrailingIconColor.hashCode()

        return result
    }
}

/**
 * Default [SelectableChipBorder] implementation.
 */
@ExperimentalMaterial3Api
@Immutable
private class DefaultSelectableChipBorder(
    private val borderColor: Color,
    private val selectedBorderColor: Color,
    private val disabledBorderColor: Color,
    private val disabledSelectedBorderColor: Color,
    private val borderWidth: Dp,
    private val selectedBorderWidth: Dp
) : SelectableChipBorder {
    @Composable
    override fun borderStroke(enabled: Boolean, selected: Boolean): State<BorderStroke?> {
        val color = if (enabled) {
            if (selected) selectedBorderColor else borderColor
        } else {
            if (selected) disabledSelectedBorderColor else disabledBorderColor
        }
        return rememberUpdatedState(
            BorderStroke(if (selected) selectedBorderWidth else borderWidth, color)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultSelectableChipBorder

        if (borderColor != other.borderColor) return false
        if (selectedBorderColor != other.selectedBorderColor) return false
        if (disabledBorderColor != other.disabledBorderColor) return false
        if (disabledSelectedBorderColor != other.disabledSelectedBorderColor) return false
        if (borderWidth != other.borderWidth) return false
        if (selectedBorderWidth != other.selectedBorderWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = borderColor.hashCode()
        result = 31 * result + selectedBorderColor.hashCode()
        result = 31 * result + disabledBorderColor.hashCode()
        result = 31 * result + disabledSelectedBorderColor.hashCode()
        result = 31 * result + borderWidth.hashCode()
        result = 31 * result + selectedBorderWidth.hashCode()

        return result
    }
}

/**
 * Default [ChipBorder] implementation.
 */
@ExperimentalMaterial3Api
@Immutable
private class DefaultChipBorder(
    private val borderColor: Color,
    private val disabledBorderColor: Color,
    private val borderWidth: Dp,
) : ChipBorder {
    @Composable
    override fun borderStroke(enabled: Boolean): State<BorderStroke?> {
        return rememberUpdatedState(
            BorderStroke(borderWidth, if (enabled) borderColor else disabledBorderColor)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultChipBorder

        if (borderColor != other.borderColor) return false
        if (disabledBorderColor != other.disabledBorderColor) return false
        if (borderWidth != other.borderWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = borderColor.hashCode()
        result = 31 * result + disabledBorderColor.hashCode()
        result = 31 * result + borderWidth.hashCode()

        return result
    }
}

/**
 * Returns the [PaddingValues] for the input chip.
 */
private fun inputChipPadding(
    hasAvatar: Boolean = false,
    hasLeadingIcon: Boolean = false,
    hasTrailingIcon: Boolean = false
): PaddingValues {
    val start = if (hasAvatar || !hasLeadingIcon) 4.dp else 8.dp
    val end = if (hasTrailingIcon) 8.dp else 4.dp
    return PaddingValues(start = start, end = end)
}

/**
 * The padding between the elements in the chip.
 */
private val HorizontalElementsPadding = 8.dp

/**
 * Returns the [PaddingValues] for the assist chip.
 */
private val AssistChipPadding = PaddingValues(horizontal = HorizontalElementsPadding)

/**
 * [PaddingValues] for the filter chip.
 */
private val FilterChipPadding = PaddingValues(horizontal = HorizontalElementsPadding)

/**
 * Returns the [PaddingValues] for the suggestion chip.
 */
private val SuggestionChipPadding = PaddingValues(horizontal = HorizontalElementsPadding)
