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

import androidx.compose.animation.AnimatedValueModel
import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.AmbientIndication
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
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
import androidx.compose.runtime.Providers
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.AmbientAnimationClock
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
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB
 * in different states. This controls the size of the shadow below the FAB.
 * @param content the content of this FAB - this is typically an [Icon].
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionState: InteractionState = remember { InteractionState() },
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit
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
        elevation = elevation.elevation(interactionState)
    ) {
        Providers(AmbientContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(MaterialTheme.typography.button) {
                Box(
                    modifier = Modifier
                        .defaultMinSizeConstraints(minWidth = FabSize, minHeight = FabSize)
                        .indication(interactionState, AmbientIndication.current()),
                    contentAlignment = Alignment.Center
                ) { content() }
            }
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
 * @param elevation [FloatingActionButtonElevation] used to resolve the elevation for this FAB
 * in different states. This controls the size of the shadow below the FAB.
 */
@OptIn(ExperimentalMaterialApi::class)
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
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation()
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
            contentAlignment = Alignment.Center
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
 * Represents the elevation for a floating action button in different states.
 *
 * See [FloatingActionButtonDefaults.elevation] for the default elevation used in a
 * [FloatingActionButton] and [ExtendedFloatingActionButton].
 */
@ExperimentalMaterialApi
@Stable
interface FloatingActionButtonElevation {
    /**
     * Represents the elevation used in a floating action button, depending on [interactionState].
     *
     * @param interactionState the [InteractionState] for this floating action button
     */
    fun elevation(interactionState: InteractionState): Dp
}

/**
 * Contains the default values used by [FloatingActionButton]
 */
@Deprecated(
    "FloatingActionButtonConstants has been replaced with FloatingActionButtonDefaults",
    ReplaceWith(
        "FloatingActionButtonDefaults",
        "androidx.compose.material.FloatingActionButtonDefaults"
    )
)
object FloatingActionButtonConstants {
    // TODO: b/152525426 add support for focused and hovered states
    /**
     * Creates a [FloatingActionButtonElevation] that will animate between the provided values
     * according to the Material specification.
     *
     * @param defaultElevation the elevation to use when the [FloatingActionButton] has no
     * [Interaction]s
     * @param pressedElevation the elevation to use when the [FloatingActionButton] is
     * [Interaction.Pressed].
     */
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    @Deprecated(
        "FloatingActionButtonConstants has been replaced with " +
            "FloatingActionButtonDefaults",
        ReplaceWith(
            "FloatingActionButtonDefaults.elevation(elevation, pressedElevation, " +
                "disabledElevation)",
            "androidx.compose.material.FloatingActionButtonDefaults"
        )
    )
    fun defaultElevation(
        defaultElevation: Dp = 6.dp,
        pressedElevation: Dp = 12.dp
        // focused: Dp = 8.dp,
        // hovered: Dp = 8.dp,
    ): FloatingActionButtonElevation {
        val clock = AmbientAnimationClock.current.asDisposableClock()
        return remember(defaultElevation, pressedElevation, clock) {
            DefaultFloatingActionButtonElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                clock = clock
            )
        }
    }
}

/**
 * Contains the default values used by [FloatingActionButton]
 */
object FloatingActionButtonDefaults {
    // TODO: b/152525426 add support for focused and hovered states
    /**
     * Creates a [FloatingActionButtonElevation] that will animate between the provided values
     * according to the Material specification.
     *
     * @param defaultElevation the elevation to use when the [FloatingActionButton] has no
     * [Interaction]s
     * @param pressedElevation the elevation to use when the [FloatingActionButton] is
     * [Interaction.Pressed].
     */
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun elevation(
        defaultElevation: Dp = 6.dp,
        pressedElevation: Dp = 12.dp
        // focused: Dp = 8.dp,
        // hovered: Dp = 8.dp,
    ): FloatingActionButtonElevation {
        val clock = AmbientAnimationClock.current.asDisposableClock()
        return remember(defaultElevation, pressedElevation, clock) {
            DefaultFloatingActionButtonElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                clock = clock
            )
        }
    }
}

/**
 * Default [FloatingActionButtonElevation] implementation.
 */
@OptIn(ExperimentalMaterialApi::class)
@Stable
private class DefaultFloatingActionButtonElevation(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val clock: AnimationClockObservable
) : FloatingActionButtonElevation {
    private val lazyAnimatedElevation = LazyAnimatedValue<Dp, AnimationVector1D> { target ->
        AnimatedValueModel(initialValue = target, typeConverter = Dp.VectorConverter, clock = clock)
    }

    override fun elevation(interactionState: InteractionState): Dp {
        val interaction = interactionState.value.lastOrNull {
            it is Interaction.Pressed
        }

        val target = when (interaction) {
            Interaction.Pressed -> pressedElevation
            else -> defaultElevation
        }

        val animatedElevation = lazyAnimatedElevation.animatedValueForTarget(target)

        if (animatedElevation.targetValue != target) {
            val lastInteraction = when (animatedElevation.targetValue) {
                pressedElevation -> Interaction.Pressed
                else -> null
            }
            animatedElevation.animateElevation(
                from = lastInteraction,
                to = interaction,
                target = target
            )
        }

        return animatedElevation.value
    }
}

private val FabSize = 56.dp
private val ExtendedFabSize = 48.dp
private val ExtendedFabIconPadding = 12.dp
private val ExtendedFabTextPadding = 20.dp
