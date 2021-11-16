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

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.tokens.ExtendedFabPrimary
import androidx.compose.material3.tokens.FabPrimary
import androidx.compose.material3.tokens.FabPrimaryLarge
import androidx.compose.material3.tokens.FabPrimarySmall
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow

/**
 * ![FAB image](https://developer.android.com/images/reference/androidx/compose/material3/fab.png)
 *
 * A floating action button (FAB) represents the primary action of a screen.
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
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = FabPrimary.PrimaryContainerShape,
    containerColor: Color = MaterialTheme.colorScheme.fromToken(FabPrimary.PrimaryContainerColor),
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit,
) {
    val shadowElevation = elevation.shadowElevation(interactionSource = interactionSource).value
    val tonalElevation = elevation.tonalElevation(interactionSource = interactionSource).value
    val clickAndSemanticsModifier = Modifier.clickable(
        interactionSource = interactionSource,
        indication = rememberRipple(),
        enabled = true,
        onClickLabel = null,
        role = Role.Button,
        onClick = onClick
    )
    Surface(
        modifier = modifier
            .minimumTouchTargetSize()
            .shadow(
                elevation = shadowElevation,
                shape = shape,
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(
                            minWidth = FabPrimary.PrimaryContainerWidth,
                            minHeight = FabPrimary.PrimaryContainerHeight,
                        )
                        .then(clickAndSemanticsModifier),
                    contentAlignment = Alignment.Center
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
    shape: Shape = FabPrimarySmall.PrimarySmallContainerShape,
    containerColor: Color = MaterialTheme.colorScheme.fromToken(FabPrimary.PrimaryContainerColor),
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            minWidth = FabPrimarySmall.PrimarySmallContainerWidth,
            minHeight = FabPrimarySmall.PrimarySmallContainerHeight,
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
    shape: Shape = FabPrimaryLarge.PrimaryLargeContainerShape,
    containerColor: Color = MaterialTheme.colorScheme.fromToken(
        FabPrimaryLarge.PrimaryLargeContainerColor
    ),
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            minWidth = FabPrimaryLarge.PrimaryLargeContainerWidth,
            minHeight = FabPrimaryLarge.PrimaryLargeContainerHeight,
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
 * The extended FAB is wider than a regular FAB, and it includes a text label.
 *
 * @sample androidx.compose.material3.samples.ExtendedFloatingActionButtonSample
 *
 * @param text Text label displayed inside this FAB
 * @param onClick callback invoked when this FAB is clicked
 * @param modifier [Modifier] to be applied to this FAB
 * @param icon Optional icon for this FAB, typically this will be a
 * [Icon].
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = ExtendedFabPrimary.PrimaryContainerShape,
    containerColor: Color = MaterialTheme.colorScheme.fromToken(
        ExtendedFabPrimary.PrimaryContainerColor
    ),
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
) {
    FloatingActionButton(
        modifier = modifier.sizeIn(
            minWidth = 80.dp,
            minHeight = ExtendedFabPrimary.PrimaryContainerHeight,
        ),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        elevation = elevation,
    ) {
        val startPadding = if (icon == null) {
            ExtendedFabTextPadding
        } else {
            ExtendedFabPrimary.PrimaryIconSize / 2
        }
        Row(
            modifier = Modifier.padding(
                start = startPadding,
                end = ExtendedFabTextPadding
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(ExtendedFabIconPadding))
            }
            ProvideTextStyle(
                value = MaterialTheme.typography.fromToken(ExtendedFabPrimary.PrimaryLabelTextFont),
                content = text,
            )
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
    val LargeIconSize = FabPrimaryLarge.PrimaryLargeIconSize

    /**
     * Creates a [FloatingActionButtonElevation] that represents the elevation of a
     * [FloatingActionButton] in different states. For use cases in which a less prominent
     * [FloatingActionButton] is possible consider the [loweredElevation].
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] is not hovered
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered
     */
    @Composable
    fun elevation(
        defaultElevation: Dp = FabPrimary.PrimaryContainerElevation,
        hoveredElevation: Dp = FabPrimary.PrimaryHoverContainerElevation,
    ): FloatingActionButtonElevation {
        return remember(defaultElevation, hoveredElevation) {
            DefaultFloatingActionButtonElevation(
                defaultElevation = defaultElevation,
                hoveredElevation = hoveredElevation,
            )
        }
    }

    /**
     * Use this to create a [FloatingActionButton] with a lowered elevation for less emphasis. Use
     * [elevation] to get a normal [FloatingActionButton].
     *
     * @param defaultElevation the elevation used when the [FloatingActionButton] is not hovered
     * @param hoveredElevation the elevation used when the [FloatingActionButton] is hovered
     */
    @Composable
    fun loweredElevation(
        defaultElevation: Dp = FabPrimary.PrimaryLoweredContainerElevation,
        hoveredElevation: Dp = FabPrimary.PrimaryLoweredHoverContainerElevation,
    ): FloatingActionButtonElevation {
        return remember(defaultElevation, hoveredElevation) {
            DefaultFloatingActionButtonElevation(
                defaultElevation = defaultElevation,
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
        val isHovered by interactionSource.collectIsHoveredAsState()
        val targetElevation = if (isHovered) hoveredElevation else defaultElevation
        val animationSpec = if (isHovered) IncomingSpec else OutgoingSpec
        return animateDpAsState(targetElevation, animationSpec)
    }
}

private val IncomingSpec = TweenSpec<Dp>(
    durationMillis = 120,
    easing = FastOutSlowInEasing
)

private val OutgoingSpec = TweenSpec<Dp>(
    durationMillis = 120,
    easing = CubicBezierEasing(0.40f, 0.00f, 0.60f, 1.00f)
)

private val ExtendedFabIconPadding = 12.dp

private val ExtendedFabTextPadding = 20.dp