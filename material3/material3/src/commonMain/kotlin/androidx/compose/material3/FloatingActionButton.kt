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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.tokens.ExtendedFabPrimaryTokens
import androidx.compose.material3.tokens.FabPrimaryLargeTokens
import androidx.compose.material3.tokens.FabPrimarySmallTokens
import androidx.compose.material3.tokens.FabPrimaryTokens
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect

/**
 * ![FAB image](https://developer.android.com/images/reference/androidx/compose/material3/fab.png)
 *
 * A floating action button (FAB) represents the primary action of a screen. To learn more about the
 * FAB visit the [Material website](https://m3.material.io/components/floating-action-button).
 *
 * FAB typically contains an icon, for a FAB with text and an icon, see
 * [ExtendedFloatingActionButton].
 *
 * @sample androidx.compose.material3.samples.FloatingActionButtonSample
 *
 * @param onClick callback invoked when this FAB is clicked
 * @param modifier [Modifier] to be applied to this FAB.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this FAB. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this FAB in different [Interaction]s.
 * @param shape The [Shape] of this FAB
 * @param containerColor The container color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color for content inside this FAB
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB
 * in different states. This controls the size of the shadow below the FAB. When [containerColor]
 * is [ColorScheme.surface], a higher elevation (surface blended with more primary) will result in
 * a darker surface color in light theme and lighter color in dark theme.
 * @param content the content of this FAB - this is typically an [Icon].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = FabPrimaryTokens.ContainerShape,
    containerColor: Color = FabPrimaryTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = elevation.tonalElevation(interactionSource = interactionSource).value,
        shadowElevation = elevation.shadowElevation(interactionSource = interactionSource).value,
        interactionSource = interactionSource,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            // Adding the text style from [ExtendedFloatingActionButton] to all FAB variations. In
            // the majority of cases this will have no impact, because icons are expected, but if a
            // developer decides to put some short text to emulate an icon, (like "?") then it will
            // have the correct styling.
            ProvideTextStyle(
                MaterialTheme.typography.fromToken(ExtendedFabPrimaryTokens.LabelTextFont),
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(
                            minWidth = FabPrimaryTokens.ContainerWidth,
                            minHeight = FabPrimaryTokens.ContainerHeight,
                        ),
                    contentAlignment = Alignment.Center,
                ) { content() }
            }
        }
    }
}

/**
 * ![Small FAB image](https://developer.android.com/images/reference/androidx/compose/material3/small-fab.png)
 *
 * A small floating action button.
 *
 * @sample androidx.compose.material3.samples.SmallFloatingActionButtonSample
 *
 * @param onClick callback invoked when this FAB is clicked
 * @param modifier [Modifier] to be applied to this FAB.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this FAB. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this FAB in different [Interaction]s.
 * @param shape The [Shape] of this FAB
 * @param containerColor The container color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color for content inside this FAB
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB
 * in different states. This controls the size of the shadow below the FAB. When [containerColor]
 * is [ColorScheme.surface], a higher elevation (surface blended with more primary) will result in
 * a darker surface color in light theme and lighter color in dark theme.
 * @param content the content of this FAB - this is typically an [Icon].
 */
@Composable
fun SmallFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = FabPrimarySmallTokens.ContainerShape,
    containerColor: Color = FabPrimaryTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            minWidth = FabPrimarySmallTokens.ContainerWidth,
            minHeight = FabPrimarySmallTokens.ContainerHeight,
        ),
        interactionSource = interactionSource,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        content = content,
    )
}

/**
 * ![Large FAB image](https://developer.android.com/images/reference/androidx/compose/material3/large-fab.png)
 *
 *  A large circular floating action button.
 *
 * @sample androidx.compose.material3.samples.LargeFloatingActionButtonSample
 *
 * @param onClick callback invoked when this FAB is clicked
 * @param modifier [Modifier] to be applied to this FAB.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this FAB. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this FAB in different [Interaction]s.
 * @param shape The [Shape] of this FAB
 * @param containerColor The container color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color for content inside this FAB
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB
 * in different states. This controls the size of the shadow below the FAB. When [containerColor]
 * is [ColorScheme.surface], a higher elevation (surface blended with more primary) will result in
 * a darker surface color in light theme and lighter color in dark theme.
 * @param content the content of this FAB - this is typically an [Icon].
 */
@Composable
fun LargeFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = FabPrimaryLargeTokens.ContainerShape,
    containerColor: Color = FabPrimaryLargeTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            minWidth = FabPrimaryLargeTokens.ContainerWidth,
            minHeight = FabPrimaryLargeTokens.ContainerHeight,
        ),
        interactionSource = interactionSource,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
        content = content,
    )
}

/**
 * ![Extended FAB image](https://developer.android.com/images/reference/androidx/compose/material3/extended-fab.png)
 *
 * The extended FAB is wider than a regular FAB, and it includes a text label. To learn more about
 * the extended FAB visit the [Material website](https://m3.material.io/components/extended-fab).
 *
 * The other extended floating action button overload supports a text label and icon.
 *
 * @sample androidx.compose.material3.samples.ExtendedFloatingActionButtonTextSample
 *
 * @param onClick callback invoked when this FAB is clicked
 * @param modifier [Modifier] to be applied to this FAB
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this FAB. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this FAB in different [Interaction]s.
 * @param shape The [Shape] of this FAB
 * @param containerColor The container color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color for content inside this FAB
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB
 * in different states. This controls the size of the shadow below the FAB. When [containerColor]
 * is [ColorScheme.surface], a higher elevation (surface blended with more primary) will result in
 * a darker surface color in light theme and lighter color in dark theme.
 * @param content the content of this FAB - this is typically an [Text] label.
 */
@Composable
fun ExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = ExtendedFabPrimaryTokens.ContainerShape,
    containerColor: Color = ExtendedFabPrimaryTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable RowScope.() -> Unit,
) {
    FloatingActionButton(
        modifier = modifier.sizeIn(minWidth = ExtendedFabMinimumWidth),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ExtendedFabTextPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * ![Extended FAB image](https://developer.android.com/images/reference/androidx/compose/material3/extended-fab.png)
 *
 * The extended FAB is wider than a regular FAB, and it includes a text label and icon. To learn
 * more about the extended FAB visit the
 * [Material website](https://m3.material.io/components/extended-fab).
 *
 * The other extended floating action button overload are for FABs without an icon.
 *
 * @sample androidx.compose.material3.samples.ExtendedFloatingActionButtonSample
 * @sample androidx.compose.material3.samples.AnimatedExtendedFloatingActionButtonSample
 *
 * @param text Text label displayed inside this FAB
 * @param icon Optional icon for this FAB, typically this will be a
 * [Icon].
 * @param onClick callback invoked when this FAB is clicked
 * @param modifier [Modifier] to be applied to this FAB
 * @param expanded The FAB will animate between expanded state and collapsed state. In an expanded
 * state the FAB will show both the [icon] and [text] parameters. In a collapsed state the FAB will
 * show only the [icon] parameter.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this FAB. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this FAB in different [Interaction]s.
 * @param shape The [Shape] of this FAB
 * @param containerColor The container color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color for content inside this FAB
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB
 * in different states. This controls the size of the shadow below the FAB. When [containerColor]
 * is [ColorScheme.surface], a higher elevation (surface blended with more primary) will result in
 * a darker surface color in light theme and lighter color in dark theme.
 */
@Composable
fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = ExtendedFabPrimaryTokens.ContainerShape,
    containerColor: Color = ExtendedFabPrimaryTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
) {
    FloatingActionButton(
        modifier = modifier.sizeIn(
            minWidth = if (expanded) ExtendedFabMinimumWidth else FabPrimaryTokens.ContainerWidth
        ),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
    ) {
        val startPadding = if (expanded) ExtendedFabPrimaryTokens.IconSize / 2 else 0.dp
        val endPadding = if (expanded) ExtendedFabTextPadding else 0.dp

        Row(
            modifier = Modifier.padding(start = startPadding, end = endPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            AnimatedVisibility(
                visible = expanded,
                enter = ExtendedFabExpandAnimation,
                exit = ExtendedFabCollapseAnimation,
            ) {
                Row {
                    Spacer(Modifier.width(ExtendedFabIconPadding))
                    text()
                }
            }
        }
    }
}

/**
 * Represents the tonal and shadow elevation for a floating action button in different states.
 *
 * See [FloatingActionButtonDefaults.elevation] for the default elevation used in a
 * [FloatingActionButton] and [ExtendedFloatingActionButton].
 */
@Stable
interface FloatingActionButtonElevation {
    /**
     * Represents the tonal elevation used in a floating action button, depending on
     * [interactionSource]. This should typically be the same value as the [shadowElevation].
     *
     * Tonal elevation is used to apply a color shift to the surface to give the it higher emphasis.
     * When surface's color is [ColorScheme.surface], a higher the elevation will result
     * in a darker color in light theme and lighter color in dark theme.
     *
     * See [shadowElevation] which controls the elevation of the shadow drawn around the FAB.
     *
     * @param interactionSource the [InteractionSource] for this floating action button
     */
    @Composable
    fun tonalElevation(interactionSource: InteractionSource): State<Dp>

    /**
     * Represents the shadow elevation used in a floating action button, depending on
     * [interactionSource]. This should typically be the same value as the [tonalElevation].
     *
     * Shadow elevation is used to apply a shadow around the surface to give it higher emphasis.
     *
     * See [tonalElevation] which controls the elevation with a color shift to the surface.
     *
     * @param interactionSource the [InteractionSource] for this floating action button
     */
    @Composable
    fun shadowElevation(interactionSource: InteractionSource): State<Dp>
}

/**
 * Contains the default values used by [FloatingActionButton]
 */
object FloatingActionButtonDefaults {
    /**
     * The recommended size of the icon inside a [LargeFloatingActionButton].
     */
    val LargeIconSize = FabPrimaryLargeTokens.IconSize

    /**
     * Creates a [FloatingActionButtonElevation] that represents the elevation of a
     * [FloatingActionButton] in different states. For use cases in which a less prominent
     * [FloatingActionButton] is possible consider the [loweredElevation].
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] has no other
     * [Interaction]s.
     * @param pressedElevation the elevation used when the [FloatingActionButton] is pressed.
     * @param focusedElevation the elevation used when the [FloatingActionButton] is focused.
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered.
     */
    @Composable
    fun elevation(
        defaultElevation: Dp = FabPrimaryTokens.ContainerElevation,
        pressedElevation: Dp = FabPrimaryTokens.PressedContainerElevation,
        focusedElevation: Dp = FabPrimaryTokens.FocusContainerElevation,
        hoveredElevation: Dp = FabPrimaryTokens.HoverContainerElevation,
    ): FloatingActionButtonElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
        ) {
            DefaultFloatingActionButtonElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
            )
        }
    }

    /**
     * Use this to create a [FloatingActionButton] with a lowered elevation for less emphasis. Use
     * [elevation] to get a normal [FloatingActionButton].
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] has no other
     * [Interaction]s.
     * @param pressedElevation the elevation used when the [FloatingActionButton] is pressed.
     * @param focusedElevation the elevation used when the [FloatingActionButton] is focused.
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered.
     */
    @Composable
    fun loweredElevation(
        defaultElevation: Dp = FabPrimaryTokens.LoweredContainerElevation,
        pressedElevation: Dp = FabPrimaryTokens.LoweredPressedContainerElevation,
        focusedElevation: Dp = FabPrimaryTokens.LoweredFocusContainerElevation,
        hoveredElevation: Dp = FabPrimaryTokens.LoweredHoverContainerElevation,
    ): FloatingActionButtonElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
        ) {
            DefaultFloatingActionButtonElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
            )
        }
    }
}

/**
 * Default [FloatingActionButtonElevation] implementation.
 */
@Stable
private class DefaultFloatingActionButtonElevation(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
) : FloatingActionButtonElevation {
    @Composable
    override fun shadowElevation(interactionSource: InteractionSource): State<Dp> {
        return animateElevation(interactionSource = interactionSource)
    }

    @Composable
    override fun tonalElevation(interactionSource: InteractionSource): State<Dp> {
        return animateElevation(interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(interactionSource: InteractionSource): State<Dp> {
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
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target = when (interaction) {
            is PressInteraction.Press -> pressedElevation
            is HoverInteraction.Enter -> hoveredElevation
            is FocusInteraction.Focus -> focusedElevation
            else -> defaultElevation
        }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        LaunchedEffect(target) {
            val lastInteraction = when (animatable.targetValue) {
                pressedElevation -> PressInteraction.Press(Offset.Zero)
                hoveredElevation -> HoverInteraction.Enter()
                focusedElevation -> FocusInteraction.Focus()
                else -> null
            }
            animatable.animateElevation(
                from = lastInteraction,
                to = interaction,
                target = target,
            )
        }
        return animatable.asState()
    }
}

private val ExtendedFabIconPadding = 12.dp

private val ExtendedFabTextPadding = 20.dp

private val ExtendedFabMinimumWidth = 80.dp

private val ExtendedFabCollapseAnimation = fadeOut(
    animationSpec = tween(
        durationMillis = MotionTokens.Duration100DurationMs.toInt(),
        easing = MotionTokens.EasingLinearCubicBezier,
    )
) + shrinkHorizontally(
    animationSpec = tween(
        durationMillis = MotionTokens.Duration500DurationMs.toInt(),
        easing = MotionTokens.EasingEmphasizedCubicBezier,
    ),
    shrinkTowards = Alignment.Start,
)

private val ExtendedFabExpandAnimation = fadeIn(
    animationSpec = tween(
        durationMillis = MotionTokens.Duration200DurationMs.toInt(),
        delayMillis = MotionTokens.Duration100DurationMs.toInt(),
        easing = MotionTokens.EasingLinearCubicBezier,
    ),
) + expandHorizontally(
    animationSpec = tween(
        durationMillis = MotionTokens.Duration500DurationMs.toInt(),
        easing = MotionTokens.EasingEmphasizedCubicBezier,
    ),
    expandFrom = Alignment.Start,
)
