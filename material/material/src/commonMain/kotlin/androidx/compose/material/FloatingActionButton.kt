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

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.animatedValue
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.ProvideTextStyle
import androidx.compose.foundation.AmbientIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSizeConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSizeIn
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A floating action button (FAB) is a button that represents the primary action of a screen.
 *
 * This FAB is typically used with an [Icon]:
 *
 * @sample androidx.compose.material.samples.SimpleFab
 *
 * See [ExtendedFloatingActionButton] for an extended FAB that contains text and an optional icon.
 *
 * @param onClick will be called when user clicked on this FAB. The FAB will be disabled
 * when it is null.
 * @param modifier [Modifier] to be applied to this FAB.
 * @param interactionState the [InteractionState] representing the different [Interaction]s
 * present on this FAB. You can create and pass in your own remembered [InteractionState] if
 * you want to read the [InteractionState] and customize the appearance / behavior of this FAB
 * in different [Interaction]s, such as customizing how the [elevation] of this FAB changes when
 * it is [Interaction.Pressed].
 * @param shape The [Shape] of this FAB
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color for content inside this FAB
 * @param elevation The z-coordinate at which to place this FAB. This controls the size
 * of the shadow below the FAB. See [FloatingActionButtonConstants.animateDefaultElevation] for
 * the default elevation that animates between [Interaction]s.
 * @param icon the content of this FAB
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionState: InteractionState = remember { InteractionState() },
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = FloatingActionButtonConstants.animateDefaultElevation(interactionState),
    icon: @Composable () -> Unit
) {
    // TODO(aelias): Avoid manually managing the ripple once http://b/157687898
    // is fixed and we have more flexibility to move the clickable modifier
    // (see candidate approach aosp/1361921)
    Surface(
        modifier = modifier.clickable(
            onClick = onClick,
            interactionState = interactionState,
            indication = null
        ),
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    ) {
        ProvideTextStyle(MaterialTheme.typography.button) {
            Box(
                modifier = Modifier
                    .defaultMinSizeConstraints(minWidth = FabSize, minHeight = FabSize)
                    .indication(interactionState, AmbientIndication.current()),
                alignment = Alignment.Center
            ) { icon() }
        }
    }
}

/**
 * A floating action button (FAB) is a button that represents the primary action of a screen.
 *
 * This extended FAB contains text and an optional icon that will be placed at the start. See
 * [FloatingActionButton] for a FAB that just contains some content, typically an icon.
 *
 * @sample androidx.compose.material.samples.SimpleExtendedFabWithIcon
 *
 * If you want FAB’s container to have a fluid width (to be defined by its relationship to something
 * else on screen, such as screen width or the layout grid) just apply an appropriate modifier.
 * For example to fill the whole available width you can do:
 *
 * @sample androidx.compose.material.samples.FluidExtendedFab
 *
 * @param text Text label displayed inside this FAB
 * @param onClick will be called when user clicked on this FAB. The FAB will be disabled
 * when it is null.
 * @param modifier [Modifier] to be applied to this FAB
 * @param icon Optional icon for this FAB, typically this will be a
 * [Icon].
 * @param interactionState the [InteractionState] representing the different [Interaction]s
 * present on this FAB. You can create and pass in your own remembered [InteractionState] if
 * you want to read the [InteractionState] and customize the appearance / behavior of this FAB
 * in different [Interaction]s, such as customizing how the [elevation] of this FAB changes when
 * it is [Interaction.Pressed].
 * @param shape The [Shape] of this FAB
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color. Will be used by text and iconography
 * @param elevation The z-coordinate at which to place this FAB. This controls the size
 * of the shadow below the button. See [FloatingActionButtonConstants.animateDefaultElevation] for
 * the default elevation that animates between [Interaction]s.
 */
@Composable
fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    interactionState: InteractionState = remember { InteractionState() },
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = FloatingActionButtonConstants.animateDefaultElevation(interactionState)
) {
    FloatingActionButton(
        modifier = modifier.preferredSizeIn(
            minWidth = ExtendedFabSize,
            minHeight = ExtendedFabSize
        ),
        onClick = onClick,
        interactionState = interactionState,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    ) {
        Box(
            modifier = Modifier.padding(
                start = ExtendedFabTextPadding,
                end = ExtendedFabTextPadding
            ),
            alignment = Alignment.Center
        ) {
            if (icon == null) {
                text()
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon()
                    Spacer(Modifier.preferredWidth(ExtendedFabIconPadding))
                    text()
                }
            }
        }
    }
}

/**
 * Contains the default values used by [FloatingActionButton]
 */
object FloatingActionButtonConstants {
    // TODO: b/152525426 add support for focused and hovered states
    /**
     * Represents the default elevation for a button in different [Interaction]s, and how the
     * elevation animates between them.
     *
     * @param interactionState the [InteractionState] for this [FloatingActionButton], representing
     * the current visual state, such as whether it is [Interaction.Pressed] or not.
     * @param defaultElevation the elevation to use when the [FloatingActionButton] is has no
     * [Interaction]s
     * @param pressedElevation the elevation to use when the [FloatingActionButton] is
     * [Interaction.Pressed].
     */
    @Composable
    fun animateDefaultElevation(
        interactionState: InteractionState,
        defaultElevation: Dp = 6.dp,
        pressedElevation: Dp = 12.dp
        // focused: Dp = 8.dp,
        // hovered: Dp = 8.dp,
    ): Dp {
        class InteractionHolder(var interaction: Interaction?)

        val interaction = interactionState.value.lastOrNull {
            it is Interaction.Pressed
        }

        val target = when (interaction) {
            Interaction.Pressed -> pressedElevation
            else -> defaultElevation
        }

        val previousInteractionHolder = remember { InteractionHolder(interaction) }

        val animatedElevation = animatedValue(target, Dp.VectorConverter)

        onCommit(target) {
            animatedElevation.animateElevation(
                from = previousInteractionHolder.interaction,
                to = interaction,
                target = target
            )

            // Update the last interaction, so we know what AnimationSpec to use if we animate
            // away from a state
            previousInteractionHolder.interaction = interaction
        }

        return animatedElevation.value
    }
}

private val FabSize = 56.dp
private val ExtendedFabSize = 48.dp
private val ExtendedFabIconPadding = 12.dp
private val ExtendedFabTextPadding = 20.dp
